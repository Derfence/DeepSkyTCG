package fr.aumombelli.gatcha.ui.component

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import fr.aumombelli.gatcha.model.CardDefinition
import fr.aumombelli.gatcha.ui.theme.SkyQualityPalette

@Composable
internal fun CardArtBackground(
    definition: CardDefinition,
    palette: SkyQualityPalette,
    inset: Dp = 0.dp,
    artShape: Shape = RectangleShape,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val cardArt = remember(context, definition.extensionId, definition.imageRef) {
        loadCardArt(definition = definition, contextAssets = context.assets)
    }
    val artModifier = Modifier
        .fillMaxSize()
        .padding(inset)
        .clip(artShape)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        palette.top,
                        palette.bottom,
                    ),
                ),
            ),
    ) {
        when {
            cardArt.primary != null -> {
                CardArtImage(
                    bitmap = cardArt.primary,
                    modifier = artModifier.testTag(CardBackgroundArtTag),
                )
            }

            cardArt.fallback != null -> {
                Box(modifier = artModifier.testTag(CardBackgroundFallbackAssetTag)) {
                    CardArtImage(
                        bitmap = cardArt.fallback,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }

            else -> {
                Box(
                    modifier = artModifier.testTag(CardBackgroundFallbackAssetTag),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = 0.14f),
                                        Color.Transparent,
                                    ),
                                ),
                            )
                            .testTag(CardBackgroundPlaceholderTag),
                    )
                }
            }
        }
    }
}

@Composable
private fun CardArtImage(
    bitmap: ImageBitmap,
    modifier: Modifier = Modifier,
) {
    Image(
        bitmap = bitmap,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier.fillMaxSize(),
    )
}

private data class LoadedCardArt(
    val primary: ImageBitmap?,
    val fallback: ImageBitmap?,
)

private fun loadCardArt(
    definition: CardDefinition,
    contextAssets: android.content.res.AssetManager,
): LoadedCardArt {
    val primary = loadBitmapFromAsset(
        contextAssets = contextAssets,
        path = cardArtAssetPath(definition),
    )
    if (primary != null) {
        return LoadedCardArt(
            primary = primary,
            fallback = null,
        )
    }
    return LoadedCardArt(
        primary = null,
        fallback = loadBitmapFromAsset(
            contextAssets = contextAssets,
            path = CardArtFallbackAssetPath,
        ),
    )
}

private fun loadBitmapFromAsset(
    contextAssets: android.content.res.AssetManager,
    path: String,
): ImageBitmap? {
    synchronized(cardArtBitmapCache) {
        if (cardArtBitmapCache.containsKey(path)) {
            return cardArtBitmapCache.getValue(path)
        }

        val bitmap = runCatching {
            contextAssets.open(path).use { stream ->
                BitmapFactory.decodeStream(stream)?.asImageBitmap()
            }
        }.getOrNull()

        cardArtBitmapCache[path] = bitmap
        return bitmap
    }
}

private val cardArtBitmapCache = mutableMapOf<String, ImageBitmap?>()
