package fr.aumombelli.gatcha

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import fr.aumombelli.gatcha.data.AppCompatibilityState
import fr.aumombelli.gatcha.data.AppStatusGateway
import fr.aumombelli.gatcha.data.AuthGateway
import fr.aumombelli.gatcha.data.CatalogGateway
import fr.aumombelli.gatcha.data.CollectionGateway
import fr.aumombelli.gatcha.data.PackGateway
import fr.aumombelli.gatcha.data.SessionGateway
import fr.aumombelli.gatcha.model.CardDefinition
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
import fr.aumombelli.gatcha.model.VariantProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.junit.After
import org.junit.Rule
import org.junit.Test

class LoginScreenTest {
    init {
        MainActivity.appContainerFactory = { compatibleAppContainer() }
    }

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun login_screen_is_shown_on_launch() {
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithTag("login-submit")
                .fetchSemanticsNodes(atLeastOneRootRequired = false)
                .isNotEmpty()
        }
        composeRule.onNodeWithTag("login-submit").assertIsDisplayed()
        composeRule.onNodeWithText("Gatcha").assertIsDisplayed()
        composeRule.onNodeWithText("Login").assertIsDisplayed()
    }

    @After
    fun tearDown() {
        MainActivity.appContainerFactory = null
    }

    private fun compatibleAppContainer(): AppContainer {
        val appStatusGateway = object : AppStatusGateway {
            private val stateFlow = MutableStateFlow<AppCompatibilityState>(AppCompatibilityState.Compatible)
            override val state: StateFlow<AppCompatibilityState> = stateFlow
            override suspend fun verifyCompatibility() = Unit
        }
        return AppContainer(
            sessionRepository = object : SessionGateway {
                override fun setActiveSession(username: String, passwordHash: String) = Unit
                override fun clearActiveSession() = Unit
                override fun requireActiveSession(): SessionCredentials = SessionCredentials("alice", "hash")
                override suspend fun readSnapshot(): StoredSessionSnapshot = StoredSessionSnapshot()
                override suspend fun saveLoginMetadata(username: String, lastSavedAt: String?, nextDrawAt: String?) = Unit
                override suspend fun commitSavedCollection(collectionBlob: String, savedAt: String?, nextDrawAt: String?) = Unit
                override suspend fun savePendingPack(collectionBlob: String, packResponse: DrawPackResponse) = Unit
                override suspend fun clearPendingPack() = Unit
                override suspend fun decodePendingPack(): DrawPackResponse? = null
            },
            catalogRepository = object : CatalogGateway {
                override suspend fun loadMetadata(): CatalogMetadata = CatalogMetadata(catalogVersion = 4)
                override suspend fun loadExtensions(): List<ExtensionDefinition> = emptyList()
                override suspend fun loadCards(): List<CardDefinition> = emptyList()
                override suspend fun loadVariantProfiles(): List<VariantProfile> = emptyList()
            },
            apiService = object : AuthGateway {
                override suspend fun createAccount(request: CreateAccountRequest): CreateAccountResponse =
                    CreateAccountResponse(username = request.username, createdAt = "2026-03-24T12:00:00Z")

                override suspend fun login(request: LoginRequest): LoginResponse =
                    LoginResponse(username = request.username)
            },
            appStatusRepository = appStatusGateway,
            collectionRepository = object : CollectionGateway {
                override suspend fun loadCollectionFromServer(): OwnedCollection = OwnedCollection(version = 4)
                override suspend fun getCachedCollectionOrEmpty(): OwnedCollection = OwnedCollection(version = 4)
                override suspend fun saveCollection(collection: OwnedCollection): String = "2026-03-24T12:00:00Z"
                override suspend fun replayPendingSaveIfNeeded(): Boolean = false
                override fun mergeCards(collection: OwnedCollection, cards: List<PackCard>): OwnedCollection = collection
            },
            packRepository = object : PackGateway {
                private val packFlow = MutableStateFlow<DrawPackResponse?>(null)
                override fun currentPackResult(): StateFlow<DrawPackResponse?> = packFlow
                override suspend fun openPack(extensionId: String, currentCollection: OwnedCollection): DrawPackResponse {
                    error("Not used in LoginScreenTest")
                }
            },
        )
    }
}
