package fr.aumombelli.dstcg.app

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import fr.aumombelli.dstcg.AppContainer
import fr.aumombelli.dstcg.audio.LocalAudioController
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
    val lifecycleOwner = LocalLifecycleOwner.current
    val audioController = appContainer.audioController
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

    DisposableEffect(lifecycleOwner, audioController) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> audioController.onAppForegrounded()
                Lifecycle.Event.ON_STOP -> audioController.onAppBackgrounded()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            audioController.release()
        }
    }

    CompositionLocalProvider(
        LocalAudioController provides audioController,
        LocalAppPerformanceProfile provides performanceProfile,
        LocalCardArtBitmapLoader provides cardArtBitmapLoader,
    ) {
        AppSceneHost(
            appContainer = appContainer,
            launchConfig = launchConfig,
        )
    }
}
