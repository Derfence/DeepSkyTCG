package fr.aumombelli.gatcha.data

import fr.aumombelli.gatcha.model.DrawPackResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PackRepository(
    private val progressRepository: ProgressGateway,
    private val collectionRepository: CollectionGateway,
    private val localPackEngine: LocalPackEngine,
) : PackGateway {
    private val currentPackResult = MutableStateFlow<DrawPackResponse?>(null)

    override fun currentPackResult(): StateFlow<DrawPackResponse?> = currentPackResult.asStateFlow()

    override fun clearCurrentPackResult() {
        currentPackResult.value = null
    }

    override suspend fun openPack(extensionId: String): DrawPackResponse {
        val progress = progressRepository.loadProgress()
        val packResponse = localPackEngine.drawPack(
            extensionId = extensionId,
            availableDrawCount = progress.availableDrawCount,
            nextChargeAt = progress.nextChargeAt,
        )
        val mergedCollection = collectionRepository.mergeCards(progress.collection, packResponse.cards)
        progressRepository.saveProgress(
            progress.copy(
                collection = mergedCollection,
                availableDrawCount = packResponse.availableDrawCount,
                nextChargeAt = packResponse.nextChargeAt,
            ),
        )
        currentPackResult.value = packResponse
        return packResponse
    }
}
