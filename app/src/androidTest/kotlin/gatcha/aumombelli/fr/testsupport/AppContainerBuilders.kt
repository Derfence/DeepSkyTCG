package fr.aumombelli.gatcha.testsupport

import fr.aumombelli.gatcha.AppContainer
import fr.aumombelli.gatcha.testCardDefinition
import fr.aumombelli.gatcha.testPackCard
import fr.aumombelli.gatcha.data.AppCompatibilityState
import fr.aumombelli.gatcha.data.AppStatusGateway
import fr.aumombelli.gatcha.data.AuthGateway
import fr.aumombelli.gatcha.data.CatalogGateway
import fr.aumombelli.gatcha.data.CollectionGateway
import fr.aumombelli.gatcha.data.PackGateway
import fr.aumombelli.gatcha.data.SessionGateway
import fr.aumombelli.gatcha.model.CatalogMetadata
import fr.aumombelli.gatcha.model.CardDefinition
import fr.aumombelli.gatcha.model.CardFinishDefinition
import fr.aumombelli.gatcha.model.CreateAccountRequest
import fr.aumombelli.gatcha.model.CreateAccountResponse
import fr.aumombelli.gatcha.model.DisplayCardVariant
import fr.aumombelli.gatcha.model.DrawPackResponse
import fr.aumombelli.gatcha.model.ExtensionDefinition
import fr.aumombelli.gatcha.model.LoginRequest
import fr.aumombelli.gatcha.model.LoginResponse
import fr.aumombelli.gatcha.model.OwnedCollection
import fr.aumombelli.gatcha.model.OwnedVariantCount
import fr.aumombelli.gatcha.model.PackCard
import fr.aumombelli.gatcha.model.SessionCredentials
import fr.aumombelli.gatcha.model.SkyQualityDefinition
import fr.aumombelli.gatcha.model.StoredSessionSnapshot
import fr.aumombelli.gatcha.model.VariantProfile
import fr.aumombelli.gatcha.model.WeightedCode
import fr.aumombelli.gatcha.model.mergePackCards
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

internal fun compatibleTestAppContainer(): AppContainer = appContainerWithStatus(
    appStatusRepository = StaticAppStatusGateway(AppCompatibilityState.Compatible),
)

internal fun blockedTestAppContainer(
    message: String = "A newer client is required.",
): AppContainer = appContainerWithStatus(
    appStatusRepository = StaticAppStatusGateway(AppCompatibilityState.Blocked(message)),
)

internal fun backNavigationTestAppContainer(): AppContainer {
    val extension = ExtensionDefinition(
        id = "astronomes-en-herbe",
        name = "Astronomes en herbe",
        coverImageRef = "cover",
    )
    val cardDefinition = testCardDefinition("ALP-001")
    val packResponse = DrawPackResponse(
        extensionId = extension.id,
        drawnAt = "2026-03-23T12:00:00Z",
        nextDrawAt = "2026-03-24T00:00:00Z",
        cards = listOf(
            testPackCard(
                cardId = "ALP-001",
                name = "Nebuleuse d'Orion",
                rarityLabel = "Common",
                imageRef = "m42_orion_nebula",
            ),
        ),
    )

    return AppContainer(
        sessionRepository = NavigationSessionGateway(),
        catalogRepository = NavigationCatalogGateway(
            extensions = listOf(extension),
            cards = listOf(cardDefinition),
            variantProfiles = listOf(navigationVariantProfile()),
        ),
        apiService = NavigationAuthGateway(),
        appStatusRepository = StaticAppStatusGateway(AppCompatibilityState.Compatible),
        collectionRepository = NavigationCollectionGateway(
            initialCollection = OwnedCollection(
                cards = mapOf(
                    "ALP-001" to fr.aumombelli.gatcha.model.OwnedCardEntry(
                        totalOwned = 1,
                        variants = listOf(
                            OwnedVariantCount(
                                skyQuality = "city",
                                finish = "standard",
                                count = 1,
                            ),
                        ),
                    ),
                ),
            ),
        ),
        packRepository = NavigationPackGateway(packResponse),
    )
}

