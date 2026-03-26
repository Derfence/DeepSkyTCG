package fr.aumombelli.gatcha.app

import androidx.compose.runtime.Composable
import fr.aumombelli.gatcha.AppContainer

@Composable
internal fun GatchaAppRoot(appContainer: AppContainer) {
    AppSceneHost(appContainer)
}
