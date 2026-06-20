package fr.aumombelli.dstcg.feature.crafting

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import fr.aumombelli.dstcg.app.NewPlayerOnboardingTarget
import fr.aumombelli.dstcg.model.CraftingCardCandidate
import fr.aumombelli.dstcg.model.CraftingMode
import fr.aumombelli.dstcg.ui.screen.dstcgContentInsetsPadding

@Composable
fun CraftingScreen(
    state: CraftingUiState,
    onRefresh: () -> Unit,
    onSelectMode: (CraftingMode) -> Unit,
    onBackHome: () -> Unit,
    onBackToModes: () -> Unit,
    onApplyCrafting: (CraftingCardCandidate) -> Unit,
    onApplyAllDarkenSky: () -> Unit = {},
    contentVisible: Boolean = true,
    onCoachmarkTargetBoundsChanged: (NewPlayerOnboardingTarget, Rect?) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier,
) {
    val contentAlpha by animateFloatAsState(
        targetValue = if (contentVisible) 1f else 0f,
        animationSpec = tween(durationMillis = 520, easing = FastOutSlowInEasing),
        label = "crafting-content-alpha",
    )
    var selectedGroup by remember { mutableStateOf<CraftingCardGroup?>(null) }
    var selectedVariantKey by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(state.selectedMode) {
        selectedGroup = null
        selectedVariantKey = null
    }

    LaunchedEffect(contentVisible, state.selectedMode, state.sections, state.completion, state.isApplying, selectedGroup) {
        if (!contentVisible || state.selectedMode != null) {
            onCoachmarkTargetBoundsChanged(NewPlayerOnboardingTarget.CraftingDarkenSkyMode, null)
        }
        if (!contentVisible || state.selectedMode == null || state.sections.isEmpty() || selectedGroup != null) {
            onCoachmarkTargetBoundsChanged(NewPlayerOnboardingTarget.CraftingCandidate, null)
        }
        if (!contentVisible || selectedGroup == null || state.completion != null || state.isApplying) {
            onCoachmarkTargetBoundsChanged(NewPlayerOnboardingTarget.CraftingConfirm, null)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .alpha(contentAlpha)
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF07111B), Color(0xFF142638), Color(0xFF243344)),
                ),
            )
            .testTag("crafting-screen"),
    ) {
        if (state.selectedMode == null) {
            CraftingModeMenu(
                onSelectMode = onSelectMode,
                onBackHome = onBackHome,
                onCoachmarkTargetBoundsChanged = onCoachmarkTargetBoundsChanged,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            CraftingCandidateLibrary(
                state = state,
                onRefresh = onRefresh,
                onBackToModes = onBackToModes,
                onOpenGroup = { group ->
                    selectedGroup = group
                    selectedVariantKey = group.candidates.firstOrNull()?.sourceVariant?.key
                },
                onApplyAllDarkenSky = onApplyAllDarkenSky,
                onCoachmarkTargetBoundsChanged = onCoachmarkTargetBoundsChanged,
                modifier = Modifier
                    .fillMaxSize()
                    .dstcgContentInsetsPadding(includeBottom = true),
            )
        }
    }

    val group = selectedGroup
    if (group != null && state.selectedMode != null) {
        CraftingFullscreenDialog(
            group = group,
            mode = state.selectedMode,
            selectedVariantKey = selectedVariantKey,
            completion = state.completion,
            isApplying = state.isApplying,
            onVariantSelected = { selectedVariantKey = it },
            onApplyCrafting = onApplyCrafting,
            onCoachmarkTargetBoundsChanged = onCoachmarkTargetBoundsChanged,
            onDismiss = {
                selectedGroup = null
                selectedVariantKey = null
            },
        )
    }
}