private fun appContainerWithStatus(
    appStatusRepository: AppStatusGateway,
): AppContainer = AppContainer(
    sessionRepository = SimpleSessionGateway(),
    catalogRepository = SimpleCatalogGateway(),
    apiService = SimpleAuthGateway(),
    appStatusRepository = appStatusRepository,
    collectionRepository = SimpleCollectionGateway(),
    packRepository = SimplePackGateway(),
)

private class StaticAppStatusGateway(
    initialState: AppCompatibilityState,
) : AppStatusGateway {
    private val stateFlow = MutableStateFlow(initialState)
    override val state: StateFlow<AppCompatibilityState> = stateFlow

    override suspend fun verifyCompatibility() = Unit
}

private class SimpleSessionGateway : SessionGateway {
    override fun setActiveSession(username: String, passwordHash: String) = Unit
    override fun clearActiveSession() = Unit
    override fun requireActiveSession(): SessionCredentials = SessionCredentials("alice", "hash")
    override suspend fun readSnapshot(): StoredSessionSnapshot = StoredSessionSnapshot()
    override suspend fun saveLoginMetadata(username: String, lastSavedAt: String?, nextDrawAt: String?) = Unit
    override suspend fun commitSavedCollection(collectionBlob: String, savedAt: String?, nextDrawAt: String?) = Unit
    override suspend fun savePendingPack(collectionBlob: String, packResponse: DrawPackResponse) = Unit
    override suspend fun clearPendingPack() = Unit
    override suspend fun decodePendingPack(): DrawPackResponse? = null
}

private class SimpleCatalogGateway : CatalogGateway {
    override suspend fun loadMetadata(): CatalogMetadata = CatalogMetadata(catalogVersion = 5)
    override suspend fun loadExtensions(): List<ExtensionDefinition> = emptyList()
    override suspend fun loadCards(): List<CardDefinition> = emptyList()
    override suspend fun loadVariantProfiles(): List<VariantProfile> = emptyList()
}

private class SimpleAuthGateway : AuthGateway {
    override suspend fun createAccount(request: CreateAccountRequest): CreateAccountResponse =
        CreateAccountResponse(username = request.username, createdAt = "2026-03-24T12:00:00Z")

    override suspend fun login(request: LoginRequest): LoginResponse =
        LoginResponse(username = request.username)
}

private class SimpleCollectionGateway : CollectionGateway {
    override suspend fun loadCollectionFromServer(): OwnedCollection = OwnedCollection(version = 5)
    override suspend fun getCachedCollectionOrEmpty(): OwnedCollection = OwnedCollection(version = 5)
    override suspend fun saveCollection(collection: OwnedCollection): String = "2026-03-24T12:00:00Z"
    override suspend fun replayPendingSaveIfNeeded(): Boolean = false
    override fun mergeCards(collection: OwnedCollection, cards: List<PackCard>): OwnedCollection = collection
}

private class SimplePackGateway : PackGateway {
    private val packFlow = MutableStateFlow<DrawPackResponse?>(null)

    override fun currentPackResult(): StateFlow<DrawPackResponse?> = packFlow

    override fun clearCurrentPackResult() {
        packFlow.value = null
    }

    override suspend fun openPack(extensionId: String, currentCollection: OwnedCollection): DrawPackResponse {
        error("Not used in this test container")
    }
}

private fun navigationVariantProfile(): VariantProfile = VariantProfile(
    id = "observation-default",
    skyQualities = listOf(
        SkyQualityDefinition(code = "city", label = "Ville"),
    ),
    finishes = listOf(
        CardFinishDefinition(code = "standard", label = "Standard"),
    ),
    skyQualityWeights = listOf(WeightedCode(code = "city", weight = 1)),
    finishWeights = listOf(WeightedCode(code = "standard", weight = 1)),
)

