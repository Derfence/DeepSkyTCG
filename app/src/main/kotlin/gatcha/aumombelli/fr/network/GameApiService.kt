package gatcha.aumombelli.fr.network

import gatcha.aumombelli.fr.data.AuthGateway
import gatcha.aumombelli.fr.model.ApiError
import gatcha.aumombelli.fr.model.CreateAccountRequest
import gatcha.aumombelli.fr.model.CreateAccountResponse
import gatcha.aumombelli.fr.model.DrawPackRequest
import gatcha.aumombelli.fr.model.DrawPackResponse
import gatcha.aumombelli.fr.model.GetCollectionRequest
import gatcha.aumombelli.fr.model.GetCollectionResponse
import gatcha.aumombelli.fr.model.LoginRequest
import gatcha.aumombelli.fr.model.LoginResponse
import gatcha.aumombelli.fr.model.SaveCollectionRequest
import gatcha.aumombelli.fr.model.SaveCollectionResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
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
    private val baseUrl: String = "http://10.0.2.2:8080",
) : AuthGateway {
    override suspend fun createAccount(request: CreateAccountRequest): CreateAccountResponse =
        post("/api/account/create", request)

    override suspend fun login(request: LoginRequest): LoginResponse =
        post("/api/account/login", request)

    suspend fun getCollection(request: GetCollectionRequest): GetCollectionResponse =
        post("/api/collection/get", request)

    suspend fun saveCollection(request: SaveCollectionRequest): SaveCollectionResponse =
        post("/api/collection/save", request)

    suspend fun drawPack(request: DrawPackRequest): DrawPackResponse =
        post("/api/packs/draw", request)

    private suspend inline fun <reified T> post(path: String, payload: Any): T {
        val response = client.post("$baseUrl$path") {
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
        throw ApiCallException(error.code, error.message, error.retryAt)
    }
}
