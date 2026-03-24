package fr.aumombelli.gatcha

import fr.aumombelli.gatcha.data.AppCompatibilityController
import fr.aumombelli.gatcha.model.CardDefinition
import fr.aumombelli.gatcha.model.DrawPackApiResponse
import fr.aumombelli.gatcha.model.DrawnCardReference
import fr.aumombelli.gatcha.model.DrawPackRequest
import fr.aumombelli.gatcha.model.LoginRequest
import fr.aumombelli.gatcha.model.LoginResponse
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
    fun `login request includes catalog version header`() = runTest {
        val catalogGateway = FakeCatalogGateway().apply {
            metadata = metadata.copy(catalogVersion = 4)
        }
        var capturedHeader: String? = null
        val engine = MockEngine { request ->
            capturedHeader = request.headers["X-Gatcha-Catalog-Version"]
            respond(
                content = json.encodeToString(LoginResponse.serializer(), LoginResponse(username = "alice")),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
            )
        }
        val apiService = GameApiService(
            client = HttpClient(engine) {
                install(ContentNegotiation) {
                    json(json)
                }
            },
            json = json,
            catalogGateway = catalogGateway,
            compatibilityController = AppCompatibilityController(),
        )

        apiService.login(
            LoginRequest(
                username = "alice",
                passwordHash = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef",
            ),
        )

        assertEquals("4", capturedHeader)
    }

    @Test
    fun `draw pack resolves card variants against local catalog`() = runTest {
        val catalogGateway = FakeCatalogGateway().apply {
            cards = listOf(
                testCardDefinition("ALP-001", name = "Nebuleuse d'Orion", rarityLabel = "Common", imageRef = "m42"),
                testCardDefinition("ALP-002", name = "Galaxie d'Andromede", rarityLabel = "Rare", imageRef = "m31"),
            )
        }
        val engine = MockEngine {
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
        }
        val apiService = GameApiService(
            client = HttpClient(engine) {
                install(ContentNegotiation) {
                    json(json)
                }
            },
            json = json,
            catalogGateway = catalogGateway,
            compatibilityController = AppCompatibilityController(),
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
}
