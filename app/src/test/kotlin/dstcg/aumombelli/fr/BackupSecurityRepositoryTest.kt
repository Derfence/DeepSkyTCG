package fr.aumombelli.dstcg

import fr.aumombelli.dstcg.data.BackupSecurityRepository
import fr.aumombelli.dstcg.data.EncryptedProgressEnvelope
import java.time.Duration
import java.time.Instant
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class BackupSecurityRepositoryTest {
    @Test
    fun `pending import is promoted after an interrupted operation`() = runTest {
        val now = Instant.parse("2026-07-15T10:00:00Z")
        val fixture = fixture(now)

        fixture.repository.beginImport("backup-id")
        val recovered = fixture.repository.status()

        assertEquals(now, recovered.lastSuccessfulImportAt)
        assertEquals(now, recovered.trustedNow)
    }

    @Test
    fun `wall clock rollback does not lower trusted time`() = runTest {
        val now = Instant.parse("2026-07-15T10:00:00Z")
        val fixture = fixture(now)
        fixture.repository.status()
        fixture.clock.advanceBy(Duration.ofMinutes(5))
        fixture.clock.withWallClock(now.minus(Duration.ofDays(1)))

        val status = fixture.repository.status()

        assertFalse(status.trustedNow.isBefore(now))
    }

    private fun fixture(now: Instant): Fixture {
        val clock = MutableTrustedTimeSource(now, elapsedRealtimeMs = 10_000L)
        return Fixture(
            clock = clock,
            repository = BackupSecurityRepository(
                dataStore = inMemoryDataStore(EncryptedProgressEnvelope()),
                cipher = newTestProgressCipher(),
                timeSource = clock,
            ),
        )
    }

    private data class Fixture(
        val clock: MutableTrustedTimeSource,
        val repository: BackupSecurityRepository,
    )
}
