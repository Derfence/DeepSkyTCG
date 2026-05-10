package fr.aumombelli.dstcg.data

import fr.aumombelli.dstcg.model.MiniGameDifficulty
import fr.aumombelli.dstcg.model.MiniGameId
import fr.aumombelli.dstcg.model.MiniGameResolvedCardRef
import fr.aumombelli.dstcg.model.MiniGameReward
import fr.aumombelli.dstcg.model.MiniGamesProgress
import fr.aumombelli.dstcg.model.dailyStateFor
import fr.aumombelli.dstcg.model.withDailyState
import fr.aumombelli.dstcg.model.withUnlockedDifficulty
import java.time.Instant
import java.time.ZoneOffset

class MiniGamesRepository(
    private val progressRepository: ProgressGateway,
    private val catalogRepository: CatalogGateway,
    private val settings: StandaloneGameSettings,
    private val drawPolicy: MiniGameDeterministicDrawPolicy = MiniGameDeterministicDrawPolicy(),
    private val cardResolver: MiniGameCardResolver = MiniGameCardResolver(drawPolicy),
    private val rewardApplier: MiniGameRewardApplier = MiniGameRewardApplier(),
) : MiniGamesGateway {
    override suspend fun loadMiniGamesState(): MiniGamesState {
        val loadedProgress = progressRepository.loadProgress().requireUsableProgress()
        return MiniGamesState(
            todayUtc = loadedProgress.trustedNow.toMiniGameDateUtc(),
            progress = loadedProgress.progress.miniGamesProgress,
        )
    }

    override suspend fun prepareResolvedCardsForToday(
        miniGameId: MiniGameId,
        slotCount: Int,
        extensionId: String?,
    ): List<MiniGameResolvedCardRef> {
        if (slotCount <= 0) return emptyList()

        val loadedProgress = progressRepository.loadProgress().requireUsableProgress()
        val progress = loadedProgress.progress
        val todayUtc = loadedProgress.trustedNow.toMiniGameDateUtc()
        val currentDailyState = progress.miniGamesProgress.dailyStateFor(
            miniGameId = miniGameId,
            dateUtc = todayUtc,
        )
        if (currentDailyState.resolvedCards.size >= slotCount) {
            return currentDailyState.resolvedCards.take(slotCount)
        }

        val cards = catalogRepository.loadCards()
        val globalCards = drawPolicy.drawGlobalCards(
            cards = cards,
            miniGameId = miniGameId,
            dateUtc = todayUtc,
            slotCount = slotCount,
            extensionId = extensionId,
        )
        check(globalCards.size >= slotCount) {
            "Le catalogue ne contient pas assez de cartes pour preparer $slotCount tirage(s) mini-jeux."
        }

        val resolvedCards = currentDailyState.resolvedCards.toMutableList()
        for (slot in resolvedCards.size until slotCount) {
            val resolvedCard = cardResolver.resolve(
                globalCard = globalCards[slot],
                cards = cards,
                collection = progress.collection,
                miniGameId = miniGameId,
                dateUtc = todayUtc,
                slot = slot,
            )
            checkNotNull(resolvedCard) {
                "Aucune carte possedee disponible pour resoudre le tirage mini-jeux."
            }
            resolvedCards += resolvedCard
        }

        val updatedMiniGamesProgress = progress.miniGamesProgress.withDailyState(
            miniGameId = miniGameId,
            dailyState = currentDailyState.copy(resolvedCards = resolvedCards),
        )
        progressRepository.saveProgress(
            progress.copy(miniGamesProgress = updatedMiniGamesProgress),
        )

        return resolvedCards
    }

    override suspend fun grantRewardForToday(
        miniGameId: MiniGameId,
        reward: MiniGameReward,
    ): MiniGameRewardGrantResult {
        val loadedProgress = progressRepository.loadProgress().requireUsableProgress()
        val progress = loadedProgress.progress
        val drawCooldown = catalogRepository.loadGameBalance().validated().drawCooldownDuration()
        val equipmentCards = catalogRepository.loadEquipmentCards()
        val rechargeMultiplier = resolveActiveEquipmentBonus(
            activeEquipmentByType = progress.activeEquipmentByType,
            equipmentCards = equipmentCards,
        ).rechargeMultiplier
        val result = rewardApplier.grantReward(
            progress = progress,
            miniGameId = miniGameId,
            todayUtc = loadedProgress.trustedNow.toMiniGameDateUtc(),
            reward = reward,
            now = loadedProgress.trustedNow,
            drawCooldown = drawCooldown,
            maxStoredDraws = settings.maxStoredDraws,
            weatherPolicy = settings.weatherPolicy,
            rechargeMultiplier = rechargeMultiplier,
        )

        if (result is MiniGameRewardGrantResult.Granted) {
            progressRepository.saveProgress(
                progress.copy(
                    rechargeState = result.rechargeState,
                    miniGamesProgress = result.miniGamesProgress,
                ),
            )
        }

        return result
    }

    override suspend fun unlockDifficulty(
        miniGameId: MiniGameId,
        difficulty: MiniGameDifficulty,
    ): MiniGamesProgress {
        val loadedProgress = progressRepository.loadProgress().requireUsableProgress()
        val updatedProgress = loadedProgress.progress.miniGamesProgress.withUnlockedDifficulty(
            miniGameId = miniGameId,
            difficulty = difficulty,
        )
        progressRepository.saveProgress(
            loadedProgress.progress.copy(miniGamesProgress = updatedProgress),
        )
        return updatedProgress
    }
}

internal fun Instant.toMiniGameDateUtc(): String =
    atZone(ZoneOffset.UTC).toLocalDate().toString()
