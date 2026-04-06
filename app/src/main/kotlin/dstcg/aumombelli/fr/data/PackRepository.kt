package fr.aumombelli.dstcg.data

import fr.aumombelli.dstcg.model.DrawPackResponse
import fr.aumombelli.dstcg.model.addRewards
import fr.aumombelli.dstcg.model.consumeEquipmentEffectsAfterPackOpen
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class PackRepository(
    private val progressRepository: ProgressGateway,
    private val collectionRepository: CollectionGateway,
    private val localPackEngine: LocalPackEngine,
) : PackGateway {
    private val currentPackResult = MutableStateFlow<DrawPackResponse?>(null)
    private val openPackMutex = Mutex()

    override fun currentPackResult(): StateFlow<DrawPackResponse?> = currentPackResult.asStateFlow()

    override fun clearCurrentPackResult() {
        currentPackResult.value = null
    }

    override suspend fun openPack(extensionId: String): DrawPackResponse = openPackMutex.withLock {
        val loadedProgress = progressRepository.loadProgress().requireUsableProgress()
        val progress = loadedProgress.progress
        val packResponse = localPackEngine.drawPack(
            extensionId = extensionId,
            progress = progress,
            now = loadedProgress.trustedNow,
        )
        val mergedCollection = collectionRepository.mergeCards(progress.collection, packResponse.cards)
        val mergedEquipmentInventory = progress.equipmentInventory.addRewards(packResponse.equipmentCards)
        progressRepository.saveProgress(
            progress.copy(
                collection = mergedCollection,
                equipmentInventory = mergedEquipmentInventory,
                rechargeState = packResponse.rechargeState,
                openedPackCount = progress.openedPackCount + 1,
            ).consumeEquipmentEffectsAfterPackOpen(),
        )
        currentPackResult.value = packResponse
        packResponse
    }
}
