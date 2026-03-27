package fr.aumombelli.gatcha.ui.screen

import androidx.compose.runtime.Composable
import fr.aumombelli.gatcha.feature.badges.BadgeBookScreen as BadgeBookFeatureScreen
import fr.aumombelli.gatcha.ui.viewmodel.BadgeBookUiState

@Composable
fun BadgeBookScreen(
    state: BadgeBookUiState,
    onRefresh: () -> Unit,
    contentVisible: Boolean = true,
) {
    BadgeBookFeatureScreen(
        state = state,
        onRefresh = onRefresh,
        contentVisible = contentVisible,
    )
}
