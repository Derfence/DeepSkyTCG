package fr.aumombelli.dstcg.notification

import fr.aumombelli.dstcg.data.ProgressLoadResult
import java.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface AppNotificationCoordinator {
    suspend fun onAppForegrounded()
    suspend fun onAppBackgrounded()
}

object NoOpAppNotificationCoordinator : AppNotificationCoordinator {
    override suspend fun onAppForegrounded() = Unit
    override suspend fun onAppBackgrounded() = Unit
}

internal object NotificationAppVisibility {
    @Volatile
    var isForeground: Boolean = false
}

internal class DefaultAppNotificationCoordinator(
    private val runtime: NotificationRuntime,
) : AppNotificationCoordinator {
    override suspend fun onAppForegrounded() = withContext(Dispatchers.IO) {
        NotificationAppVisibility.isForeground = true
        runtime.scheduler.cancelReturnReminder()
        runtime.scheduler.cancelFullStock()
        val now = runtime.gameSettings.timeSource.now().wallClockUtc
        runtime.preferences.recordAppOpened(now)
        when (val result = runtime.progressRepository.loadProgress()) {
            is ProgressLoadResult.Ok -> runtime.preferences.setFullStockArmed(
                result.progress.rechargeState.availableDrawCount < runtime.gameSettings.maxStoredDraws,
            )
            is ProgressLoadResult.Recovered -> runtime.preferences.setFullStockArmed(
                result.progress.rechargeState.availableDrawCount < runtime.gameSettings.maxStoredDraws,
            )
            is ProgressLoadResult.Compromised -> runtime.preferences.setFullStockArmed(false)
        }
    }

    override suspend fun onAppBackgrounded() = withContext(Dispatchers.IO) {
        NotificationAppVisibility.isForeground = false
        val now = runtime.gameSettings.timeSource.now().wallClockUtc
        val settings = runtime.preferences.current()
        val lastOpenedAt = settings.lastAppOpenedAt
        if (settings.returnReminderEnabled && lastOpenedAt != null) {
            runtime.scheduler.scheduleReturnReminder(lastOpenedAt = lastOpenedAt, now = now)
        } else {
            runtime.scheduler.cancelReturnReminder()
        }
        reconcileFullStock(now, settings.fullStockEnabled)
    }

    private suspend fun reconcileFullStock(now: Instant, enabled: Boolean) {
        if (!enabled) {
            runtime.preferences.setFullStockArmed(false)
            runtime.scheduler.cancelFullStock()
            return
        }
        val loaded = when (val result = runtime.progressRepository.loadProgress()) {
            is ProgressLoadResult.Ok -> result.progress to result.trustedNow
            is ProgressLoadResult.Recovered -> result.progress to result.trustedNow
            is ProgressLoadResult.Compromised -> null
        }
        if (loaded == null) {
            runtime.scheduler.cancelFullStock()
            return
        }
        val (progress, trustedNow) = loaded
        if (progress.rechargeState.availableDrawCount >= runtime.gameSettings.maxStoredDraws) {
            runtime.preferences.setFullStockArmed(false)
            runtime.scheduler.cancelFullStock()
            return
        }
        runtime.preferences.setFullStockArmed(true)
        val dueAt = runtime.fullStockDueAt(progress, trustedNow)
        if (dueAt == null) {
            runtime.scheduler.cancelFullStock()
        } else {
            runtime.scheduler.scheduleFullStock(at = dueAt, now = trustedNow)
        }
    }
}
