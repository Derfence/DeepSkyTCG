package fr.aumombelli.gatcha.ui.component

import android.content.res.AssetManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import fr.aumombelli.gatcha.model.CardDefinition
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

internal const val CardArtCreditsAssetPath = "card_art/card_art_credits.json"
internal const val CardImageCreditTag = "astro-card-image-credit"

@Serializable
internal data class CardArtCreditEntry(
    val artist: String? = null,
    val credit: String? = null,
    val license: String? = null,
    val sourcePage: String? = null,
)

private val cardArtCreditsJson = Json {
    ignoreUnknownKeys = true
}

private typealias CardArtCreditsCatalog = Map<String, Map<String, CardArtCreditEntry>>

@Composable
internal fun rememberCardArtCredit(definition: CardDefinition): CardArtCreditEntry? {
    val context = LocalContext.current
    val catalog = remember(context) {
        loadCardArtCredits(context.assets)
    }
    return remember(catalog, definition.extensionId, definition.imageRef) {
        catalog[definition.extensionId]?.get(definition.imageRef)
    }
}

internal fun cardArtCreditArtistName(artist: String?): String =
    artist?.trim()?.takeIf { it.isNotEmpty() } ?: "Inconnu"

private fun loadCardArtCredits(contextAssets: AssetManager): CardArtCreditsCatalog {
    synchronized(cardArtCreditsCacheLock) {
        cardArtCreditsCache?.let { return it }

        val loaded = runCatching {
            contextAssets.open(CardArtCreditsAssetPath).bufferedReader().use { reader ->
                cardArtCreditsJson.decodeFromString<CardArtCreditsCatalog>(reader.readText())
            }
        }.getOrDefault(emptyMap())

        cardArtCreditsCache = loaded
        return loaded
    }
}

private val cardArtCreditsCacheLock = Any()
private var cardArtCreditsCache: CardArtCreditsCatalog? = null
