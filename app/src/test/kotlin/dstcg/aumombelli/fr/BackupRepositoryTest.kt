package fr.aumombelli.dstcg

import fr.aumombelli.dstcg.data.BackupCodec
import fr.aumombelli.dstcg.data.BackupCryptography
import fr.aumombelli.dstcg.data.BackupException
import fr.aumombelli.dstcg.data.BackupFormatException
import fr.aumombelli.dstcg.data.BackupInput
import fr.aumombelli.dstcg.data.BackupRepository
import fr.aumombelli.dstcg.data.BackupSecurityRepository
import fr.aumombelli.dstcg.data.BackupSecurityGateway
import fr.aumombelli.dstcg.data.BackupSecurityStatus
import fr.aumombelli.dstcg.data.EncryptedProgressEnvelope
import fr.aumombelli.dstcg.data.ProgressGateway
import fr.aumombelli.dstcg.data.ProgressLoadResult
import fr.aumombelli.dstcg.data.BackupTooOldException
import fr.aumombelli.dstcg.data.PortableBackupPayload
import fr.aumombelli.dstcg.data.ProgressSnapshot
import fr.aumombelli.dstcg.model.OwnedCollection
import fr.aumombelli.dstcg.model.StandaloneProgress
import java.time.Duration
import java.time.Instant
import java.util.concurrent.Executors
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupRepositoryTest {
    private val password = "phrase secrète robuste"
    private val initialNow = Instant.parse("2026-07-15T10:00:00Z")

    @Test
    fun `successful import records cutoff and restores progression`() = runTest {
        val fixture = fixture()
        fixture.progress.progress = fixture.progress.progress.copy(openedPackCount = 8)
        val document = fixture.repository.exportBackup(password)
        fixture.progress.progress = fixture.progress.progress.copy(openedPackCount = 1)
        fixture.clock.advanceBy(Duration.ofMinutes(1))

        val preview = fixture.repository.inspectBackup(BackupInput(document.bytes), password)
        val result = fixture.repository.importBackup(preview.token)

        assertEquals(8, fixture.progress.progress.openedPackCount)
        assertEquals(fixture.clock.currentEvidence.wallClockUtc, result.importedAt)
        assertEquals(result.importedAt, fixture.repository.lastSuccessfulImportAt.first())
    }

    @Test(expected = BackupTooOldException::class)
    fun `same backup cannot be imported twice`() = runTest {
        val fixture = fixture()
        val document = fixture.repository.exportBackup(password)
        fixture.clock.advanceBy(Duration.ofMinutes(1))
        val preview = fixture.repository.inspectBackup(BackupInput(document.bytes), password)
        fixture.repository.importBackup(preview.token)

        fixture.repository.inspectBackup(BackupInput(document.bytes), password)
    }

    @Test
    fun `export immediately after import is newer than cutoff`() = runTest {
        val fixture = fixture()
        val first = fixture.repository.exportBackup(password)
        fixture.clock.advanceBy(Duration.ofMinutes(1))
        val preview = fixture.repository.inspectBackup(BackupInput(first.bytes), password)
        val imported = fixture.repository.importBackup(preview.token)

        val second = fixture.repository.exportBackup(password)

        assertTrue(second.createdAt.isAfter(imported.importedAt))
    }

    @Test
    fun `progress reset does not erase import cutoff`() = runTest {
        val fixture = fixture()
        val document = fixture.repository.exportBackup(password)
        fixture.clock.advanceBy(Duration.ofMinutes(1))
        val preview = fixture.repository.inspectBackup(BackupInput(document.bytes), password)
        val imported = fixture.repository.importBackup(preview.token)

        fixture.progress.resetProgress()

        assertEquals(imported.importedAt, fixture.repository.lastSuccessfulImportAt.first())
    }

    @Test
    fun `backup created exactly at cutoff is rejected`() = runTest {
        val fixture = fixture()
        val first = fixture.repository.exportBackup(password)
        fixture.clock.advanceBy(Duration.ofMinutes(1))
        val preview = fixture.repository.inspectBackup(BackupInput(first.bytes), password)
        val imported = fixture.repository.importBackup(preview.token)
        val document = BackupCodec().encrypt(
            payload(
                backupId = "equal-date",
                createdAt = imported.importedAt,
                appVersionCode = 42,
                progress = fixture.progress.progress,
            ),
            password,
        )

        assertSuspendThrows(BackupTooOldException::class.java) {
            fixture.repository.inspectBackup(BackupInput(document), password)
        }
    }

    @Test
    fun `backup from a future app version is rejected`() = runTest {
        val fixture = fixture()
        val document = BackupCodec().encrypt(
            payload(
                backupId = "future",
                createdAt = initialNow,
                appVersionCode = 43,
                progress = fixture.progress.progress,
            ),
            password,
        )

        assertSuspendThrows(BackupFormatException::class.java) {
            fixture.repository.inspectBackup(BackupInput(document), password)
        }
    }

    @Test
    fun `failed validation does not restore progression`() = runTest {
        val fixture = fixture()
        fixture.progress.progress = fixture.progress.progress.copy(openedPackCount = 8)
        val document = fixture.repository.exportBackup(password)
        fixture.progress.progress = fixture.progress.progress.copy(openedPackCount = 2)
        fixture.progress.validationFailure = IllegalArgumentException("Référence inconnue")

        assertSuspendThrows(IllegalArgumentException::class.java) {
            fixture.repository.inspectBackup(BackupInput(document.bytes), password)
        }

        assertEquals(2, fixture.progress.progress.openedPackCount)
    }

    @Test
    fun `failed restore cancels pending import without changing cutoff`() = runTest {
        val fixture = fixture()
        val document = fixture.repository.exportBackup(password)
        val preview = fixture.repository.inspectBackup(BackupInput(document.bytes), password)
        fixture.progress.restoreFailure = IllegalStateException("Écriture impossible")

        assertSuspendThrows(IllegalStateException::class.java) {
            fixture.repository.importBackup(preview.token)
        }

        assertNull(fixture.security.status().lastSuccessfulImportAt)
    }

    @Test
    fun `discarded preview cannot be imported`() = runTest {
        val fixture = fixture()
        val document = fixture.repository.exportBackup(password)
        val preview = fixture.repository.inspectBackup(BackupInput(document.bytes), password)

        fixture.repository.discardBackupPreview(preview.token)

        assertSuspendThrows(BackupException::class.java) {
            fixture.repository.importBackup(preview.token)
        }
    }

    @Test
    fun `finalization failure keeps a recoverable pending cutoff`() = runTest {
        val security = FailingCompletionSecurity(initialNow)
        val fixture = fixture(security = security)
        fixture.progress.progress = fixture.progress.progress.copy(openedPackCount = 9)
        val document = fixture.repository.exportBackup(password)
        fixture.progress.progress = fixture.progress.progress.copy(openedPackCount = 1)
        val preview = fixture.repository.inspectBackup(BackupInput(document.bytes), password)

        assertSuspendThrows(IllegalStateException::class.java) {
            fixture.repository.importBackup(preview.token)
        }

        assertEquals(9, fixture.progress.progress.openedPackCount)
        assertEquals(initialNow, security.status().lastSuccessfulImportAt)
    }

    @Test
    fun `cryptography runs on injected dispatcher`() = runTest {
        val executor = Executors.newSingleThreadExecutor { runnable ->
            Thread(runnable, "backup-crypto-test")
        }
        val dispatcher = executor.asCoroutineDispatcher()
        try {
            val cryptography = RecordingCryptography()
            val fixture = fixture(codec = cryptography, cryptoDispatcher = dispatcher)

            val document = fixture.repository.exportBackup(password)
            fixture.repository.inspectBackup(BackupInput(document.bytes), password)

            assertEquals(listOf("backup-crypto-test", "backup-crypto-test"), cryptography.threadNames)
        } finally {
            dispatcher.close()
            executor.shutdownNow()
        }
    }

    private fun fixture(
        codec: BackupCryptography = BackupCodec(),
        cryptoDispatcher: CoroutineDispatcher = Dispatchers.Default,
        security: BackupSecurityGateway? = null,
    ): Fixture {
        val clock = MutableTrustedTimeSource(initialNow, elapsedRealtimeMs = 1_000L)
        val resolvedSecurity = security ?: BackupSecurityRepository(
            dataStore = inMemoryDataStore(EncryptedProgressEnvelope()),
            cipher = newTestProgressCipher(),
            timeSource = clock,
        )
        val progress = MutableProgressGateway()
        return Fixture(
            clock = clock,
            progress = progress,
            security = resolvedSecurity,
            repository = BackupRepository(
                progressRepository = progress,
                securityRepository = resolvedSecurity,
                codec = codec,
                appVersionCode = 42,
                cryptoDispatcher = cryptoDispatcher,
                backupIdFactory = { "backup-${progress.progress.openedPackCount}" },
                previewTokenFactory = { "preview" },
            ),
        )
    }

    private data class Fixture(
        val clock: MutableTrustedTimeSource,
        val progress: MutableProgressGateway,
        val security: BackupSecurityGateway,
        val repository: BackupRepository,
    )

    private class MutableProgressGateway : ProgressGateway {
        var progress = StandaloneProgress(collection = OwnedCollection())
        var validationFailure: Throwable? = null
        var restoreFailure: Throwable? = null
        private val trustedNow = Instant.parse("2026-07-15T10:00:00Z")

        override suspend fun loadProgress(): ProgressLoadResult = ProgressLoadResult.Ok(progress, trustedNow)
        override suspend fun saveProgress(progress: StandaloneProgress) {
            this.progress = progress
        }
        override suspend fun updateProgress(transform: (StandaloneProgress) -> StandaloneProgress) {
            progress = transform(progress)
        }
        override suspend fun resetProgress() {
            progress = StandaloneProgress(collection = OwnedCollection())
        }
        override suspend fun resetNewPlayerOnboarding() = Unit
        override suspend fun validateRestorableProgress(progress: StandaloneProgress) {
            validationFailure?.let { throw it }
        }
        override suspend fun restoreProgress(progress: StandaloneProgress) {
            restoreFailure?.let { throw it }
            this.progress = progress
        }
    }

    private fun payload(
        backupId: String,
        createdAt: Instant,
        appVersionCode: Int,
        progress: StandaloneProgress,
    ): PortableBackupPayload = PortableBackupPayload(
        backupId = backupId,
        createdAtUtc = createdAt.toString(),
        appVersionCode = appVersionCode,
        progressSchemaVersion = ProgressSnapshot.CURRENT_SCHEMA_VERSION,
        progress = progress,
    )

    private suspend fun <T : Throwable> assertSuspendThrows(
        type: Class<T>,
        block: suspend () -> Unit,
    ): T = try {
        block()
        throw AssertionError("${type.simpleName} attendue.")
    } catch (exception: Throwable) {
        if (!type.isInstance(exception)) throw exception
        type.cast(exception)
    }

    private class RecordingCryptography : BackupCryptography {
        private val delegate = BackupCodec()
        val threadNames = mutableListOf<String>()

        override fun encrypt(payload: PortableBackupPayload, password: String): ByteArray {
            threadNames += Thread.currentThread().name
            return delegate.encrypt(payload, password)
        }

        override fun decrypt(bytes: ByteArray, password: String): PortableBackupPayload {
            threadNames += Thread.currentThread().name
            return delegate.decrypt(bytes, password)
        }
    }

    private class FailingCompletionSecurity(
        private val now: Instant,
    ) : BackupSecurityGateway {
        private var cutoff: Instant? = null
        private var pendingBackupId: String? = null

        override val lastSuccessfulImportAt = kotlinx.coroutines.flow.flowOf<Instant?>(null)

        override suspend fun status(): BackupSecurityStatus {
            if (pendingBackupId != null) {
                cutoff = listOfNotNull(cutoff, now).maxOrNull()
                pendingBackupId = null
            }
            return BackupSecurityStatus(cutoff, now)
        }

        override suspend fun beginImport(backupId: String): Instant {
            pendingBackupId = backupId
            return now
        }

        override suspend fun completeImport(backupId: String): Instant {
            throw IllegalStateException("Finalisation impossible")
        }

        override suspend fun cancelImport(backupId: String) {
            if (pendingBackupId == backupId) pendingBackupId = null
        }
    }
}
