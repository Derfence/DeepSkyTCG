package fr.aumombelli.dstcg.data

import fr.aumombelli.dstcg.model.CardDefinition
import fr.aumombelli.dstcg.model.CraftingApplyResult
import fr.aumombelli.dstcg.model.CraftingCardCandidate
import fr.aumombelli.dstcg.model.CraftingCardRef
import fr.aumombelli.dstcg.model.CraftingMode
import fr.aumombelli.dstcg.model.DrawPackResponse
import fr.aumombelli.dstcg.model.EquipmentCardDefinition
import fr.aumombelli.dstcg.model.EquipmentSettingsDefinition
import fr.aumombelli.dstcg.model.EquipmentState
import fr.aumombelli.dstcg.model.ExtensionDefinition
import fr.aumombelli.dstcg.model.GameBalanceDefinition
import fr.aumombelli.dstcg.model.MiniGameDifficulty
import fr.aumombelli.dstcg.model.MiniGameId
import fr.aumombelli.dstcg.model.MiniGameResolvedCardRef
import fr.aumombelli.dstcg.model.MiniGameReward
import fr.aumombelli.dstcg.model.MiniGamesProgress
import fr.aumombelli.dstcg.model.OwnedCollection
import fr.aumombelli.dstcg.model.PackCard
import fr.aumombelli.dstcg.model.StandaloneProgress
import fr.aumombelli.dstcg.model.TradeCardCandidate
import fr.aumombelli.dstcg.model.TradeCardRef
import fr.aumombelli.dstcg.model.TradeValidationResult
import fr.aumombelli.dstcg.model.VariantProfile
import kotlinx.coroutines.flow.StateFlow

interface CatalogGateway {
    suspend fun loadExtensions(): List<ExtensionDefinition>
    suspend fun loadCards(): List<CardDefinition>
    suspend fun loadVariantProfiles(): List<VariantProfile>
    suspend fun loadGameBalance(): GameBalanceDefinition
    suspend fun loadEquipmentCards(): List<EquipmentCardDefinition>
    suspend fun loadEquipmentSettings(): EquipmentSettingsDefinition
}

interface ProgressGateway {
    suspend fun loadProgress(): ProgressLoadResult
    suspend fun saveProgress(progress: StandaloneProgress)
    suspend fun updateProgress(transform: (StandaloneProgress) -> StandaloneProgress)
    suspend fun resetProgress()
}

interface CollectionGateway {
    suspend fun loadCollection(): OwnedCollection
    suspend fun saveCollection(collection: OwnedCollection)
    fun mergeCards(collection: OwnedCollection, cards: List<PackCard>): OwnedCollection
}

interface CraftingGateway {
    suspend fun loadCraftingCandidates(mode: CraftingMode): List<CraftingCardCandidate>
    suspend fun hasDarkenSkyCandidates(): Boolean
    suspend fun applyCrafting(
        mode: CraftingMode,
        source: CraftingCardRef,
    ): CraftingApplyResult
}

interface EquipmentGateway {
    suspend fun loadEquipmentState(): EquipmentState
    suspend fun activateEquipment(equipmentCardId: String): EquipmentState
}

interface PackGateway {
    fun currentPackResult(): StateFlow<DrawPackResponse?>
    fun clearCurrentPackResult()
    suspend fun openPack(extensionId: String, isEpicBoosted: Boolean = false): DrawPackResponse
}

interface MiniGamesGateway {
    suspend fun loadMiniGamesState(): MiniGamesState
    suspend fun prepareResolvedCardsForToday(
        miniGameId: MiniGameId,
        slotCount: Int,
        extensionId: String? = null,
    ): List<MiniGameResolvedCardRef>

    suspend fun consumeAttemptForToday(
        miniGameId: MiniGameId,
    ): MiniGameAttemptConsumeResult

    suspend fun grantRewardForToday(
        miniGameId: MiniGameId,
        reward: MiniGameReward,
    ): MiniGameRewardGrantResult

    suspend fun unlockDifficulty(
        miniGameId: MiniGameId,
        difficulty: MiniGameDifficulty,
    ): MiniGamesProgress
}

data class MiniGamesState(
    val todayUtc: String,
    val progress: MiniGamesProgress,
)

interface TradeGateway {
    suspend fun loadTradeCandidates(): List<TradeCardCandidate>
    suspend fun catalogFingerprint(): String
    suspend fun validateTrade(
        localOutgoing: TradeCardRef,
        remoteOutgoing: TradeCardRef,
    ): TradeValidationResult
    suspend fun applyTrade(
        tradeId: String,
        outgoing: TradeCardRef,
        incoming: TradeCardRef,
    ): TradeValidationResult
}
