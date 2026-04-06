package fr.aumombelli.dstcg.testsupport

import fr.aumombelli.dstcg.model.PackRechargeState
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit

private val androidTestRechargeReferenceNow: Instant = Instant.parse("2026-03-24T12:00:00Z")

internal fun androidTestRechargeStateWithNextChargeAt(
    availableDrawCount: Int,
    nextChargeAt: String?,
    now: Instant = androidTestRechargeReferenceNow,
    drawCooldown: Duration = Duration.ofHours(6),
): PackRechargeState = if (availableDrawCount >= 10 || nextChargeAt == null) {
    PackRechargeState(availableDrawCount = availableDrawCount)
} else {
    val normalizedNow = now.truncatedTo(ChronoUnit.SECONDS)
    val remainingSeconds = Duration.between(
        normalizedNow,
        Instant.parse(nextChargeAt).truncatedTo(ChronoUnit.SECONDS),
    ).seconds.coerceAtLeast(0L)
    val cooldownUnits = drawCooldown.seconds * 5L
    PackRechargeState(
        availableDrawCount = availableDrawCount,
        accumulatedChargeUnits = (cooldownUnits - (remainingSeconds * 5L)).coerceIn(0L, cooldownUnits - 1L),
        lastChargeEvaluationAt = normalizedNow.toString(),
    )
}
