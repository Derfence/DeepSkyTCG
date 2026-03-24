package fr.aumombelli.gatcha

import fr.aumombelli.gatcha.data.AppCompatibilityController
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
            metadata = metadata.copy(catalogVersion = 2)
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

        assertEquals("2", capturedHeader)
    }
}
