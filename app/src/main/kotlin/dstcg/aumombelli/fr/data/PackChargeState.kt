package fr.aumombelli.dstcg.data

import fr.aumombelli.dstcg.model.PackRechargeState
import fr.aumombelli.dstcg.model.StandaloneProgress
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import kotlin.math.ceil

internal const val DEFAULT_MAX_STORED_DRAWS = 10
internal val DEFAULT_DRAW_COOLDOWN: Duration = Duration.ofHours(6)
private const val BASE_RECHARGE_UNITS_PER_SECOND = 5L

/**
 * Derived presentation model for pack recharge.
 *
 * The persisted source of truth remains [PackRechargeState]. This DTO exists so
 * UI code can consume already-normalized values without duplicating any recharge
 * or weather rules.
 */
internal data class PackChargeUiStatus(
    val rechargeState: PackRechargeState,
    val currentWeather: WeatherState,
    val availableDrawCount: Int,
    val maxStoredDraws: Int,
    val nextChargeAt: String?,
    val remainingDuration: Duration?,
    val rechargeProgress: Float,
    val isDrawLocked: Boolean,
)

/**
 * Normalizes a persisted recharge state against a trusted instant.
 *
 * The algorithm advances time in UTC-day segments so each segment uses the
 * weather multiplier of its own UTC date. Recharge progress is stored in exact
 * integer units to avoid floating point drift.
 */
internal fun normalizePackRechargeState(
    rechargeState: PackRechargeState,
    now: Instant,
    drawCooldown: Duration,
    maxStoredDraws: Int,
    weatherPolicy: WeatherPolicy,
): PackRechargeState {
    if (maxStoredDraws <= 0) {
        return PackRechargeState(
            availableDrawCount = 0,
            accumulatedChargeUnits = 0L,
            lastChargeEvaluationAt = null,
        )
    }
    if (drawCooldown.isZero || drawCooldown.isNegative) {
        return PackRechargeState(
            availableDrawCount = maxStoredDraws,
            accumulatedChargeUnits = 0L,
            lastChargeEvaluationAt = null,
        )
    }

    val cooldownUnits = drawCooldown.rechargeCooldownUnits()
    var availableDrawCount = rechargeState.availableDrawCount.coerceIn(0, maxStoredDraws)
    var accumulatedChargeUnits = rechargeState.accumulatedChargeUnits.coerceAtLeast(0L)
    val normalizedNow = now.normalizedRechargeInstant()

    if (availableDrawCount >= maxStoredDraws) {
        return PackRechargeState(
            availableDrawCount = maxStoredDraws,
            accumulatedChargeUnits = 0L,
            lastChargeEvaluationAt = null,
        )
    }

    if (accumulatedChargeUnits >= cooldownUnits) {
        val recoveredDrawCount = (accumulatedChargeUnits / cooldownUnits).toInt()
        availableDrawCount = (availableDrawCount + recoveredDrawCount).coerceAtMost(maxStoredDraws)
        accumulatedChargeUnits %= cooldownUnits
        if (availableDrawCount >= maxStoredDraws) {
            return PackRechargeState(
                availableDrawCount = maxStoredDraws,
                accumulatedChargeUnits = 0L,
                lastChargeEvaluationAt = null,
            )
        }
    }

    val lastChargeEvaluationAt = rechargeState.lastChargeEvaluationAt
        ?.let { runCatching { Instant.parse(it) }.getOrNull() }
        ?.normalizedRechargeInstant()
        ?: normalizedNow

    if (!normalizedNow.isAfter(lastChargeEvaluationAt)) {
        return PackRechargeState(
            availableDrawCount = availableDrawCount,
            accumulatedChargeUnits = accumulatedChargeUnits.coerceIn(0L, cooldownUnits - 1L),
            lastChargeEvaluationAt = lastChargeEvaluationAt.toString(),
        )
    }

    var cursor = lastChargeEvaluationAt
    while (cursor.isBefore(normalizedNow) && availableDrawCount < maxStoredDraws) {
        val nextDayStart = cursor.nextUtcDayStart()
        val segmentEnd = minOf(nextDayStart, normalizedNow)
        val elapsedSeconds = Duration.between(cursor, segmentEnd).seconds.coerceAtLeast(0L)
        val unitsPerSecond = weatherPolicy.weatherAt(cursor).rechargeUnitsPerSecond
        if (elapsedSeconds > 0L && unitsPerSecond > 0L) {
            accumulatedChargeUnits += elapsedSeconds * unitsPerSecond
            if (accumulatedChargeUnits >= cooldownUnits) {
                val recoveredDrawCount = (accumulatedChargeUnits / cooldownUnits).toInt()
                availableDrawCount = (availableDrawCount + recoveredDrawCount).coerceAtMost(maxStoredDraws)
                accumulatedChargeUnits %= cooldownUnits
                if (availableDrawCount >= maxStoredDraws) {
                    return PackRechargeState(
                        availableDrawCount = maxStoredDraws,
                        accumulatedChargeUnits = 0L,
                        lastChargeEvaluationAt = null,
                    )
                }
            }
        }
        cursor = segmentEnd
    }

    return PackRechargeState(
        availableDrawCount = availableDrawCount,
        accumulatedChargeUnits = accumulatedChargeUnits.coerceIn(0L, cooldownUnits - 1L),
        lastChargeEvaluationAt = normalizedNow.toString(),
    )
}

