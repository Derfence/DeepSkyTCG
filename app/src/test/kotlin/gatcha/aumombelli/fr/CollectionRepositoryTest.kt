package fr.aumombelli.gatcha

import fr.aumombelli.gatcha.data.AppCompatibilityController
import fr.aumombelli.gatcha.data.CollectionCrypto
import fr.aumombelli.gatcha.data.CollectionMigrationService
import fr.aumombelli.gatcha.data.CollectionRepository
import fr.aumombelli.gatcha.model.GetCollectionResponse
import fr.aumombelli.gatcha.model.LoginResponse
import fr.aumombelli.gatcha.model.OwnedCollection
import fr.aumombelli.gatcha.model.PackCard
import fr.aumombelli.gatcha.model.SaveCollectionResponse
import fr.aumombelli.gatcha.model.SessionCredentials
import fr.aumombelli.gatcha.model.mergePackCards
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

class CollectionRepositoryTest {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    @Test
    fun `merge cards increments owned counts`() {
        val merged = OwnedCollection(cards = mapOf("ALP-001" to 1)).mergePackCards(
            cards = listOf(
                PackCard("ALP-001", "Spark Fox", "Common", "spark_fox"),
                PackCard("MON-006", "Eclipse Regent", "Epic", "eclipse_regent"),
            ),
        )

        assertEquals(2, merged.cards["ALP-001"])
        assertEquals(1, merged.cards["MON-006"])
    }

    @Test
    fun `missing cached blob returns empty collection at current catalog version`() = runTest {
        val sessionGateway = FakeSessionGateway().apply {
            activeSession = SessionCredentials("alice", PasswordHash)
        }
        val repository = newRepository(
            sessionGateway = sessionGateway,
        )

        val collection = repository.getCachedCollectionOrEmpty()

        assertEquals(3, collection.version)
        assertEquals(emptyMap<String, Int>(), collection.cards)
    }

    @Test
    fun `load collection from server migrates old blob and saves upgraded version`() = runTest {
        val oldBlob = CollectionCrypto.serializeAndEncrypt(
            OwnedCollection(version = 1, cards = mapOf("ALP-001" to 1)),
            PasswordHash,
        )
        val paths = mutableListOf<String>()
        val repository = newRepository(
            sessionGateway = FakeSessionGateway().apply {
                activeSession = SessionCredentials("alice", PasswordHash)
                snapshot = snapshot.copy(nextDrawAt = "2026-03-24T00:00:00Z")
            },
            handler = { path ->
                paths += path
                when (path) {
                    "/api/collection/get" -> jsonBody(
                        GetCollectionResponse(
                            collectionBlob = oldBlob,
                            savedAt = "2026-03-23T12:00:00Z",
                        ),
                    )
                    "/api/collection/save" -> jsonBody(
                        SaveCollectionResponse(savedAt = "2026-03-24T12:00:00Z"),
                    )
                    else -> error("Unexpected path $path")
                }
            },
        )

        val migrated = repository.loadCollectionFromServer()

        assertEquals(listOf("/api/collection/get", "/api/collection/save"), paths)
        assertEquals(3, migrated.version)
    }

    @Test
    fun `replay pending save migrates old blob before saving`() = runTest {
        val oldBlob = CollectionCrypto.serializeAndEncrypt(
            OwnedCollection(version = 1, cards = mapOf("ALP-001" to 1)),
            PasswordHash,
        )
        val sessionGateway = FakeSessionGateway().apply {
            activeSession = SessionCredentials("alice", PasswordHash)
            snapshot = snapshot.copy(
                lastUsername = "alice",
                pendingCollectionBlob = oldBlob,
                nextDrawAt = "2026-03-24T00:00:00Z",
            )
        }
        val paths = mutableListOf<String>()
        val repository = newRepository(
            sessionGateway = sessionGateway,
            handler = { path ->
                paths += path
                when (path) {
                    "/api/collection/save" -> jsonBody(
                        SaveCollectionResponse(savedAt = "2026-03-24T12:00:00Z"),
                    )
                    else -> error("Unexpected path $path")
                }
            },
        )

        val replayed = repository.replayPendingSaveIfNeeded()

        assertEquals(true, replayed)
        assertEquals(listOf("/api/collection/save"), paths)
        val committedBlob = sessionGateway.committedCollections.single().first
        val migrated = CollectionCrypto.decryptAndDeserialize(committedBlob, PasswordHash)
        assertEquals(3, migrated.version)
    }

    private fun newRepository(
        sessionGateway: FakeSessionGateway,
        handler: suspend (String) -> String = { path ->
            when (path) {
                "/api/account/login" -> jsonBody(LoginResponse(username = "alice"))
                else -> error("Unexpected path $path")
            }
        },
    ): CollectionRepository {
        val engine = MockEngine { request ->
            respond(
                content = handler(request.url.encodedPath),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
            )
        }
        val client = HttpClient(engine) {
            install(ContentNegotiation) {
                json(json)
            }
        }
        val catalogGateway = FakeCatalogGateway()
        val apiService = GameApiService(
            client = client,
            json = json,
            catalogGateway = catalogGateway,
            compatibilityController = AppCompatibilityController(),
        )
        return CollectionRepository(
            apiService = apiService,
            sessionRepository = sessionGateway,
            collectionMigrationService = CollectionMigrationService(catalogGateway),
        )
    }

    private fun <T> jsonBody(body: T): String = json.encodeToString(serializer(body), body)

    @Suppress("UNCHECKED_CAST")
    private fun <T> serializer(body: T): kotlinx.serialization.KSerializer<T> =
        when (body) {
            is GetCollectionResponse -> GetCollectionResponse.serializer() as kotlinx.serialization.KSerializer<T>
            is SaveCollectionResponse -> SaveCollectionResponse.serializer() as kotlinx.serialization.KSerializer<T>
            is LoginResponse -> LoginResponse.serializer() as kotlinx.serialization.KSerializer<T>
            else -> error("Unsupported serializer for ${body?.let { it::class.simpleName }}")
        }

    private companion object {
        const val PasswordHash = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"
    }
}
