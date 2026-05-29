package fr.aumombelli.dstcg.app

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import fr.aumombelli.dstcg.AppContainer
import fr.aumombelli.dstcg.feature.badges.BadgeBookScreen
import fr.aumombelli.dstcg.feature.badges.BadgeBookViewModel
import fr.aumombelli.dstcg.ui.viewmodel.DstcgViewModelFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
internal fun BadgeBookScene(
    appContainer: AppContainer,
    sceneState: AppSceneUiState,
    transitions: AppSceneTransitionController,
    scope: CoroutineScope,
) {
    val badgeBookViewModel: BadgeBookViewModel = viewModel(
        key = "badges",
        factory = DstcgViewModelFactory {
            BadgeBookViewModel(
                catalogRepository = appContainer.catalogRepository,
                progressRepository = appContainer.progressRepository,
            )
        },
    )
    val uiState by badgeBookViewModel.uiState.collectAsState()

    LaunchedEffect(sceneState.badgeBookRefreshSignal) {
        if (!uiState.isLoading || uiState.sections.isNotEmpty() || uiState.errorMessage != null) {
            badgeBookViewModel.refresh()
        }
    }

    val badgeBookBackAllowed = !sceneState.transitionLocked
    val navigateBackToHome: () -> Unit = {
        if (badgeBookBackAllowed) {
            scope.launch { transitions.animateBadgeBookToHome() }
        }
    }

    BackHandler(enabled = badgeBookBackAllowed) {
        navigateBackToHome()
    }

    BadgeBookScreen(
        state = uiState,
        onRefresh = badgeBookViewModel::refresh,
        onBack = navigateBackToHome,
        contentVisible = sceneState.badgeBookContentVisible,
    )
}
