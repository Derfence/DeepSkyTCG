package fr.aumombelli.gatcha.ui.component

import android.content.res.AssetManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.runtime.staticCompositionLocalOf
import fr.aumombelli.gatcha.model.CardDefinition
import fr.aumombelli.gatcha.performance.AppPerformanceProfile

internal class CardArtBitmapLoader(
    private val assetManager: AssetManager,
    maxCacheBytes: Int,
) {
    private val cache = object : LruCache<String, Bitmap>(maxCacheBytes) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.allocationByteCount
    }
    private val missingAssetKeys = mutableSetOf<String>()
    private val lock = Any()

    fun loadCardArt(
        definition: CardDefinition,
        request: CardArtBitmapRequest,
    ): LoadedCardArtBitmap {
        val primary = loadBitmapFromAsset(
            path = cardArtAssetPath(definition),
            request = request,
        )
        if (primary != null) {
            return LoadedCardArtBitmap(
                primary = primary,
                fallback = null,
            )
        }

        return LoadedCardArtBitmap(
            primary = null,
            fallback = loadBitmapFromAsset(
                path = CardArtFallbackAssetPath,
                request = request,
            ),
        )
    }

    private fun loadBitmapFromAsset(
        path: String,
        request: CardArtBitmapRequest,
    ): Bitmap? {
        val cacheKey = request.cacheKey(path)

        synchronized(lock) {
            cache.get(cacheKey)?.let { return it }
            if (cacheKey in missingAssetKeys) {
                return null
            }
        }

        val bitmap = decodeBitmapFromAsset(
            path = path,
            request = request,
        )

        synchronized(lock) {
            if (bitmap != null) {
                cache.put(cacheKey, bitmap)
                missingAssetKeys.remove(cacheKey)
            } else {
                missingAssetKeys += cacheKey
            }
        }
        return bitmap
    }

    private fun decodeBitmapFromAsset(
        path: String,
        request: CardArtBitmapRequest,
    ): Bitmap? {
        val bounds = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        val decodedBounds = runCatching {
            assetManager.open(path).use { stream ->
                BitmapFactory.decodeStream(stream, null, bounds)
            }
        }.isSuccess
        if (!decodedBounds || bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            return null
        }

        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = computeInSampleSize(
                sourceWidth = bounds.outWidth,
                sourceHeight = bounds.outHeight,
                targetWidth = request.targetWidthPx,
                targetHeight = request.targetHeightPx,
            )
            inPreferredConfig = request.bitmapConfig
            inScaled = false
        }

        return runCatching {
            assetManager.open(path).use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOptions)
            }
        }.getOrNull()
    }
}

internal data class CardArtBitmapRequest(
    val bucketLabel: String,
    val targetWidthPx: Int,
    val targetHeightPx: Int,
    val bitmapConfig: Bitmap.Config,
) {
    fun cacheKey(path: String): String = buildString {
        append(path)
        append('#')
        append(bucketLabel)
        append('#')
        append(targetWidthPx)
        append('x')
        append(targetHeightPx)
        append('#')
        append(bitmapConfig.name)
    }
}

internal data class LoadedCardArtBitmap(
    val primary: Bitmap?,
    val fallback: Bitmap?,
)

internal val LocalCardArtBitmapLoader = staticCompositionLocalOf<CardArtBitmapLoader?> { null }

internal fun cardArtBitmapRequest(
    mode: AstroCardSurfaceMode,
    performanceProfile: AppPerformanceProfile,
): CardArtBitmapRequest = when (mode) {
    AstroCardSurfaceMode.Thumbnail -> CardArtBitmapRequest(
        bucketLabel = "thumbnail",
        targetWidthPx = performanceProfile.thumbnailTargetWidthPx,
        targetHeightPx = performanceProfile.thumbnailTargetHeightPx,
        bitmapConfig = Bitmap.Config.RGB_565,
    )
    AstroCardSurfaceMode.Preview,
    AstroCardSurfaceMode.PackReveal,
    -> CardArtBitmapRequest(
        bucketLabel = "detail",
        targetWidthPx = performanceProfile.detailTargetWidthPx,
        targetHeightPx = performanceProfile.detailTargetHeightPx,
        bitmapConfig = Bitmap.Config.ARGB_8888,
    )
}

private fun computeInSampleSize(
    sourceWidth: Int,
    sourceHeight: Int,
    targetWidth: Int,
    targetHeight: Int,
): Int {
    var inSampleSize = 1

    if (sourceHeight <= targetHeight && sourceWidth <= targetWidth) {
        return inSampleSize
    }

    var halfHeight = sourceHeight / 2
    var halfWidth = sourceWidth / 2

    while (
        halfHeight / inSampleSize >= targetHeight &&
        halfWidth / inSampleSize >= targetWidth
    ) {
        inSampleSize *= 2
    }

    return inSampleSize.coerceAtLeast(1)
}
