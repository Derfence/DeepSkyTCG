package fr.aumombelli.gatcha.data

import fr.aumombelli.gatcha.model.GetCollectionRequest
import fr.aumombelli.gatcha.model.OwnedCollection
import fr.aumombelli.gatcha.model.PackCard
import fr.aumombelli.gatcha.model.SaveCollectionRequest
import fr.aumombelli.gatcha.model.mergePackCards
import fr.aumombelli.gatcha.network.GameApiService

class CollectionRepository(
    private val apiService: GameApiService,
    private val sessionRepository: SessionGateway,
) : CollectionGateway {
    override suspend fun loadCollectionFromServer(): OwnedCollection {
        val session = sessionRepository.requireActiveSession()
        val response = apiService.getCollection(
            GetCollectionRequest(
                username = session.username,
                passwordHash = session.passwordHash,
            ),
        )
        sessionRepository.commitSavedCollection(
            collectionBlob = response.collectionBlob,
            savedAt = response.savedAt,
            nextDrawAt = sessionRepository.readSnapshot().nextDrawAt,
        )
        return CollectionCrypto.decryptAndDeserialize(response.collectionBlob, session.passwordHash)
    }

    override suspend fun getCachedCollectionOrEmpty(): OwnedCollection {
        val snapshot = sessionRepository.readSnapshot()
        val session = sessionRepository.requireActiveSession()
        val blob = snapshot.lastCollectionBlob ?: return OwnedCollection()
        return runCatching { CollectionCrypto.decryptAndDeserialize(blob, session.passwordHash) }
            .getOrDefault(OwnedCollection())
    }

    override suspend fun saveCollection(collection: OwnedCollection): String {
        val session = sessionRepository.requireActiveSession()
        val blob = CollectionCrypto.serializeAndEncrypt(collection, session.passwordHash)
        val response = apiService.saveCollection(
            SaveCollectionRequest(
                username = session.username,
                passwordHash = session.passwordHash,
                collectionBlob = blob,
            ),
        )
        sessionRepository.commitSavedCollection(blob, response.savedAt, sessionRepository.readSnapshot().nextDrawAt)
        return response.savedAt
    }

    override suspend fun replayPendingSaveIfNeeded(): Boolean {
        val session = sessionRepository.requireActiveSession()
        val snapshot = sessionRepository.readSnapshot()
        val pendingBlob = snapshot.pendingCollectionBlob ?: return false
        if (snapshot.lastUsername != null && snapshot.lastUsername != session.username) {
            return false
        }
        val response = apiService.saveCollection(
            SaveCollectionRequest(
                username = session.username,
                passwordHash = session.passwordHash,
                collectionBlob = pendingBlob,
            ),
        )
        sessionRepository.commitSavedCollection(pendingBlob, response.savedAt, snapshot.nextDrawAt)
        return true
    }

    override fun mergeCards(collection: OwnedCollection, cards: List<PackCard>): OwnedCollection {
        return collection.mergePackCards(cards)
    }
}
