package fr.aumombelli.dstcg.ui.screen

import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.runtime.Composable
import fr.aumombelli.dstcg.app.NewPlayerOnboardingTarget
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
    onCoachmarkTargetBoundsChanged: (NewPlayerOnboardingTarget, Rect?) -> Unit = { _, _ -> },
    packReadySignal: Int,
    modifier: Modifier = Modifier,
    showBackground: Boolean = true,
    sceneVisible: Boolean = true,
    extensionListVisible: Boolean = true,
    interactionsEnabled: Boolean = true,
    backgroundOnly: Boolean = false,
    backEnabled: Boolean = true,
    onBack: (() -> Unit)? = null,
) {
    PackSelectionFeatureScreen(
        state = state,
        onRefresh = onRefresh,
        onSelectExtension = onSelectExtension,
        onSelectBooster = onSelectBooster,
        onOpenPack = onOpenPack,
        onPackRevealReady = onPackRevealReady,
        onBack = onBack,
        onSelectedBoosterBoundsChanged = onSelectedBoosterBoundsChanged,
        onCoachmarkTargetBoundsChanged = onCoachmarkTargetBoundsChanged,
        packReadySignal = packReadySignal,
        modifier = modifier,
        showBackground = showBackground,
        sceneVisible = sceneVisible,
        extensionListVisible = extensionListVisible,
        interactionsEnabled = interactionsEnabled,
        backgroundOnly = backgroundOnly,
        backEnabled = backEnabled,
    )
}
