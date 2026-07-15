package fr.aumombelli.dstcg.data

import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class BackupRepository(
    private val progressRepository: ProgressGateway,
    private val securityRepository: BackupSecurityRepository,
    private val codec: BackupCodec = BackupCodec(),
    private val appVersionCode: Int,
    private val backupIdFactory: () -> String = { UUID.randomUUID().toString() },
    private val previewTokenFactory: () -> String = { UUID.randomUUID().toString() },
) : BackupGateway {
    private val previewMutex = Mutex()
    private val importMutex = Mutex()
    private var inspectedBackup: InspectedBackup? = null

    override val lastSuccessfulImportAt: Flow<Instant?> = securityRepository.lastSuccessfulImportAt

    override suspend fun exportBackup(password: String): BackupDocument {
        validateAndNormalizeBackupPassword(password)
        val status = securityRepository.status()
        val loaded = progressRepository.loadProgress().requireUsableProgress()
        val minimumCreatedAt = status.lastSuccessfulImportAt?.plusMillis(1)
        val createdAt = if (minimumCreatedAt != null && status.trustedNow.isBefore(minimumCreatedAt)) {
            minimumCreatedAt
        } else {
            status.trustedNow
        }
        val payload = PortableBackupPayload(
            backupId = backupIdFactory(),
            createdAtUtc = createdAt.toString(),
            appVersionCode = appVersionCode,
            progressSchemaVersion = ProgressSnapshot.CURRENT_SCHEMA_VERSION,
            progress = loaded.progress,
        )
        val bytes = codec.encrypt(payload, password)
        return BackupDocument(
            fileName = "deep-sky-${FILE_DATE_FORMATTER.format(createdAt)}.dstcgsave",
            bytes = bytes,
            createdAt = createdAt,
        )
    }

    override suspend fun inspectBackup(input: BackupInput, password: String): BackupPreview {
        if (input.bytes.isEmpty() || input.bytes.size > MAX_BACKUP_SIZE_BYTES) {
            throw BackupFormatException("La sauvegarde est vide ou dépasse la limite de 5 Mio.")
        }
        val payload = codec.decrypt(input.bytes, password)
        validatePayload(payload)
        val createdAt = runCatching { Instant.parse(payload.createdAtUtc) }
            .getOrElse { throw BackupFormatException("La date de création de la sauvegarde est invalide.") }
        val status = securityRepository.status()
        val cutoff = status.lastSuccessfulImportAt
        if (cutoff != null && !createdAt.isAfter(cutoff)) {
            throw BackupTooOldException(
                "Cette sauvegarde a été créée le ${createdAt.toDisplayUtc()} et la dernière importation " +
                    "a été réalisée le ${cutoff.toDisplayUtc()}. Elle ne peut pas être réimportée.",
            )
        }
        val token = previewTokenFactory()
        val preview = payload.toPreview(token, createdAt)
        previewMutex.withLock {
            inspectedBackup = InspectedBackup(token, payload, preview)
        }
        return preview
    }

    override suspend fun importBackup(previewToken: String): BackupImportResult = importMutex.withLock {
        val inspected = previewMutex.withLock {
            inspectedBackup?.takeIf { it.token == previewToken }
                ?: throw BackupException("L'aperçu de sauvegarde a expiré. Sélectionne à nouveau le fichier.")
        }
        val createdAt = inspected.preview.createdAt
        val latestStatus = securityRepository.status()
        latestStatus.lastSuccessfulImportAt?.let { cutoff ->
            if (!createdAt.isAfter(cutoff)) {
                throw BackupTooOldException("Cette sauvegarde est antérieure ou égale à la dernière importation.")
            }
        }

        val backupId = inspected.payload.backupId
        securityRepository.beginImport(backupId)
        try {
            progressRepository.restoreProgress(inspected.payload.progress)
        } catch (exception: Exception) {
            runCatching { securityRepository.cancelImport(backupId) }
            throw exception
        }
        // If finalization fails after the progress write, keep the pending marker.
        // It will be promoted on the next read and will still block rollback.
        val importedAt = securityRepository.completeImport(backupId)
        previewMutex.withLock { inspectedBackup = null }
        BackupImportResult(importedAt = importedAt, backupCreatedAt = createdAt)
    }

    private fun validatePayload(payload: PortableBackupPayload) {
        if (payload.payloadVersion != PortableBackupPayload.CURRENT_PAYLOAD_VERSION) {
            throw BackupFormatException("Cette version de contenu de sauvegarde n'est pas prise en charge.")
        }
        if (payload.progressSchemaVersion > ProgressSnapshot.CURRENT_SCHEMA_VERSION) {
            throw BackupFormatException("Cette sauvegarde nécessite une version plus récente de l'application.")
        }
        if (payload.backupId.isBlank()) {
            throw BackupFormatException("L'identifiant de la sauvegarde est invalide.")
        }
    }

    private fun PortableBackupPayload.toPreview(token: String, createdAt: Instant): BackupPreview = BackupPreview(
        token = token,
        createdAt = createdAt,
        appVersionCode = appVersionCode,
        progressSchemaVersion = progressSchemaVersion,
        distinctCardCount = progress.collection.cards.size,
        totalOwnedCardCount = progress.collection.cards.values.sumOf { it.totalOwned.coerceAtLeast(0) },
        openedPackCount = progress.openedPackCount.coerceAtLeast(0),
    )

    private data class InspectedBackup(
        val token: String,
        val payload: PortableBackupPayload,
        val preview: BackupPreview,
    )

    companion object {
        const val MAX_BACKUP_SIZE_BYTES = 5 * 1024 * 1024
        private val FILE_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")
            .withZone(ZoneOffset.UTC)
    }
}

private fun Instant.toDisplayUtc(): String = DateTimeFormatter.ofPattern("dd/MM/yyyy 'à' HH:mm:ss 'UTC'")
    .withZone(ZoneOffset.UTC)
    .format(this)
