package fr.aumombelli.dstcg.data

import fr.aumombelli.dstcg.model.MiniGameDailyState
import fr.aumombelli.dstcg.model.MiniGameId
import fr.aumombelli.dstcg.model.MiniGameReward
import fr.aumombelli.dstcg.model.MiniGamesProgress
import fr.aumombelli.dstcg.model.PackRechargeState
import fr.aumombelli.dstcg.model.StandaloneProgress
import fr.aumombelli.dstcg.model.dailyStateFor
import fr.aumombelli.dstcg.model.withDailyState
import java.time.Duration
import java.time.Instant

sealed interface MiniGameRewardGrantResult {
    val miniGamesProgress: MiniGamesProgress

    data class Granted(
        override val miniGamesProgress: MiniGamesProgress,
        val rechargeState: PackRechargeState,
        val dailyState: MiniGameDailyState,
    ) : MiniGameRewardGrantResult

    data class AlreadyGranted(
        override val miniGamesProgress: MiniGamesProgress,
        val dailyState: MiniGameDailyState,
    ) : MiniGameRewardGrantResult
}

class MiniGameRewardApplier {
    fun grantReward(
        progress: StandaloneProgress,
        miniGameId: MiniGameId,
        todayUtc: String,
        reward: MiniGameReward,
        now: Instant,
        drawCooldown: Duration,
        maxStoredDraws: Int,
        weatherPolicy: WeatherPolicy,
        rechargeMultiplier: Double = 1.0,
    ): MiniGameRewardGrantResult {
        val dailyState = progress.miniGamesProgress.dailyStateFor(
            miniGameId = miniGameId,
            dateUtc = todayUtc,
        )
        if (dailyState.hasPlayed || dailyState.reward != null) {
            return MiniGameRewardGrantResult.AlreadyGranted(
                miniGamesProgress = progress.miniGamesProgress.withDailyState(miniGameId, dailyState),
                dailyState = dailyState,
            )
        }

        val sanitizedReward = MiniGameReward(
            reductionMinutes = reward.reductionMinutes.coerceAtLeast(0L),
        )
        val updatedRechargeState = applyPackRechargeReduction(
            rechargeState = progress.rechargeState,
            now = now,
            reduction = Duration.ofMinutes(sanitizedReward.reductionMinutes),
            drawCooldown = drawCooldown,
            maxStoredDraws = maxStoredDraws,
            weatherPolicy = weatherPolicy,
            rechargeMultiplier = rechargeMultiplier,
        )
        val updatedDailyState = dailyState.copy(
            hasPlayed = true,
            reward = sanitizedReward,
        )

        return MiniGameRewardGrantResult.Granted(
            miniGamesProgress = progress.miniGamesProgress.withDailyState(
                miniGameId = miniGameId,
                dailyState = updatedDailyState,
            ),
            rechargeState = updatedRechargeState,
            dailyState = updatedDailyState,
        )
    }
}
