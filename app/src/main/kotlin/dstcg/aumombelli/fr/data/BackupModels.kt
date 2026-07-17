package fr.aumombelli.dstcg.data

import fr.aumombelli.dstcg.model.StandaloneProgress
import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

@Serializable
data class BackupEnvelope(
    val format: String = FORMAT,
    val formatVersion: Int = CURRENT_FORMAT_VERSION,
    val kdf: String = KDF,
    val kdfIterations: Int = KDF_ITERATIONS,
    val saltBase64: String,
    val cipher: String = CIPHER,
    val nonceBase64: String,
    val ciphertextBase64: String,
) {
    companion object {
        const val FORMAT = "DSTCG_BACKUP"
        const val CURRENT_FORMAT_VERSION = 1
        const val KDF = "PBKDF2WithHmacSHA256"
        const val KDF_ITERATIONS = 600_000
        const val CIPHER = "AES/GCM/NoPadding"
    }
}

@Serializable
data class PortableBackupPayload(
    val payloadVersion: Int = CURRENT_PAYLOAD_VERSION,
    val backupId: String,
    val createdAtUtc: String,
    val appVersionCode: Int,
    val progressSchemaVersion: Int,
    val progress: StandaloneProgress,
) {
    companion object {
        const val CURRENT_PAYLOAD_VERSION = 1
    }
}

data class BackupInput(
    val bytes: ByteArray,
)

data class BackupDocument(
    val fileName: String,
    val bytes: ByteArray,
    val createdAt: Instant,
)

data class BackupPreview(
    val token: String,
    val createdAt: Instant,
    val appVersionCode: Int,
    val progressSchemaVersion: Int,
    val distinctCardCount: Int,
    val totalOwnedCardCount: Int,
    val openedPackCount: Int,
)

data class BackupImportResult(
    val importedAt: Instant,
    val backupCreatedAt: Instant,
)

interface BackupGateway {
    val lastSuccessfulImportAt: Flow<Instant?>

    suspend fun exportBackup(password: String): BackupDocument
    suspend fun inspectBackup(input: BackupInput, password: String): BackupPreview
    suspend fun importBackup(previewToken: String): BackupImportResult
    suspend fun discardBackupPreview(previewToken: String?)
}

open class BackupException(message: String, cause: Throwable? = null) : IllegalStateException(message, cause)

class BackupPasswordException(message: String) : BackupException(message)

class BackupFormatException(message: String, cause: Throwable? = null) : BackupException(message, cause)

class BackupTooOldException(message: String) : BackupException(message)

class BackupSecurityStateException(message: String, cause: Throwable? = null) : BackupException(message, cause)

object UnavailableBackupGateway : BackupGateway {
    override val lastSuccessfulImportAt: Flow<Instant?> = kotlinx.coroutines.flow.flowOf(null)

    override suspend fun exportBackup(password: String): BackupDocument =
        throw BackupException("La sauvegarde n'est pas disponible.")

    override suspend fun inspectBackup(input: BackupInput, password: String): BackupPreview =
        throw BackupException("La sauvegarde n'est pas disponible.")

    override suspend fun importBackup(previewToken: String): BackupImportResult =
        throw BackupException("La sauvegarde n'est pas disponible.")

    override suspend fun discardBackupPreview(previewToken: String?) = Unit
}
