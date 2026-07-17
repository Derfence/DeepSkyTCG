package fr.aumombelli.dstcg.notification

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.notificationPreferencesDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "dstcg_notification_settings",
)

data class NotificationSettings(
    val fullStockEnabled: Boolean = false,
    val returnReminderEnabled: Boolean = false,
    val automaticPermissionRequested: Boolean = false,
    val lastAppOpenedAt: Instant? = null,
    val fullStockArmed: Boolean = false,
    val returnReminderArmed: Boolean = false,
)

interface NotificationPreferencesGateway {
    val settings: Flow<NotificationSettings>
    suspend fun current(): NotificationSettings
    suspend fun completeAutomaticPermissionRequest(granted: Boolean)
    suspend fun setFullStockEnabled(enabled: Boolean)
    suspend fun setReturnReminderEnabled(enabled: Boolean)
    suspend fun recordAppOpened(at: Instant)
    suspend fun setFullStockArmed(armed: Boolean)
    suspend fun claimFullStockNotification(): Boolean
    suspend fun claimReturnReminder(expectedLastOpenedAt: Instant): Boolean
}

object DisabledNotificationPreferences : NotificationPreferencesGateway {
    private val disabled = MutableStateFlow(NotificationSettings())
    override val settings: Flow<NotificationSettings> = disabled
    override suspend fun current(): NotificationSettings = disabled.value
    override suspend fun completeAutomaticPermissionRequest(granted: Boolean) = Unit
    override suspend fun setFullStockEnabled(enabled: Boolean) = Unit
    override suspend fun setReturnReminderEnabled(enabled: Boolean) = Unit
    override suspend fun recordAppOpened(at: Instant) = Unit
    override suspend fun setFullStockArmed(armed: Boolean) = Unit
    override suspend fun claimFullStockNotification(): Boolean = false
    override suspend fun claimReturnReminder(expectedLastOpenedAt: Instant): Boolean = false
}

class NotificationPreferencesRepository(
    private val dataStore: DataStore<Preferences>,
) : NotificationPreferencesGateway {
    override val settings: Flow<NotificationSettings> = dataStore.data.map(::toSettings)

    override suspend fun current(): NotificationSettings = settings.first()

    override suspend fun completeAutomaticPermissionRequest(granted: Boolean) {
        dataStore.edit { preferences ->
            preferences[AutomaticPermissionRequestedKey] = true
            preferences[FullStockEnabledKey] = granted
            preferences[ReturnReminderEnabledKey] = granted
        }
    }

    override suspend fun setFullStockEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[FullStockEnabledKey] = enabled
            if (!enabled) preferences[FullStockArmedKey] = false
        }
    }

    override suspend fun setReturnReminderEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[ReturnReminderEnabledKey] = enabled
            preferences[ReturnReminderArmedKey] = enabled
        }
    }

    override suspend fun recordAppOpened(at: Instant) {
        dataStore.edit { preferences ->
            preferences[LastAppOpenedAtEpochMsKey] = at.toEpochMilli()
            preferences[ReturnReminderArmedKey] = true
        }
    }

    override suspend fun setFullStockArmed(armed: Boolean) {
        dataStore.edit { preferences -> preferences[FullStockArmedKey] = armed }
    }

    override suspend fun claimFullStockNotification(): Boolean {
        var claimed = false
        dataStore.edit { preferences ->
            claimed = preferences[FullStockEnabledKey] == true &&
                preferences[FullStockArmedKey] == true
            if (claimed) preferences[FullStockArmedKey] = false
        }
        return claimed
    }

    override suspend fun claimReturnReminder(expectedLastOpenedAt: Instant): Boolean {
        var claimed = false
        dataStore.edit { preferences ->
            claimed = preferences[ReturnReminderEnabledKey] == true &&
                preferences[ReturnReminderArmedKey] == true &&
                preferences[LastAppOpenedAtEpochMsKey] == expectedLastOpenedAt.toEpochMilli()
            if (claimed) preferences[ReturnReminderArmedKey] = false
        }
        return claimed
    }

    private fun toSettings(preferences: Preferences): NotificationSettings = NotificationSettings(
        fullStockEnabled = preferences[FullStockEnabledKey] ?: false,
        returnReminderEnabled = preferences[ReturnReminderEnabledKey] ?: false,
        automaticPermissionRequested = preferences[AutomaticPermissionRequestedKey] ?: false,
        lastAppOpenedAt = preferences[LastAppOpenedAtEpochMsKey]?.let(Instant::ofEpochMilli),
        fullStockArmed = preferences[FullStockArmedKey] ?: false,
        returnReminderArmed = preferences[ReturnReminderArmedKey] ?: false,
    )

    companion object {
        private val FullStockEnabledKey = booleanPreferencesKey("full_stock_enabled")
        private val ReturnReminderEnabledKey = booleanPreferencesKey("return_reminder_enabled")
        private val AutomaticPermissionRequestedKey = booleanPreferencesKey("automatic_permission_requested")
        private val LastAppOpenedAtEpochMsKey = longPreferencesKey("last_app_opened_at_epoch_ms")
        private val FullStockArmedKey = booleanPreferencesKey("full_stock_armed")
        private val ReturnReminderArmedKey = booleanPreferencesKey("return_reminder_armed")

        fun fromContext(context: Context): NotificationPreferencesRepository =
            NotificationPreferencesRepository(context.applicationContext.notificationPreferencesDataStore)
    }
}
