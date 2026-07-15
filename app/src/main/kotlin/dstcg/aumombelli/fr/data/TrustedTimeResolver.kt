package fr.aumombelli.dstcg.data

import java.time.Duration
import java.time.Instant

data class TrustedTimeAnchor(
    val wallClockUtc: String,
    val elapsedRealtimeMs: Long,
    val bootSessionId: String,
    val tamperFlag: Boolean = false,
)

data class TrustedTimeResolution(
    val trustedNow: Instant,
    val timeEvidence: TrustedTimeEvidence,
    val tamperDetected: Boolean,
) {
    fun toAnchor(): TrustedTimeAnchor = TrustedTimeAnchor(
        wallClockUtc = trustedNow.toString(),
        elapsedRealtimeMs = timeEvidence.elapsedRealtimeMs,
        bootSessionId = timeEvidence.bootSessionId,
        tamperFlag = tamperDetected,
    )
}

class TrustedTimeResolver(
    private val timeSource: TrustedTimeSource,
    private val tolerance: Duration = Duration.ofMinutes(2),
) {
    fun resolve(anchor: TrustedTimeAnchor?): TrustedTimeResolution {
        val evidence = timeSource.now()
        if (anchor == null) {
            return TrustedTimeResolution(
                trustedNow = evidence.wallClockUtc,
                timeEvidence = evidence,
                tamperDetected = false,
            )
        }

        val storedWallClock = runCatching { Instant.parse(anchor.wallClockUtc) }
            .getOrElse { evidence.wallClockUtc }
        val sameBoot = anchor.bootSessionId == evidence.bootSessionId
        var tamperDetected = anchor.tamperFlag

        val trustedNow = if (sameBoot) {
            val elapsedDeltaMs = evidence.elapsedRealtimeMs - anchor.elapsedRealtimeMs
            if (elapsedDeltaMs < 0L) {
                tamperDetected = true
                storedWallClock
            } else {
                val monotonicWallClock = storedWallClock.plusMillis(elapsedDeltaMs)
                when {
                    evidence.wallClockUtc.isBefore(storedWallClock.minus(tolerance)) -> {
                        tamperDetected = true
                        storedWallClock
                    }

                    evidence.wallClockUtc.isAfter(monotonicWallClock.plus(tolerance)) -> {
                        tamperDetected = true
                        monotonicWallClock
                    }

                    else -> monotonicWallClock
                }
            }
        } else {
            if (evidence.wallClockUtc.isBefore(storedWallClock.minus(tolerance))) {
                tamperDetected = true
                storedWallClock
            } else {
                evidence.wallClockUtc
            }
        }

        return TrustedTimeResolution(
            trustedNow = trustedNow,
            timeEvidence = evidence,
            tamperDetected = tamperDetected,
        )
    }
}
