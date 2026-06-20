package fr.aumombelli.dstcg.trade

import java.nio.charset.StandardCharsets

internal data class BluetoothTradeAdvertisement(
    val sessionId: String,
    val catalogFingerprintPrefix: String,
    val deviceName: String,
    val status: BluetoothTradeAdvertisementStatus = BluetoothTradeAdvertisementStatus.Active,
)

internal enum class BluetoothTradeAdvertisementStatus {
    Active,
    Leaving,
}

internal object BluetoothTradeAdvertisementCodec {
    fun encode(
        sessionId: String,
        catalogFingerprint: String,
        deviceName: String,
        status: BluetoothTradeAdvertisementStatus = BluetoothTradeAdvertisementStatus.Active,
    ): ByteArray {
        val normalizedSessionId = sessionId.take(SessionIdBytes)
            .padEnd(SessionIdBytes, '0')
        val normalizedFingerprint = catalogFingerprint.take(FingerprintBytes)
            .padEnd(FingerprintBytes, '0')
        val encodedName = deviceName.trim().encodeUtf8Prefix(MaxNameBytes)
        return byteArrayOf(status.toVersionByte()) +
            normalizedSessionId.toByteArray(StandardCharsets.US_ASCII) +
            normalizedFingerprint.toByteArray(StandardCharsets.US_ASCII) +
            encodedName
    }

    fun decode(bytes: ByteArray): BluetoothTradeAdvertisement? {
        if (bytes.isEmpty()) return null
        val version = bytes.first()
        val status = version.toAdvertisementStatus() ?: return null
        if (bytes.size < HeaderBytes) return null
        val sessionId = bytes.copyOfRange(1, 1 + SessionIdBytes)
            .toString(StandardCharsets.US_ASCII)
            .trimEnd('0')
        val fingerprint = bytes.copyOfRange(1 + SessionIdBytes, HeaderBytes)
            .toString(StandardCharsets.US_ASCII)
            .trimEnd('0')
        val name = bytes.copyOfRange(HeaderBytes, bytes.size)
            .decodeToString()
            .trim()
        if (sessionId.isBlank() || name.isBlank()) return null
        return BluetoothTradeAdvertisement(
            sessionId = sessionId,
            catalogFingerprintPrefix = fingerprint,
            deviceName = name,
            status = status,
        )
    }

    private const val SessionIdBytes = 8
    private const val FingerprintBytes = 6
    private const val HeaderBytes = 1 + SessionIdBytes + FingerprintBytes
    private const val MaxNameBytes = 12
}

private fun BluetoothTradeAdvertisementStatus.toVersionByte(): Byte = when (this) {
    BluetoothTradeAdvertisementStatus.Active -> 1
    BluetoothTradeAdvertisementStatus.Leaving -> 2
}

private fun Byte.toAdvertisementStatus(): BluetoothTradeAdvertisementStatus? = when (this.toInt()) {
    1 -> BluetoothTradeAdvertisementStatus.Active
    2 -> BluetoothTradeAdvertisementStatus.Leaving
    else -> null
}

private fun String.encodeUtf8Prefix(maxBytes: Int): ByteArray {
    var endIndex = length
    while (endIndex >= 0) {
        val encoded = take(endIndex).encodeToByteArray()
        if (encoded.size <= maxBytes) return encoded
        endIndex -= 1
    }
    return ByteArray(0)
}
