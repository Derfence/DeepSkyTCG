package fr.aumombelli.gatcha

import fr.aumombelli.gatcha.data.AppCompatibilityController
import fr.aumombelli.gatcha.data.AppCompatibilityState
import fr.aumombelli.gatcha.data.AppStatusApi
import fr.aumombelli.gatcha.data.AppStatusRepository
import fr.aumombelli.gatcha.model.AppStatusResponse
import fr.aumombelli.gatcha.model.CompatibilityStatuses
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class AppStatusRepositoryTest {
    @Test
    fun `verify compatibility marks compatible response as compatible`() = runTest {
        val repository = newRepository(
            response = AppStatusResponse(
                serverCatalogVersion = 5,
                minimumSupportedCatalogVersion = 5,
                compatibilityStatus = CompatibilityStatuses.Compatible,
                message = "Catalog version 5 is compatible with the server.",
            ),
        )

        repository.verifyCompatibility()

        assertEquals(AppCompatibilityState.Compatible, repository.state.value)
    }

    @Test
    fun `verify compatibility blocks client update required response`() = runTest {
        val repository = newRepository(
            response = AppStatusResponse(
                serverCatalogVersion = 6,
                minimumSupportedCatalogVersion = 6,
                compatibilityStatus = CompatibilityStatuses.ClientUpdateRequired,
                message = "A newer client is required.",
            ),
        )

        repository.verifyCompatibility()

        assertEquals(
            AppCompatibilityState.Blocked("A newer client is required."),
            repository.state.value,
        )
    }

    @Test
    fun `verify compatibility blocks server update pending response`() = runTest {
        val repository = newRepository(
            response = AppStatusResponse(
                serverCatalogVersion = 6,
                minimumSupportedCatalogVersion = 5,
                compatibilityStatus = CompatibilityStatuses.ServerUpdatePending,
                message = "The server is still being upgraded.",
            ),
        )

        repository.verifyCompatibility()

        assertEquals(
            AppCompatibilityState.Blocked("The server is still being upgraded."),
            repository.state.value,
        )
    }

    @Test
    fun `verify compatibility blocks unknown status response`() = runTest {
        val repository = newRepository(
            response = AppStatusResponse(
                serverCatalogVersion = 5,
                minimumSupportedCatalogVersion = 5,
                compatibilityStatus = "mystery_status",
                message = "Unexpected compatibility state.",
            ),
        )

        repository.verifyCompatibility()

        assertEquals(
            AppCompatibilityState.Blocked("Catalog compatibility could not be determined."),
            repository.state.value,
        )
    }

    @Test
    fun `verify compatibility blocks network failures`() = runTest {
        val repository = newRepository(
            failure = IllegalStateException("Server offline."),
        )

        repository.verifyCompatibility()

        assertEquals(
            AppCompatibilityState.Blocked("Server offline."),
            repository.state.value,
        )
    }

    private fun newRepository(
        response: AppStatusResponse? = null,
        failure: Throwable? = null,
    ): AppStatusRepository {
        val api = object : AppStatusApi {
            override suspend fun fetchAppStatus(): AppStatusResponse {
                failure?.let { throw it }
                return checkNotNull(response) { "AppStatusResponse must be configured for the test." }
            }
        }

        return AppStatusRepository(
            apiService = api,
            compatibilityController = AppCompatibilityController(),
        )
    }
}
