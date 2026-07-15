package fr.aumombelli.dstcg

import fr.aumombelli.dstcg.data.BackupCodec
import fr.aumombelli.dstcg.data.BackupInput
import fr.aumombelli.dstcg.data.BackupRepository
import fr.aumombelli.dstcg.data.BackupSecurityRepository
import fr.aumombelli.dstcg.data.EncryptedProgressEnvelope
import fr.aumombelli.dstcg.data.ProgressGateway
import fr.aumombelli.dstcg.data.ProgressLoadResult
import fr.aumombelli.dstcg.data.BackupTooOldException
import fr.aumombelli.dstcg.model.OwnedCollection
import fr.aumombelli.dstcg.model.StandaloneProgress
import java.time.Duration
import java.time.Instant
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
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

    private fun fixture(): Fixture {
        val clock = MutableTrustedTimeSource(initialNow, elapsedRealtimeMs = 1_000L)
        val security = BackupSecurityRepository(
            dataStore = inMemoryDataStore(EncryptedProgressEnvelope()),
            cipher = newTestProgressCipher(),
            timeSource = clock,
        )
        val progress = MutableProgressGateway()
        return Fixture(
            clock = clock,
            progress = progress,
            repository = BackupRepository(
                progressRepository = progress,
                securityRepository = security,
                codec = BackupCodec(),
                appVersionCode = 42,
                backupIdFactory = { "backup-${progress.progress.openedPackCount}" },
                previewTokenFactory = { "preview" },
            ),
        )
    }

    private data class Fixture(
        val clock: MutableTrustedTimeSource,
        val progress: MutableProgressGateway,
        val repository: BackupRepository,
    )

    private class MutableProgressGateway : ProgressGateway {
        var progress = StandaloneProgress(collection = OwnedCollection())
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
        override suspend fun restoreProgress(progress: StandaloneProgress) {
            this.progress = progress
        }
    }
}
