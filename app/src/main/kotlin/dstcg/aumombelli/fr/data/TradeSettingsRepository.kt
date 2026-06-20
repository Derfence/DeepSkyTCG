package fr.aumombelli.dstcg.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.io.IOException
import java.security.SecureRandom
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val TradeLocalNameKey = stringPreferencesKey("trade_local_name")

val Context.tradeSettingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "dstcg_trade_settings",
)

class TradeSettingsRepository(
    private val dataStore: DataStore<Preferences>,
    private val defaultNameFactory: () -> String = { defaultTradeLocalName() },
) : TradeSettingsGateway {
    override val settings: Flow<TradeSettings> = dataStore.data
        .catch { error ->
            if (error is IOException) {
                emit(emptyPreferences())
            } else {
                throw error
            }
        }
        .map { preferences ->
            TradeSettings(
                localName = preferences[TradeLocalNameKey].orEmpty(),
            )
        }

    override suspend fun ensureLocalName(): String {
        val storedName = settings.first().localName
        val currentName = storedName.normalizeTradeLocalName()
        if (currentName.isNotBlank()) {
            if (storedName != currentName) {
                dataStore.edit { preferences ->
                    preferences[TradeLocalNameKey] = currentName
                }
            }
            return currentName
        }

        val generatedName = defaultNameFactory().normalizeTradeLocalName()
            .ifBlank { defaultTradeLocalName() }
        dataStore.edit { preferences ->
            preferences[TradeLocalNameKey] = generatedName
        }
        return generatedName
    }

    override suspend fun setLocalName(name: String): String {
        val normalizedName = name.normalizeTradeLocalName()
        val storedName = normalizedName.ifBlank { defaultNameFactory().normalizeTradeLocalName() }
        dataStore.edit { preferences ->
            preferences[TradeLocalNameKey] = storedName
        }
        return storedName
    }
}

internal fun String.normalizeTradeLocalName(): String =
    trim()
        .replace(Regex("\\s+"), " ")
        .takeUtf8Prefix(MaxTradeLocalNameBytes)

internal fun defaultTradeLocalName(): String {
    val suffix = SecureRandom().nextInt(9_000) + 1_000
    return "Obs. $suffix"
}

internal const val MaxTradeLocalNameBytes = 12

private fun String.takeUtf8Prefix(maxBytes: Int): String {
    var endIndex = length
    while (endIndex >= 0) {
        val candidate = take(endIndex)
        if (candidate.encodeToByteArray().size <= maxBytes) return candidate
        endIndex -= 1
    }
    return ""
}
