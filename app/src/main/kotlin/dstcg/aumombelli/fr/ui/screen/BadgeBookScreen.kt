package fr.aumombelli.dstcg.ui.screen

import androidx.compose.runtime.Composable
import fr.aumombelli.dstcg.feature.badges.BadgeBookScreen as BadgeBookFeatureScreen
import fr.aumombelli.dstcg.ui.viewmodel.BadgeBookUiState

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
