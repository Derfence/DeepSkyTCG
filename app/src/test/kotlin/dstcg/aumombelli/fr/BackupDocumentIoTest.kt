package fr.aumombelli.dstcg

import fr.aumombelli.dstcg.data.readBackupBytesLimited
import fr.aumombelli.dstcg.data.matchesBackupDocument
import fr.aumombelli.dstcg.data.writeBackupDocument
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupDocumentIoTest {
    @Test
    fun `in-memory document round trip preserves bytes`() {
        val expected = byteArrayOf(1, 2, 3, 4)
        val output = ByteArrayOutputStream()

        output.writeBackupDocument(expected)
        val restored = ByteArrayInputStream(output.toByteArray()).readBackupBytesLimited(expected.size)

        assertArrayEquals(expected, restored)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `document larger than limit is rejected`() {
        ByteArrayInputStream(byteArrayOf(1, 2, 3)).readBackupBytesLimited(2)
    }

    @Test
    fun `document verification checks content and length`() {
        val expected = byteArrayOf(1, 2, 3, 4)

        assertTrue(ByteArrayInputStream(expected).matchesBackupDocument(expected))
        assertFalse(ByteArrayInputStream(byteArrayOf(1, 2, 3)).matchesBackupDocument(expected))
        assertFalse(ByteArrayInputStream(byteArrayOf(1, 2, 3, 5)).matchesBackupDocument(expected))
    }
}
