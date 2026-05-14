package fr.aumombelli.dstcg.feature.minigames

import fr.aumombelli.dstcg.model.MiniGameDifficulty
import fr.aumombelli.dstcg.model.MiniGameReward
import java.time.Duration

internal fun formatReward(reward: MiniGameReward): String =
    formatRewardDuration(Duration.ofSeconds(reward.reductionSeconds.coerceAtLeast(0L)))

private fun formatRewardDuration(duration: Duration): String {
    val totalSeconds = duration.seconds.coerceAtLeast(0L)
    val hours = totalSeconds / 3600L
    val minutes = (totalSeconds % 3600L) / 60L
    val seconds = totalSeconds % 60L
    return when {
        hours > 0L && minutes > 0L && seconds > 0L -> "${hours}h ${minutes}min ${seconds}s"
        hours > 0L && minutes > 0L -> "${hours}h ${minutes}min"
        hours > 0L && seconds > 0L -> "${hours}h ${seconds}s"
        hours > 0L -> "${hours}h"
        minutes > 0L && seconds > 0L -> "${minutes}min ${seconds}s"
        minutes > 0L -> "${minutes}min"
        seconds > 0L -> "${seconds}s"
        else -> "0min"
    }
}

internal fun memoryDifficultyNameForReward(rewardLabel: String): String =
    MiniGameDifficulty.entries.firstOrNull { formatReward(it.reward) == rewardLabel }?.displayName
        ?: "Memory"
