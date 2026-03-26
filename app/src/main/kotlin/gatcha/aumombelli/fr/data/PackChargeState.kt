package fr.aumombelli.gatcha.data

import fr.aumombelli.gatcha.model.StandaloneProgress
import java.time.Duration
import java.time.Instant

internal const val DEFAULT_MAX_STORED_DRAWS = 10
internal val DEFAULT_DRAW_COOLDOWN: Duration = Duration.ofHours(6)

internal data class NormalizedPackChargeState(
    val availableDrawCount: Int,
    val nextChargeAt: Instant?,
)

internal data class PackChargeUiStatus(
    val availableDrawCount: Int,
    val maxStoredDraws: Int,
    val nextChargeAt: String?,
    val remainingDuration: Duration?,
    val rechargeProgress: Float,
    val isDrawLocked: Boolean,
)

internal fun normalizePackChargeState(
    availableDrawCount: Int,
    nextChargeAt: String?,
    now: Instant,
    drawCooldown: Duration,
    maxStoredDraws: Int,
): NormalizedPackChargeState {
    if (maxStoredDraws <= 0) {
        return NormalizedPackChargeState(
            availableDrawCount = 0,
            nextChargeAt = null,
        )
    }
    if (drawCooldown.isZero || drawCooldown.isNegative) {
        return NormalizedPackChargeState(
            availableDrawCount = maxStoredDraws,
            nextChargeAt = null,
        )
    }

    val clampedAvailableDrawCount = availableDrawCount.coerceIn(0, maxStoredDraws)
    if (clampedAvailableDrawCount >= maxStoredDraws) {
        return NormalizedPackChargeState(
            availableDrawCount = maxStoredDraws,
            nextChargeAt = null,
        )
    }

    val parsedNextChargeAt = nextChargeAt
        ?.let { runCatching { Instant.parse(it) }.getOrNull() }
        ?: now.plus(drawCooldown)
    if (now.isBefore(parsedNextChargeAt)) {
        return NormalizedPackChargeState(
            availableDrawCount = clampedAvailableDrawCount,
            nextChargeAt = parsedNextChargeAt,
        )
    }

    val drawCooldownMillis = drawCooldown.toMillis()
    val elapsedMillis = Duration.between(parsedNextChargeAt, now).toMillis()
    val recoveredDrawCount = 1 + (elapsedMillis / drawCooldownMillis)
    val updatedAvailableDrawCount = (clampedAvailableDrawCount + recoveredDrawCount.toInt())
        .coerceAtMost(maxStoredDraws)
    val updatedNextChargeAt = if (updatedAvailableDrawCount >= maxStoredDraws) {
        null
    } else {
        parsedNextChargeAt.plusMillis(recoveredDrawCount * drawCooldownMillis)
    }

    return NormalizedPackChargeState(
        availableDrawCount = updatedAvailableDrawCount,
        nextChargeAt = updatedNextChargeAt,
    )
}

internal fun consumePackCharge(
    normalizedState: NormalizedPackChargeState,
    now: Instant,
    drawCooldown: Duration,
    maxStoredDraws: Int,
): NormalizedPackChargeState {
    if (maxStoredDraws <= 0) {
        return NormalizedPackChargeState(availableDrawCount = 0, nextChargeAt = null)
    }
    if (drawCooldown.isZero || drawCooldown.isNegative) {
        return NormalizedPackChargeState(
            availableDrawCount = maxStoredDraws,
            nextChargeAt = null,
        )
    }

    val wasFull = normalizedState.availableDrawCount >= maxStoredDraws
    val updatedAvailableDrawCount = (normalizedState.availableDrawCount - 1).coerceAtLeast(0)
    val updatedNextChargeAt = when {
        updatedAvailableDrawCount >= maxStoredDraws -> null
        wasFull -> now.plus(drawCooldown)
        else -> normalizedState.nextChargeAt ?: now.plus(drawCooldown)
    }

    return NormalizedPackChargeState(
        availableDrawCount = updatedAvailableDrawCount,
        nextChargeAt = updatedNextChargeAt,
    )
}

internal fun buildPackChargeUiStatus(
    availableDrawCount: Int,
    nextChargeAt: String?,
    now: Instant,
    drawCooldown: Duration,
    maxStoredDraws: Int,
): PackChargeUiStatus {
    val normalizedState = normalizePackChargeState(
        availableDrawCount = availableDrawCount,
        nextChargeAt = nextChargeAt,
        now = now,
        drawCooldown = drawCooldown,
        maxStoredDraws = maxStoredDraws,
    )
    val remainingDuration = when {
        normalizedState.availableDrawCount >= maxStoredDraws -> null
        normalizedState.nextChargeAt == null -> null
        else -> Duration.between(now, normalizedState.nextChargeAt)
            .takeUnless { it.isNegative }
            ?: Duration.ZERO
    }
    val rechargeProgress = when {
        maxStoredDraws <= 0 -> 0f
        normalizedState.availableDrawCount >= maxStoredDraws -> 1f
        drawCooldown.isZero || drawCooldown.isNegative -> 1f
        remainingDuration == null -> 1f
        else -> (1f - (remainingDuration.toMillis().toFloat() / drawCooldown.toMillis().toFloat()))
            .coerceIn(0f, 1f)
    }

    return PackChargeUiStatus(
        availableDrawCount = normalizedState.availableDrawCount,
        maxStoredDraws = maxStoredDraws,
        nextChargeAt = normalizedState.nextChargeAt?.toString(),
        remainingDuration = remainingDuration,
        rechargeProgress = rechargeProgress,
        isDrawLocked = normalizedState.availableDrawCount == 0,
    )
}

internal fun StandaloneProgress.withNormalizedPackCharge(
    now: Instant,
    drawCooldown: Duration,
    maxStoredDraws: Int,
): StandaloneProgress {
    val normalizedState = normalizePackChargeState(
        availableDrawCount = availableDrawCount,
        nextChargeAt = nextChargeAt,
        now = now,
        drawCooldown = drawCooldown,
        maxStoredDraws = maxStoredDraws,
    )
    return copy(
        availableDrawCount = normalizedState.availableDrawCount,
        nextChargeAt = normalizedState.nextChargeAt?.toString(),
    )
}
