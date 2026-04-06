package fr.aumombelli.dstcg

import android.content.Context
import fr.aumombelli.dstcg.data.AndroidKeystoreProgressCipher
import fr.aumombelli.dstcg.data.CatalogGateway
import fr.aumombelli.dstcg.data.CollectionGateway
import fr.aumombelli.dstcg.data.CollectionRepository
import fr.aumombelli.dstcg.data.EquipmentGateway
import fr.aumombelli.dstcg.data.EquipmentRepository
import fr.aumombelli.dstcg.data.GameCatalogRepository
import fr.aumombelli.dstcg.data.LocalPackEngine
import fr.aumombelli.dstcg.data.PackGateway
import fr.aumombelli.dstcg.data.PackRepository
import fr.aumombelli.dstcg.data.ProgressGateway
import fr.aumombelli.dstcg.data.ProgressRepository
import fr.aumombelli.dstcg.data.StandaloneGameSettings

class AppContainer(
    val progressRepository: ProgressGateway,
    val catalogRepository: CatalogGateway,
    val collectionRepository: CollectionGateway,
    val equipmentRepository: EquipmentGateway,
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
            val equipmentRepository = EquipmentRepository(
                progressRepository = progressRepository,
                catalogRepository = catalogRepository,
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
                equipmentRepository = equipmentRepository,
                packRepository = packRepository,
                gameSettings = gameSettings,
            )
        }
    }
}
