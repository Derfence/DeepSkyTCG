package fr.aumombelli.dstcg.notification

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.time.Duration
import java.time.Instant
import java.util.concurrent.TimeUnit

interface NotificationScheduler {
    fun scheduleFullStock(at: Instant, now: Instant)
    fun cancelFullStock()
    fun scheduleReturnReminder(lastOpenedAt: Instant, now: Instant)
    fun cancelReturnReminder()
}

class AndroidNotificationScheduler(
    context: Context,
    private val workManager: WorkManager = WorkManager.getInstance(context.applicationContext),
) : NotificationScheduler {
    override fun scheduleFullStock(at: Instant, now: Instant) {
        val request = OneTimeWorkRequestBuilder<FullStockNotificationWorker>()
            .setInitialDelay(delayMillis(now, at), TimeUnit.MILLISECONDS)
            .addTag(FullStockWorkName)
            .build()
        workManager.enqueueUniqueWork(FullStockWorkName, ExistingWorkPolicy.REPLACE, request)
    }

    override fun cancelFullStock() {
        workManager.cancelUniqueWork(FullStockWorkName)
    }

    override fun scheduleReturnReminder(lastOpenedAt: Instant, now: Instant) {
        val dueAt = lastOpenedAt.plus(ReturnReminderDelay)
        val input = Data.Builder()
            .putLong(ReturnLastOpenedAtKey, lastOpenedAt.toEpochMilli())
            .build()
        val request = OneTimeWorkRequestBuilder<ReturnReminderWorker>()
            .setInputData(input)
            .setInitialDelay(delayMillis(now, dueAt), TimeUnit.MILLISECONDS)
            .addTag(ReturnReminderWorkName)
            .build()
        workManager.enqueueUniqueWork(ReturnReminderWorkName, ExistingWorkPolicy.REPLACE, request)
    }

    override fun cancelReturnReminder() {
        workManager.cancelUniqueWork(ReturnReminderWorkName)
    }

    private fun delayMillis(now: Instant, dueAt: Instant): Long =
        Duration.between(now, dueAt).toMillis().coerceAtLeast(0L)
}

internal val ReturnReminderDelay: Duration = Duration.ofDays(7)
internal const val FullStockWorkName = "dstcg-full-stock-notification"
internal const val ReturnReminderWorkName = "dstcg-return-reminder-notification"
internal const val ReturnLastOpenedAtKey = "last_opened_at_epoch_ms"
