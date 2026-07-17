package fr.aumombelli.dstcg.notification

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import fr.aumombelli.dstcg.data.ProgressLoadResult
import java.time.Duration
import java.time.Instant

class FullStockNotificationWorker(
    appContext: Context,
    workerParameters: WorkerParameters,
) : CoroutineWorker(appContext, workerParameters) {
    override suspend fun doWork(): Result {
        if (NotificationAppVisibility.isForeground) return Result.success()
        val runtime = NotificationRuntime.create(applicationContext)
        val settings = runtime.preferences.current()
        if (!settings.fullStockEnabled) return Result.success()
        val loaded = when (val result = runtime.progressRepository.loadProgress()) {
            is ProgressLoadResult.Ok -> result.progress to result.trustedNow
            is ProgressLoadResult.Recovered -> result.progress to result.trustedNow
            is ProgressLoadResult.Compromised -> return Result.failure()
        }
        val (progress, trustedNow) = loaded
        if (progress.rechargeState.availableDrawCount < runtime.gameSettings.maxStoredDraws) {
            runtime.preferences.setFullStockArmed(true)
            runtime.fullStockDueAt(progress, trustedNow)?.let { dueAt ->
                runtime.scheduler.scheduleFullStock(at = dueAt, now = trustedNow)
            }
            return Result.success()
        }
        if (runtime.preferences.claimFullStockNotification()) {
            runtime.publisher.publish(LocalNotificationType.FullStock)
        }
        return Result.success()
    }
}

class ReturnReminderWorker(
    appContext: Context,
    workerParameters: WorkerParameters,
) : CoroutineWorker(appContext, workerParameters) {
    override suspend fun doWork(): Result {
        if (NotificationAppVisibility.isForeground) return Result.success()
        val expectedLastOpenedAtMillis = inputData.getLong(ReturnLastOpenedAtKey, Long.MIN_VALUE)
        if (expectedLastOpenedAtMillis == Long.MIN_VALUE) return Result.failure()
        val expectedLastOpenedAt = Instant.ofEpochMilli(expectedLastOpenedAtMillis)
        val runtime = NotificationRuntime.create(applicationContext)
        val settings = runtime.preferences.current()
        if (!settings.returnReminderEnabled || settings.lastAppOpenedAt != expectedLastOpenedAt) {
            return Result.success()
        }
        val now = runtime.gameSettings.timeSource.now().wallClockUtc
        val elapsed = Duration.between(expectedLastOpenedAt, now)
        if (elapsed < ReturnReminderDelay) {
            runtime.scheduler.scheduleReturnReminder(expectedLastOpenedAt, now)
            return Result.success()
        }
        if (runtime.preferences.claimReturnReminder(expectedLastOpenedAt)) {
            runtime.publisher.publish(LocalNotificationType.ReturnReminder)
        }
        return Result.success()
    }
}
