package fr.aumombelli.gatcha.ui.component

import android.graphics.Bitmap
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
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import fr.aumombelli.gatcha.model.CardDefinition
import fr.aumombelli.gatcha.performance.LocalAppPerformanceProfile
import fr.aumombelli.gatcha.ui.theme.SkyQualityPalette

@Composable
internal fun CardArtBackground(
    definition: CardDefinition,
    mode: AstroCardSurfaceMode,
    palette: SkyQualityPalette,
    inset: Dp = 0.dp,
    artShape: Shape = RectangleShape,
    modifier: Modifier = Modifier,
) {
    val appContext = LocalContext.current.applicationContext
    val performanceProfile = LocalAppPerformanceProfile.current
    val cardArtLoader = LocalCardArtBitmapLoader.current ?: remember(
        appContext,
        performanceProfile.cardArtCacheMaxBytes,
    ) {
        CardArtBitmapLoader(
            assetManager = appContext.assets,
            maxCacheBytes = performanceProfile.cardArtCacheMaxBytes,
        )
    }
    val bitmapRequest = remember(mode, performanceProfile) {
        cardArtBitmapRequest(
            mode = mode,
            performanceProfile = performanceProfile,
        )
    }
    val cardArt = remember(cardArtLoader, definition.extensionId, definition.imageRef, bitmapRequest) {
        cardArtLoader.loadCardArt(
            definition = definition,
            request = bitmapRequest,
        )
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
    bitmap: Bitmap,
    modifier: Modifier = Modifier,
) {
    val imageBitmap = remember(bitmap) { bitmap.asImageBitmap() }
    Image(
        bitmap = imageBitmap,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier.fillMaxSize(),
    )
}
