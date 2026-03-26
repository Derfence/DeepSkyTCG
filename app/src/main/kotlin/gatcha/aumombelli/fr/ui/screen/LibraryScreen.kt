package fr.aumombelli.gatcha.ui.screen

import androidx.compose.runtime.Composable
import fr.aumombelli.gatcha.feature.library.LibraryScreen as LibraryFeatureScreen
import fr.aumombelli.gatcha.ui.viewmodel.LibraryUiState

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
