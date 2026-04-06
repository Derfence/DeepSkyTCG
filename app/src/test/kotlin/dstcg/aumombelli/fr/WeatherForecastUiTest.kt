package fr.aumombelli.dstcg

import fr.aumombelli.dstcg.data.DeterministicWeatherCalendar
import fr.aumombelli.dstcg.feature.packs.selection.buildWeatherForecastDayUiModels
import fr.aumombelli.dstcg.feature.packs.selection.formatWeatherForecastUtcTimeLabel
import java.time.Instant
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WeatherForecastUiTest {
    @Test
    fun `forecast builder returns seven ordered utc days with french labels`() {
        val forecast = buildWeatherForecastDayUiModels(
            now = Instant.parse("2026-03-24T12:00:00Z"),
            weatherPolicy = DeterministicWeatherCalendar,
        )

        assertEquals(7, forecast.size)
        assertEquals(
            listOf(
                LocalDate.of(2026, 3, 24),
                LocalDate.of(2026, 3, 25),
                LocalDate.of(2026, 3, 26),
                LocalDate.of(2026, 3, 27),
                LocalDate.of(2026, 3, 28),
                LocalDate.of(2026, 3, 29),
                LocalDate.of(2026, 3, 30),
            ),
            forecast.map { it.dateUtc },
        )
        assertEquals(
            listOf("Mar", "Mer", "Jeu", "Ven", "Sam", "Dim", "Lun"),
            forecast.map { it.dayLabel },
        )
        assertEquals(
            listOf("x1", "x0", "x1", "x0.8", "x1", "x0.8", "x1"),
            forecast.map { it.multiplierLabel },
        )
    }

    @Test
    fun `forecast builder maps each utc day to the weather policy`() {
        val forecast = buildWeatherForecastDayUiModels(
            now = Instant.parse("2026-03-24T12:00:00Z"),
            weatherPolicy = DeterministicWeatherCalendar,
        )

        forecast.forEach { day ->
            assertEquals(
                DeterministicWeatherCalendar.weatherOn(day.dateUtc),
                day.weatherState,
            )
        }
    }

    @Test
    fun `forecast builder shifts when trusted time crosses utc midnight`() {
        val beforeMidnight = buildWeatherForecastDayUiModels(
            now = Instant.parse("2026-03-24T23:59:59Z"),
            weatherPolicy = DeterministicWeatherCalendar,
        )
        val afterMidnight = buildWeatherForecastDayUiModels(
            now = Instant.parse("2026-03-25T00:00:00Z"),
            weatherPolicy = DeterministicWeatherCalendar,
        )

        assertEquals(LocalDate.of(2026, 3, 24), beforeMidnight.first().dateUtc)
        assertEquals("Mar", beforeMidnight.first().dayLabel)
        assertEquals(LocalDate.of(2026, 3, 25), afterMidnight.first().dateUtc)
        assertEquals("Mer", afterMidnight.first().dayLabel)
        assertTrue(
            beforeMidnight.drop(1).map { it.dateUtc } == afterMidnight.dropLast(1).map { it.dateUtc },
        )
    }

    @Test
    fun `utc time label is formatted with a stable hh mm utc pattern`() {
        assertEquals(
            "12:34 UTC",
            formatWeatherForecastUtcTimeLabel(Instant.parse("2026-03-24T12:34:56Z")),
        )
    }
}
