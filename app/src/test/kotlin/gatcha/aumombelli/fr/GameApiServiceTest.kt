package fr.aumombelli.gatcha

import fr.aumombelli.gatcha.data.AppCompatibilityController
import fr.aumombelli.gatcha.model.CardDefinition
import fr.aumombelli.gatcha.model.DrawPackApiResponse
import fr.aumombelli.gatcha.model.DrawPackRequest
import fr.aumombelli.gatcha.model.PackCard
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
            metadata = metadata.copy(catalogVersion = 3)
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

        assertEquals("3", capturedHeader)
    }

    @Test
    fun `draw pack resolves card ids against local catalog`() = runTest {
        val catalogGateway = FakeCatalogGateway().apply {
            cards = listOf(
                CardDefinition("ALP-001", "core-alpha", "Spark Fox", "Common", 40, "spark_fox"),
                CardDefinition("ALP-002", "core-alpha", "Steam Golem", "Rare", 12, "steam_golem"),
            )
        }
        val engine = MockEngine {
            respond(
                content = json.encodeToString(
                    DrawPackApiResponse.serializer(),
                    DrawPackApiResponse(
                        extensionId = "core-alpha",
                        drawnAt = "2026-03-24T12:00:00Z",
                        nextDrawAt = "2026-03-25T00:00:00Z",
                        cardIds = listOf("ALP-002", "ALP-001"),
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
                extensionId = "core-alpha",
            ),
        )

        assertEquals(
            listOf(
                PackCard("ALP-002", "Steam Golem", "Rare", "steam_golem"),
                PackCard("ALP-001", "Spark Fox", "Common", "spark_fox"),
            ),
            response.cards,
        )
    }
}
