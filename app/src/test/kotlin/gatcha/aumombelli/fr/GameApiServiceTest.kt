package fr.aumombelli.gatcha

import fr.aumombelli.gatcha.data.AppCompatibilityController
import fr.aumombelli.gatcha.data.AppCompatibilityState
import fr.aumombelli.gatcha.model.ApiError
import fr.aumombelli.gatcha.model.AppStatusResponse
import fr.aumombelli.gatcha.model.CompatibilityStatuses
import fr.aumombelli.gatcha.model.DrawPackApiResponse
import fr.aumombelli.gatcha.model.DrawPackRequest
import fr.aumombelli.gatcha.model.DrawnCardReference
import fr.aumombelli.gatcha.model.LoginRequest
import fr.aumombelli.gatcha.model.LoginResponse
import fr.aumombelli.gatcha.network.ApiCallException
import fr.aumombelli.gatcha.network.GameApiService
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class GameApiServiceTest {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    @Test
    fun `fetch app status request omits catalog version header`() = runTest {
        val catalogGateway = FakeCatalogGateway().apply {
            metadata = metadata.copy(catalogVersion = 5)
        }
        var capturedHeader: String? = "missing"
        val apiService = newApiService(
            engine = MockEngine { request ->
                capturedHeader = request.headers["X-Gatcha-Catalog-Version"]
                respond(
                    content = json.encodeToString(
                        AppStatusResponse.serializer(),
                        AppStatusResponse(
                            serverCatalogVersion = 5,
                            minimumSupportedCatalogVersion = 5,
                            compatibilityStatus = CompatibilityStatuses.Compatible,
                            message = "Catalog version 5 is compatible with the server.",
                        ),
                    ),
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                )
            },
            catalogGateway = catalogGateway,
        )

        apiService.fetchAppStatus()

        assertEquals(null, capturedHeader)
    }

    @Test
    fun `login request includes catalog version header`() = runTest {
        val catalogGateway = FakeCatalogGateway().apply {
            metadata = metadata.copy(catalogVersion = 5)
        }
        var capturedHeader: String? = null
        val apiService = newApiService(
            engine = MockEngine { request ->
                capturedHeader = request.headers["X-Gatcha-Catalog-Version"]
                respond(
                    content = json.encodeToString(LoginResponse.serializer(), LoginResponse(username = "alice")),
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                )
            },
            catalogGateway = catalogGateway,
        )

        apiService.login(
            LoginRequest(
                username = "alice",
                passwordHash = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef",
            ),
        )

        assertEquals("5", capturedHeader)
    }

    @Test
    fun `api errors propagate retryAt`() = runTest {
        val apiService = newApiService(
            engine = MockEngine {
                respond(
                    content = json.encodeToString(
                        ApiError.serializer(),
                        ApiError(
                            code = "draw_cooldown",
                            message = "Retry later.",
                            retryAt = "2026-03-25T00:00:00Z",
                        ),
                    ),
                    status = HttpStatusCode.TooManyRequests,
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                )
            },
        )

        val exception = try {
            apiService.login(
                LoginRequest(
                    username = "alice",
                    passwordHash = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef",
                ),
            )
            error("Expected ApiCallException to be thrown.")
        } catch (error: ApiCallException) {
            error
        }

        assertEquals("draw_cooldown", exception.code)
        assertEquals("Retry later.", exception.message)
        assertEquals("2026-03-25T00:00:00Z", exception.retryAt)
    }

    @Test
    fun `compatibility api errors mark the app as blocked`() = runTest {
        val compatibilityController = AppCompatibilityController()
        val apiService = newApiService(
            engine = MockEngine {
                respond(
                    content = json.encodeToString(
                        ApiError.serializer(),
                        ApiError(
                            code = CompatibilityStatuses.ClientUpdateRequired,
                            message = "A newer client is required.",
                        ),
                    ),
                    status = HttpStatusCode.Conflict,
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                )
            },
            compatibilityController = compatibilityController,
        )

        try {
            apiService.login(
                LoginRequest(
                    username = "alice",
                    passwordHash = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef",
                ),
            )
            error("Expected ApiCallException to be thrown.")
        } catch (_: ApiCallException) {
        }

        assertEquals(
            AppCompatibilityState.Blocked("A newer client is required."),
            compatibilityController.state.value,
        )
    }

    @Test
    fun `draw pack resolves card variants against local catalog`() = runTest {
        val catalogGateway = FakeCatalogGateway().apply {
            cards = listOf(
                testCardDefinition("ALP-001", name = "Nebuleuse d'Orion", rarityLabel = "Common", imageRef = "m42"),
                testCardDefinition("ALP-002", name = "Galaxie d'Andromede", rarityLabel = "Rare", imageRef = "m31"),
            )
        }
        val apiService = newApiService(
            engine = MockEngine {
                respond(
                    content = json.encodeToString(
                        DrawPackApiResponse.serializer(),
                        DrawPackApiResponse(
                            extensionId = "astronomes-en-herbe",
                            drawnAt = "2026-03-24T12:00:00Z",
                            nextDrawAt = "2026-03-25T00:00:00Z",
                            cards = listOf(
                                DrawnCardReference("ALP-002", "rural", "holographic"),
                                DrawnCardReference("ALP-001", "city", "standard"),
                            ),
                        ),
                    ),
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                )
            },
            catalogGateway = catalogGateway,
        )

        val response = apiService.drawPack(
            DrawPackRequest(
                username = "alice",
                passwordHash = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef",
                extensionId = "astronomes-en-herbe",
            ),
        )

        assertEquals(
            listOf(
                testPackCard(
                    "ALP-002",
                    "Galaxie d'Andromede",
                    "Rare",
                    "m31",
                    skyQuality = "rural",
                    skyQualityLabel = "Campagne",
                    finish = "holographic",
                    finishLabel = "Holographique",
                    isHolographic = true,
                ),
                testPackCard(
                    "ALP-001",
                    "Nebuleuse d'Orion",
                    "Common",
                    "m42",
                    skyQuality = "city",
                    skyQualityLabel = "Ville",
                    finish = "standard",
                    finishLabel = "Standard",
                ),
            ),
            response.cards,
        )
    }

    private fun newApiService(
        engine: MockEngine,
        catalogGateway: FakeCatalogGateway = FakeCatalogGateway(),
        compatibilityController: AppCompatibilityController = AppCompatibilityController(),
    ): GameApiService = GameApiService(
        client = HttpClient(engine) {
            install(ContentNegotiation) {
                json(json)
            }
        },
        json = json,
        catalogGateway = catalogGateway,
        compatibilityController = compatibilityController,
    )
}
