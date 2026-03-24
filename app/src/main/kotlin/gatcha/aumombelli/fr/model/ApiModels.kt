package fr.aumombelli.gatcha.model

import kotlinx.serialization.Serializable

object CompatibilityStatuses {
    const val Compatible = "compatible"
    const val ClientUpdateRequired = "client_update_required"
    const val ServerUpdatePending = "server_update_pending"
}

@Serializable
data class AppStatusRequest(
    val catalogVersion: Int,
)

@Serializable
data class AppStatusResponse(
    val serverCatalogVersion: Int,
    val minimumSupportedCatalogVersion: Int,
    val compatibilityStatus: String,
    val message: String? = null,
)

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
data class DrawPackApiResponse(
    val extensionId: String,
    val drawnAt: String,
    val nextDrawAt: String,
    val cards: List<DrawnCardReference>,
)

@Serializable
data class DrawnCardReference(
    val cardId: String,
    val skyQuality: String,
    val finish: String,
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
