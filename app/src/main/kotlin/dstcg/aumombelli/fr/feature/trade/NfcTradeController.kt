package fr.aumombelli.dstcg.feature.trade

import android.app.Activity
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.os.Handler
import android.os.Looper
import fr.aumombelli.dstcg.data.TradeGateway
import fr.aumombelli.dstcg.model.TradeCardRef
import fr.aumombelli.dstcg.model.TradeValidationResult
import fr.aumombelli.dstcg.model.isValid
import fr.aumombelli.dstcg.trade.NfcTradeApdu
import fr.aumombelli.dstcg.trade.NfcTradeCodec
import fr.aumombelli.dstcg.trade.NfcTradePacket
import fr.aumombelli.dstcg.trade.NfcTradeProtocolVersion
import fr.aumombelli.dstcg.trade.NfcTradeSessionHandler
import fr.aumombelli.dstcg.trade.NfcTradeSessionRegistry
import fr.aumombelli.dstcg.trade.NfcTradeTypeAck
import fr.aumombelli.dstcg.trade.NfcTradeTypeCommit
import fr.aumombelli.dstcg.trade.NfcTradeTypeCommitted
import fr.aumombelli.dstcg.trade.NfcTradeTypeFailure
import fr.aumombelli.dstcg.trade.NfcTradeTypeHello
import fr.aumombelli.dstcg.trade.NfcTradeTypeMatch
import java.security.SecureRandom
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

internal sealed interface NfcTradeControllerEvent {
    data object Waiting : NfcTradeControllerEvent
    data object Exchanging : NfcTradeControllerEvent
    data object Succeeded : NfcTradeControllerEvent
    data class Failed(val message: String) : NfcTradeControllerEvent
}

