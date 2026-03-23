package gatcha.aumombelli.fr

import gatcha.aumombelli.fr.network.ApiCallException
import gatcha.aumombelli.fr.ui.viewmodel.LoginEvent
import gatcha.aumombelli.fr.ui.viewmodel.LoginViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `init restores last username from session snapshot`() = runTest {
        val sessionGateway = FakeSessionGateway().apply {
            snapshot = snapshot.copy(lastUsername = "restored-user")
        }

        val viewModel = LoginViewModel(
            apiService = FakeAuthGateway(),
            sessionRepository = sessionGateway,
            collectionRepository = FakeCollectionGateway(),
        )

        advanceUntilIdle()

        assertEquals("restored-user", viewModel.uiState.value.username)
    }

    @Test
    fun `submit in create mode creates account logs in and emits navigation`() = runTest {
        val authGateway = FakeAuthGateway().apply {
            loginResponse = loginResponse.copy(
                username = "alice",
                lastSavedAt = "2026-03-23T12:00:00Z",
                nextDrawAt = "2026-03-24T00:00:00Z",
            )
        }
        val sessionGateway = FakeSessionGateway()
        val collectionGateway = FakeCollectionGateway().apply {
            serverCollection = gatcha.aumombelli.fr.model.OwnedCollection(cards = mapOf("ALP-001" to 1))
        }
        val viewModel = LoginViewModel(authGateway, sessionGateway, collectionGateway)

        viewModel.toggleMode()
        viewModel.updateUsername("Alice")
        viewModel.updateEmail("alice@example.com")
        viewModel.updatePassword("secret")

        val event = async { viewModel.events.first() }
        viewModel.submit()
        advanceUntilIdle()

        assertEquals(1, authGateway.createRequests.size)
        assertEquals(1, authGateway.loginRequests.size)
        assertEquals("alice", sessionGateway.activeSession?.username)
        assertEquals(1, collectionGateway.replayPendingSaveCallCount.get())
        assertEquals(1, collectionGateway.loadCollectionCallCount.get())
        assertEquals(LoginEvent.NavigateToMenu, event.await())
        assertEquals(false, viewModel.uiState.value.isLoading)
        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `submit surfaces authentication error`() = runTest {
        val authGateway = FakeAuthGateway().apply {
            loginFailure = ApiCallException("invalid_credentials", "Invalid credentials.")
        }
        val viewModel = LoginViewModel(
            apiService = authGateway,
            sessionRepository = FakeSessionGateway(),
            collectionRepository = FakeCollectionGateway(),
        )

        viewModel.updateUsername("alice")
        viewModel.updatePassword("wrong")
        viewModel.submit()
        advanceUntilIdle()

        assertEquals("Invalid credentials.", viewModel.uiState.value.errorMessage)
        assertEquals(false, viewModel.uiState.value.isLoading)
    }
}
