package fr.aumombelli.dstcg.data

import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.security.MessageDigest

internal fun InputStream.readBackupBytesLimited(maxBytes: Int): ByteArray {
    val output = ByteArrayOutputStream()
    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
    var total = 0
    while (true) {
        val read = read(buffer)
        if (read < 0) break
        total += read
        if (total > maxBytes) {
            throw IllegalArgumentException("La sauvegarde dépasse la limite de 5 Mio.")
        }
        output.write(buffer, 0, read)
    }
    return output.toByteArray()
}

internal fun OutputStream.writeBackupDocument(bytes: ByteArray) {
    write(bytes)
    flush()
}

internal fun InputStream.matchesBackupDocument(expected: ByteArray): Boolean {
    val digest = MessageDigest.getInstance("SHA-256")
    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
    var total = 0L
    while (true) {
        val read = read(buffer)
        if (read < 0) break
        total += read
        if (total > expected.size) return false
        digest.update(buffer, 0, read)
    }
    if (total != expected.size.toLong()) return false
    val expectedDigest = MessageDigest.getInstance("SHA-256").digest(expected)
    return MessageDigest.isEqual(expectedDigest, digest.digest())
}
