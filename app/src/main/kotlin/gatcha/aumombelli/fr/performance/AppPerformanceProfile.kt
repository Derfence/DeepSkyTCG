package fr.aumombelli.gatcha.performance

import android.app.ActivityManager
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext

@Immutable
internal data class AppPerformanceProfile(
    val isLowRamDevice: Boolean,
    val cardArtCacheMaxBytes: Int,
    val thumbnailTargetWidthPx: Int,
    val thumbnailTargetHeightPx: Int,
    val detailTargetWidthPx: Int,
    val detailTargetHeightPx: Int,
    val enableAnimatedBackdrop: Boolean,
    val backdropStarDensityMultiplier: Float,
    val enableAnimatedThumbnailTwinkles: Boolean,
    val enableAnimatedBadgeCoins: Boolean,
)

internal val LocalAppPerformanceProfile = staticCompositionLocalOf {
    defaultAppPerformanceProfile()
}

@Composable
internal fun rememberAppPerformanceProfile(): AppPerformanceProfile {
    val appContext = LocalContext.current.applicationContext
    return remember(appContext) {
        resolveAppPerformanceProfile(appContext)
    }
}

private fun resolveAppPerformanceProfile(context: Context): AppPerformanceProfile {
    val activityManager = context.getSystemService(ActivityManager::class.java)
    val isLowRamDevice = activityManager?.isLowRamDevice == true

    return if (isLowRamDevice) {
        AppPerformanceProfile(
            isLowRamDevice = true,
            cardArtCacheMaxBytes = 16 * 1024 * 1024,
            thumbnailTargetWidthPx = 256,
            thumbnailTargetHeightPx = 448,
            detailTargetWidthPx = 768,
            detailTargetHeightPx = 1344,
            enableAnimatedBackdrop = false,
            backdropStarDensityMultiplier = 0.5f,
            enableAnimatedThumbnailTwinkles = false,
            enableAnimatedBadgeCoins = false,
        )
    } else {
        AppPerformanceProfile(
            isLowRamDevice = false,
            cardArtCacheMaxBytes = 32 * 1024 * 1024,
            thumbnailTargetWidthPx = 384,
            thumbnailTargetHeightPx = 672,
            detailTargetWidthPx = 1024,
            detailTargetHeightPx = 1796,
            enableAnimatedBackdrop = true,
            backdropStarDensityMultiplier = 1f,
            enableAnimatedThumbnailTwinkles = true,
            enableAnimatedBadgeCoins = true,
        )
    }
}

private fun defaultAppPerformanceProfile(): AppPerformanceProfile = AppPerformanceProfile(
    isLowRamDevice = false,
    cardArtCacheMaxBytes = 32 * 1024 * 1024,
    thumbnailTargetWidthPx = 384,
    thumbnailTargetHeightPx = 672,
    detailTargetWidthPx = 1024,
    detailTargetHeightPx = 1796,
    enableAnimatedBackdrop = true,
    backdropStarDensityMultiplier = 1f,
    enableAnimatedThumbnailTwinkles = true,
    enableAnimatedBadgeCoins = true,
)
