package fr.aumombelli.gatcha.network

import fr.aumombelli.gatcha.data.AppCompatibilityController
import fr.aumombelli.gatcha.data.AppStatusApi
import fr.aumombelli.gatcha.data.AuthGateway
import fr.aumombelli.gatcha.data.CatalogGateway
import fr.aumombelli.gatcha.model.ApiError
import fr.aumombelli.gatcha.model.AppStatusRequest
import fr.aumombelli.gatcha.model.AppStatusResponse
import fr.aumombelli.gatcha.model.CompatibilityStatuses
import fr.aumombelli.gatcha.model.CreateAccountRequest
import fr.aumombelli.gatcha.model.CreateAccountResponse
import fr.aumombelli.gatcha.model.DrawPackApiResponse
import fr.aumombelli.gatcha.model.DrawPackRequest
import fr.aumombelli.gatcha.model.DrawPackResponse
import fr.aumombelli.gatcha.model.GetCollectionRequest
import fr.aumombelli.gatcha.model.GetCollectionResponse
import fr.aumombelli.gatcha.model.LoginRequest
import fr.aumombelli.gatcha.model.LoginResponse
import fr.aumombelli.gatcha.model.PackCard
import fr.aumombelli.gatcha.model.SaveCollectionRequest
import fr.aumombelli.gatcha.model.SaveCollectionResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

class ApiCallException(
    val code: String,
    override val message: String,
    val retryAt: String? = null,
) : Exception(message)

class GameApiService(
    private val client: HttpClient,
    private val json: Json,
    private val catalogGateway: CatalogGateway,
    private val compatibilityController: AppCompatibilityController,
    private val baseUrl: String = "http://gatcha.aumombelli.fr:8080",
) : AuthGateway, AppStatusApi {
    override suspend fun fetchAppStatus(): AppStatusResponse =
        post(
            path = "/api/app/status",
            payload = AppStatusRequest(catalogGateway.loadMetadata().catalogVersion),
            includeCatalogVersionHeader = false,
        )

    override suspend fun createAccount(request: CreateAccountRequest): CreateAccountResponse =
        post("/api/account/create", request)

    override suspend fun login(request: LoginRequest): LoginResponse =
        post("/api/account/login", request)

    suspend fun getCollection(request: GetCollectionRequest): GetCollectionResponse =
        post("/api/collection/get", request)

    suspend fun saveCollection(request: SaveCollectionRequest): SaveCollectionResponse =
        post("/api/collection/save", request)

    suspend fun drawPack(request: DrawPackRequest): DrawPackResponse {
        val response: DrawPackApiResponse = post("/api/packs/draw", request)
        val cardsById = catalogGateway.loadCards().associateBy { it.id }
        val cards = response.cardIds.map { cardId ->
            val definition = checkNotNull(cardsById[cardId]) {
                "Server returned an unknown card id for the current catalog: $cardId"
            }
            PackCard(
                cardId = definition.id,
                name = definition.name,
                rarityLabel = definition.rarityLabel,
                imageRef = definition.imageRef,
            )
        }
        return DrawPackResponse(
            extensionId = response.extensionId,
            drawnAt = response.drawnAt,
            nextDrawAt = response.nextDrawAt,
            cards = cards,
        )
    }

    private suspend inline fun <reified T> post(
        path: String,
        payload: Any,
        includeCatalogVersionHeader: Boolean = true,
    ): T {
        val catalogVersion = if (includeCatalogVersionHeader) {
            catalogGateway.loadMetadata().catalogVersion
        } else {
            null
        }
        val response = client.post("$baseUrl$path") {
            headers {
                append(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                catalogVersion?.let { append(CatalogVersionHeader, it.toString()) }
            }
            setBody(payload)
        }
        if (response.status.isSuccess()) {
            return response.body()
        }

        val rawBody = response.bodyAsText()
        val error = try {
            json.decodeFromString(ApiError.serializer(), rawBody)
        } catch (_: SerializationException) {
            ApiError(code = "unknown_error", message = rawBody.ifBlank { "Unknown network error." })
        }
        if (error.code in CompatibilityErrorCodes) {
            compatibilityController.markBlocked(error.message)
        }
        throw ApiCallException(error.code, error.message, error.retryAt)
    }

    private companion object {
        const val CatalogVersionHeader = "X-Gatcha-Catalog-Version"
        val CompatibilityErrorCodes = setOf(
            CompatibilityStatuses.ClientUpdateRequired,
            CompatibilityStatuses.ServerUpdatePending,
        )
    }
}
