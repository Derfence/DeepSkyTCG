package fr.aumombelli.gatcha.data

import fr.aumombelli.gatcha.model.CardDefinition
import fr.aumombelli.gatcha.model.AppStatusResponse
import fr.aumombelli.gatcha.model.CatalogMetadata
import fr.aumombelli.gatcha.model.CreateAccountRequest
import fr.aumombelli.gatcha.model.CreateAccountResponse
import fr.aumombelli.gatcha.model.DrawPackResponse
import fr.aumombelli.gatcha.model.ExtensionDefinition
import fr.aumombelli.gatcha.model.LoginRequest
import fr.aumombelli.gatcha.model.LoginResponse
import fr.aumombelli.gatcha.model.OwnedCollection
import fr.aumombelli.gatcha.model.PackCard
import fr.aumombelli.gatcha.model.SessionCredentials
import fr.aumombelli.gatcha.model.StoredSessionSnapshot
import kotlinx.coroutines.flow.StateFlow

interface AuthGateway {
    suspend fun createAccount(request: CreateAccountRequest): CreateAccountResponse
    suspend fun login(request: LoginRequest): LoginResponse
}

interface CatalogGateway {
    suspend fun loadMetadata(): CatalogMetadata
    suspend fun loadExtensions(): List<ExtensionDefinition>
    suspend fun loadCards(): List<CardDefinition>
}

sealed interface AppCompatibilityState {
    data object Checking : AppCompatibilityState
    data object Compatible : AppCompatibilityState
    data class Blocked(
        val message: String,
        val canRetry: Boolean = true,
    ) : AppCompatibilityState
}

interface AppStatusGateway {
    val state: StateFlow<AppCompatibilityState>
    suspend fun verifyCompatibility()
}

interface AppStatusApi {
    suspend fun fetchAppStatus(): AppStatusResponse
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
