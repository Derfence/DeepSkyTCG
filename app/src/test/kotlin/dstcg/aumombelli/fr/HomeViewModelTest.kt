package fr.aumombelli.dstcg

import fr.aumombelli.dstcg.feature.home.HomeViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `init loads local progress and clears loading state`() = runTest {
        val progressGateway = FakeProgressGateway().apply {
            progress = progress.copy(collection = ownedCollectionOf("ALP-001" to 1))
        }

        val viewModel = HomeViewModel(progressGateway)
        advanceUntilIdle()

        assertEquals(1, progressGateway.loadCallCount.get())
        assertEquals(false, viewModel.uiState.value.isLoading)
        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `reset is ignored while loading`() = runTest {
        val progressGateway = FakeProgressGateway()
        val viewModel = HomeViewModel(progressGateway)

        viewModel.resetProgress()
        advanceUntilIdle()

        assertEquals(0, progressGateway.resetCallCount.get())
        assertEquals(false, viewModel.uiState.value.isLoading)
    }

    @Test
    fun `init surfaces progress loading failure`() = runTest {
        val progressGateway = FakeProgressGateway().apply {
            loadFailure = IllegalStateException("Saved progression could not be read.")
        }

        val viewModel = HomeViewModel(progressGateway)
        advanceUntilIdle()

        assertEquals(false, viewModel.uiState.value.isLoading)
        assertEquals("Saved progression could not be read.", viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `init surfaces recovered progress silently`() = runTest {
        val progressGateway = FakeProgressGateway().apply {
            recoveryNotice = "La progression locale a été sécurisée."
        }

        val viewModel = HomeViewModel(progressGateway)
        advanceUntilIdle()

        assertEquals(false, viewModel.uiState.value.isLoading)
        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `reset is allowed even when an error is present`() = runTest {
        val progressGateway = FakeProgressGateway().apply {
            loadFailure = IllegalStateException("Saved progression could not be read.")
        }

        val viewModel = HomeViewModel(progressGateway)
        advanceUntilIdle()

        viewModel.resetProgress()
        advanceUntilIdle()

        assertEquals(1, progressGateway.resetCallCount.get())
        assertEquals("Saved progression could not be read.", viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `healthy progress can be reset and reloads clean state`() = runTest {
        val progressGateway = FakeProgressGateway().apply {
            progress = progress.copy(collection = ownedCollectionOf("ALP-001" to 3))
        }

        val viewModel = HomeViewModel(progressGateway)
        advanceUntilIdle()

        viewModel.resetProgress()
        advanceUntilIdle()

        assertEquals(1, progressGateway.resetCallCount.get())
        assertNull(viewModel.uiState.value.errorMessage)
        assertEquals(false, viewModel.uiState.value.isLoading)
    }

    @Test
    fun `compromised progress can be reset and reloads clean state`() = runTest {
        val progressGateway = FakeProgressGateway().apply {
            compromisedMessage = "La progression locale semble corrompue."
        }

        val viewModel = HomeViewModel(progressGateway)
        advanceUntilIdle()

        viewModel.resetProgress()
        advanceUntilIdle()

        assertEquals(1, progressGateway.resetCallCount.get())
        assertNull(viewModel.uiState.value.errorMessage)
    }
}
