package fr.aumombelli.dstcg.data

import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream

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
