package fr.aumombelli.dstcg.app

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import fr.aumombelli.dstcg.feature.minigames.MiniGamesMenuScreen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
internal fun MiniGamesScene(
    sceneState: AppSceneUiState,
    transitions: AppSceneTransitionController,
    scope: CoroutineScope,
) {
    val backAllowed = !sceneState.transitionLocked
    val navigateBackToHome: () -> Unit = {
        if (backAllowed) {
            scope.launch { transitions.animateMiniGamesMenuToHome() }
        }
    }

    BackHandler(enabled = backAllowed) {
        navigateBackToHome()
    }

    MiniGamesMenuScreen(
        onBack = navigateBackToHome,
        contentVisible = sceneState.miniGamesMenuContentVisible,
    )
}
