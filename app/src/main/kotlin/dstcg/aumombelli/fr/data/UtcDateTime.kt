package fr.aumombelli.dstcg.data

import java.time.Instant
import java.time.LocalDate

private const val SECONDS_PER_UTC_DAY = 86_400L

internal fun Instant.toUtcLocalDateCompat(): LocalDate =
    LocalDate.ofEpochDay(Math.floorDiv(epochSecond, SECONDS_PER_UTC_DAY))

internal fun Instant.nextUtcDayStartCompat(): Instant {
    val nextEpochDay = Math.floorDiv(epochSecond, SECONDS_PER_UTC_DAY) + 1L
    return Instant.ofEpochSecond(nextEpochDay * SECONDS_PER_UTC_DAY)
}

internal fun LocalDate.toUtcStartInstantCompat(): Instant =
    Instant.ofEpochSecond(toEpochDay() * SECONDS_PER_UTC_DAY)
