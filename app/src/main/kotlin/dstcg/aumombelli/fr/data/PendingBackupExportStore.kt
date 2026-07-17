package fr.aumombelli.dstcg.data

import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

interface PendingBackupExportStore {
    suspend fun stage(document: BackupDocument)

    suspend fun load(): ByteArray?

    suspend fun discard()
}

class FilePendingBackupExportStore(
    private val directory: File,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : PendingBackupExportStore {
    private val mutex = Mutex()
    private val pendingFile: File
        get() = File(directory, PENDING_FILE_NAME)
    private val temporaryFile: File
        get() = File(directory, TEMPORARY_FILE_NAME)

    override suspend fun stage(document: BackupDocument) = mutex.withLock {
        require(document.bytes.isNotEmpty()) { "La sauvegarde à exporter est vide." }
        require(document.bytes.size <= BackupRepository.MAX_BACKUP_SIZE_BYTES) {
            "La sauvegarde à exporter dépasse la limite de 5 Mio."
        }
        withContext(ioDispatcher) {
            if (!directory.exists() && !directory.mkdirs()) {
                throw IOException("Le stockage temporaire de la sauvegarde n'a pas pu être créé.")
            }
            temporaryFile.delete()
            try {
                FileOutputStream(temporaryFile).use { output ->
                    output.write(document.bytes)
                    output.flush()
                    output.fd.sync()
                }
                replacePendingFile()
            } catch (exception: Exception) {
                temporaryFile.delete()
                throw exception
            }
        }
    }

    override suspend fun load(): ByteArray? = mutex.withLock {
        withContext(ioDispatcher) {
            if (!pendingFile.exists()) return@withContext null
            val size = pendingFile.length()
            if (size <= 0L || size > BackupRepository.MAX_BACKUP_SIZE_BYTES.toLong()) {
                throw IOException("La sauvegarde temporaire est vide ou invalide.")
            }
            pendingFile.readBytes()
        }
    }

    override suspend fun discard() = mutex.withLock {
        withContext(ioDispatcher) {
            temporaryFile.delete()
            pendingFile.delete()
            if (directory.isDirectory && directory.list().isNullOrEmpty()) {
                directory.delete()
            }
        }
    }

    private fun replacePendingFile() {
        try {
            Files.move(
                temporaryFile.toPath(),
                pendingFile.toPath(),
                StandardCopyOption.ATOMIC_MOVE,
                StandardCopyOption.REPLACE_EXISTING,
            )
        } catch (_: AtomicMoveNotSupportedException) {
            Files.move(
                temporaryFile.toPath(),
                pendingFile.toPath(),
                StandardCopyOption.REPLACE_EXISTING,
            )
        }
    }

    private companion object {
        const val PENDING_FILE_NAME = "pending.dstcgsave"
        const val TEMPORARY_FILE_NAME = "pending.dstcgsave.tmp"
    }
}

class InMemoryPendingBackupExportStore : PendingBackupExportStore {
    private val mutex = Mutex()
    private var bytes: ByteArray? = null

    override suspend fun stage(document: BackupDocument) = mutex.withLock {
        bytes?.fill(0)
        bytes = document.bytes.copyOf()
    }

    override suspend fun load(): ByteArray? = mutex.withLock { bytes?.copyOf() }

    override suspend fun discard() = mutex.withLock {
        bytes?.fill(0)
        bytes = null
    }
}
