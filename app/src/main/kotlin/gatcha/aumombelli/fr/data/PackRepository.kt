package fr.aumombelli.gatcha.data

import fr.aumombelli.gatcha.model.DrawPackResponse
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
            availableDrawCount = progress.availableDrawCount,
            nextChargeAt = progress.nextChargeAt,
            now = loadedProgress.trustedNow,
        )
        val mergedCollection = collectionRepository.mergeCards(progress.collection, packResponse.cards)
        progressRepository.saveProgress(
            progress.copy(
                collection = mergedCollection,
                availableDrawCount = packResponse.availableDrawCount,
                nextChargeAt = packResponse.nextChargeAt,
                openedPackCount = progress.openedPackCount + 1,
            ),
        )
        currentPackResult.value = packResponse
        packResponse
    }
}
