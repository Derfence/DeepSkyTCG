package fr.aumombelli.gatcha

import fr.aumombelli.gatcha.feature.start.StartEvent
import fr.aumombelli.gatcha.feature.start.StartViewModel
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
class StartViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `init loads local progress and clears loading state`() = runTest {
        val progressGateway = FakeProgressGateway().apply {
            progress = progress.copy(collection = ownedCollectionOf("ALP-001" to 1).copy(version = 5))
        }

        val viewModel = StartViewModel(progressGateway)
        advanceUntilIdle()

        assertEquals(1, progressGateway.loadCallCount.get())
        assertEquals(false, viewModel.uiState.value.isLoading)
        assertEquals(false, viewModel.uiState.value.isTransitioningToMenu)
        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `begin emits navigation once local progress is ready`() = runTest {
        val viewModel = StartViewModel(FakeProgressGateway())
        advanceUntilIdle()

        val event = async { viewModel.events.first() }
        viewModel.begin()
        advanceUntilIdle()

        assertEquals(StartEvent.ReadyToEnterMenu, event.await())
        assertEquals(true, viewModel.uiState.value.isTransitioningToMenu)
    }

    @Test
    fun `init surfaces progress loading failure`() = runTest {
        val progressGateway = FakeProgressGateway().apply {
            loadFailure = IllegalStateException("Saved progression could not be read.")
        }

        val viewModel = StartViewModel(progressGateway)
        advanceUntilIdle()

        assertEquals(false, viewModel.uiState.value.isLoading)
        assertEquals("Saved progression could not be read.", viewModel.uiState.value.errorMessage)
    }
}
