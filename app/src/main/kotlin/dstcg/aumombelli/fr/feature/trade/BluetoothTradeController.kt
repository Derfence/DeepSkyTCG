package fr.aumombelli.dstcg.feature.trade

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattServer
import android.bluetooth.BluetoothGattServerCallback
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.ParcelUuid
import fr.aumombelli.dstcg.data.TradeGateway
import fr.aumombelli.dstcg.model.TradeCardRef
import fr.aumombelli.dstcg.model.TradeValidationResult
import fr.aumombelli.dstcg.model.isValid
import fr.aumombelli.dstcg.trade.BluetoothTradeAdvertisementCodec
import fr.aumombelli.dstcg.trade.BluetoothTradeAdvertisementStatus
import fr.aumombelli.dstcg.trade.BluetoothTradeFrameCodec
import fr.aumombelli.dstcg.trade.BluetoothTradeTieBreaker
import fr.aumombelli.dstcg.trade.TradeCodec
import fr.aumombelli.dstcg.trade.TradePacket
import fr.aumombelli.dstcg.trade.TradeTypeAck
import fr.aumombelli.dstcg.trade.TradeTypeCommit
import fr.aumombelli.dstcg.trade.TradeTypeCommitted
import fr.aumombelli.dstcg.trade.TradeTypeConfirm
import fr.aumombelli.dstcg.trade.TradeTypeFailure
import fr.aumombelli.dstcg.trade.TradeTypeHello
import fr.aumombelli.dstcg.trade.TradeTypePrepare
import fr.aumombelli.dstcg.trade.TradeTypePrepared
import fr.aumombelli.dstcg.trade.TradeTypeResume
import fr.aumombelli.dstcg.trade.failureResponse
import fr.aumombelli.dstcg.trade.verificationCodeFor
import java.security.SecureRandom
import java.util.ArrayDeque
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

data class BluetoothTradePartner(
    val id: String,
    val displayName: String,
    val sessionId: String,
    val isCompatible: Boolean?,
)

internal sealed interface BluetoothTradeControllerEvent {
    data object Discovering : BluetoothTradeControllerEvent
    data class PartnerFound(val partner: BluetoothTradePartner) : BluetoothTradeControllerEvent
    data class PartnerLeft(val partnerId: String, val sessionId: String) : BluetoothTradeControllerEvent
    data class Connecting(val partner: BluetoothTradePartner) : BluetoothTradeControllerEvent
    data class RemoteOffer(
        val partnerName: String,
        val remoteCard: TradeCardRef,
        val verificationCode: String,
    ) : BluetoothTradeControllerEvent
    data object RemoteConfirmed : BluetoothTradeControllerEvent
    data object Exchanging : BluetoothTradeControllerEvent
    data class Succeeded(val receivedCard: TradeCardRef) : BluetoothTradeControllerEvent
    data class Failed(val message: String) : BluetoothTradeControllerEvent
}

