package fr.aumombelli.dstcg.ui.screen

import androidx.compose.ui.Modifier
import androidx.compose.runtime.Composable
import fr.aumombelli.dstcg.feature.packs.opening.PackOpeningScreen as PackOpeningFeatureScreen
import fr.aumombelli.dstcg.ui.motion.PackRevealBounds
import fr.aumombelli.dstcg.ui.viewmodel.PackOpeningUiState

@Composable
fun PackOpeningScreen(
    state: PackOpeningUiState,
    onDone: () -> Unit,
    showPersistentDismissHint: Boolean = false,
    initialBoosterBounds: PackRevealBounds? = null,
    modifier: Modifier = Modifier,
) {
    PackOpeningFeatureScreen(
        state = state,
        onDone = onDone,
        showPersistentDismissHint = showPersistentDismissHint,
        initialBoosterBounds = initialBoosterBounds,
        modifier = modifier,
    )
}
