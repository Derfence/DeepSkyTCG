package fr.aumombelli.gatcha.feature.packs.selection

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

internal fun formatNextDrawAt(nextDrawAt: String?): String? {
    val instant = nextDrawAt?.let { runCatching { Instant.parse(it) }.getOrNull() } ?: return null
    if (!instant.isAfter(Instant.now())) return null
    return DateTimeFormatter.ofPattern("dd/MM HH:mm")
        .withZone(ZoneId.systemDefault())
        .format(instant)
}
