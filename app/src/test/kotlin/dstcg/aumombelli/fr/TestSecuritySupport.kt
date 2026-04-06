package fr.aumombelli.dstcg

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import fr.aumombelli.dstcg.data.AesGcmProgressCipher
import fr.aumombelli.dstcg.data.ClockTrustedTimeSource
import fr.aumombelli.dstcg.data.DEFAULT_MAX_STORED_DRAWS
import fr.aumombelli.dstcg.data.ProgressCipher
import fr.aumombelli.dstcg.data.RandomEntropySource
import fr.aumombelli.dstcg.data.StandaloneGameSettings
import fr.aumombelli.dstcg.data.TrustedTimeEvidence
import fr.aumombelli.dstcg.data.TrustedTimeSource
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import kotlin.random.Random
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

fun testGameSettings(
    now: Instant,
    maxStoredDraws: Int = DEFAULT_MAX_STORED_DRAWS,
    elapsedRealtimeMs: Long = 1_000L,
    bootSessionId: String = "test-boot",
    randomSeed: Int = 0,
): StandaloneGameSettings = StandaloneGameSettings(
    maxStoredDraws = maxStoredDraws,
    timeSource = ClockTrustedTimeSource(
        clock = Clock.fixed(now, ZoneOffset.UTC),
        elapsedRealtimeMsProvider = { elapsedRealtimeMs },
        bootSessionIdProvider = { bootSessionId },
    ),
    entropySource = RandomEntropySource(Random(randomSeed)),
)

class MutableTrustedTimeSource(
    wallClockUtc: Instant,
    elapsedRealtimeMs: Long = 0L,
    bootSessionId: String = "test-boot",
) : TrustedTimeSource {
    var currentEvidence: TrustedTimeEvidence = TrustedTimeEvidence(
        wallClockUtc = wallClockUtc,
        elapsedRealtimeMs = elapsedRealtimeMs,
        bootSessionId = bootSessionId,
    )

    override fun now(): TrustedTimeEvidence = currentEvidence

    fun advanceBy(duration: Duration) {
        currentEvidence = currentEvidence.copy(
            wallClockUtc = currentEvidence.wallClockUtc.plus(duration),
            elapsedRealtimeMs = currentEvidence.elapsedRealtimeMs + duration.toMillis(),
        )
    }

    fun withWallClock(wallClockUtc: Instant) {
        currentEvidence = currentEvidence.copy(wallClockUtc = wallClockUtc)
    }

    fun reboot(
        wallClockUtc: Instant = currentEvidence.wallClockUtc,
        elapsedRealtimeMs: Long = 0L,
        bootSessionId: String = currentEvidence.bootSessionId + "-reboot",
    ) {
        currentEvidence = TrustedTimeEvidence(
            wallClockUtc = wallClockUtc,
            elapsedRealtimeMs = elapsedRealtimeMs,
            bootSessionId = bootSessionId,
        )
    }
}

fun newTestProgressCipher(secretKey: SecretKey = newTestSecretKey()): ProgressCipher =
    AesGcmProgressCipher(keyProvider = { secretKey })

fun newTestSecretKey(): SecretKey =
    KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()

fun inMemoryPreferencesDataStore(): DataStore<Preferences> =
    InMemoryDataStore(emptyPreferences())

fun <T> inMemoryDataStore(initialValue: T): DataStore<T> =
    InMemoryDataStore(initialValue)

private class InMemoryDataStore<T>(
    initialValue: T,
) : DataStore<T> {
    private val mutex = Mutex()
    private val state = MutableStateFlow(initialValue)

    override val data: Flow<T> = state

    override suspend fun updateData(transform: suspend (t: T) -> T): T = mutex.withLock {
        val updatedValue = transform(state.value)
        state.value = updatedValue
        updatedValue
    }
}
