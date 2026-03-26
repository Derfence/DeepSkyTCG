package fr.aumombelli.gatcha.ui.screen

import androidx.compose.ui.Modifier
import androidx.compose.runtime.Composable
import fr.aumombelli.gatcha.feature.packs.opening.PackOpeningScreen as PackOpeningFeatureScreen
import fr.aumombelli.gatcha.ui.motion.PackRevealBounds
import fr.aumombelli.gatcha.ui.viewmodel.PackOpeningUiState

@Composable
fun PackOpeningScreen(
    state: PackOpeningUiState,
    onDone: () -> Unit,
    initialBoosterBounds: PackRevealBounds? = null,
    modifier: Modifier = Modifier,
) {
    PackOpeningFeatureScreen(
        state = state,
        onDone = onDone,
        initialBoosterBounds = initialBoosterBounds,
        modifier = modifier,
    )
}