@SuppressLint("MissingPermission")
internal class BluetoothTradeController(
    context: Context,
    private val tradeGateway: TradeGateway,
    private val localCard: TradeCardRef,
    private val catalogFingerprint: String,
    private val localName: String,
    private val onEvent: (BluetoothTradeControllerEvent) -> Unit,
) {
    private val appContext = context.applicationContext
    private val bluetoothManager = appContext.getSystemService(BluetoothManager::class.java)
    private val bluetoothAdapter = bluetoothManager?.adapter
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mainHandler = Handler(Looper.getMainLooper())
    private val secureRandom = SecureRandom()
    private val sessionId = randomSessionId()
    private val knownDevices = ConcurrentHashMap<String, BluetoothDevice>()
    private val knownPartners = ConcurrentHashMap<String, BluetoothTradePartner>()
    private val serverDecoders = ConcurrentHashMap<String, BluetoothTradeFrameCodec.Decoder>()
    private val clientDecoder = BluetoothTradeFrameCodec.Decoder()
    private val clientWriteQueue = ArrayDeque<ByteArray>()
    private val serverNotifyQueue = ArrayDeque<ByteArray>()

    private var stopped = false
    private var gattServer: BluetoothGattServer? = null
    private var outboundCharacteristic: BluetoothGattCharacteristic? = null
    private var clientGatt: BluetoothGatt? = null
    private var clientWriteCharacteristic: BluetoothGattCharacteristic? = null
    private var serverNotifyDevice: BluetoothDevice? = null
    private var clientWriteInFlight = false
    private var serverNotifyInFlight = false
    private var activePeer: ActivePeer? = null
    private var tradeId: String? = null
    private var localNonce: String? = null
    private var remoteNonce: String? = null
    private var remoteCard: TradeCardRef? = null
    private var remoteSessionId: String? = null
    private var remoteName: String? = null
    private var helloResponseSent = false
    private var localConfirmed = false
    private var remoteConfirmed = false
    private var prepareSent = false
    private var preparedReceived = false
    private var commitSent = false
    private var succeeded = false
    private var leavingAdvertisementStarted = false
    private var successPendingUntilOutboundDrained: TradeCardRef? = null
    private var finalAckBestEffort = false

    fun start() {
        val adapter = bluetoothAdapter
        when {
            adapter == null -> {
                emit(BluetoothTradeControllerEvent.Failed("Ce téléphone ne prend pas en charge le Bluetooth."))
                return
            }
            !adapter.isEnabled -> {
                emit(BluetoothTradeControllerEvent.Failed("Le Bluetooth est désactivé."))
                return
            }
            adapter.bluetoothLeAdvertiser == null -> {
                emit(BluetoothTradeControllerEvent.Failed("Ce téléphone ne peut pas annoncer un échange Bluetooth."))
                return
            }
        }

        if (!openGattServer()) return
        startAdvertising()
        startScanning()
        emit(BluetoothTradeControllerEvent.Discovering)
    }

    fun stop() {
        if (stopped) return
        stopped = true
        publishLeavingPresence()
        runCatching { clientGatt?.disconnect() }
        runCatching { clientGatt?.close() }
        runCatching { gattServer?.close() }
        scope.cancel()
    }

    fun connectTo(partnerId: String) {
        val partner = knownPartners[partnerId]
        val device = knownDevices[partnerId]
        if (partner == null || device == null) {
            emit(BluetoothTradeControllerEvent.Failed("Partenaire Bluetooth introuvable."))
            return
        }
        if (partner.isCompatible == false) {
            emit(BluetoothTradeControllerEvent.Failed("Les catalogues ne semblent pas compatibles."))
            return
        }
        activePeer = ActivePeer(
            id = partner.id,
            displayName = partner.displayName,
            sessionId = partner.sessionId,
            device = device,
            role = PeerRole.Client,
        )
        emit(BluetoothTradeControllerEvent.Connecting(partner))
        runCatching { bluetoothAdapter?.bluetoothLeScanner?.stopScan(scanCallback) }
        clientGatt = device.connectGatt(appContext, false, clientGattCallback, BluetoothDevice.TRANSPORT_LE)
    }

    fun confirmExchange() {
        val currentTradeId = tradeId
        val currentRemoteCard = remoteCard
        if (currentTradeId == null || currentRemoteCard == null) {
            emit(BluetoothTradeControllerEvent.Failed("Aucune proposition distante à confirmer."))
            return
        }
        localConfirmed = true
        sendPacket(
            TradePacket(
                type = TradeTypeConfirm,
                tradeId = currentTradeId,
                catalogFingerprint = catalogFingerprint,
                nonce = localNonce ?: randomNonce().also { localNonce = it },
                sessionId = sessionId,
            ),
        )
        maybePrepareTrade()
    }

    private fun openGattServer(): Boolean {
        val server = bluetoothManager?.openGattServer(appContext, serverCallback)
        if (server == null) {
            emit(BluetoothTradeControllerEvent.Failed("Impossible de préparer l'échange Bluetooth."))
            return false
        }
        val service = BluetoothGattService(BluetoothTradeUuids.Service, BluetoothGattService.SERVICE_TYPE_PRIMARY)
        val inbound = BluetoothGattCharacteristic(
            BluetoothTradeUuids.InboundCharacteristic,
            BluetoothGattCharacteristic.PROPERTY_WRITE or BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE,
            BluetoothGattCharacteristic.PERMISSION_WRITE,
        )
        val outbound = BluetoothGattCharacteristic(
            BluetoothTradeUuids.OutboundCharacteristic,
            BluetoothGattCharacteristic.PROPERTY_NOTIFY,
            BluetoothGattCharacteristic.PERMISSION_READ,
        )
        outbound.addDescriptor(
            BluetoothGattDescriptor(
                BluetoothTradeUuids.ClientConfigurationDescriptor,
                BluetoothGattDescriptor.PERMISSION_READ or BluetoothGattDescriptor.PERMISSION_WRITE,
            ),
        )
        service.addCharacteristic(inbound)
        service.addCharacteristic(outbound)
        gattServer = server
        outboundCharacteristic = outbound
        val added = server.addService(service)
        if (!added) {
            emit(BluetoothTradeControllerEvent.Failed("Service d'échange Bluetooth indisponible."))
        }
        return added
    }

    private fun startAdvertising() {
        startAdvertising(
            status = BluetoothTradeAdvertisementStatus.Active,
            connectable = true,
            callback = advertiseCallback,
        )
    }

    private fun publishLeavingPresence() {
        val advertiser = bluetoothAdapter?.bluetoothLeAdvertiser ?: return
        if (leavingAdvertisementStarted) return
        leavingAdvertisementStarted = true
        runCatching { bluetoothAdapter?.bluetoothLeScanner?.stopScan(scanCallback) }
        runCatching { advertiser.stopAdvertising(advertiseCallback) }
        startAdvertising(
            status = BluetoothTradeAdvertisementStatus.Leaving,
            connectable = false,
            callback = leavingAdvertiseCallback,
        )
        mainHandler.postDelayed(
            { runCatching { advertiser.stopAdvertising(leavingAdvertiseCallback) } },
            BluetoothTradeLeavingAdvertisementDurationMillis,
        )
    }

    private fun startAdvertising(
        status: BluetoothTradeAdvertisementStatus,
        connectable: Boolean,
        callback: AdvertiseCallback,
    ) {
        val adapter = bluetoothAdapter ?: return
        val serviceUuid = ParcelUuid(BluetoothTradeUuids.Service)
        val advertiseData = AdvertiseData.Builder()
            .addServiceUuid(serviceUuid)
            .setIncludeDeviceName(false)
            .setIncludeTxPowerLevel(false)
            .build()
        val scanResponse = AdvertiseData.Builder()
            .addManufacturerData(
                BluetoothTradeUuids.ManufacturerId,
                BluetoothTradeAdvertisementCodec.encode(
                    sessionId = sessionId,
                    catalogFingerprint = catalogFingerprint,
                    deviceName = localName,
                    status = status,
                ),
            )
            .build()
        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
            .setConnectable(connectable)
            .build()
        adapter.bluetoothLeAdvertiser.startAdvertising(settings, advertiseData, scanResponse, callback)
    }

    private fun startScanning() {
        val adapter = bluetoothAdapter ?: return
        val filter = ScanFilter.Builder()
            .setServiceUuid(ParcelUuid(BluetoothTradeUuids.Service))
            .build()
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()
        adapter.bluetoothLeScanner?.startScan(listOf(filter), settings, scanCallback)
    }

    private val advertiseCallback = object : AdvertiseCallback() {
        override fun onStartFailure(errorCode: Int) {
            emit(BluetoothTradeControllerEvent.Failed("Annonce Bluetooth impossible ($errorCode)."))
        }
    }

    private val leavingAdvertiseCallback = object : AdvertiseCallback() {
        override fun onStartFailure(errorCode: Int) = Unit
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            handleScanResult(result)
        }

        override fun onBatchScanResults(results: MutableList<ScanResult>) {
            results.forEach(::handleScanResult)
        }

        override fun onScanFailed(errorCode: Int) {
            emit(BluetoothTradeControllerEvent.Failed("Découverte Bluetooth impossible ($errorCode)."))
        }
    }

    private fun handleScanResult(result: ScanResult) {
        val payload = result.scanRecord
            ?.getManufacturerSpecificData(BluetoothTradeUuids.ManufacturerId)
            ?: return
        val advertisement = BluetoothTradeAdvertisementCodec.decode(payload) ?: return
        if (advertisement.sessionId == sessionId) return
        val device = result.device ?: return
        val id = device.address ?: advertisement.sessionId
        if (advertisement.status == BluetoothTradeAdvertisementStatus.Leaving) {
            removeKnownPartner(
                id = id,
                sessionId = advertisement.sessionId,
            )
            return
        }
        val partner = BluetoothTradePartner(
            id = id,
            displayName = advertisement.deviceName,
            sessionId = advertisement.sessionId,
            isCompatible = catalogFingerprint.startsWith(advertisement.catalogFingerprintPrefix),
        )
        knownDevices[id] = device
        knownPartners[id] = partner
        emit(BluetoothTradeControllerEvent.PartnerFound(partner))
    }

    private fun removeKnownPartner(
        id: String,
        sessionId: String,
    ) {
        val matchingIds = knownPartners
            .filter { (partnerId, partner) -> partnerId == id || partner.sessionId == sessionId }
            .keys
        val idsToRemove = if (matchingIds.isEmpty()) setOf(id) else matchingIds
        idsToRemove.forEach { partnerId ->
            knownDevices.remove(partnerId)
            knownPartners.remove(partnerId)
            emit(
                BluetoothTradeControllerEvent.PartnerLeft(
                    partnerId = partnerId,
                    sessionId = sessionId,
                ),
            )
        }
    }

    private val clientGattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (stopped) return
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> gatt.discoverServices()
                BluetoothProfile.STATE_DISCONNECTED -> {
                    if (finalAckBestEffort && completeSuccessAfterBestEffortAckFailure()) {
                        runCatching { gatt.close() }
                        return
                    }
                    if (activePeer?.role == PeerRole.Client && activePeer?.device == gatt.device && !succeeded) {
                        emit(BluetoothTradeControllerEvent.Failed("Connexion Bluetooth interrompue."))
                    }
                    runCatching { gatt.close() }
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                emit(BluetoothTradeControllerEvent.Failed("Service Bluetooth distant indisponible."))
                return
            }
            val service = gatt.getService(BluetoothTradeUuids.Service)
            val inbound = service?.getCharacteristic(BluetoothTradeUuids.InboundCharacteristic)
            val outbound = service?.getCharacteristic(BluetoothTradeUuids.OutboundCharacteristic)
            if (inbound == null || outbound == null) {
                emit(BluetoothTradeControllerEvent.Failed("Partenaire Bluetooth incompatible."))
                return
            }
            clientWriteCharacteristic = inbound
            gatt.setCharacteristicNotification(outbound, true)
            val descriptor = outbound.getDescriptor(BluetoothTradeUuids.ClientConfigurationDescriptor)
            if (descriptor == null) {
                sendHello()
            } else {
                descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                if (!gatt.writeDescriptor(descriptor)) {
                    emit(BluetoothTradeControllerEvent.Failed("Notifications Bluetooth indisponibles."))
                }
            }
        }

        override fun onDescriptorWrite(gatt: BluetoothGatt, descriptor: BluetoothGattDescriptor, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                sendHello()
            } else {
                emit(BluetoothTradeControllerEvent.Failed("Notifications Bluetooth refusées."))
            }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
        ) {
            handleClientCharacteristicChanged(characteristic.value ?: return)
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray,
        ) {
            handleClientCharacteristicChanged(value)
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int,
        ) {
            clientWriteInFlight = false
            if (status == BluetoothGatt.GATT_SUCCESS) {
                drainClientWriteQueue()
            } else {
                if (!completeSuccessAfterBestEffortAckFailure()) {
                    emit(BluetoothTradeControllerEvent.Failed("Envoi Bluetooth impossible."))
                }
            }
        }
    }

    private val serverCallback = object : BluetoothGattServerCallback() {
        override fun onConnectionStateChange(device: BluetoothDevice, status: Int, newState: Int) {
            if (stopped) return
            if (
                newState == BluetoothProfile.STATE_DISCONNECTED &&
                activePeer?.role == PeerRole.Server &&
                activePeer?.device == device &&
                !succeeded
            ) {
                if (finalAckBestEffort && completeSuccessAfterBestEffortAckFailure()) return
                emit(BluetoothTradeControllerEvent.Failed("Connexion Bluetooth interrompue."))
            }
        }

        override fun onCharacteristicWriteRequest(
            device: BluetoothDevice,
            requestId: Int,
            characteristic: BluetoothGattCharacteristic,
            preparedWrite: Boolean,
            responseNeeded: Boolean,
            offset: Int,
            value: ByteArray,
        ) {
            if (characteristic.uuid == BluetoothTradeUuids.InboundCharacteristic) {
                handleServerWrite(device, value)
            }
            if (responseNeeded) {
                gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, null)
            }
        }

        override fun onDescriptorWriteRequest(
            device: BluetoothDevice,
            requestId: Int,
            descriptor: BluetoothGattDescriptor,
            preparedWrite: Boolean,
            responseNeeded: Boolean,
            offset: Int,
            value: ByteArray,
        ) {
            if (descriptor.uuid == BluetoothTradeUuids.ClientConfigurationDescriptor) {
                serverNotifyDevice = device
            }
            if (responseNeeded) {
                gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, value)
            }
        }

        override fun onNotificationSent(device: BluetoothDevice, status: Int) {
            serverNotifyInFlight = false
            if (status == BluetoothGatt.GATT_SUCCESS) {
                drainServerNotifyQueue()
            } else {
                if (!completeSuccessAfterBestEffortAckFailure()) {
                    emit(BluetoothTradeControllerEvent.Failed("Notification Bluetooth impossible."))
                }
            }
        }
    }

    private fun sendHello() {
        val nextTradeId = tradeId ?: UUID.randomUUID().toString().also { tradeId = it }
        val nextNonce = localNonce ?: randomNonce().also { localNonce = it }
        sendPacket(
            TradePacket(
                type = TradeTypeHello,
                tradeId = nextTradeId,
                catalogFingerprint = catalogFingerprint,
                nonce = nextNonce,
                sessionId = sessionId,
                deviceName = localName,
                card = localCard,
            ),
        )
    }

    private fun handleClientCharacteristicChanged(value: ByteArray) {
        runCatching { clientDecoder.accept(value) }
            .onSuccess { payload -> payload?.let(::handlePayload) }
            .onFailure { emit(BluetoothTradeControllerEvent.Failed("Message Bluetooth invalide.")) }
    }

    private fun handleServerWrite(device: BluetoothDevice, value: ByteArray) {
        val decoder = serverDecoders.getOrPut(device.address ?: device.toString()) {
            BluetoothTradeFrameCodec.Decoder()
        }
        runCatching { decoder.accept(value) }
            .onSuccess { payload -> payload?.let { handlePayload(it, device) } }
            .onFailure { emit(BluetoothTradeControllerEvent.Failed("Message Bluetooth invalide.")) }
    }

    private fun handlePayload(payload: ByteArray, serverDevice: BluetoothDevice? = null) {
        val packet = runCatching { TradeCodec.decode(payload) }.getOrElse {
            emit(BluetoothTradeControllerEvent.Failed("Message d'échange invalide."))
            return
        }
        if (packet.type == TradeTypeFailure || !packet.ok) {
            emit(BluetoothTradeControllerEvent.Failed(packet.reason ?: "Échange refusé."))
            return
        }
        when (packet.type) {
            TradeTypeHello -> handleHello(packet, serverDevice)
            TradeTypeConfirm -> handleConfirm(packet)
            TradeTypePrepare -> handlePrepare(packet)
            TradeTypeResume -> handlePrepare(packet)
            TradeTypePrepared -> handlePrepared(packet)
            TradeTypeCommit -> handleCommit(packet)
            TradeTypeCommitted -> handleCommitted(packet)
            TradeTypeAck -> finishSuccess()
            else -> sendFailure("Message d'échange inattendu.")
        }
    }

    private fun handleHello(packet: TradePacket, serverDevice: BluetoothDevice?) {
        val card = packet.card
        if (packet.catalogFingerprint != catalogFingerprint) {
            sendFailure("Catalogues incompatibles.")
            return
        }
        if (card == null) {
            sendFailure("Carte distante absente.")
            return
        }
        if (serverDevice != null && activePeer?.role == PeerRole.Client) {
            if (BluetoothTradeTieBreaker.shouldKeepOutgoingConnection(sessionId, packet.sessionId)) {
                runCatching { gattServer?.cancelConnection(serverDevice) }
                return
            }
            closeOutgoingClientConnection()
            tradeId = null
            activePeer = null
        }
        if (activePeer == null && serverDevice != null) {
            activePeer = ActivePeer(
                id = serverDevice.address ?: packet.sessionId,
                displayName = packet.deviceName ?: "Partenaire",
                sessionId = packet.sessionId,
                device = serverDevice,
                role = PeerRole.Server,
            )
            serverNotifyDevice = serverDevice
            emit(
                BluetoothTradeControllerEvent.Connecting(
                    BluetoothTradePartner(
                        id = activePeer?.id.orEmpty(),
                        displayName = packet.deviceName ?: "Partenaire",
                        sessionId = packet.sessionId,
                        isCompatible = true,
                    ),
                ),
            )
        }
        val peer = activePeer
        if (
            peer?.role == PeerRole.Server &&
            serverDevice != null &&
            clientGatt != null &&
            !BluetoothTradeTieBreaker.shouldKeepOutgoingConnection(sessionId, packet.sessionId)
        ) {
            sendFailure("Connexion croisée, reprise côté partenaire.")
            return
        }

        tradeId = tradeId ?: packet.tradeId
        if (tradeId != packet.tradeId) {
            sendFailure("Échange inconnu.")
            return
        }
        remoteNonce = packet.nonce
        remoteCard = card
        remoteSessionId = packet.sessionId
        remoteName = packet.deviceName ?: peer?.displayName ?: "Partenaire"
        scope.launch {
            val validation = tradeGateway.validateTrade(
                localOutgoing = localCard,
                remoteOutgoing = card,
            )
            if (!validation.isValid()) {
                val message = (validation as TradeValidationResult.Invalid).message
                sendFailure(message)
                emit(BluetoothTradeControllerEvent.Failed(message))
                return@launch
            }
            if (peer?.role == PeerRole.Server && !helloResponseSent) {
                helloResponseSent = true
                sendHello()
            }
            val code = verificationCodeFor(
                tradeId = checkNotNull(tradeId),
                firstSessionId = sessionId,
                secondSessionId = packet.sessionId,
            )
            emit(
                BluetoothTradeControllerEvent.RemoteOffer(
                    partnerName = remoteName ?: "Partenaire",
                    remoteCard = card,
                    verificationCode = code,
                ),
            )
        }
    }

    private fun handleConfirm(packet: TradePacket) {
        if (!isCurrentTrade(packet)) return
        remoteConfirmed = true
        emit(BluetoothTradeControllerEvent.RemoteConfirmed)
        maybePrepareTrade()
    }

    private fun maybePrepareTrade() {
        val currentTradeId = tradeId ?: return
        val currentRemoteCard = remoteCard ?: return
        if (!localConfirmed || !remoteConfirmed || prepareSent) return
        prepareSent = true
        emit(BluetoothTradeControllerEvent.Exchanging)
        scope.launch {
            val result = tradeGateway.prepareTrade(
                tradeId = currentTradeId,
                outgoing = localCard,
                incoming = currentRemoteCard,
            )
            if (!result.isValid()) {
                val message = (result as TradeValidationResult.Invalid).message
                sendFailure(message)
                emit(BluetoothTradeControllerEvent.Failed(message))
                return@launch
            }
            sendPacket(
                TradePacket(
                    type = TradeTypePrepare,
                    tradeId = currentTradeId,
                    catalogFingerprint = catalogFingerprint,
                    nonce = localNonce ?: randomNonce().also { localNonce = it },
                    sessionId = sessionId,
                ),
            )
            maybeCommitTrade()
        }
    }

    private fun handlePrepare(packet: TradePacket) {
        if (!isCurrentTrade(packet)) return
        val currentTradeId = tradeId ?: return
        val currentRemoteCard = remoteCard ?: return
        emit(BluetoothTradeControllerEvent.Exchanging)
        scope.launch {
            val result = tradeGateway.prepareTrade(
                tradeId = currentTradeId,
                outgoing = localCard,
                incoming = currentRemoteCard,
            )
            if (!result.isValid()) {
                val message = (result as TradeValidationResult.Invalid).message
                sendFailure(message)
                emit(BluetoothTradeControllerEvent.Failed(message))
                return@launch
            }
            sendPacket(
                TradePacket(
                    type = TradeTypePrepared,
                    tradeId = currentTradeId,
                    catalogFingerprint = catalogFingerprint,
                    nonce = localNonce ?: randomNonce().also { localNonce = it },
                    sessionId = sessionId,
                ),
            )
        }
    }

    private fun handlePrepared(packet: TradePacket) {
        if (!isCurrentTrade(packet)) return
        preparedReceived = true
        maybeCommitTrade()
    }

    private fun maybeCommitTrade() {
        val currentTradeId = tradeId ?: return
        val currentRemoteCard = remoteCard ?: return
        if (!prepareSent || !preparedReceived || commitSent) return
        commitSent = true
        scope.launch {
            val result = tradeGateway.applyTrade(
                tradeId = currentTradeId,
                outgoing = localCard,
                incoming = currentRemoteCard,
            )
            if (!result.isValid()) {
                val message = (result as TradeValidationResult.Invalid).message
                sendFailure(message)
                emit(BluetoothTradeControllerEvent.Failed(message))
                return@launch
            }
            sendPacket(
                TradePacket(
                    type = TradeTypeCommit,
                    tradeId = currentTradeId,
                    catalogFingerprint = catalogFingerprint,
                    nonce = localNonce ?: randomNonce().also { localNonce = it },
                    sessionId = sessionId,
                ),
            )
        }
    }

    private fun handleCommit(packet: TradePacket) {
        if (!isCurrentTrade(packet)) return
        val currentTradeId = tradeId ?: return
        val currentRemoteCard = remoteCard ?: return
        scope.launch {
            val result = tradeGateway.applyTrade(
                tradeId = currentTradeId,
                outgoing = localCard,
                incoming = currentRemoteCard,
            )
            if (!result.isValid()) {
                val message = (result as TradeValidationResult.Invalid).message
                sendFailure(message)
                emit(BluetoothTradeControllerEvent.Failed(message))
                return@launch
            }
            sendPacket(
                TradePacket(
                    type = TradeTypeCommitted,
                    tradeId = currentTradeId,
                    catalogFingerprint = catalogFingerprint,
                    nonce = localNonce ?: randomNonce().also { localNonce = it },
                    sessionId = sessionId,
                ),
            )
            finishSuccess()
        }
    }

    private fun handleCommitted(packet: TradePacket) {
        if (!isCurrentTrade(packet)) return
        if (!prepareSuccessCompletion()) return
        finalAckBestEffort = true
        sendPacket(
            TradePacket(
                type = TradeTypeAck,
                tradeId = checkNotNull(tradeId),
                catalogFingerprint = catalogFingerprint,
                nonce = localNonce ?: randomNonce().also { localNonce = it },
                sessionId = sessionId,
            ),
        )
        maybeCompleteSuccessAfterOutboundDrained()
    }

    private fun finishSuccess() {
        if (!prepareSuccessCompletion()) return
        maybeCompleteSuccessAfterOutboundDrained()
    }

    private fun prepareSuccessCompletion(): Boolean {
        if (succeeded) return false
        successPendingUntilOutboundDrained = remoteCard ?: return false
        return true
    }

    private fun maybeCompleteSuccessAfterOutboundDrained() {
        val card = successPendingUntilOutboundDrained ?: return
        if (
            clientWriteInFlight ||
            serverNotifyInFlight ||
            clientWriteQueue.isNotEmpty() ||
            serverNotifyQueue.isNotEmpty()
        ) {
            return
        }
        successPendingUntilOutboundDrained = null
        completeSuccess(card)
    }

    private fun completeSuccess(card: TradeCardRef) {
        if (succeeded) return
        finalAckBestEffort = false
        succeeded = true
        publishLeavingPresence()
        emit(BluetoothTradeControllerEvent.Succeeded(card))
    }

    private fun completeSuccessAfterBestEffortAckFailure(): Boolean {
        if (!finalAckBestEffort) return false
        val card = successPendingUntilOutboundDrained ?: return false
        finalAckBestEffort = false
        clientWriteQueue.clear()
        serverNotifyQueue.clear()
        clientWriteInFlight = false
        serverNotifyInFlight = false
        successPendingUntilOutboundDrained = null
        completeSuccess(card)
        return true
    }

    private fun closeOutgoingClientConnection() {
        runCatching { clientGatt?.disconnect() }
        runCatching { clientGatt?.close() }
        clientGatt = null
        clientWriteCharacteristic = null
        clientWriteQueue.clear()
        clientWriteInFlight = false
    }

    private fun isCurrentTrade(packet: TradePacket): Boolean {
        val currentTradeId = tradeId
        if (packet.catalogFingerprint != catalogFingerprint || currentTradeId == null || packet.tradeId != currentTradeId) {
            sendFailure("Échange inconnu.")
            return false
        }
        return true
    }

    private fun sendFailure(message: String) {
        val currentTradeId = tradeId ?: UUID.randomUUID().toString()
        sendPacket(
            TradePacket(
                type = TradeTypeFailure,
                tradeId = currentTradeId,
                catalogFingerprint = catalogFingerprint,
                nonce = localNonce ?: randomNonce().also { localNonce = it },
                sessionId = sessionId,
                ok = false,
                reason = message,
            ).failureResponse(message),
        )
    }

    private fun sendPacket(packet: TradePacket) {
        val chunks = BluetoothTradeFrameCodec.encode(TradeCodec.encode(packet))
        when (activePeer?.role) {
            PeerRole.Client -> enqueueClientWrites(chunks)
            PeerRole.Server -> enqueueServerNotifications(chunks)
            null -> Unit
        }
    }

    private fun enqueueClientWrites(chunks: List<ByteArray>) {
        clientWriteQueue.addAll(chunks)
        drainClientWriteQueue()
    }

    private fun drainClientWriteQueue() {
        if (clientWriteInFlight) return
        val gatt = clientGatt
        val characteristic = clientWriteCharacteristic
        if (gatt == null || characteristic == null) {
            completeSuccessAfterBestEffortAckFailure()
            return
        }
        val chunk = clientWriteQueue.poll()
        if (chunk == null) {
            maybeCompleteSuccessAfterOutboundDrained()
            return
        }
        characteristic.value = chunk
        characteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
        clientWriteInFlight = true
        if (!gatt.writeCharacteristic(characteristic)) {
            clientWriteInFlight = false
            if (!completeSuccessAfterBestEffortAckFailure()) {
                emit(BluetoothTradeControllerEvent.Failed("Envoi Bluetooth impossible."))
            }
        }
    }

    private fun enqueueServerNotifications(chunks: List<ByteArray>) {
        serverNotifyQueue.addAll(chunks)
        drainServerNotifyQueue()
    }

    private fun drainServerNotifyQueue() {
        if (serverNotifyInFlight) return
        val server = gattServer
        val device = serverNotifyDevice ?: activePeer?.device
        val characteristic = outboundCharacteristic
        if (server == null || device == null || characteristic == null) {
            completeSuccessAfterBestEffortAckFailure()
            return
        }
        val chunk = serverNotifyQueue.poll()
        if (chunk == null) {
            maybeCompleteSuccessAfterOutboundDrained()
            return
        }
        characteristic.value = chunk
        serverNotifyInFlight = true
        if (!server.notifyCharacteristicChanged(device, characteristic, false)) {
            serverNotifyInFlight = false
            if (!completeSuccessAfterBestEffortAckFailure()) {
                emit(BluetoothTradeControllerEvent.Failed("Notification Bluetooth impossible."))
            }
        }
    }

    private fun emit(event: BluetoothTradeControllerEvent) {
        mainHandler.post {
            if (!stopped) onEvent(event)
        }
    }

    private fun randomNonce(): String {
        val bytes = ByteArray(8)
        secureRandom.nextBytes(bytes)
        return bytes.joinToString(separator = "") { byte -> "%02x".format(byte) }
    }

    private fun randomSessionId(): String {
        val bytes = ByteArray(4)
        secureRandom.nextBytes(bytes)
        return bytes.joinToString(separator = "") { byte -> "%02x".format(byte) }
    }

    private data class ActivePeer(
        val id: String,
        val displayName: String,
        val sessionId: String,
        val device: BluetoothDevice,
        val role: PeerRole,
    )

    private enum class PeerRole {
        Client,
        Server,
    }
}

internal const val BluetoothTradeLeavingAdvertisementDurationMillis = 1_600L

private object BluetoothTradeUuids {
    const val ManufacturerId = 0xFFFF
    val Service = UUID.fromString("f82b4f55-7427-45fd-8a7d-4cfef6cc0a01")
    val InboundCharacteristic = UUID.fromString("f82b4f55-7427-45fd-8a7d-4cfef6cc0a02")
    val OutboundCharacteristic = UUID.fromString("f82b4f55-7427-45fd-8a7d-4cfef6cc0a03")
    val ClientConfigurationDescriptor = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
}
