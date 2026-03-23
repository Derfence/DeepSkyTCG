package gatcha.aumombelli.fr

import android.content.Context
import gatcha.aumombelli.fr.data.CollectionRepository
import gatcha.aumombelli.fr.data.GameCatalogRepository
import gatcha.aumombelli.fr.data.PackRepository
import gatcha.aumombelli.fr.data.SessionRepository
import gatcha.aumombelli.fr.network.GameApiService
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
