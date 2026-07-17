package fr.aumombelli.dstcg.notification

import android.content.Context
import fr.aumombelli.dstcg.data.AndroidKeystoreProgressCipher
import fr.aumombelli.dstcg.data.CatalogGateway
import fr.aumombelli.dstcg.data.GameCatalogRepository
import fr.aumombelli.dstcg.data.ProgressGateway
import fr.aumombelli.dstcg.data.ProgressRepository
import fr.aumombelli.dstcg.data.StandaloneGameSettings
import fr.aumombelli.dstcg.data.derivedFullStockAt
import fr.aumombelli.dstcg.data.drawCooldownDuration
import fr.aumombelli.dstcg.data.validated
import fr.aumombelli.dstcg.model.StandaloneProgress
import java.time.Instant

internal class NotificationRuntime(
    val progressRepository: ProgressGateway,
    val preferences: NotificationPreferencesGateway,
    val scheduler: NotificationScheduler,
    val publisher: NotificationPublisher,
    val gameSettings: StandaloneGameSettings,
    private val catalogRepository: CatalogGateway,
) {
    suspend fun fullStockDueAt(progress: StandaloneProgress, now: Instant): Instant? {
        val drawCooldown = catalogRepository.loadGameBalance().validated().drawCooldownDuration()
        val equipmentCards = catalogRepository.loadEquipmentCards()
        return progress.derivedFullStockAt(
            now = now,
            drawCooldown = drawCooldown,
            maxStoredDraws = gameSettings.maxStoredDraws,
            weatherPolicy = gameSettings.weatherPolicy,
            equipmentCards = equipmentCards,
        )
    }

    companion object {
        fun create(context: Context): NotificationRuntime {
            val appContext = context.applicationContext
            val catalogRepository = GameCatalogRepository(appContext)
            val settings = StandaloneGameSettings.offlineDefault(appContext)
            return NotificationRuntime(
                progressRepository = ProgressRepository.fromContext(
                    context = appContext,
                    catalogRepository = catalogRepository,
                    settings = settings,
                    progressCipher = AndroidKeystoreProgressCipher(),
                ),
                preferences = NotificationPreferencesRepository.fromContext(appContext),
                scheduler = AndroidNotificationScheduler(appContext),
                publisher = AndroidNotificationPublisher(appContext),
                gameSettings = settings,
                catalogRepository = catalogRepository,
            )
        }
    }
}
