package fr.aumombelli.dstcg

import fr.aumombelli.dstcg.data.DeterministicWeatherCalendar
import fr.aumombelli.dstcg.data.WeatherState
import java.time.Instant
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class DeterministicWeatherCalendarTest {
    @Test
    fun `weather is deterministic for the same UTC date`() {
        val morning = Instant.parse("2026-01-04T00:15:00Z")
        val evening = Instant.parse("2026-01-04T23:45:00Z")

        assertEquals(
            DeterministicWeatherCalendar.weatherAt(morning),
            DeterministicWeatherCalendar.weatherAt(evening),
        )
        assertEquals(WeatherState.Rain, DeterministicWeatherCalendar.weatherAt(morning))
    }

    @Test
    fun `weather cycle repeats every twenty days`() {
        val start = LocalDate.of(2026, 1, 1)
        repeat(20) { dayOffset ->
            assertEquals(
                DeterministicWeatherCalendar.weatherOn(start.plusDays(dayOffset.toLong())),
                DeterministicWeatherCalendar.weatherOn(start.plusDays(dayOffset.toLong() + 20L)),
            )
        }
    }

    @Test
    fun `weather cycle keeps an average recharge multiplier of one point eleven`() {
        val start = LocalDate.of(2026, 1, 1)
        val averageUnitsPerSecond = (0 until 20)
            .map { DeterministicWeatherCalendar.weatherOn(start.plusDays(it.toLong())).rechargeUnitsPerSecond }
            .average()

        assertEquals(5.55, averageUnitsPerSecond, 0.0001)
    }
}
