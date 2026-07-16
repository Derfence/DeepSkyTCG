package fr.aumombelli.dstcg.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.dataStore
import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

internal val Context.secureBackupSecurityDataStore: DataStore<EncryptedProgressEnvelope> by dataStore(
    fileName = "dstcg_backup_security_state.json",
    serializer = EncryptedProgressEnvelopeSerializer,
)

@Serializable
data class PendingBackupImport(
    val backupId: String,
    val startedAtUtc: String,
)

@Serializable
data class BackupSecurityState(
    val schemaVersion: Int = 1,
    val lastSuccessfulImportAtUtc: String? = null,
    val pendingImport: PendingBackupImport? = null,
    val lastTrustedWallClockUtc: String? = null,
    val lastTrustedElapsedRealtimeMs: Long = 0L,
    val lastObservedBootMarker: String = "",
    val tamperFlag: Boolean = false,
)

internal data class BackupSecurityStatus(
    val lastSuccessfulImportAt: Instant?,
    val trustedNow: Instant,
)

internal interface BackupSecurityGateway {
    val lastSuccessfulImportAt: Flow<Instant?>

    suspend fun status(): BackupSecurityStatus
    suspend fun beginImport(backupId: String): Instant
    suspend fun completeImport(backupId: String): Instant
    suspend fun cancelImport(backupId: String)
}

internal class BackupSecurityRepository(
    private val dataStore: DataStore<EncryptedProgressEnvelope>,
    private val cipher: ProgressCipher,
    timeSource: TrustedTimeSource,
) : BackupSecurityGateway {
    private val json = Json { encodeDefaults = true; ignoreUnknownKeys = true }
    private val mutex = Mutex()
    private val trustedTimeResolver = TrustedTimeResolver(timeSource)

    override val lastSuccessfulImportAt: Flow<Instant?> = dataStore.data.map { envelope ->
        val state = decryptState(envelope)
        state.effectiveLastImportAt()
    }

    override suspend fun status(): BackupSecurityStatus = mutex.withLock {
        val state = readState().promotePending()
        val resolution = trustedTimeResolver.resolve(state.toTimeAnchor())
        val updated = state.withTimeResolution(resolution)
        writeState(updated)
        BackupSecurityStatus(
            lastSuccessfulImportAt = updated.lastSuccessfulImportAtUtc?.let(Instant::parse),
            trustedNow = resolution.trustedNow,
        )
    }

    override suspend fun beginImport(backupId: String): Instant = mutex.withLock {
        val state = readState().promotePending()
        val resolution = trustedTimeResolver.resolve(state.toTimeAnchor())
        val startedAt = resolution.trustedNow
        writeState(
            state.withTimeResolution(resolution).copy(
                pendingImport = PendingBackupImport(backupId, startedAt.toString()),
            ),
        )
        startedAt
    }

    override suspend fun completeImport(backupId: String): Instant = mutex.withLock {
        val state = readState()
        val pending = state.pendingImport?.takeIf { it.backupId == backupId }
            ?: throw BackupSecurityStateException("L'import provisoire est introuvable.")
        val resolution = trustedTimeResolver.resolve(state.toTimeAnchor())
        val completedAt = Instant.parse(pending.startedAtUtc)
        val previous = state.lastSuccessfulImportAtUtc?.let(Instant::parse)
        val effective = listOfNotNull(previous, completedAt).maxOrNull()
        writeState(
            state.withTimeResolution(resolution).copy(
                lastSuccessfulImportAtUtc = effective?.toString(),
                pendingImport = null,
            ),
        )
        requireNotNull(effective)
    }

    override suspend fun cancelImport(backupId: String) = mutex.withLock {
        val state = readState()
        if (state.pendingImport?.backupId == backupId) {
            val resolution = trustedTimeResolver.resolve(state.toTimeAnchor())
            writeState(state.withTimeResolution(resolution).copy(pendingImport = null))
        }
    }

    private suspend fun readState(): BackupSecurityState =
        decryptState(dataStore.data.first())

    private fun decryptState(envelope: EncryptedProgressEnvelope): BackupSecurityState {
        if (envelope.isEmpty()) return BackupSecurityState()
        return try {
            val plaintext = cipher.decrypt(envelope.toPayload())
            try {
                json.decodeFromString(BackupSecurityState.serializer(), plaintext.decodeToString())
            } finally {
                plaintext.fill(0)
            }
        } catch (exception: Exception) {
            throw BackupSecurityStateException(
                "L'état de sécurité des sauvegardes est corrompu. L'import est bloqué.",
                exception,
            )
        }
    }

    private suspend fun writeState(state: BackupSecurityState) {
        val plaintext = json.encodeToString(BackupSecurityState.serializer(), state).encodeToByteArray()
        val payload = try {
            cipher.encrypt(plaintext)
        } finally {
            plaintext.fill(0)
        }
        dataStore.updateData { EncryptedProgressEnvelope.fromPayload(payload) }
    }

    private fun BackupSecurityState.promotePending(): BackupSecurityState {
        val pendingAt = pendingImport?.startedAtUtc?.let(Instant::parse) ?: return this
        val previous = lastSuccessfulImportAtUtc?.let(Instant::parse)
        val effective = listOfNotNull(previous, pendingAt).maxOrNull()
        return copy(lastSuccessfulImportAtUtc = effective?.toString(), pendingImport = null)
    }

    private fun BackupSecurityState.effectiveLastImportAt(): Instant? = listOfNotNull(
        lastSuccessfulImportAtUtc?.let(Instant::parse),
        pendingImport?.startedAtUtc?.let(Instant::parse),
    ).maxOrNull()

    private fun BackupSecurityState.toTimeAnchor(): TrustedTimeAnchor? =
        lastTrustedWallClockUtc?.let { wallClock ->
            TrustedTimeAnchor(
                wallClockUtc = wallClock,
                elapsedRealtimeMs = lastTrustedElapsedRealtimeMs,
                bootSessionId = lastObservedBootMarker,
                tamperFlag = tamperFlag,
            )
        }

    private fun BackupSecurityState.withTimeResolution(resolution: TrustedTimeResolution): BackupSecurityState = copy(
        lastTrustedWallClockUtc = resolution.trustedNow.toString(),
        lastTrustedElapsedRealtimeMs = resolution.timeEvidence.elapsedRealtimeMs,
        lastObservedBootMarker = resolution.timeEvidence.bootSessionId,
        tamperFlag = resolution.tamperDetected,
    )

    companion object {
        private const val KEY_ALIAS = "dstcg_backup_security_key"

        fun fromContext(
            context: Context,
            timeSource: TrustedTimeSource,
        ): BackupSecurityRepository = BackupSecurityRepository(
            dataStore = context.secureBackupSecurityDataStore,
            cipher = AndroidKeystoreProgressCipher(KEY_ALIAS),
            timeSource = timeSource,
        )
    }
}
