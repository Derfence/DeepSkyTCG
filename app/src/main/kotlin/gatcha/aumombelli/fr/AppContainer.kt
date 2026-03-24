package fr.aumombelli.gatcha

import android.content.Context
import fr.aumombelli.gatcha.data.AppCompatibilityController
import fr.aumombelli.gatcha.data.AppStatusGateway
import fr.aumombelli.gatcha.data.AppStatusRepository
import fr.aumombelli.gatcha.data.AuthGateway
import fr.aumombelli.gatcha.data.CatalogGateway
import fr.aumombelli.gatcha.data.CollectionGateway
import fr.aumombelli.gatcha.data.CollectionMigrationService
import fr.aumombelli.gatcha.data.CollectionRepository
import fr.aumombelli.gatcha.data.GameCatalogRepository
import fr.aumombelli.gatcha.data.PackGateway
import fr.aumombelli.gatcha.data.PackRepository
import fr.aumombelli.gatcha.data.SessionGateway
import fr.aumombelli.gatcha.data.SessionRepository
import fr.aumombelli.gatcha.network.GameApiService
import fr.aumombelli.gatcha.network.LocalRoutingDns
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

class AppContainer(
    val sessionRepository: SessionGateway,
    val catalogRepository: CatalogGateway,
    val apiService: AuthGateway,
    val appStatusRepository: AppStatusGateway,
    val collectionRepository: CollectionGateway,
    val packRepository: PackGateway,
) {
    companion object {
        fun create(context: Context): AppContainer {
            val appContext = context.applicationContext
            val json = Json {
                ignoreUnknownKeys = true
                encodeDefaults = true
            }

            val httpClient = HttpClient(OkHttp) {
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
            val compatibilityController = AppCompatibilityController()
            val apiService = GameApiService(httpClient, json, catalogRepository, compatibilityController)
            val collectionMigrationService = CollectionMigrationService(catalogRepository)
            val collectionRepository = CollectionRepository(
                apiService = apiService,
                sessionRepository = sessionRepository,
                collectionMigrationService = collectionMigrationService,
            )
            val packRepository = PackRepository(apiService, sessionRepository, collectionRepository)
            val appStatusRepository = AppStatusRepository(apiService, compatibilityController)

            return AppContainer(
                sessionRepository = sessionRepository,
                catalogRepository = catalogRepository,
                apiService = apiService,
                appStatusRepository = appStatusRepository,
                collectionRepository = collectionRepository,
                packRepository = packRepository,
            )
        }
    }
}
