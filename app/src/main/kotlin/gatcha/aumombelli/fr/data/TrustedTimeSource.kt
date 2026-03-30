package fr.aumombelli.gatcha.data

import android.content.Context
import android.os.SystemClock
import android.provider.Settings
import java.time.Clock
import java.time.Instant

data class TrustedTimeEvidence(
    val wallClockUtc: Instant,
    val elapsedRealtimeMs: Long,
    val bootSessionId: String,
)

interface TrustedTimeSource {
    fun now(): TrustedTimeEvidence
}

class ClockTrustedTimeSource(
    private val clock: Clock,
    private val elapsedRealtimeMsProvider: () -> Long = { 0L },
    private val bootSessionIdProvider: () -> String = { "clock" },
) : TrustedTimeSource {
    override fun now(): TrustedTimeEvidence = TrustedTimeEvidence(
        wallClockUtc = clock.instant(),
        elapsedRealtimeMs = elapsedRealtimeMsProvider().coerceAtLeast(0L),
        bootSessionId = bootSessionIdProvider(),
    )
}

class AndroidTrustedTimeSource(
    private val context: Context,
    private val clock: Clock = Clock.systemUTC(),
) : TrustedTimeSource {
    override fun now(): TrustedTimeEvidence = TrustedTimeEvidence(
        wallClockUtc = clock.instant(),
        elapsedRealtimeMs = SystemClock.elapsedRealtime(),
        bootSessionId = resolveBootSessionId(),
    )

    private fun resolveBootSessionId(): String {
        val bootCount = runCatching {
            Settings.Global.getInt(context.contentResolver, Settings.Global.BOOT_COUNT)
        }.getOrElse { 0 }
        return bootCount.toString()
    }
}
