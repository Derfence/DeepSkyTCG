package fr.aumombelli.dstcg.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import fr.aumombelli.dstcg.MainActivity
import fr.aumombelli.dstcg.R
import fr.aumombelli.dstcg.app.AppLaunchScene
import fr.aumombelli.dstcg.app.AppLaunchSceneExtraKey

enum class LocalNotificationType {
    FullStock,
    ReturnReminder,
}

interface NotificationPublisher {
    fun canPostNotifications(): Boolean
    fun publish(type: LocalNotificationType): Boolean
}

class AndroidNotificationPublisher(
    private val context: Context,
) : NotificationPublisher {
    private val notificationManager = NotificationManagerCompat.from(context)

    init {
        createChannels()
    }

    override fun canPostNotifications(): Boolean =
        hasRuntimePermission(context) && notificationManager.areNotificationsEnabled()

    override fun publish(type: LocalNotificationType): Boolean {
        if (!canPostNotifications()) return false
        val content = type.content()
        val notification = NotificationCompat.Builder(context, content.channelId)
            .setSmallIcon(R.drawable.ic_notification_telescope)
            .setLargeIcon(
                BitmapFactory.decodeResource(
                    context.resources,
                    R.drawable.logo_badge_17,
                ),
            )
            .setContentTitle(content.title)
            .setContentText(content.message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content.message))
            .setContentIntent(homePendingIntent(type))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        notificationManager.notify(content.notificationId, notification)
        return true
    }

    private fun createChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannels(
            listOf(
                NotificationChannel(
                    FullStockChannelId,
                    "Stock de packs plein",
                    NotificationManager.IMPORTANCE_DEFAULT,
                ).apply {
                    description = "Prévient lorsque les dix packs sont disponibles."
                },
                NotificationChannel(
                    ReturnReminderChannelId,
                    "Rappel après 7 jours",
                    NotificationManager.IMPORTANCE_DEFAULT,
                ).apply {
                    description = "Invite à revenir après sept jours sans ouvrir l’application."
                },
            ),
        )
    }

    private fun homePendingIntent(type: LocalNotificationType): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
            .putExtra(AppLaunchSceneExtraKey, AppLaunchScene.Home.wireValue)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        return PendingIntent.getActivity(
            context,
            type.ordinal + 1,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    companion object {
        const val FullStockChannelId = "pack_stock_full"
        const val ReturnReminderChannelId = "return_reminder"

        fun hasRuntimePermission(context: Context): Boolean =
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS,
                ) == PackageManager.PERMISSION_GRANTED
    }
}

internal fun LocalNotificationType.content(): NotificationContent = when (this) {
    LocalNotificationType.FullStock -> NotificationContent(
        channelId = AndroidNotificationPublisher.FullStockChannelId,
        notificationId = 1001,
        title = "Tes packs sont rechargés",
        message = "Ton stock est plein : 10 packs t’attendent.",
    )
    LocalNotificationType.ReturnReminder -> NotificationContent(
        channelId = AndroidNotificationPublisher.ReturnReminderChannelId,
        notificationId = 1002,
        title = "L’observatoire t’attend",
        message = "Cela fait 7 jours que tu n’as pas joué. De nouveaux packs t’attendent.",
    )
}

internal data class NotificationContent(
    val channelId: String,
    val notificationId: Int,
    val title: String,
    val message: String,
)
