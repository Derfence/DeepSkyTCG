package fr.aumombelli.gatcha

import fr.aumombelli.gatcha.data.AppCompatibilityState
import fr.aumombelli.gatcha.data.AppStatusGateway
import fr.aumombelli.gatcha.data.AuthGateway
import fr.aumombelli.gatcha.data.CatalogGateway
import fr.aumombelli.gatcha.data.CollectionGateway
import fr.aumombelli.gatcha.data.PackGateway
import fr.aumombelli.gatcha.data.SessionGateway
import fr.aumombelli.gatcha.model.CatalogMetadata
import fr.aumombelli.gatcha.model.CardDefinition
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
import fr.aumombelli.gatcha.model.VariantProfile
import fr.aumombelli.gatcha.model.mergePackCards
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
    var metadata = CatalogMetadata(catalogVersion = 5)
    var extensions: List<ExtensionDefinition> = emptyList()
    var cards: List<CardDefinition> = emptyList()
    var variantProfiles: List<VariantProfile> = testVariantProfiles()
    var metadataFailure: Throwable? = null
    var extensionsFailure: Throwable? = null
    var cardsFailure: Throwable? = null
    var variantProfilesFailure: Throwable? = null

    override suspend fun loadMetadata(): CatalogMetadata {
        metadataFailure?.let { throw it }
        return metadata
    }

    override suspend fun loadExtensions(): List<ExtensionDefinition> {
        extensionsFailure?.let { throw it }
        return extensions
    }

    override suspend fun loadCards(): List<CardDefinition> {
        cardsFailure?.let { throw it }
        return cards
    }

    override suspend fun loadVariantProfiles(): List<VariantProfile> {
        variantProfilesFailure?.let { throw it }
        return variantProfiles
    }
}

class FakePackGateway : PackGateway {
    private val packFlow = MutableStateFlow<DrawPackResponse?>(null)
    var openPackResponse: DrawPackResponse? = null
    var openPackFailure: Throwable? = null
    val openPackCalls = mutableListOf<Pair<String, OwnedCollection>>()

    override fun currentPackResult(): StateFlow<DrawPackResponse?> = packFlow

    override fun clearCurrentPackResult() {
        packFlow.value = null
    }

    override suspend fun openPack(extensionId: String, currentCollection: OwnedCollection): DrawPackResponse {
        openPackCalls += extensionId to currentCollection
        openPackFailure?.let { throw it }
        return checkNotNull(openPackResponse) { "openPackResponse must be configured in FakePackGateway." }
            .also { packFlow.value = it }
    }
}

class FakeAppStatusGateway : AppStatusGateway {
    private val mutableState = MutableStateFlow<AppCompatibilityState>(AppCompatibilityState.Checking)
    var verifyCallCount = AtomicInteger(0)
    var onVerify: (suspend () -> Unit)? = null

    override val state: StateFlow<AppCompatibilityState> = mutableState

    override suspend fun verifyCompatibility() {
        verifyCallCount.incrementAndGet()
        onVerify?.invoke()
    }

    fun updateState(state: AppCompatibilityState) {
        mutableState.value = state
    }
}
