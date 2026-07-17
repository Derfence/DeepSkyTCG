package fr.aumombelli.dstcg

import fr.aumombelli.dstcg.data.DeterministicWeatherCalendar
import fr.aumombelli.dstcg.data.WeatherState
import fr.aumombelli.dstcg.data.WeatherPolicy
import fr.aumombelli.dstcg.data.buildPackChargeUiStatus
import fr.aumombelli.dstcg.data.derivedFullStockAt
import fr.aumombelli.dstcg.data.normalizePackRechargeState
import fr.aumombelli.dstcg.model.PackRechargeState
import fr.aumombelli.dstcg.model.ActiveEquipmentEffect
import fr.aumombelli.dstcg.model.EquipmentBonusUnit
import fr.aumombelli.dstcg.model.EquipmentCardDefinition
import fr.aumombelli.dstcg.model.EquipmentType
import fr.aumombelli.dstcg.model.OwnedCollection
import fr.aumombelli.dstcg.model.StandaloneProgress
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

    @Test
    fun `full stock instant is derived from every missing charge`() {
        val now = Instant.parse("2026-02-01T00:00:00Z")
        val progress = StandaloneProgress(
            collection = OwnedCollection(),
            rechargeState = PackRechargeState(
                availableDrawCount = 8,
                lastChargeEvaluationAt = now.toString(),
            ),
        )

        val dueAt = progress.derivedFullStockAt(
            now = now,
            drawCooldown = drawCooldown,
            maxStoredDraws = maxStoredDraws,
            weatherPolicy = AlwaysClearWeather,
            equipmentCards = emptyList(),
        )

        assertEquals(Instant.parse("2026-02-01T12:00:00Z"), dueAt)
    }

    @Test
    fun `full stock instant accounts for accumulated partial charge`() {
        val now = Instant.parse("2026-02-01T00:00:00Z")
        val progress = StandaloneProgress(
            collection = OwnedCollection(),
            rechargeState = PackRechargeState(
                availableDrawCount = 9,
                accumulatedChargeUnits = 54_000L,
                lastChargeEvaluationAt = now.toString(),
            ),
        )

        val dueAt = progress.derivedFullStockAt(
            now = now,
            drawCooldown = drawCooldown,
            maxStoredDraws = maxStoredDraws,
            weatherPolicy = AlwaysClearWeather,
            equipmentCards = emptyList(),
        )

        assertEquals(Instant.parse("2026-02-01T03:00:00Z"), dueAt)
    }

    @Test
    fun `full stock instant consumes observatory validity between charges`() {
        val now = Instant.parse("2026-02-01T00:00:00Z")
        val observatory = EquipmentCardDefinition(
            id = "observatory-test",
            type = EquipmentType.Observatory,
            displayName = "Observatoire de test",
            level = 1,
            imageRef = "test",
            packsAffected = 1,
            bonusValue = 2.0,
            bonusUnit = EquipmentBonusUnit.RechargeMultiplier,
            dropWeight = 1,
            description = "Double une recharge.",
        )
        val progress = StandaloneProgress(
            collection = OwnedCollection(),
            rechargeState = PackRechargeState(
                availableDrawCount = 8,
                lastChargeEvaluationAt = now.toString(),
            ),
            activeEquipmentByType = mapOf(
                EquipmentType.Observatory to ActiveEquipmentEffect(
                    equipmentCardId = observatory.id,
                    equipmentType = EquipmentType.Observatory,
                    packsRemaining = 1,
                ),
            ),
        )

        val dueAt = progress.derivedFullStockAt(
            now = now,
            drawCooldown = drawCooldown,
            maxStoredDraws = maxStoredDraws,
            weatherPolicy = AlwaysClearWeather,
            equipmentCards = listOf(observatory),
        )

        assertEquals(Instant.parse("2026-02-01T09:00:00Z"), dueAt)
    }

    @Test
    fun `full stock has no future notification instant`() {
        val progress = StandaloneProgress(
            collection = OwnedCollection(),
            rechargeState = PackRechargeState(availableDrawCount = maxStoredDraws),
        )

        assertNull(
            progress.derivedFullStockAt(
                now = Instant.parse("2026-02-01T00:00:00Z"),
                drawCooldown = drawCooldown,
                maxStoredDraws = maxStoredDraws,
                weatherPolicy = AlwaysClearWeather,
                equipmentCards = emptyList(),
            ),
        )
    }

    private object AlwaysClearWeather : WeatherPolicy {
        override fun weatherAt(instant: Instant): WeatherState = WeatherState.Clear
    }
}
