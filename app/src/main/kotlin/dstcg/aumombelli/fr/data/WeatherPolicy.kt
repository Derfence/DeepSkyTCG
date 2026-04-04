package fr.aumombelli.dstcg.data

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit

/**
 * Deterministic weather states used to modulate pack recharge speed.
 *
 * The recharge rate is expressed in integer units per elapsed second with a
 * base of `5` units per second for the `x1` state.
 */
enum class WeatherState(
    val label: String,
    val multiplierLabel: String,
    val rechargeUnitsPerSecond: Long,
) {
    Rain(
        label = "Pluie",
        multiplierLabel = "x0",
        rechargeUnitsPerSecond = 0L,
    ),
    Cloudy(
        label = "Nuageux",
        multiplierLabel = "x0.8",
        rechargeUnitsPerSecond = 4L,
    ),
    Clear(
        label = "Clair",
        multiplierLabel = "x1",
        rechargeUnitsPerSecond = 5L,
    ),
    Pure(
        label = "Pur",
        multiplierLabel = "x2",
        rechargeUnitsPerSecond = 10L,
    ),
}

/**
 * Abstraction allowing the recharge engine to query the active weather for a
 * trusted instant.
 */
interface WeatherPolicy {
    fun weatherAt(instant: Instant): WeatherState
}

/**
 * Deterministic UTC-only weather cycle shared by every device.
 *
 * The mapping intentionally repeats over a 20-day cycle so the average recharge
 * multiplier remains strictly above 1 (`1.11`) while staying fully offline.
 */
object DeterministicWeatherCalendar : WeatherPolicy {
    private val anchorDate: LocalDate = LocalDate.of(2026, 1, 1)

    override fun weatherAt(instant: Instant): WeatherState =
        weatherOn(LocalDate.ofInstant(instant, ZoneOffset.UTC))

    /**
     * Resolves the weather for a UTC calendar day.
     */
    fun weatherOn(date: LocalDate): WeatherState {
        val dayOffset = ChronoUnit.DAYS.between(anchorDate, date).toInt()
        val slot = Math.floorMod((dayOffset * 11) + 7, 20)
        return when (slot) {
            0 -> WeatherState.Rain
            in 1..4 -> WeatherState.Cloudy
            in 5..15 -> WeatherState.Clear
            else -> WeatherState.Pure
        }
    }
}
