package fr.aumombelli.dstcg.feature.packs.selection

import java.time.Duration

internal fun formatRemainingDuration(remainingDuration: Duration?): String? {
    val duration = remainingDuration ?: return null
    val totalSeconds = duration.seconds.coerceAtLeast(0)
    val hours = totalSeconds / 3_600
    val minutes = (totalSeconds % 3_600) / 60
    val seconds = totalSeconds % 60

    return when {
        hours > 0 && minutes > 0 -> "${hours}h ${minutes}min"
        hours > 0 -> "${hours}h"
        minutes > 0 -> "${minutes}min"
        else -> "${seconds}s"
    }
}
