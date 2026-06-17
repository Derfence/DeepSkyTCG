package fr.aumombelli.dstcg.audio

import android.content.res.AssetManager
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

internal const val AudioCreditsAssetPath = "audio/audio_credits.json"

@Serializable
internal data class AudioCreditEntry(
    val fileName: String,
    val usage: String? = null,
    val title: String? = null,
    val artist: String? = null,
    val license: String? = null,
    val licenseUrl: String? = null,
    val sourcePage: String? = null,
    val downloadedAt: String? = null,
    val changes: String? = null,
    val notes: String? = null,
)

@Serializable
internal data class AudioCreditsCatalog(
    val schemaVersion: Int = 1,
    val entries: List<AudioCreditEntry> = emptyList(),
)

private val audioCreditsJson = Json {
    ignoreUnknownKeys = true
}

internal fun decodeAudioCreditsCatalog(rawJson: String): AudioCreditsCatalog =
    audioCreditsJson.decodeFromString<AudioCreditsCatalog>(rawJson)

internal fun loadAudioCredits(contextAssets: AssetManager): List<AudioCreditEntry> {
    synchronized(audioCreditsCacheLock) {
        audioCreditsCache?.let { return it }

        val loaded = runCatching {
            contextAssets.open(AudioCreditsAssetPath).bufferedReader().use { reader ->
                decodeAudioCreditsCatalog(reader.readText()).entries
            }
        }.getOrDefault(emptyList())

        audioCreditsCache = loaded
        return loaded
    }
}

private val audioCreditsCacheLock = Any()
private var audioCreditsCache: List<AudioCreditEntry>? = null
