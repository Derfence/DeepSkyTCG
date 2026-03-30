package fr.aumombelli.gatcha

import android.content.Context
import fr.aumombelli.gatcha.data.AndroidKeystoreProgressCipher
import fr.aumombelli.gatcha.data.CatalogGateway
import fr.aumombelli.gatcha.data.CollectionGateway
import fr.aumombelli.gatcha.data.CollectionRepository
import fr.aumombelli.gatcha.data.GameCatalogRepository
import fr.aumombelli.gatcha.data.LocalPackEngine
import fr.aumombelli.gatcha.data.PackGateway
import fr.aumombelli.gatcha.data.PackRepository
import fr.aumombelli.gatcha.data.ProgressGateway
import fr.aumombelli.gatcha.data.ProgressRepository
import fr.aumombelli.gatcha.data.StandaloneGameSettings

class AppContainer(
    val progressRepository: ProgressGateway,
    val catalogRepository: CatalogGateway,
    val collectionRepository: CollectionGateway,
    val packRepository: PackGateway,
    val gameSettings: StandaloneGameSettings,
) {
    companion object {
        fun create(context: Context): AppContainer {
            val appContext = context.applicationContext
            val catalogRepository = GameCatalogRepository(appContext)
            val gameSettings = StandaloneGameSettings.offlineDefault(appContext)
            val progressRepository = ProgressRepository.fromContext(
                context = appContext,
                catalogRepository = catalogRepository,
                settings = gameSettings,
                progressCipher = AndroidKeystoreProgressCipher(),
            )
            val collectionRepository = CollectionRepository(
                progressRepository = progressRepository,
            )
            val packRepository = PackRepository(
                progressRepository = progressRepository,
                collectionRepository = collectionRepository,
                localPackEngine = LocalPackEngine(
                    catalogRepository = catalogRepository,
                    settings = gameSettings,
                ),
            )

            return AppContainer(
                progressRepository = progressRepository,
                catalogRepository = catalogRepository,
                collectionRepository = collectionRepository,
                packRepository = packRepository,
                gameSettings = gameSettings,
            )
        }
    }
}
