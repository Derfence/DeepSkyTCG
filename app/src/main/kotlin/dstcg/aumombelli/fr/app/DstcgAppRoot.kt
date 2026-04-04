package fr.aumombelli.dstcg.app

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import fr.aumombelli.dstcg.AppContainer
import fr.aumombelli.dstcg.performance.LocalAppPerformanceProfile
import fr.aumombelli.dstcg.performance.rememberAppPerformanceProfile
import fr.aumombelli.dstcg.ui.component.CardArtBitmapLoader
import fr.aumombelli.dstcg.ui.component.LocalCardArtBitmapLoader

@Composable
internal fun DstcgAppRoot(
    appContainer: AppContainer,
    launchConfig: AppLaunchConfig,
) {
    val appContext = LocalContext.current.applicationContext
    val performanceProfile = rememberAppPerformanceProfile()
    val cardArtBitmapLoader = remember(
        appContext,
        performanceProfile.cardArtCacheMaxBytes,
    ) {
        CardArtBitmapLoader(
            assetManager = appContext.assets,
            maxCacheBytes = performanceProfile.cardArtCacheMaxBytes,
        )
    }

    CompositionLocalProvider(
        LocalAppPerformanceProfile provides performanceProfile,
        LocalCardArtBitmapLoader provides cardArtBitmapLoader,
    ) {
        AppSceneHost(
            appContainer = appContainer,
            launchConfig = launchConfig,
        )
    }
}
