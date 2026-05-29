package fr.aumombelli.dstcg.trade

import fr.aumombelli.dstcg.model.TradeCardRef
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

internal const val NfcTradeProtocolVersion: Int = 1
internal const val NfcTradeTypeHello: String = "hello"
internal const val NfcTradeTypeMatch: String = "match"
internal const val NfcTradeTypeCommit: String = "commit"
internal const val NfcTradeTypeCommitted: String = "committed"
internal const val NfcTradeTypeAck: String = "ack"
internal const val NfcTradeTypeFailure: String = "fail"

@Serializable
internal data class NfcTradePacket(
    @SerialName("t")
    val type: String,
    @SerialName("v")
    val protocolVersion: Int = NfcTradeProtocolVersion,
    @SerialName("tid")
    val tradeId: String,
    @SerialName("fp")
    val catalogFingerprint: String,
    @SerialName("n")
    val nonce: String,
    @SerialName("c")
    val card: TradeCardRef? = null,
    @SerialName("ok")
    val ok: Boolean = true,
    @SerialName("r")
    val reason: String? = null,
)

internal enum class NfcTradeCardPresence {
    Required,
    Forbidden,
    Any,
}

internal data class NfcTradePacketExpectation(
    val expectedType: String,
    val expectedCatalogFingerprint: String,
    val expectedTradeId: String? = null,
    val expectedNonce: String? = null,
    val cardPresence: NfcTradeCardPresence = NfcTradeCardPresence.Forbidden,
)

internal fun NfcTradePacket.commonValidationErrorFor(expectation: NfcTradePacketExpectation): String? {
    if (protocolVersion != NfcTradeProtocolVersion) {
        return "Version d'echange incompatible."
    }
    if (catalogFingerprint != expectation.expectedCatalogFingerprint) {
        return "Catalogues incompatibles."
    }
    if (tradeId.isBlank()) {
        return "Identifiant d'echange NFC invalide."
    }
    val expectedTradeId = expectation.expectedTradeId
    if (expectedTradeId != null && tradeId != expectedTradeId) {
        return "Echange NFC inconnu."
    }
    if (nonce.isBlank()) {
        return "Jeton NFC invalide."
    }
    val expectedNonce = expectation.expectedNonce
    if (expectedNonce != null && nonce != expectedNonce) {
        return "Jeton NFC inattendu."
    }
    return null
}

internal fun NfcTradePacket.validationErrorFor(expectation: NfcTradePacketExpectation): String? {
    commonValidationErrorFor(expectation)?.let { return it }
    if (type != expectation.expectedType) {
        return "Message NFC inattendu."
    }
    return when (expectation.cardPresence) {
        NfcTradeCardPresence.Required -> {
            if (card == null) "Carte distante absente." else null
        }
        NfcTradeCardPresence.Forbidden -> {
            if (card != null) "Carte distante inattendue." else null
        }
        NfcTradeCardPresence.Any -> null
    }
}

internal fun NfcTradePacket.failureResponse(message: String): NfcTradePacket =
    copy(
        type = NfcTradeTypeFailure,
        ok = false,
        reason = message,
        card = null,
    )

internal object NfcTradeCodec {
    private val json = Json {
        encodeDefaults = false
        ignoreUnknownKeys = true
    }

    fun encode(packet: NfcTradePacket): ByteArray =
        json.encodeToString(NfcTradePacket.serializer(), packet).encodeToByteArray()

    fun decode(bytes: ByteArray): NfcTradePacket =
        json.decodeFromString(NfcTradePacket.serializer(), bytes.decodeToString())
}
