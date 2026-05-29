package fr.aumombelli.dstcg.ui.component

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import fr.aumombelli.dstcg.model.EquipmentCardDefinition
import fr.aumombelli.dstcg.performance.AppPerformanceProfile
import fr.aumombelli.dstcg.performance.LocalAppPerformanceProfile

internal enum class EquipmentArtMode {
    Inventory,
    Detail,
    PackReveal,
}

@Composable
internal fun EquipmentArtBackground(
    definition: EquipmentCardDefinition,
    mode: EquipmentArtMode,
    modifier: Modifier = Modifier,
    artTestTag: String? = null,
    fallbackTestTag: String? = null,
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
        equipmentArtBitmapRequest(
            mode = mode,
            performanceProfile = performanceProfile,
        )
    }
    val equipmentArt = remember(cardArtLoader, definition.imageRef, bitmapRequest) {
        cardArtLoader.loadEquipmentArt(
            definition = definition,
            request = bitmapRequest,
        )
    }

    if (equipmentArt != null) {
        Box(modifier = modifier) {
            EquipmentArtImage(
                bitmap = equipmentArt,
                modifier = Modifier
                    .fillMaxSize()
                    .let { base ->
                        if (artTestTag != null) {
                            base.testTag(artTestTag)
                        } else {
                            base
                        }
                    },
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.08f),
                                Color.Black.copy(alpha = 0.42f),
                            ),
                        ),
                    ),
            )
        }
    } else if (fallbackTestTag != null) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .testTag(fallbackTestTag),
        )
    }
}

internal fun equipmentArtBitmapRequest(
    mode: EquipmentArtMode,
    performanceProfile: AppPerformanceProfile,
): CardArtBitmapRequest = when (mode) {
    EquipmentArtMode.Inventory -> CardArtBitmapRequest(
        bucketLabel = "equipment-thumbnail",
        targetWidthPx = performanceProfile.thumbnailTargetWidthPx,
        targetHeightPx = performanceProfile.thumbnailTargetHeightPx,
        bitmapConfig = Bitmap.Config.RGB_565,
    )

    EquipmentArtMode.Detail,
    EquipmentArtMode.PackReveal,
    -> CardArtBitmapRequest(
        bucketLabel = "equipment-detail",
        targetWidthPx = performanceProfile.detailTargetWidthPx,
        targetHeightPx = performanceProfile.detailTargetHeightPx,
        bitmapConfig = Bitmap.Config.ARGB_8888,
    )
}

@Composable
private fun EquipmentArtImage(
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
