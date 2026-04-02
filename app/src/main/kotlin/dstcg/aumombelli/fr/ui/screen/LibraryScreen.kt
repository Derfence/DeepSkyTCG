package fr.aumombelli.dstcg.ui.screen

import androidx.compose.runtime.Composable
import fr.aumombelli.dstcg.feature.library.LibraryScreen as LibraryFeatureScreen
import fr.aumombelli.dstcg.ui.viewmodel.LibraryUiState

@Composable
fun LibraryScreen(
    state: LibraryUiState,
    onRefresh: () -> Unit,
    contentVisible: Boolean = true,
) {
    LibraryFeatureScreen(
        state = state,
        onRefresh = onRefresh,
        contentVisible = contentVisible,
    )
}
