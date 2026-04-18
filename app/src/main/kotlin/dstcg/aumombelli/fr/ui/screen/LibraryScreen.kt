package fr.aumombelli.dstcg.ui.screen

import androidx.compose.runtime.Composable
import fr.aumombelli.dstcg.feature.library.LibraryScreen as LibraryFeatureScreen
import fr.aumombelli.dstcg.ui.viewmodel.LibraryUiState

@Composable
fun LibraryScreen(
    state: LibraryUiState,
    onRefresh: () -> Unit,
    contentVisible: Boolean = true,
    interactionsEnabled: Boolean = true,
    showOnboardingHint: Boolean = false,
    onOnboardingHintConsumed: () -> Unit = {},
    showOnboardingVariantWalkthrough: Boolean = false,
    onOnboardingVariantWalkthroughCompleted: () -> Unit = {},
) {
    LibraryFeatureScreen(
        state = state,
        onRefresh = onRefresh,
        contentVisible = contentVisible,
        interactionsEnabled = interactionsEnabled,
        showOnboardingHint = showOnboardingHint,
        onOnboardingHintConsumed = onOnboardingHintConsumed,
        showOnboardingVariantWalkthrough = showOnboardingVariantWalkthrough,
        onOnboardingVariantWalkthroughCompleted = onOnboardingVariantWalkthroughCompleted,
    )
}
