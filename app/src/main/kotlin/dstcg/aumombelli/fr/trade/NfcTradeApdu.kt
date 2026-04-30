package fr.aumombelli.dstcg.trade

internal object NfcTradeApdu {
    private val aid = byteArrayOf(
        0xF0.toByte(),
        0x44.toByte(),
        0x53.toByte(),
        0x54.toByte(),
        0x43.toByte(),
        0x47.toByte(),
        0x01.toByte(),
    )
    private val okStatus = byteArrayOf(0x90.toByte(), 0x00.toByte())
    private val notFoundStatus = byteArrayOf(0x6A.toByte(), 0x82.toByte())
    private val failedStatus = byteArrayOf(0x6F.toByte(), 0x00.toByte())

    fun selectAidCommand(): ByteArray =
        byteArrayOf(0x00, 0xA4.toByte(), 0x04, 0x00, aid.size.toByte()) + aid + byteArrayOf(0x00)

    fun isSelectAid(command: ByteArray): Boolean {
        if (
            command.size < 6 ||
            command[0] != 0x00.toByte() ||
            command[1] != 0xA4.toByte() ||
            command[2] != 0x04.toByte() ||
            command[3] != 0x00.toByte()
        ) {
            return false
        }
        val length = command[4].toInt() and 0xFF
        if (command.size != 6 + length || command.last() != 0x00.toByte()) return false
        return command.copyOfRange(5, 5 + length).contentEquals(aid)
    }

    fun command(payload: ByteArray): ByteArray {
        require(payload.size <= 240) { "NFC trade payload is too large (${payload.size} bytes)." }
        return byteArrayOf(
            0x80.toByte(),
            0x10.toByte(),
            0x00.toByte(),
            0x00.toByte(),
            payload.size.toByte(),
        ) + payload + byteArrayOf(0x00)
    }

    fun payloadFromCommand(command: ByteArray): ByteArray? {
        if (
            command.size < 6 ||
            command[0] != 0x80.toByte() ||
            command[1] != 0x10.toByte() ||
            command[2] != 0x00.toByte() ||
            command[3] != 0x00.toByte()
        ) {
            return null
        }
        val length = command[4].toInt() and 0xFF
        if (command.size != 6 + length || command.last() != 0x00.toByte()) return null
        return command.copyOfRange(5, 5 + length)
    }

    fun successResponse(payload: ByteArray = ByteArray(0)): ByteArray = payload + okStatus

    fun notFoundResponse(): ByteArray = notFoundStatus

    fun failedResponse(): ByteArray = failedStatus

    fun responsePayload(response: ByteArray): ByteArray? {
        if (response.size < 2) return null
        val status = response.copyOfRange(response.size - 2, response.size)
        if (!status.contentEquals(okStatus)) return null
        return response.copyOfRange(0, response.size - 2)
    }
}
