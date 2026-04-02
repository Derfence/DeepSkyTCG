package fr.aumombelli.dstcg

import androidx.compose.runtime.Composable
import fr.aumombelli.dstcg.app.AppLaunchConfig
import fr.aumombelli.dstcg.app.DstcgAppRoot

@Composable
fun DstcgApp(
    appContainer: AppContainer,
    launchConfig: AppLaunchConfig = AppLaunchConfig(),
) {
    DstcgAppRoot(
        appContainer = appContainer,
        launchConfig = launchConfig,
    )
}
