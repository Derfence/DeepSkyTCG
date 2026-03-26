package fr.aumombelli.gatcha

import fr.aumombelli.gatcha.data.SecurityUtils
import fr.aumombelli.gatcha.feature.auth.LoginEvent
import fr.aumombelli.gatcha.network.ApiCallException
import fr.aumombelli.gatcha.ui.viewmodel.LoginViewModel
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
    fun `submit requires username and password`() = runTest {
        val authGateway = FakeAuthGateway()
        val viewModel = LoginViewModel(
            apiService = authGateway,
            sessionRepository = FakeSessionGateway(),
            collectionRepository = FakeCollectionGateway(),
        )

        viewModel.submit()
        advanceUntilIdle()

        assertEquals("Username and password are required.", viewModel.uiState.value.errorMessage)
        assertEquals(0, authGateway.createRequests.size)
        assertEquals(0, authGateway.loginRequests.size)
    }

    @Test
    fun `submit in create mode requires email`() = runTest {
        val authGateway = FakeAuthGateway()
        val viewModel = LoginViewModel(
            apiService = authGateway,
            sessionRepository = FakeSessionGateway(),
            collectionRepository = FakeCollectionGateway(),
        )

        viewModel.toggleMode()
        viewModel.updateUsername("alice")
        viewModel.updatePassword("secret")
        viewModel.submit()
        advanceUntilIdle()

        assertEquals("Email is required to create an account.", viewModel.uiState.value.errorMessage)
        assertEquals(0, authGateway.createRequests.size)
        assertEquals(0, authGateway.loginRequests.size)
    }

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
            serverCollection = ownedCollectionOf("ALP-001" to 1)
        }
        val viewModel = LoginViewModel(authGateway, sessionGateway, collectionGateway)

        viewModel.toggleMode()
        viewModel.updateUsername(" Alice ")
        viewModel.updateEmail(" alice@example.com ")
        viewModel.updatePassword("secret")

        val event = async { viewModel.events.first() }
        viewModel.submit()
        advanceUntilIdle()

        assertEquals(1, authGateway.createRequests.size)
        assertEquals(1, authGateway.loginRequests.size)
        assertEquals("alice", authGateway.createRequests.single().username)
        assertEquals("alice@example.com", authGateway.createRequests.single().email)
        assertEquals(
            SecurityUtils.computeClientPasswordHash("alice", "secret"),
            authGateway.createRequests.single().passwordHash,
        )
        assertEquals("alice", authGateway.loginRequests.single().username)
        assertEquals(
            SecurityUtils.computeClientPasswordHash("alice", "secret"),
            authGateway.loginRequests.single().passwordHash,
        )
        assertEquals("alice", sessionGateway.activeSession?.username)
        assertEquals(
            SecurityUtils.computeClientPasswordHash("alice", "secret"),
            sessionGateway.activeSession?.passwordHash,
        )
        assertEquals(1, collectionGateway.replayPendingSaveCallCount.get())
        assertEquals(1, collectionGateway.loadCollectionCallCount.get())
        assertEquals(LoginEvent.AuthenticationSucceeded, event.await())
        assertEquals(true, viewModel.uiState.value.isTransitioningToMenu)
        assertEquals(false, viewModel.uiState.value.isLoading)
        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `submit surfaces replay pending save failure`() = runTest {
        val collectionGateway = FakeCollectionGateway().apply {
            replayFailure = IllegalStateException("Pending save replay failed.")
        }
        val viewModel = LoginViewModel(
            apiService = FakeAuthGateway(),
            sessionRepository = FakeSessionGateway(),
            collectionRepository = collectionGateway,
        )

        viewModel.updateUsername("alice")
        viewModel.updatePassword("secret")
        viewModel.submit()
        advanceUntilIdle()

        assertEquals("Pending save replay failed.", viewModel.uiState.value.errorMessage)
        assertEquals(false, viewModel.uiState.value.isTransitioningToMenu)
        assertEquals(1, collectionGateway.replayPendingSaveCallCount.get())
        assertEquals(0, collectionGateway.loadCollectionCallCount.get())
    }

    @Test
    fun `submit surfaces collection loading failure after login`() = runTest {
        val collectionGateway = FakeCollectionGateway().apply {
            loadCollectionFailure = IllegalStateException("Collection unavailable.")
        }
        val viewModel = LoginViewModel(
            apiService = FakeAuthGateway(),
            sessionRepository = FakeSessionGateway(),
            collectionRepository = collectionGateway,
        )

        viewModel.updateUsername("alice")
        viewModel.updatePassword("secret")
        viewModel.submit()
        advanceUntilIdle()

        assertEquals("Collection unavailable.", viewModel.uiState.value.errorMessage)
        assertEquals(false, viewModel.uiState.value.isTransitioningToMenu)
        assertEquals(1, collectionGateway.replayPendingSaveCallCount.get())
        assertEquals(1, collectionGateway.loadCollectionCallCount.get())
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
