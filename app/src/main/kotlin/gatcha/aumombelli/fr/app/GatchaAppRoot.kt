package fr.aumombelli.gatcha.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import fr.aumombelli.gatcha.AppContainer
import fr.aumombelli.gatcha.feature.bootstrap.AppBootstrapScreen
import fr.aumombelli.gatcha.feature.bootstrap.AppBootstrapViewModel
import fr.aumombelli.gatcha.ui.viewmodel.GatchaViewModelFactory

@Composable
internal fun GatchaAppRoot(appContainer: AppContainer) {
    val bootstrapViewModel: AppBootstrapViewModel = viewModel(
        factory = GatchaViewModelFactory {
            AppBootstrapViewModel(appContainer.appStatusRepository)
        },
    )
    val bootstrapState by bootstrapViewModel.uiState.collectAsState()

    if (!bootstrapState.isCompatible) {
        AppBootstrapScreen(
            state = bootstrapState,
            onRetry = bootstrapViewModel::retry,
        )
        return
    }

    AppSceneHost(appContainer)
}
