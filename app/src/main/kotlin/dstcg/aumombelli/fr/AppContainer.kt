package fr.aumombelli.dstcg

import android.content.Context
import fr.aumombelli.dstcg.audio.AndroidAudioController
import fr.aumombelli.dstcg.audio.AudioController
import fr.aumombelli.dstcg.audio.AudioSettingsRepository
import fr.aumombelli.dstcg.audio.audioSettingsDataStore
import fr.aumombelli.dstcg.data.AndroidKeystoreProgressCipher
import fr.aumombelli.dstcg.data.CatalogGateway
import fr.aumombelli.dstcg.data.CollectionGateway
import fr.aumombelli.dstcg.data.CollectionRepository
import fr.aumombelli.dstcg.data.CraftingGateway
import fr.aumombelli.dstcg.data.CraftingRepository
import fr.aumombelli.dstcg.data.EquipmentGateway
import fr.aumombelli.dstcg.data.EquipmentRepository
import fr.aumombelli.dstcg.data.GameCatalogRepository
import fr.aumombelli.dstcg.data.HomeMenuNoveltyEvaluator
import fr.aumombelli.dstcg.data.LocalPackEngine
import fr.aumombelli.dstcg.data.MiniGamesGateway
import fr.aumombelli.dstcg.data.MiniGamesRepository
import fr.aumombelli.dstcg.data.PackGateway
import fr.aumombelli.dstcg.data.PackRepository
import fr.aumombelli.dstcg.data.ProgressGateway
import fr.aumombelli.dstcg.data.ProgressRepository
import fr.aumombelli.dstcg.data.StandaloneGameSettings
import fr.aumombelli.dstcg.data.TradeGateway
import fr.aumombelli.dstcg.data.TradeRepository
import fr.aumombelli.dstcg.data.TradeSettingsGateway
import fr.aumombelli.dstcg.data.TradeSettingsRepository
import fr.aumombelli.dstcg.data.tradeSettingsDataStore

class AppContainer(
    val progressRepository: ProgressGateway,
    val catalogRepository: CatalogGateway,
    val collectionRepository: CollectionGateway,
    val craftingRepository: CraftingGateway,
    val equipmentRepository: EquipmentGateway,
    val packRepository: PackGateway,
    val miniGamesRepository: MiniGamesGateway,
    val tradeRepository: TradeGateway,
    val tradeSettingsRepository: TradeSettingsGateway,
    val gameSettings: StandaloneGameSettings,
    val audioController: AudioController,
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
            val craftingRepository = CraftingRepository(
                catalogRepository = catalogRepository,
                progressRepository = progressRepository,
            )
            val homeMenuNoveltyEvaluator = HomeMenuNoveltyEvaluator(
                catalogRepository = catalogRepository,
            )
            val equipmentRepository = EquipmentRepository(
                progressRepository = progressRepository,
                catalogRepository = catalogRepository,
                homeMenuNoveltyEvaluator = homeMenuNoveltyEvaluator,
            )
            val packRepository = PackRepository(
                progressRepository = progressRepository,
                collectionRepository = collectionRepository,
                localPackEngine = LocalPackEngine(
                    catalogRepository = catalogRepository,
                    settings = gameSettings,
                ),
                homeMenuNoveltyEvaluator = homeMenuNoveltyEvaluator,
            )
            val miniGamesRepository = MiniGamesRepository(
                progressRepository = progressRepository,
                catalogRepository = catalogRepository,
                settings = gameSettings,
            )
            val tradeRepository = TradeRepository(
                catalogRepository = catalogRepository,
                progressRepository = progressRepository,
            )
            val tradeSettingsRepository = TradeSettingsRepository(appContext.tradeSettingsDataStore)
            val audioController = AndroidAudioController(
                context = appContext,
                settingsRepository = AudioSettingsRepository(appContext.audioSettingsDataStore),
            )

            return AppContainer(
                progressRepository = progressRepository,
                catalogRepository = catalogRepository,
                collectionRepository = collectionRepository,
                craftingRepository = craftingRepository,
                equipmentRepository = equipmentRepository,
                packRepository = packRepository,
                miniGamesRepository = miniGamesRepository,
                tradeRepository = tradeRepository,
                tradeSettingsRepository = tradeSettingsRepository,
                gameSettings = gameSettings,
                audioController = audioController,
            )
        }
    }
}
