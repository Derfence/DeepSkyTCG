package fr.aumombelli.gatcha

import androidx.compose.runtime.Composable
import fr.aumombelli.gatcha.app.AppLaunchConfig
import fr.aumombelli.gatcha.app.GatchaAppRoot

@Composable
fun GatchaApp(
    appContainer: AppContainer,
    launchConfig: AppLaunchConfig = AppLaunchConfig(),
) {
    GatchaAppRoot(
        appContainer = appContainer,
        launchConfig = launchConfig,
    )
}
