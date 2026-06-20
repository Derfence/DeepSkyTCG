package fr.aumombelli.dstcg.trade

import fr.aumombelli.dstcg.model.TradeCardRef
import java.security.MessageDigest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

internal const val TradeProtocolVersion: Int = 1
internal const val TradeTypeHello: String = "hello"
internal const val TradeTypeConfirm: String = "confirm"
internal const val TradeTypePrepare: String = "prepare"
internal const val TradeTypePrepared: String = "prepared"
internal const val TradeTypeCommit: String = "commit"
internal const val TradeTypeCommitted: String = "committed"
internal const val TradeTypeAck: String = "ack"
internal const val TradeTypeResume: String = "resume"
internal const val TradeTypeFailure: String = "fail"

@Serializable
internal data class TradePacket(
    @SerialName("t")
    val type: String,
    @SerialName("v")
    val protocolVersion: Int = TradeProtocolVersion,
    @SerialName("tid")
    val tradeId: String,
    @SerialName("fp")
    val catalogFingerprint: String,
    @SerialName("n")
    val nonce: String,
    @SerialName("sid")
    val sessionId: String,
    @SerialName("name")
    val deviceName: String? = null,
    @SerialName("c")
    val card: TradeCardRef? = null,
    @SerialName("code")
    val verificationCode: String? = null,
    @SerialName("ok")
    val ok: Boolean = true,
    @SerialName("r")
    val reason: String? = null,
)

internal enum class TradeCardPresence {
    Required,
    Forbidden,
    Any,
}

internal data class TradePacketExpectation(
    val expectedType: String,
    val expectedCatalogFingerprint: String,
    val expectedTradeId: String? = null,
    val expectedNonce: String? = null,
    val cardPresence: TradeCardPresence = TradeCardPresence.Forbidden,
)

internal fun TradePacket.commonValidationErrorFor(expectation: TradePacketExpectation): String? {
    if (protocolVersion != TradeProtocolVersion) {
        return "Version d'échange incompatible."
    }
    if (catalogFingerprint != expectation.expectedCatalogFingerprint) {
        return "Catalogues incompatibles."
    }
    if (tradeId.isBlank()) {
        return "Identifiant d'échange invalide."
    }
    val expectedTradeId = expectation.expectedTradeId
    if (expectedTradeId != null && tradeId != expectedTradeId) {
        return "Échange inconnu."
    }
    if (nonce.isBlank()) {
        return "Jeton d'échange invalide."
    }
    val expectedNonce = expectation.expectedNonce
    if (expectedNonce != null && nonce != expectedNonce) {
        return "Jeton d'échange inattendu."
    }
    return null
}

internal fun TradePacket.validationErrorFor(expectation: TradePacketExpectation): String? {
    commonValidationErrorFor(expectation)?.let { return it }
    if (type != expectation.expectedType) {
        return "Message d'échange inattendu."
    }
    return when (expectation.cardPresence) {
        TradeCardPresence.Required -> if (card == null) "Carte distante absente." else null
        TradeCardPresence.Forbidden -> if (card != null) "Carte distante inattendue." else null
        TradeCardPresence.Any -> null
    }
}

internal fun TradePacket.failureResponse(message: String): TradePacket =
    copy(
        type = TradeTypeFailure,
        ok = false,
        reason = message,
        card = null,
    )

internal fun verificationCodeFor(
    tradeId: String,
    firstSessionId: String,
    secondSessionId: String,
): String {
    val orderedSessions = listOf(firstSessionId, secondSessionId).sorted().joinToString(":")
    val digest = MessageDigest.getInstance("SHA-256")
        .digest("$tradeId:$orderedSessions".encodeToByteArray())
    val value = digest.take(4).fold(0) { acc, byte -> (acc shl 8) xor (byte.toInt() and 0xFF) }
    return (value.mod(10_000)).toString().padStart(4, '0')
}

internal object TradeCodec {
    private val json = Json {
        encodeDefaults = false
        ignoreUnknownKeys = true
    }

    fun encode(packet: TradePacket): ByteArray =
        json.encodeToString(TradePacket.serializer(), packet).encodeToByteArray()

    fun decode(bytes: ByteArray): TradePacket =
        json.decodeFromString(TradePacket.serializer(), bytes.decodeToString())
}