private class NavigationAuthGateway : AuthGateway {
    override suspend fun createAccount(request: CreateAccountRequest): CreateAccountResponse =
        CreateAccountResponse(username = request.username, createdAt = "2026-03-23T12:00:00Z")

    override suspend fun login(request: LoginRequest): LoginResponse =
        LoginResponse(username = request.username, lastSavedAt = null, nextDrawAt = null)
}

private class NavigationCatalogGateway(
    private val extensions: List<ExtensionDefinition>,
    private val cards: List<CardDefinition>,
    private val variantProfiles: List<VariantProfile>,
) : CatalogGateway {
    override suspend fun loadMetadata(): CatalogMetadata = CatalogMetadata(catalogVersion = 1)
    override suspend fun loadExtensions(): List<ExtensionDefinition> = extensions
    override suspend fun loadCards(): List<CardDefinition> = cards
    override suspend fun loadVariantProfiles(): List<VariantProfile> = variantProfiles
}

private class NavigationCollectionGateway(
    initialCollection: OwnedCollection,
) : CollectionGateway {
    private var collection = initialCollection

    override suspend fun loadCollectionFromServer(): OwnedCollection = collection

    override suspend fun getCachedCollectionOrEmpty(): OwnedCollection = collection

    override suspend fun saveCollection(collection: OwnedCollection): String = "2026-03-23T12:00:00Z"

    override suspend fun replayPendingSaveIfNeeded(): Boolean = false

    override fun mergeCards(collection: OwnedCollection, cards: List<PackCard>): OwnedCollection =
        collection.mergePackCards(cards).also { this.collection = it }
}

private class NavigationSessionGateway : SessionGateway {
    private var activeSession: SessionCredentials? = null
    private var snapshot = StoredSessionSnapshot(lastUsername = "alice")

    override fun setActiveSession(username: String, passwordHash: String) {
        activeSession = SessionCredentials(username = username, passwordHash = passwordHash)
    }

    override fun clearActiveSession() {
        activeSession = null
    }

    override fun requireActiveSession(): SessionCredentials =
        checkNotNull(activeSession) { "No active session configured for host test." }

    override suspend fun readSnapshot(): StoredSessionSnapshot = snapshot

    override suspend fun saveLoginMetadata(username: String, lastSavedAt: String?, nextDrawAt: String?) {
        snapshot = snapshot.copy(
            lastUsername = username,
            lastSavedAt = lastSavedAt,
            nextDrawAt = nextDrawAt,
        )
    }

    override suspend fun commitSavedCollection(collectionBlob: String, savedAt: String?, nextDrawAt: String?) {
        snapshot = snapshot.copy(
            lastCollectionBlob = collectionBlob,
            lastSavedAt = savedAt,
            nextDrawAt = nextDrawAt,
            pendingCollectionBlob = null,
            pendingPackJson = null,
        )
    }

    override suspend fun savePendingPack(collectionBlob: String, packResponse: DrawPackResponse) {
        snapshot = snapshot.copy(
            pendingCollectionBlob = collectionBlob,
            nextDrawAt = packResponse.nextDrawAt,
        )
    }

    override suspend fun clearPendingPack() {
        snapshot = snapshot.copy(
            pendingCollectionBlob = null,
            pendingPackJson = null,
        )
    }

    override suspend fun decodePendingPack(): DrawPackResponse? = null
}

private class NavigationPackGateway(
    private val openPackResponse: DrawPackResponse,
) : PackGateway {
    private val packFlow = MutableStateFlow<DrawPackResponse?>(null)

    override fun currentPackResult(): StateFlow<DrawPackResponse?> = packFlow

    override fun clearCurrentPackResult() {
        packFlow.value = null
    }

    override suspend fun openPack(extensionId: String, currentCollection: OwnedCollection): DrawPackResponse =
        openPackResponse.also { packFlow.value = it }
}
