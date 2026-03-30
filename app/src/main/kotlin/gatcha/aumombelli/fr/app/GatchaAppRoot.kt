package fr.aumombelli.gatcha.app

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import fr.aumombelli.gatcha.AppContainer
import fr.aumombelli.gatcha.performance.LocalAppPerformanceProfile
import fr.aumombelli.gatcha.performance.rememberAppPerformanceProfile
import fr.aumombelli.gatcha.ui.component.CardArtBitmapLoader
import fr.aumombelli.gatcha.ui.component.LocalCardArtBitmapLoader

@Composable
internal fun GatchaAppRoot(
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
