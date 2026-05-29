package fr.aumombelli.dstcg.data

import fr.aumombelli.dstcg.model.AstronomyPackRevealSlot
import fr.aumombelli.dstcg.model.DrawPackResponse
import fr.aumombelli.dstcg.model.HomeMenuNoveltyState
import fr.aumombelli.dstcg.model.LibraryCardNoveltyState
import fr.aumombelli.dstcg.model.addRewards
import fr.aumombelli.dstcg.model.buildNewLibraryCardIds
import fr.aumombelli.dstcg.model.consumeEquipmentEffectsAfterPackOpen
import fr.aumombelli.dstcg.model.hasEquipmentStockCrossedZero
import fr.aumombelli.dstcg.model.hasNewLibraryCard
import fr.aumombelli.dstcg.model.or
import fr.aumombelli.dstcg.model.recordAffectedPackIfEquipmentActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class PackRepository(
    private val progressRepository: ProgressGateway,
    private val collectionRepository: CollectionGateway,
    private val localPackEngine: LocalPackEngine,
    private val homeMenuNoveltyEvaluator: HomeMenuNoveltyEvaluator,
) : PackGateway {
    private val currentPackResult = MutableStateFlow<DrawPackResponse?>(null)
    private val openPackMutex = Mutex()

    override fun currentPackResult(): StateFlow<DrawPackResponse?> = currentPackResult.asStateFlow()

    override fun clearCurrentPackResult() {
        currentPackResult.value = null
    }

    override suspend fun openPack(extensionId: String, isEpicBoosted: Boolean): DrawPackResponse = openPackMutex.withLock {
        val loadedProgress = progressRepository.loadProgress().requireUsableProgress()
        val progress = loadedProgress.progress
        val packResponse = localPackEngine.drawPack(
            extensionId = extensionId,
            progress = progress,
            now = loadedProgress.trustedNow,
            isEpicBoosted = isEpicBoosted,
        )
        val mergedCollection = collectionRepository.mergeCards(progress.collection, packResponse.cards)
        val mergedEquipmentInventory = progress.equipmentInventory.addRewards(packResponse.equipmentCards)
        val afterProgress = progress.copy(
            collection = mergedCollection,
            equipmentInventory = mergedEquipmentInventory,
            rechargeState = packResponse.rechargeState,
            openedPackCount = progress.openedPackCount + 1,
            hasOpenedEpicBoostedPack = progress.hasOpenedEpicBoostedPack || packResponse.isEpicBoosted,
        )
            .recordAffectedPackIfEquipmentActive()
            .consumeEquipmentEffectsAfterPackOpen()
        val drawnNewLibraryCardIds = buildNewLibraryCardIds(
            beforeProgress = progress,
            afterProgress = afterProgress,
        )
        val newLibraryCardIds = progress.libraryCardNoveltyState.newCardIds + drawnNewLibraryCardIds
        val decoratedPackResponse = packResponse.copy(
            revealSlots = packResponse.revealSlots.map { slot ->
                when (slot) {
                    is AstronomyPackRevealSlot -> slot.copy(
                        isFirstEncounter = slot.card.cardId in drawnNewLibraryCardIds,
                    )

                    else -> slot
                }
            },
        )
        val noveltyState = progress.homeMenuNoveltyState.or(
            other = HomeMenuNoveltyState(
                library = hasNewLibraryCard(
                    beforeProgress = progress,
                    afterProgress = afterProgress,
                ),
                equipment = hasEquipmentStockCrossedZero(
                    beforeProgress = progress,
                    afterProgress = afterProgress,
                ),
                badgeBook = homeMenuNoveltyEvaluator.hasNewBadgeUnlock(
                    beforeProgress = progress,
                    afterProgress = afterProgress,
                ),
            ),
        )
        progressRepository.saveProgress(
            afterProgress.copy(
                homeMenuNoveltyState = noveltyState,
                libraryCardNoveltyState = LibraryCardNoveltyState(newCardIds = newLibraryCardIds),
            ),
        )
        currentPackResult.value = decoratedPackResponse
        decoratedPackResponse
    }
}
