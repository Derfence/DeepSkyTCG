package gatcha.aumombelli.fr.data

import gatcha.aumombelli.fr.model.CardDefinition
import gatcha.aumombelli.fr.model.CreateAccountRequest
import gatcha.aumombelli.fr.model.CreateAccountResponse
import gatcha.aumombelli.fr.model.DrawPackResponse
import gatcha.aumombelli.fr.model.ExtensionDefinition
import gatcha.aumombelli.fr.model.LoginRequest
import gatcha.aumombelli.fr.model.LoginResponse
import gatcha.aumombelli.fr.model.OwnedCollection
import gatcha.aumombelli.fr.model.PackCard
import gatcha.aumombelli.fr.model.SessionCredentials
import gatcha.aumombelli.fr.model.StoredSessionSnapshot
import kotlinx.coroutines.flow.StateFlow

interface AuthGateway {
    suspend fun createAccount(request: CreateAccountRequest): CreateAccountResponse
    suspend fun login(request: LoginRequest): LoginResponse
}

interface CatalogGateway {
    suspend fun loadExtensions(): List<ExtensionDefinition>
    suspend fun loadCards(): List<CardDefinition>
}

interface SessionGateway {
    fun setActiveSession(username: String, passwordHash: String)
    fun clearActiveSession()
    fun requireActiveSession(): SessionCredentials
    suspend fun readSnapshot(): StoredSessionSnapshot
    suspend fun saveLoginMetadata(username: String, lastSavedAt: String?, nextDrawAt: String?)
    suspend fun commitSavedCollection(collectionBlob: String, savedAt: String?, nextDrawAt: String?)
    suspend fun savePendingPack(collectionBlob: String, packResponse: DrawPackResponse)
    suspend fun clearPendingPack()
    suspend fun decodePendingPack(): DrawPackResponse?
}

interface CollectionGateway {
    suspend fun loadCollectionFromServer(): OwnedCollection
    suspend fun getCachedCollectionOrEmpty(): OwnedCollection
    suspend fun saveCollection(collection: OwnedCollection): String
    suspend fun replayPendingSaveIfNeeded(): Boolean
    fun mergeCards(collection: OwnedCollection, cards: List<PackCard>): OwnedCollection
}

interface PackGateway {
    fun currentPackResult(): StateFlow<DrawPackResponse?>
    suspend fun openPack(extensionId: String, currentCollection: OwnedCollection): DrawPackResponse
}
