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
