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
    initialBoosterDecorSeed: Any? = Unit,
    modifier: Modifier = Modifier,
    dismissSignal: Int = 0,
    onDismissRequest: (() -> Unit)? = null,
) {
    PackOpeningFeatureScreen(
        state = state,
        onDone = onDone,
        onDismissRequest = onDismissRequest,
        dismissSignal = dismissSignal,
        showPersistentDismissHint = showPersistentDismissHint,
        initialBoosterBounds = initialBoosterBounds,
        initialBoosterDecorSeed = initialBoosterDecorSeed,
        modifier = modifier,
    )
}
