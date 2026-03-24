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
    private val collectionMigrationService: CollectionMigrationService,
) : CollectionGateway {
    override suspend fun loadCollectionFromServer(): OwnedCollection {
        val session = sessionRepository.requireActiveSession()
        val response = apiService.getCollection(
            GetCollectionRequest(
                username = session.username,
                passwordHash = session.passwordHash,
            ),
        )
        val decrypted = CollectionCrypto.decryptAndDeserialize(response.collectionBlob, session.passwordHash)
        val migrated = collectionMigrationService.migrateToCurrentVersion(decrypted)
        if (migrated != decrypted) {
            saveCollection(migrated)
            return migrated
        }
        sessionRepository.commitSavedCollection(
            collectionBlob = response.collectionBlob,
            savedAt = response.savedAt,
            nextDrawAt = sessionRepository.readSnapshot().nextDrawAt,
        )
        return migrated
    }

    override suspend fun getCachedCollectionOrEmpty(): OwnedCollection {
        val snapshot = sessionRepository.readSnapshot()
        val session = sessionRepository.requireActiveSession()
        val blob = snapshot.lastCollectionBlob ?: return collectionMigrationService.emptyCollection()
        return runCatching {
            val decrypted = CollectionCrypto.decryptAndDeserialize(blob, session.passwordHash)
            val migrated = collectionMigrationService.migrateToCurrentVersion(decrypted)
            if (migrated != decrypted) {
                val migratedBlob = CollectionCrypto.serializeAndEncrypt(migrated, session.passwordHash)
                sessionRepository.commitSavedCollection(
                    collectionBlob = migratedBlob,
                    savedAt = snapshot.lastSavedAt,
                    nextDrawAt = snapshot.nextDrawAt,
                )
            }
            migrated
        }.getOrDefault(collectionMigrationService.emptyCollection())
    }

    override suspend fun saveCollection(collection: OwnedCollection): String {
        val session = sessionRepository.requireActiveSession()
        val migrated = collectionMigrationService.migrateToCurrentVersion(collection)
        val blob = CollectionCrypto.serializeAndEncrypt(migrated, session.passwordHash)
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
        val decrypted = CollectionCrypto.decryptAndDeserialize(pendingBlob, session.passwordHash)
        val migrated = collectionMigrationService.migrateToCurrentVersion(decrypted)
        val migratedBlob = CollectionCrypto.serializeAndEncrypt(migrated, session.passwordHash)
        val response = apiService.saveCollection(
            SaveCollectionRequest(
                username = session.username,
                passwordHash = session.passwordHash,
                collectionBlob = migratedBlob,
            ),
        )
        sessionRepository.commitSavedCollection(migratedBlob, response.savedAt, snapshot.nextDrawAt)
        return true
    }

    override fun mergeCards(collection: OwnedCollection, cards: List<PackCard>): OwnedCollection {
        return collection.mergePackCards(cards)
    }
}
