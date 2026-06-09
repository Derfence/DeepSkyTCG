package fr.aumombelli.dstcg.ui.component

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import fr.aumombelli.dstcg.model.CardDefinition
import fr.aumombelli.dstcg.performance.LocalAppPerformanceProfile
import fr.aumombelli.dstcg.ui.theme.SkyQualityPalette

@Composable
internal fun CardArtBackground(
    definition: CardDefinition,
    mode: AstroCardSurfaceMode,
    palette: SkyQualityPalette,
    inset: Dp = 0.dp,
    artShape: Shape = RectangleShape,
    artVisibility: CardArtVisibility = CardArtVisibility.Visible,
    contentScale: ContentScale = ContentScale.Crop,
    modifier: Modifier = Modifier,
) {
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
        if (artVisibility == CardArtVisibility.Hidden) {
            HiddenCardArtPlaceholder(
                modifier = artModifier.testTag(CardBackgroundHiddenPlaceholderTag),
            )
        } else {
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

            when {
                cardArt.primary != null -> {
                    Box(modifier = artModifier.testTag(CardBackgroundArtTag)) {
                        CardArtImage(
                            bitmap = cardArt.primary,
                            contentScale = contentScale,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }

                cardArt.fallback != null -> {
                    Box(modifier = artModifier.testTag(CardBackgroundFallbackAssetTag)) {
                        CardArtImage(
                            bitmap = cardArt.fallback,
                            contentScale = contentScale,
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
}

@Composable
private fun HiddenCardArtPlaceholder(
    modifier: Modifier = Modifier,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.background(
            Brush.linearGradient(
                colors = listOf(
                    Color(0xFF132238),
                    Color(0xFF22334E),
                    Color(0xFF0F1827),
                ),
            ),
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.12f),
                            Color.Transparent,
                        ),
                    ),
                ),
        )
        Surface(
            shape = CircleShape,
            color = Color.Black.copy(alpha = 0.24f),
        ) {
            Text(
                text = "?",
                color = Color(0xFFE7F0FA),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp),
            )
        }
    }
}

@Composable
private fun CardArtImage(
    bitmap: Bitmap,
    contentScale: ContentScale = ContentScale.Crop,
    modifier: Modifier = Modifier,
) {
    val imageBitmap = remember(bitmap) { bitmap.asImageBitmap() }
    Image(
        bitmap = imageBitmap,
        contentDescription = null,
        contentScale = contentScale,
        modifier = modifier.fillMaxSize(),
    )
}
