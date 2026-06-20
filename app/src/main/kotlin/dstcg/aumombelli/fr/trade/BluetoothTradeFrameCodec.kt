package fr.aumombelli.dstcg.trade

import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer

internal object BluetoothTradeFrameCodec {
    const val DefaultChunkSize: Int = 20

    fun encode(payload: ByteArray, chunkSize: Int = DefaultChunkSize): List<ByteArray> {
        require(chunkSize > LengthPrefixSize) { "Le fragment BLE est trop petit." }
        val framedPayload = ByteBuffer.allocate(LengthPrefixSize + payload.size)
            .putInt(payload.size)
            .put(payload)
            .array()
        return framedPayload.asIterable()
            .chunked(chunkSize)
            .map { chunk -> chunk.toByteArray() }
    }

    class Decoder {
        private val buffer = ByteArrayOutputStream()
        private var expectedPayloadSize: Int? = null

        fun accept(chunk: ByteArray): ByteArray? {
            buffer.write(chunk)
            val bytes = buffer.toByteArray()
            if (expectedPayloadSize == null && bytes.size >= LengthPrefixSize) {
                expectedPayloadSize = ByteBuffer.wrap(bytes, 0, LengthPrefixSize).int
                    .takeIf { it in 0..MaxPayloadSize }
                    ?: throw IllegalArgumentException("Taille de message BLE invalide.")
            }
            val payloadSize = expectedPayloadSize ?: return null
            val framedSize = LengthPrefixSize + payloadSize
            if (bytes.size < framedSize) return null

            val payload = bytes.copyOfRange(LengthPrefixSize, framedSize)
            reset()
            val remaining = bytes.copyOfRange(framedSize, bytes.size)
            if (remaining.isNotEmpty()) {
                buffer.write(remaining)
            }
            return payload
        }

        fun reset() {
            buffer.reset()
            expectedPayloadSize = null
        }
    }

    private const val LengthPrefixSize = 4
    private const val MaxPayloadSize = 64 * 1024
}
