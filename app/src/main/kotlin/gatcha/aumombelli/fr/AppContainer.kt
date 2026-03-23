package fr.aumombelli.gatcha

import android.content.Context
import fr.aumombelli.gatcha.data.CollectionRepository
import fr.aumombelli.gatcha.data.GameCatalogRepository
import fr.aumombelli.gatcha.data.PackRepository
import fr.aumombelli.gatcha.data.SessionRepository
import fr.aumombelli.gatcha.network.GameApiService
import fr.aumombelli.gatcha.network.LocalRoutingDns
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

class AppContainer(
    context: Context,
) {
    private val appContext = context.applicationContext
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val httpClient = HttpClient(OkHttp) {
        engine {
            config {
                dns(LocalRoutingDns())
            }
        }
        install(ContentNegotiation) {
            json(json)
        }
    }

    val sessionRepository = SessionRepository(appContext)
    val catalogRepository = GameCatalogRepository(appContext)
    val apiService = GameApiService(httpClient, json)
    val collectionRepository = CollectionRepository(apiService, sessionRepository)
    val packRepository = PackRepository(apiService, sessionRepository, collectionRepository)
}