internal class NfcTradeController(
    private val activity: Activity,
    private val tradeGateway: TradeGateway,
    private val localCard: TradeCardRef,
    private val catalogFingerprint: String,
    private val onEvent: (NfcTradeControllerEvent) -> Unit,
) : NfcTradeSessionHandler {
    private val nfcAdapter: NfcAdapter? = NfcAdapter.getDefaultAdapter(activity)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mainHandler = Handler(Looper.getMainLooper())
    private val secureRandom = SecureRandom()
    private val readerBusy = AtomicBoolean(false)
    private var stopped = false
    private var activeCardSideTrade: ActiveRemoteTrade? = null

    private val readerCallback = NfcAdapter.ReaderCallback { tag ->
        if (!readerBusy.compareAndSet(false, true)) return@ReaderCallback
        scope.launch {
            runReaderExchange(tag)
            readerBusy.set(false)
        }
    }

    fun start() {
        val adapter = nfcAdapter
        when {
            adapter == null -> {
                onEvent(NfcTradeControllerEvent.Failed("Ce telephone ne prend pas en charge le NFC."))
                return
            }
            !adapter.isEnabled -> {
                onEvent(NfcTradeControllerEvent.Failed("Le NFC est desactive."))
                return
            }
        }

        NfcTradeSessionRegistry.install(this)
        onEvent(NfcTradeControllerEvent.Waiting)
        mainHandler.postDelayed(::enableReaderWindow, 160L + secureRandom.nextInt(460))
    }

    fun stop() {
        stopped = true
        disableReader()
        mainHandler.removeCallbacksAndMessages(null)
        NfcTradeSessionRegistry.clear(this)
        scope.cancel()
    }

    override suspend fun handleIncoming(packet: NfcTradePacket): NfcTradePacket {
        if (packet.protocolVersion != NfcTradeProtocolVersion) {
            return packet.failure("Version d'echange incompatible.")
        }
        if (packet.catalogFingerprint != catalogFingerprint) {
            return packet.failure("Catalogues incompatibles.")
        }

        return when (packet.type) {
            NfcTradeTypeHello -> handleHello(packet)
            NfcTradeTypeCommit -> handleCommit(packet)
            NfcTradeTypeAck -> handleAck(packet)
            else -> packet.failure("Message NFC inattendu.")
        }
    }

    override fun onNfcDeactivated(reason: Int) {
        if (activeCardSideTrade != null) {
            emit(NfcTradeControllerEvent.Exchanging)
        }
    }

    private fun enableReaderWindow() {
        if (stopped) return
        if (activeCardSideTrade != null) {
            disableReader()
            mainHandler.postDelayed(::enableReaderWindow, CardWindowMillis)
            return
        }
        nfcAdapter?.enableReaderMode(
            activity,
            readerCallback,
            NfcAdapter.FLAG_READER_NFC_A or
                NfcAdapter.FLAG_READER_NFC_B or
                NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK,
            null,
        )
        mainHandler.postDelayed(::disableReaderWindow, ReaderWindowMillis)
    }

    private fun disableReaderWindow() {
        if (stopped) return
        disableReader()
        mainHandler.postDelayed(::enableReaderWindow, CardWindowMillis + secureRandom.nextInt(220))
    }

    private fun disableReader() {
        runCatching { nfcAdapter?.disableReaderMode(activity) }
    }

    private suspend fun runReaderExchange(tag: Tag) {
        emit(NfcTradeControllerEvent.Exchanging)
        val isoDep = IsoDep.get(tag)
        if (isoDep == null) {
            emit(NfcTradeControllerEvent.Failed("Telephone NFC incompatible."))
            return
        }

        val tradeId = UUID.randomUUID().toString()
        val nonce = randomNonce()
        runCatching {
            isoDep.useConnected {
                transceive(NfcTradeApdu.selectAidCommand()).requireSuccessPayload()
                val hello = NfcTradePacket(
                    type = NfcTradeTypeHello,
                    tradeId = tradeId,
                    catalogFingerprint = catalogFingerprint,
                    nonce = nonce,
                    card = localCard,
                )
                val match = transceivePacket(hello)
                if (match.type == NfcTradeTypeFailure || !match.ok) {
                    error(match.reason ?: "Echange refuse.")
                }
                val remoteCard = checkNotNull(match.card) { "Carte distante absente." }
                val validation = tradeGateway.validateTrade(
                    localOutgoing = localCard,
                    remoteOutgoing = remoteCard,
                )
                if (!validation.isValid()) {
                    error((validation as TradeValidationResult.Invalid).message)
                }

                val commitResponse = transceivePacket(
                    NfcTradePacket(
                        type = NfcTradeTypeCommit,
                        tradeId = tradeId,
                        catalogFingerprint = catalogFingerprint,
                        nonce = nonce,
                    ),
                )
                if (commitResponse.type == NfcTradeTypeFailure || !commitResponse.ok) {
                    error(commitResponse.reason ?: "Validation distante impossible.")
                }

                val localApplyResult = tradeGateway.applyTrade(
                    tradeId = tradeId,
                    outgoing = localCard,
                    incoming = remoteCard,
                )
                if (!localApplyResult.isValid()) {
                    error((localApplyResult as TradeValidationResult.Invalid).message)
                }

                transceivePacket(
                    NfcTradePacket(
                        type = NfcTradeTypeAck,
                        tradeId = tradeId,
                        catalogFingerprint = catalogFingerprint,
                        nonce = nonce,
                    ),
                )
                emit(NfcTradeControllerEvent.Succeeded)
            }
        }.onFailure { exception ->
            emit(NfcTradeControllerEvent.Failed(exception.message ?: "Echange NFC interrompu."))
        }
    }

    private suspend fun handleHello(packet: NfcTradePacket): NfcTradePacket {
        emit(NfcTradeControllerEvent.Exchanging)
        val remoteCard = packet.card ?: return packet.failure("Carte distante absente.")
        val validation = tradeGateway.validateTrade(
            localOutgoing = localCard,
            remoteOutgoing = remoteCard,
        )
        if (validation is TradeValidationResult.Invalid) {
            emit(NfcTradeControllerEvent.Failed(validation.message))
            return packet.failure(validation.message)
        }
        activeCardSideTrade = ActiveRemoteTrade(
            tradeId = packet.tradeId,
            remoteCard = remoteCard,
            remoteNonce = packet.nonce,
        )
        return NfcTradePacket(
            type = NfcTradeTypeMatch,
            tradeId = packet.tradeId,
            catalogFingerprint = catalogFingerprint,
            nonce = randomNonce(),
            card = localCard,
        )
    }

    private suspend fun handleCommit(packet: NfcTradePacket): NfcTradePacket {
        val activeTrade = activeCardSideTrade
        if (activeTrade == null || activeTrade.tradeId != packet.tradeId) {
            return packet.failure("Echange NFC inconnu.")
        }
        val result = tradeGateway.applyTrade(
            tradeId = activeTrade.tradeId,
            outgoing = localCard,
            incoming = activeTrade.remoteCard,
        )
        if (result is TradeValidationResult.Invalid) {
            emit(NfcTradeControllerEvent.Failed(result.message))
            return packet.failure(result.message)
        }
        return NfcTradePacket(
            type = NfcTradeTypeCommitted,
            tradeId = packet.tradeId,
            catalogFingerprint = catalogFingerprint,
            nonce = activeTrade.remoteNonce,
        )
    }

    private fun handleAck(packet: NfcTradePacket): NfcTradePacket {
        if (activeCardSideTrade?.tradeId == packet.tradeId) {
            activeCardSideTrade = null
            emit(NfcTradeControllerEvent.Succeeded)
        }
        return packet.copy(type = NfcTradeTypeAck)
    }

    private fun emit(event: NfcTradeControllerEvent) {
        mainHandler.post {
            if (!stopped) onEvent(event)
        }
    }

    private fun randomNonce(): String {
        val bytes = ByteArray(8)
        secureRandom.nextBytes(bytes)
        return bytes.joinToString(separator = "") { byte -> "%02x".format(byte) }
    }

    private fun NfcTradePacket.failure(message: String): NfcTradePacket =
        copy(
            type = NfcTradeTypeFailure,
            ok = false,
            reason = message,
        )

    private fun ByteArray.requireSuccessPayload(): ByteArray =
        NfcTradeApdu.responsePayload(this) ?: error("Service d'echange NFC introuvable.")

    private fun IsoDep.transceivePacket(packet: NfcTradePacket): NfcTradePacket {
        val response = transceive(NfcTradeApdu.command(NfcTradeCodec.encode(packet))).requireSuccessPayload()
        return NfcTradeCodec.decode(response)
    }

    private suspend fun IsoDep.useConnected(block: suspend IsoDep.() -> Unit) {
        connect()
        timeout = IsoDepTimeoutMillis.toInt()
        try {
            block()
        } finally {
            runCatching { close() }
        }
    }

    private data class ActiveRemoteTrade(
        val tradeId: String,
        val remoteCard: TradeCardRef,
        val remoteNonce: String,
    )

    private companion object {
        const val ReaderWindowMillis = 720L
        const val CardWindowMillis = 780L
        const val IsoDepTimeoutMillis = 4_000L
    }
}
