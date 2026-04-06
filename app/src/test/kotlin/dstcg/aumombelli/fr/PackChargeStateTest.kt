package fr.aumombelli.dstcg

import fr.aumombelli.dstcg.data.DeterministicWeatherCalendar
import fr.aumombelli.dstcg.data.WeatherState
import fr.aumombelli.dstcg.data.buildPackChargeUiStatus
import fr.aumombelli.dstcg.data.normalizePackRechargeState
import fr.aumombelli.dstcg.model.PackRechargeState
import java.time.Duration
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PackChargeStateTest {
    private val drawCooldown: Duration = Duration.ofHours(6)
    private val maxStoredDraws = 10

    @Test
    fun `rain freezes charge accumulation`() {
        val now = Instant.parse("2026-01-04T02:00:00Z")
        val rechargeState = PackRechargeState(
            availableDrawCount = 0,
            accumulatedChargeUnits = 36_000L,
            lastChargeEvaluationAt = "2026-01-04T00:00:00Z",
        )

        val normalized = normalizePackRechargeState(
            rechargeState = rechargeState,
            now = now,
            drawCooldown = drawCooldown,
            maxStoredDraws = maxStoredDraws,
            weatherPolicy = DeterministicWeatherCalendar,
        )
        val chargeStatus = buildPackChargeUiStatus(
            rechargeState = normalized,
            now = now,
            drawCooldown = drawCooldown,
            maxStoredDraws = maxStoredDraws,
            weatherPolicy = DeterministicWeatherCalendar,
        )

        assertEquals(36_000L, normalized.accumulatedChargeUnits)
        assertEquals(WeatherState.Rain, chargeStatus.currentWeather)
        assertEquals("2026-01-05T04:00:00Z", chargeStatus.nextChargeAt)
    }

    @Test
    fun `cloudy weather slows recharge`() {
        val now = Instant.parse("2026-01-06T01:00:00Z")
        val rechargeState = PackRechargeState(
            availableDrawCount = 0,
            accumulatedChargeUnits = 0L,
            lastChargeEvaluationAt = "2026-01-06T00:00:00Z",
        )

        val normalized = normalizePackRechargeState(
            rechargeState = rechargeState,
            now = now,
            drawCooldown = drawCooldown,
            maxStoredDraws = maxStoredDraws,
            weatherPolicy = DeterministicWeatherCalendar,
        )
        val chargeStatus = buildPackChargeUiStatus(
            rechargeState = normalized,
            now = now,
            drawCooldown = drawCooldown,
            maxStoredDraws = maxStoredDraws,
            weatherPolicy = DeterministicWeatherCalendar,
        )

        assertEquals(14_400L, normalized.accumulatedChargeUnits)
        assertEquals(WeatherState.Cloudy, chargeStatus.currentWeather)
        assertEquals("2026-01-06T07:30:00Z", chargeStatus.nextChargeAt)
    }

    @Test
    fun `pure weather accelerates recharge`() {
        val now = Instant.parse("2026-01-11T01:00:00Z")
        val rechargeState = PackRechargeState(
            availableDrawCount = 0,
            accumulatedChargeUnits = 0L,
            lastChargeEvaluationAt = "2026-01-11T00:00:00Z",
        )

        val normalized = normalizePackRechargeState(
            rechargeState = rechargeState,
            now = now,
            drawCooldown = drawCooldown,
            maxStoredDraws = maxStoredDraws,
            weatherPolicy = DeterministicWeatherCalendar,
        )
        val chargeStatus = buildPackChargeUiStatus(
            rechargeState = normalized,
            now = now,
            drawCooldown = drawCooldown,
            maxStoredDraws = maxStoredDraws,
            weatherPolicy = DeterministicWeatherCalendar,
        )

        assertEquals(36_000L, normalized.accumulatedChargeUnits)
        assertEquals(WeatherState.Pure, chargeStatus.currentWeather)
        assertEquals("2026-01-11T03:00:00Z", chargeStatus.nextChargeAt)
    }

    @Test
    fun `crossing midnight applies the next UTC weather state`() {
        val now = Instant.parse("2026-01-04T02:00:00Z")
        val rechargeState = PackRechargeState(
            availableDrawCount = 0,
            accumulatedChargeUnits = 0L,
            lastChargeEvaluationAt = "2026-01-03T22:00:00Z",
        )

        val normalized = normalizePackRechargeState(
            rechargeState = rechargeState,
            now = now,
            drawCooldown = drawCooldown,
            maxStoredDraws = maxStoredDraws,
            weatherPolicy = DeterministicWeatherCalendar,
        )
        val chargeStatus = buildPackChargeUiStatus(
            rechargeState = normalized,
            now = now,
            drawCooldown = drawCooldown,
            maxStoredDraws = maxStoredDraws,
            weatherPolicy = DeterministicWeatherCalendar,
        )

        assertEquals(36_000L, normalized.accumulatedChargeUnits)
        assertEquals(WeatherState.Rain, chargeStatus.currentWeather)
        assertEquals("2026-01-05T04:00:00Z", chargeStatus.nextChargeAt)
    }

    @Test
    fun `normalization caps the stock and clears charge tracking when full`() {
        val normalized = normalizePackRechargeState(
            rechargeState = PackRechargeState(
                availableDrawCount = 9,
                accumulatedChargeUnits = 50_000L,
                lastChargeEvaluationAt = "2026-01-11T00:00:00Z",
            ),
            now = Instant.parse("2026-01-11T03:00:00Z"),
            drawCooldown = drawCooldown,
            maxStoredDraws = maxStoredDraws,
            weatherPolicy = DeterministicWeatherCalendar,
        )

        assertEquals(10, normalized.availableDrawCount)
        assertEquals(0L, normalized.accumulatedChargeUnits)
        assertNull(normalized.lastChargeEvaluationAt)
    }
}
