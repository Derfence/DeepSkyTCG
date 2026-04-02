package fr.aumombelli.dstcg.ui.screen

import androidx.compose.ui.Modifier
import androidx.compose.runtime.Composable
import fr.aumombelli.dstcg.feature.packs.selection.PackSelectionScreen as PackSelectionFeatureScreen
import fr.aumombelli.dstcg.ui.motion.PackRevealBounds
import fr.aumombelli.dstcg.ui.viewmodel.PackSelectionUiState

@Composable
fun PackSelectionScreen(
    state: PackSelectionUiState,
    onRefresh: () -> Unit,
    onSelectExtension: (String) -> Unit,
    onSelectBooster: (Int) -> Unit,
    onOpenPack: (String) -> Unit,
    onPackRevealReady: () -> Unit,
    onSelectedBoosterBoundsChanged: (PackRevealBounds?) -> Unit = {},
    packReadySignal: Int,
    modifier: Modifier = Modifier,
    showBackground: Boolean = true,
    sceneVisible: Boolean = true,
    extensionListVisible: Boolean = true,
    interactionsEnabled: Boolean = true,
) {
    PackSelectionFeatureScreen(
        state = state,
        onRefresh = onRefresh,
        onSelectExtension = onSelectExtension,
        onSelectBooster = onSelectBooster,
        onOpenPack = onOpenPack,
        onPackRevealReady = onPackRevealReady,
        onSelectedBoosterBoundsChanged = onSelectedBoosterBoundsChanged,
        packReadySignal = packReadySignal,
        modifier = modifier,
        showBackground = showBackground,
        sceneVisible = sceneVisible,
        extensionListVisible = extensionListVisible,
        interactionsEnabled = interactionsEnabled,
    )
}