/**
 * Consumes one immediately available draw from an already-normalized state.
 */
internal fun consumePackCharge(
    normalizedState: PackRechargeState,
    now: Instant,
    drawCooldown: Duration,
    maxStoredDraws: Int,
): PackRechargeState {
    if (maxStoredDraws <= 0) {
        return PackRechargeState(
            availableDrawCount = 0,
            accumulatedChargeUnits = 0L,
            lastChargeEvaluationAt = null,
        )
    }
    if (drawCooldown.isZero || drawCooldown.isNegative) {
        return PackRechargeState(
            availableDrawCount = maxStoredDraws,
            accumulatedChargeUnits = 0L,
            lastChargeEvaluationAt = null,
        )
    }

    val updatedAvailableDrawCount = (normalizedState.availableDrawCount - 1).coerceAtLeast(0)
    return PackRechargeState(
        availableDrawCount = updatedAvailableDrawCount,
        accumulatedChargeUnits = normalizedState.accumulatedChargeUnits,
        lastChargeEvaluationAt = if (updatedAvailableDrawCount >= maxStoredDraws) {
            null
        } else {
            now.normalizedRechargeInstant().toString()
        },
    )
}

/**
 * Builds the complete UI status for the pack screen from the persisted recharge
 * state and the central recharge engine.
 */
internal fun buildPackChargeUiStatus(
    rechargeState: PackRechargeState,
    now: Instant,
    drawCooldown: Duration,
    maxStoredDraws: Int,
    weatherPolicy: WeatherPolicy,
): PackChargeUiStatus {
    val normalizedNow = now.normalizedRechargeInstant()
    val normalizedRechargeState = normalizePackRechargeState(
        rechargeState = rechargeState,
        now = normalizedNow,
        drawCooldown = drawCooldown,
        maxStoredDraws = maxStoredDraws,
        weatherPolicy = weatherPolicy,
    )
    val nextChargeAt = computeNextChargeAt(
        rechargeState = normalizedRechargeState,
        now = normalizedNow,
        drawCooldown = drawCooldown,
        maxStoredDraws = maxStoredDraws,
        weatherPolicy = weatherPolicy,
    )
    val remainingDuration = nextChargeAt?.let { Duration.between(normalizedNow, it) }
        ?.takeUnless { it.isNegative }
        ?: if (nextChargeAt == null) null else Duration.ZERO
    val cooldownUnits = drawCooldown.rechargeCooldownUnits().coerceAtLeast(1L)
    val rechargeProgress = when {
        maxStoredDraws <= 0 -> 0f
        normalizedRechargeState.availableDrawCount >= maxStoredDraws -> 1f
        drawCooldown.isZero || drawCooldown.isNegative -> 1f
        else -> (normalizedRechargeState.accumulatedChargeUnits.toFloat() / cooldownUnits.toFloat())
            .coerceIn(0f, 1f)
    }

    return PackChargeUiStatus(
        rechargeState = normalizedRechargeState,
        currentWeather = weatherPolicy.weatherAt(normalizedNow),
        availableDrawCount = normalizedRechargeState.availableDrawCount,
        maxStoredDraws = maxStoredDraws,
        nextChargeAt = nextChargeAt?.toString(),
        remainingDuration = remainingDuration,
        rechargeProgress = rechargeProgress,
        isDrawLocked = normalizedRechargeState.availableDrawCount == 0,
    )
}

