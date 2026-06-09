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
import fr.aumombelli.dstcg.trade.NfcTradeCardPresence
import fr.aumombelli.dstcg.trade.NfcTradeCodec
import fr.aumombelli.dstcg.trade.NfcTradePacket
import fr.aumombelli.dstcg.trade.NfcTradePacketExpectation
import fr.aumombelli.dstcg.trade.NfcTradeSessionHandler
import fr.aumombelli.dstcg.trade.NfcTradeSessionRegistry
import fr.aumombelli.dstcg.trade.NfcTradeTypeAck
import fr.aumombelli.dstcg.trade.NfcTradeTypeCommit
import fr.aumombelli.dstcg.trade.NfcTradeTypeCommitted
import fr.aumombelli.dstcg.trade.NfcTradeTypeFailure
import fr.aumombelli.dstcg.trade.NfcTradeTypeHello
import fr.aumombelli.dstcg.trade.NfcTradeTypeMatch
import fr.aumombelli.dstcg.trade.commonValidationErrorFor
import fr.aumombelli.dstcg.trade.failureResponse
import fr.aumombelli.dstcg.trade.validationErrorFor
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
    data class Succeeded(val receivedCard: TradeCardRef) : NfcTradeControllerEvent
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
                onEvent(NfcTradeControllerEvent.Failed("Ce téléphone ne prend pas en charge le NFC."))
                return
            }
            !adapter.isEnabled -> {
                onEvent(NfcTradeControllerEvent.Failed("Le NFC est désactivé."))
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
        packet.commonValidationErrorFor(
            NfcTradePacketExpectation(
                expectedType = packet.type,
                expectedCatalogFingerprint = catalogFingerprint,
                cardPresence = NfcTradeCardPresence.Any,
            ),
        )?.let { error -> return packet.failureResponse(error) }

        return when (packet.type) {
            NfcTradeTypeHello -> handleHello(packet)
            NfcTradeTypeCommit -> handleCommit(packet)
            NfcTradeTypeAck -> handleAck(packet)
            else -> packet.failureResponse("Message NFC inattendu.")
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
                    .requireExpectedResponse(
                        expectedType = NfcTradeTypeMatch,
                        expectedTradeId = tradeId,
                        expectedFailureNonce = nonce,
                        cardPresence = NfcTradeCardPresence.Required,
                        failureMessage = "Échange refusé.",
                    )
                val remoteCard = checkNotNull(match.card) { "Carte distante absente." }
                val remoteNonce = match.nonce
                if (remoteNonce == nonce) {
                    error("Jeton NFC distant invalide.")
                }
                val validation = tradeGateway.validateTrade(
                    localOutgoing = localCard,
                    remoteOutgoing = remoteCard,
                )
                if (!validation.isValid()) {
                    error((validation as TradeValidationResult.Invalid).message)
                }

                transceivePacket(
                    NfcTradePacket(
                        type = NfcTradeTypeCommit,
                        tradeId = tradeId,
                        catalogFingerprint = catalogFingerprint,
                        nonce = remoteNonce,
                    ),
                ).requireExpectedResponse(
                    expectedType = NfcTradeTypeCommitted,
                    expectedTradeId = tradeId,
                    expectedSuccessNonce = nonce,
                    expectedFailureNonce = remoteNonce,
                    cardPresence = NfcTradeCardPresence.Forbidden,
                    failureMessage = "Validation distante impossible.",
                )

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
                        nonce = remoteNonce,
                    ),
                ).requireExpectedResponse(
                    expectedType = NfcTradeTypeAck,
                    expectedTradeId = tradeId,
                    expectedSuccessNonce = nonce,
                    expectedFailureNonce = remoteNonce,
                    cardPresence = NfcTradeCardPresence.Forbidden,
                    failureMessage = "Confirmation distante impossible.",
                )
                emit(NfcTradeControllerEvent.Succeeded(remoteCard))
            }
        }.onFailure { exception ->
            emit(NfcTradeControllerEvent.Failed(exception.message ?: "Échange NFC interrompu."))
        }
    }

    private suspend fun handleHello(packet: NfcTradePacket): NfcTradePacket {
        packet.validationErrorFor(
            NfcTradePacketExpectation(
                expectedType = NfcTradeTypeHello,
                expectedCatalogFingerprint = catalogFingerprint,
                cardPresence = NfcTradeCardPresence.Required,
            ),
        )?.let { error -> return packet.failureResponse(error) }
        emit(NfcTradeControllerEvent.Exchanging)
        val remoteCard = checkNotNull(packet.card)
        val validation = tradeGateway.validateTrade(
            localOutgoing = localCard,
            remoteOutgoing = remoteCard,
        )
        if (validation is TradeValidationResult.Invalid) {
            emit(NfcTradeControllerEvent.Failed(validation.message))
            return packet.failureResponse(validation.message)
        }
        val localNonce = randomNonce(excluding = packet.nonce)
        activeCardSideTrade = ActiveRemoteTrade(
            tradeId = packet.tradeId,
            remoteCard = remoteCard,
            remoteNonce = packet.nonce,
            localNonce = localNonce,
        )
        return NfcTradePacket(
            type = NfcTradeTypeMatch,
            tradeId = packet.tradeId,
            catalogFingerprint = catalogFingerprint,
            nonce = localNonce,
            card = localCard,
        )
    }

    private suspend fun handleCommit(packet: NfcTradePacket): NfcTradePacket {
        val activeTrade = activeCardSideTrade
        if (activeTrade == null || activeTrade.tradeId != packet.tradeId) {
            return packet.failureResponse("Échange NFC inconnu.")
        }
        packet.validationErrorFor(
            NfcTradePacketExpectation(
                expectedType = NfcTradeTypeCommit,
                expectedCatalogFingerprint = catalogFingerprint,
                expectedTradeId = activeTrade.tradeId,
                expectedNonce = activeTrade.localNonce,
                cardPresence = NfcTradeCardPresence.Forbidden,
            ),
        )?.let { error -> return packet.failureResponse(error) }
        val result = tradeGateway.applyTrade(
            tradeId = activeTrade.tradeId,
            outgoing = localCard,
            incoming = activeTrade.remoteCard,
        )
        if (result is TradeValidationResult.Invalid) {
            emit(NfcTradeControllerEvent.Failed(result.message))
            return packet.failureResponse(result.message)
        }
        return NfcTradePacket(
            type = NfcTradeTypeCommitted,
            tradeId = packet.tradeId,
            catalogFingerprint = catalogFingerprint,
            nonce = activeTrade.remoteNonce,
        )
    }

    private fun handleAck(packet: NfcTradePacket): NfcTradePacket {
        val activeTrade = activeCardSideTrade
        if (activeTrade == null || activeTrade.tradeId != packet.tradeId) {
            return packet.failureResponse("Échange NFC inconnu.")
        }
        packet.validationErrorFor(
            NfcTradePacketExpectation(
                expectedType = NfcTradeTypeAck,
                expectedCatalogFingerprint = catalogFingerprint,
                expectedTradeId = activeTrade.tradeId,
                expectedNonce = activeTrade.localNonce,
                cardPresence = NfcTradeCardPresence.Forbidden,
            ),
        )?.let { error -> return packet.failureResponse(error) }
        activeCardSideTrade = null
        emit(NfcTradeControllerEvent.Succeeded(activeTrade.remoteCard))
        return NfcTradePacket(
            type = NfcTradeTypeAck,
            tradeId = packet.tradeId,
            catalogFingerprint = catalogFingerprint,
            nonce = activeTrade.remoteNonce,
        )
    }

    private fun NfcTradePacket.requireExpectedResponse(
        expectedType: String,
        expectedTradeId: String,
        expectedSuccessNonce: String? = null,
        expectedFailureNonce: String? = null,
        cardPresence: NfcTradeCardPresence,
        failureMessage: String,
    ): NfcTradePacket {
        if (type == NfcTradeTypeFailure) {
            validationErrorFor(
                NfcTradePacketExpectation(
                    expectedType = NfcTradeTypeFailure,
                    expectedCatalogFingerprint = this@NfcTradeController.catalogFingerprint,
                    expectedTradeId = expectedTradeId,
                    expectedNonce = expectedFailureNonce,
                    cardPresence = NfcTradeCardPresence.Forbidden,
                ),
            )?.let { validationError -> error(validationError) }
            error(reason ?: failureMessage)
        }
        validationErrorFor(
            NfcTradePacketExpectation(
                expectedType = expectedType,
                expectedCatalogFingerprint = this@NfcTradeController.catalogFingerprint,
                expectedTradeId = expectedTradeId,
                expectedNonce = expectedSuccessNonce,
                cardPresence = cardPresence,
            ),
        )?.let { validationError -> error(validationError) }
        if (!ok) {
            error(reason ?: failureMessage)
        }
        return this
    }

    private fun emit(event: NfcTradeControllerEvent) {
        mainHandler.post {
            if (!stopped) onEvent(event)
        }
    }

    private fun randomNonce(excluding: String? = null): String {
        while (true) {
            val bytes = ByteArray(8)
            secureRandom.nextBytes(bytes)
            val nonce = bytes.joinToString(separator = "") { byte -> "%02x".format(byte) }
            if (nonce != excluding) return nonce
        }
    }

    private fun ByteArray.requireSuccessPayload(): ByteArray =
        NfcTradeApdu.responsePayload(this) ?: error("Service d'échange NFC introuvable.")

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
        val localNonce: String,
    )

    private companion object {
        const val ReaderWindowMillis = 720L
        const val CardWindowMillis = 780L
        const val IsoDepTimeoutMillis = 4_000L
    }
}
