package fr.aumombelli.dstcg

import fr.aumombelli.dstcg.data.nextUtcDayStartCompat
import fr.aumombelli.dstcg.data.toUtcLocalDateCompat
import fr.aumombelli.dstcg.data.toUtcStartInstantCompat
import java.time.Instant
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class UtcDateTimeTest {
    @Test
    fun `instant is converted to utc date around midnight`() {
        assertEquals(
            LocalDate.of(2026, 3, 24),
            Instant.parse("2026-03-24T23:59:59Z").toUtcLocalDateCompat(),
        )
        assertEquals(
            LocalDate.of(2026, 3, 25),
            Instant.parse("2026-03-25T00:00:00Z").toUtcLocalDateCompat(),
        )
    }

    @Test
    fun `next utc day start is stable at and before midnight`() {
        assertEquals(
            Instant.parse("2026-03-25T00:00:00Z"),
            Instant.parse("2026-03-24T23:59:59Z").nextUtcDayStartCompat(),
        )
        assertEquals(
            Instant.parse("2026-03-26T00:00:00Z"),
            Instant.parse("2026-03-25T00:00:00Z").nextUtcDayStartCompat(),
        )
    }

    @Test
    fun `local date is converted to utc day start`() {
        assertEquals(
            Instant.parse("2026-03-25T00:00:00Z"),
            LocalDate.of(2026, 3, 25).toUtcStartInstantCompat(),
        )
    }
}