/**
 * Returns a copy of the progress whose pack recharge has been normalized for
 * the provided trusted instant.
 */
internal fun StandaloneProgress.withNormalizedPackCharge(
    now: Instant,
    drawCooldown: Duration,
    maxStoredDraws: Int,
    weatherPolicy: WeatherPolicy,
): StandaloneProgress {
    return copy(
        rechargeState = normalizePackRechargeState(
            rechargeState = rechargeState,
            now = now,
            drawCooldown = drawCooldown,
            maxStoredDraws = maxStoredDraws,
            weatherPolicy = weatherPolicy,
        ),
    )
}

/**
 * Derives the next recharge instant from the canonical recharge state.
 */
internal fun PackRechargeState.derivedNextChargeAt(
    now: Instant,
    drawCooldown: Duration,
    maxStoredDraws: Int,
    weatherPolicy: WeatherPolicy,
): Instant? = computeNextChargeAt(
    rechargeState = normalizePackRechargeState(
        rechargeState = this,
        now = now,
        drawCooldown = drawCooldown,
        maxStoredDraws = maxStoredDraws,
        weatherPolicy = weatherPolicy,
    ),
    now = now.normalizedRechargeInstant(),
    drawCooldown = drawCooldown,
    maxStoredDraws = maxStoredDraws,
    weatherPolicy = weatherPolicy,
)

private fun computeNextChargeAt(
    rechargeState: PackRechargeState,
    now: Instant,
    drawCooldown: Duration,
    maxStoredDraws: Int,
    weatherPolicy: WeatherPolicy,
): Instant? {
    if (maxStoredDraws <= 0 || drawCooldown.isZero || drawCooldown.isNegative) {
        return null
    }
    if (rechargeState.availableDrawCount >= maxStoredDraws) {
        return null
    }

    val normalizedNow = now.normalizedRechargeInstant()
    val cooldownUnits = drawCooldown.rechargeCooldownUnits()
    var remainingUnits = (cooldownUnits - rechargeState.accumulatedChargeUnits)
        .coerceIn(1L, cooldownUnits)
    var cursor = rechargeState.lastChargeEvaluationAt
        ?.let { runCatching { Instant.parse(it) }.getOrNull() }
        ?.normalizedRechargeInstant()
        ?: normalizedNow
    if (cursor.isBefore(normalizedNow)) {
        cursor = normalizedNow
    }

    while (true) {
        val nextDayStart = cursor.nextUtcDayStart()
        val unitsPerSecond = weatherPolicy.weatherAt(cursor).rechargeUnitsPerSecond
        if (unitsPerSecond <= 0L) {
            cursor = nextDayStart
            continue
        }

        val secondsUntilBoundary = Duration.between(cursor, nextDayStart).seconds.coerceAtLeast(0L)
        val unitsUntilBoundary = secondsUntilBoundary * unitsPerSecond
        if (unitsUntilBoundary >= remainingUnits) {
            val secondsNeeded = ceil(remainingUnits.toDouble() / unitsPerSecond.toDouble()).toLong()
            return cursor.plusSeconds(secondsNeeded)
        }

        remainingUnits -= unitsUntilBoundary
        cursor = nextDayStart
    }
}

private fun Duration.rechargeCooldownUnits(): Long = seconds.coerceAtLeast(0L) * BASE_RECHARGE_UNITS_PER_SECOND

private fun Instant.normalizedRechargeInstant(): Instant = truncatedTo(ChronoUnit.SECONDS)

private fun Instant.nextUtcDayStart(): Instant =
    LocalDate.ofInstant(this, ZoneOffset.UTC)
        .plusDays(1)
        .atStartOfDay(ZoneOffset.UTC)
        .toInstant()
