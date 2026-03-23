package gatcha.aumombelli.fr.data

import gatcha.aumombelli.fr.model.DrawPackRequest
import gatcha.aumombelli.fr.model.DrawPackResponse
import gatcha.aumombelli.fr.model.OwnedCollection
import gatcha.aumombelli.fr.model.SaveCollectionRequest
import gatcha.aumombelli.fr.network.GameApiService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PendingSaveException(
    message: String,
    cause: Throwable,
) : Exception(message, cause)

class PackRepository(
    private val apiService: GameApiService,
    private val sessionRepository: SessionRepository,
    private val collectionRepository: CollectionRepository,
) {
    private val currentPackResult = MutableStateFlow<DrawPackResponse?>(null)

    fun currentPackResult(): StateFlow<DrawPackResponse?> = currentPackResult.asStateFlow()

    suspend fun openPack(extensionId: String, currentCollection: OwnedCollection): DrawPackResponse {
        val session = sessionRepository.requireActiveSession()
        val packResponse = apiService.drawPack(
            DrawPackRequest(
                username = session.username,
                passwordHash = session.passwordHash,
                extensionId = extensionId,
            ),
        )
        val mergedCollection = collectionRepository.mergeCards(currentCollection, packResponse.cards)
        val mergedBlob = CollectionCrypto.serializeAndEncrypt(mergedCollection, session.passwordHash)

        sessionRepository.savePendingPack(mergedBlob, packResponse)

        try {
            val saveResponse = apiService.saveCollection(
                SaveCollectionRequest(
                    username = session.username,
                    passwordHash = session.passwordHash,
                    collectionBlob = mergedBlob,
                ),
            )
            sessionRepository.commitSavedCollection(
                collectionBlob = mergedBlob,
                savedAt = saveResponse.savedAt,
                nextDrawAt = packResponse.nextDrawAt,
            )
            currentPackResult.value = packResponse
            return packResponse
        } catch (exception: Exception) {
            throw PendingSaveException(
                message = "Pack drawn but save failed. Retry will be needed after the next login.",
                cause = exception,
            )
        }
    }
}
