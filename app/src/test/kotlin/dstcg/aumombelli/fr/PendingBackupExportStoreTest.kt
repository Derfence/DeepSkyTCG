package fr.aumombelli.dstcg

import fr.aumombelli.dstcg.data.BackupDocument
import fr.aumombelli.dstcg.data.FilePendingBackupExportStore
import java.time.Instant
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class PendingBackupExportStoreTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun `staged encrypted export survives store recreation`() = runTest {
        val directory = temporaryFolder.newFolder("pending-export")
        val expected = byteArrayOf(1, 2, 3, 4)
        FilePendingBackupExportStore(directory).stage(document(expected))

        val restored = FilePendingBackupExportStore(directory).load()

        assertArrayEquals(expected, restored)
    }

    @Test
    fun `discard removes staged export`() = runTest {
        val directory = temporaryFolder.newFolder("pending-export")
        val store = FilePendingBackupExportStore(directory)
        store.stage(document(byteArrayOf(1)))

        store.discard()

        assertNull(FilePendingBackupExportStore(directory).load())
        assertFalse(directory.exists())
    }

    private fun document(bytes: ByteArray) = BackupDocument(
        fileName = "backup.dstcgsave",
        bytes = bytes,
        createdAt = Instant.parse("2026-07-17T10:00:00Z"),
    )
}
