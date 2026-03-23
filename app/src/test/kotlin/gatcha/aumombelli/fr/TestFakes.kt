package gatcha.aumombelli.fr

import gatcha.aumombelli.fr.data.AuthGateway
import gatcha.aumombelli.fr.data.CatalogGateway
import gatcha.aumombelli.fr.data.CollectionGateway
import gatcha.aumombelli.fr.data.PackGateway
import gatcha.aumombelli.fr.data.SessionGateway
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
import gatcha.aumombelli.fr.model.mergePackCards
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class FakeAuthGateway : AuthGateway {
    var createAccountResponse = CreateAccountResponse(username = "alice", createdAt = "2026-03-23T12:00:00Z")
    var loginResponse = LoginResponse(username = "alice", lastSavedAt = null, nextDrawAt = null)
    var createAccountFailure: Throwable? = null
    var loginFailure: Throwable? = null
    val createRequests = mutableListOf<CreateAccountRequest>()
    val loginRequests = mutableListOf<LoginRequest>()

    override suspend fun createAccount(request: CreateAccountRequest): CreateAccountResponse {
        createRequests += request
        createAccountFailure?.let { throw it }
        return createAccountResponse
    }

    override suspend fun login(request: LoginRequest): LoginResponse {
        loginRequests += request
        loginFailure?.let { throw it }
        return loginResponse
    }
}

class FakeSessionGateway : SessionGateway {
    var snapshot = StoredSessionSnapshot()
    var activeSession: SessionCredentials? = null
    val savedLoginMetadata = mutableListOf<Triple<String, String?, String?>>()
    val committedCollections = mutableListOf<Triple<String, String?, String?>>()
    val savedPendingPacks = mutableListOf<Pair<String, DrawPackResponse>>()

    override fun setActiveSession(username: String, passwordHash: String) {
        activeSession = SessionCredentials(username, passwordHash)
    }

    override fun clearActiveSession() {
        activeSession = null
    }

    override fun requireActiveSession(): SessionCredentials =
        checkNotNull(activeSession) { "No active session configured in test fake." }

    override suspend fun readSnapshot(): StoredSessionSnapshot = snapshot

    override suspend fun saveLoginMetadata(username: String, lastSavedAt: String?, nextDrawAt: String?) {
        savedLoginMetadata += Triple(username, lastSavedAt, nextDrawAt)
        snapshot = snapshot.copy(
            lastUsername = username,
            lastSavedAt = lastSavedAt,
            nextDrawAt = nextDrawAt,
        )
    }

    override suspend fun commitSavedCollection(collectionBlob: String, savedAt: String?, nextDrawAt: String?) {
        committedCollections += Triple(collectionBlob, savedAt, nextDrawAt)
        snapshot = snapshot.copy(
            lastCollectionBlob = collectionBlob,
            lastSavedAt = savedAt,
            nextDrawAt = nextDrawAt,
            pendingCollectionBlob = null,
            pendingPackJson = null,
        )
    }

    override suspend fun savePendingPack(collectionBlob: String, packResponse: DrawPackResponse) {
        savedPendingPacks += collectionBlob to packResponse
    }

    override suspend fun clearPendingPack() {
        snapshot = snapshot.copy(pendingCollectionBlob = null, pendingPackJson = null)
    }

    override suspend fun decodePendingPack(): DrawPackResponse? = null
}

class FakeCollectionGateway : CollectionGateway {
    var serverCollection = OwnedCollection()
    var cachedCollection = OwnedCollection()
    var replayPendingSaveResult = false
    var loadCollectionFailure: Throwable? = null
    var replayFailure: Throwable? = null
    var loadCollectionCallCount = AtomicInteger(0)
    var replayPendingSaveCallCount = AtomicInteger(0)

    override suspend fun loadCollectionFromServer(): OwnedCollection {
        loadCollectionCallCount.incrementAndGet()
        loadCollectionFailure?.let { throw it }
        return serverCollection
    }

    override suspend fun getCachedCollectionOrEmpty(): OwnedCollection = cachedCollection

    override suspend fun saveCollection(collection: OwnedCollection): String = "2026-03-23T12:00:00Z"

    override suspend fun replayPendingSaveIfNeeded(): Boolean {
        replayPendingSaveCallCount.incrementAndGet()
        replayFailure?.let { throw it }
        return replayPendingSaveResult
    }

    override fun mergeCards(collection: OwnedCollection, cards: List<PackCard>): OwnedCollection =
        collection.mergePackCards(cards)
}

class FakeCatalogGateway : CatalogGateway {
    var extensions: List<ExtensionDefinition> = emptyList()
    var cards: List<CardDefinition> = emptyList()
    var extensionsFailure: Throwable? = null
    var cardsFailure: Throwable? = null

    override suspend fun loadExtensions(): List<ExtensionDefinition> {
        extensionsFailure?.let { throw it }
        return extensions
    }

    override suspend fun loadCards(): List<CardDefinition> {
        cardsFailure?.let { throw it }
        return cards
    }
}

class FakePackGateway : PackGateway {
    private val packFlow = MutableStateFlow<DrawPackResponse?>(null)
    var openPackResponse: DrawPackResponse? = null
    var openPackFailure: Throwable? = null
    val openPackCalls = mutableListOf<Pair<String, OwnedCollection>>()

    override fun currentPackResult(): StateFlow<DrawPackResponse?> = packFlow

    override suspend fun openPack(extensionId: String, currentCollection: OwnedCollection): DrawPackResponse {
        openPackCalls += extensionId to currentCollection
        openPackFailure?.let { throw it }
        return checkNotNull(openPackResponse) { "openPackResponse must be configured in FakePackGateway." }
            .also { packFlow.value = it }
    }
}
