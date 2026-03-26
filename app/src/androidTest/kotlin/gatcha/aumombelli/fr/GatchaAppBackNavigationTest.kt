package fr.aumombelli.gatcha

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
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
import fr.aumombelli.gatcha.ui.theme.GatchaTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class GatchaAppBackNavigationTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun android_back_from_library_returns_to_main_menu() {
        setAppContent(hostTestAppContainer())
        loginAndReachMainMenu()

        composeRule.onNodeWithTag("menu-library").performClick()
        advanceBy(2_700)
        composeRule.onNodeWithTag("library-grid").assertIsDisplayed()

        pressAndroidBack()
        advanceBy(2_100)
        composeRule.onNodeWithTag("menu-open-pack").assertIsDisplayed()
    }

    @Test
    fun android_back_from_pack_selection_returns_to_extension_list_then_menu() {
        setAppContent(hostTestAppContainer())
        loginAndReachMainMenu()

        composeRule.onNodeWithTag("menu-open-pack").performClick()
        advanceBy(1_900)
        composeRule.onNodeWithTag("pack-extension-enter-astronomes-en-herbe").performClick()
        advanceBy(1_900)
        composeRule.onNodeWithTag("pack-booster-0").assertIsDisplayed()

        pressAndroidBack()
        advanceBy(900)
        composeRule.onNodeWithTag("pack-extension-enter-astronomes-en-herbe").assertIsDisplayed()

        pressAndroidBack()
        advanceBy(1_900)
        composeRule.onNodeWithTag("menu-open-pack").assertIsDisplayed()
    }

    @Test
    fun android_back_from_pack_opening_returns_to_main_menu() {
        setAppContent(hostTestAppContainer())
        loginAndReachMainMenu()

        composeRule.onNodeWithTag("menu-open-pack").performClick()
        advanceBy(1_900)
        composeRule.onNodeWithTag("pack-extension-enter-astronomes-en-herbe").performClick()
        advanceBy(1_900)
        composeRule.onNodeWithTag("pack-booster-0").performClick()
        advanceBy(1_200)
        composeRule.onNodeWithTag("pack-opening-title").assertIsDisplayed()

        pressAndroidBack()
        composeRule.onNodeWithTag("menu-open-pack").assertIsDisplayed()
    }

    @Test
    fun android_back_from_main_menu_finishes_activity() {
        setAppContent(hostTestAppContainer())
        loginAndReachMainMenu()

        pressAndroidBack()
        assertTrue(composeRule.activity.isFinishing)
    }

    private fun setAppContent(appContainer: AppContainer) {
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            GatchaTheme {
                GatchaApp(appContainer = appContainer)
            }
        }
        advanceBy(2_200)
    }

    private fun loginAndReachMainMenu() {
        composeRule.onNodeWithTag("login-username").performTextInput("alice")
        composeRule.onNodeWithTag("login-password").performTextInput("password")
        composeRule.onNodeWithTag("login-submit").performClick()
        advanceBy(1_600)
        composeRule.onNodeWithTag("menu-open-pack").assertIsDisplayed()
    }

    private fun advanceBy(durationMillis: Long) {
        composeRule.mainClock.advanceTimeBy(durationMillis)
        composeRule.waitForIdle()
    }

    private fun pressAndroidBack() {
        composeRule.activity.runOnUiThread {
            composeRule.activity.onBackPressedDispatcher.onBackPressed()
        }
        composeRule.waitForIdle()
    }
}

private fun hostTestAppContainer(): AppContainer {
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
        sessionRepository = HostTestSessionGateway(),
        catalogRepository = HostTestCatalogGateway(
            extensions = listOf(extension),
            cards = listOf(cardDefinition),
            variantProfiles = listOf(hostTestVariantProfile()),
        ),
        apiService = HostTestAuthGateway(),
        appStatusRepository = HostTestAppStatusGateway(),
        collectionRepository = HostTestCollectionGateway(
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
        packRepository = HostTestPackGateway(packResponse),
    )
}

private fun hostTestVariantProfile(): VariantProfile = VariantProfile(
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

private class HostTestAppStatusGateway : AppStatusGateway {
    private val mutableState = MutableStateFlow<AppCompatibilityState>(AppCompatibilityState.Compatible)

    override val state: StateFlow<AppCompatibilityState> = mutableState

    override suspend fun verifyCompatibility() {
        mutableState.value = AppCompatibilityState.Compatible
    }
}

private class HostTestAuthGateway : AuthGateway {
    override suspend fun createAccount(request: CreateAccountRequest): CreateAccountResponse =
        CreateAccountResponse(username = request.username, createdAt = "2026-03-23T12:00:00Z")

    override suspend fun login(request: LoginRequest): LoginResponse =
        LoginResponse(username = request.username, lastSavedAt = null, nextDrawAt = null)
}

private class HostTestCatalogGateway(
    private val extensions: List<ExtensionDefinition>,
    private val cards: List<CardDefinition>,
    private val variantProfiles: List<VariantProfile>,
) : CatalogGateway {
    override suspend fun loadMetadata(): CatalogMetadata = CatalogMetadata(catalogVersion = 1)

    override suspend fun loadExtensions(): List<ExtensionDefinition> = extensions

    override suspend fun loadCards(): List<CardDefinition> = cards

    override suspend fun loadVariantProfiles(): List<VariantProfile> = variantProfiles
}

private class HostTestCollectionGateway(
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

private class HostTestSessionGateway : SessionGateway {
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

private class HostTestPackGateway(
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
