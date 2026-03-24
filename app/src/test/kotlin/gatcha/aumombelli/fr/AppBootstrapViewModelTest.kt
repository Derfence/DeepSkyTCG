package fr.aumombelli.gatcha

import fr.aumombelli.gatcha.data.AppCompatibilityState
import fr.aumombelli.gatcha.ui.viewmodel.AppBootstrapViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AppBootstrapViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `init verifies compatibility and exposes compatible state`() = runTest {
        val appStatusGateway = FakeAppStatusGateway().apply {
            onVerify = {
                updateState(AppCompatibilityState.Compatible)
            }
        }

        val viewModel = AppBootstrapViewModel(appStatusGateway)
        advanceUntilIdle()

        assertEquals(1, appStatusGateway.verifyCallCount.get())
        assertEquals(true, viewModel.uiState.value.isCompatible)
        assertEquals(false, viewModel.uiState.value.isLoading)
    }

    @Test
    fun `blocked compatibility state exposes retryable message`() = runTest {
        val appStatusGateway = FakeAppStatusGateway().apply {
            onVerify = {
                updateState(
                    AppCompatibilityState.Blocked(
                        message = "A newer client is required.",
                        canRetry = true,
                    ),
                )
            }
        }

        val viewModel = AppBootstrapViewModel(appStatusGateway)
        advanceUntilIdle()

        assertEquals(false, viewModel.uiState.value.isCompatible)
        assertEquals("A newer client is required.", viewModel.uiState.value.message)
        assertEquals(true, viewModel.uiState.value.canRetry)
    }
}
