package fr.aumombelli.gatcha.ui.screen

import androidx.compose.runtime.Composable
import fr.aumombelli.gatcha.feature.bootstrap.AppBootstrapScreen as BootstrapFeatureScreen
import fr.aumombelli.gatcha.ui.viewmodel.AppBootstrapUiState

@Composable
fun AppBootstrapScreen(
    state: AppBootstrapUiState,
    onRetry: () -> Unit,
) {
    BootstrapFeatureScreen(
        state = state,
        onRetry = onRetry,
    )
}
