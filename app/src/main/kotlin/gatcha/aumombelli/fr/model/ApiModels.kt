package gatcha.aumombelli.fr.model

import kotlinx.serialization.Serializable

@Serializable
data class CreateAccountRequest(
    val username: String,
    val email: String,
    val passwordHash: String,
)

@Serializable
data class CreateAccountResponse(
    val username: String,
    val createdAt: String,
)

@Serializable
data class LoginRequest(
    val username: String,
    val passwordHash: String,
)

@Serializable
data class LoginResponse(
    val username: String,
    val lastSavedAt: String? = null,
    val nextDrawAt: String? = null,
)

@Serializable
data class GetCollectionRequest(
    val username: String,
    val passwordHash: String,
)

@Serializable
data class GetCollectionResponse(
    val collectionBlob: String,
    val savedAt: String? = null,
)

@Serializable
data class SaveCollectionRequest(
    val username: String,
    val passwordHash: String,
    val collectionBlob: String,
)

@Serializable
data class SaveCollectionResponse(
    val savedAt: String,
)

@Serializable
data class DrawPackRequest(
    val username: String,
    val passwordHash: String,
    val extensionId: String,
)

@Serializable
data class DrawPackResponse(
    val extensionId: String,
    val drawnAt: String,
    val nextDrawAt: String,
    val cards: List<PackCard>,
)

@Serializable
data class ApiError(
    val code: String,
    val message: String,
    val retryAt: String? = null,
)
