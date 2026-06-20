package fr.aumombelli.dstcg.app

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import fr.aumombelli.dstcg.AppContainer
import fr.aumombelli.dstcg.audio.SoundCue
import fr.aumombelli.dstcg.feature.crafting.CraftingEvent
import fr.aumombelli.dstcg.feature.crafting.CraftingOnboardingToolsWalkthrough
import fr.aumombelli.dstcg.feature.crafting.CraftingScreen
import fr.aumombelli.dstcg.feature.crafting.CraftingViewModel
import fr.aumombelli.dstcg.model.CraftingMode
import fr.aumombelli.dstcg.model.NewPlayerOnboardingStep
import fr.aumombelli.dstcg.ui.viewmodel.DstcgViewModelFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
internal fun CraftingScene(
    appContainer: AppContainer,
    sceneState: AppSceneUiState,
    onboardingCoordinator: NewPlayerOnboardingCoordinator,
    onboardingStep: NewPlayerOnboardingStep?,
    blockingModalSpec: NewPlayerBlockingModalSpec?,
    transitions: AppSceneTransitionController,
    scope: CoroutineScope,
    updateSceneState: ((AppSceneUiState) -> AppSceneUiState) -> Unit,
) {
    val craftingViewModel: CraftingViewModel = viewModel(
        key = "crafting",
        factory = DstcgViewModelFactory {
            CraftingViewModel(
                craftingRepository = appContainer.craftingRepository,
            )
        },
    )
    val uiState by craftingViewModel.uiState.collectAsState()

    LaunchedEffect(sceneState.craftingRefreshSignal) {
        if (uiState.selectedMode != null) {
            craftingViewModel.refresh()
        }
    }

    LaunchedEffect(craftingViewModel) {
        craftingViewModel.events.collect { event ->
            when (event) {
                is CraftingEvent.Applied -> {
                    if (event.mode == CraftingMode.DarkenSky) {
                        onboardingCoordinator.onSkyDarkeningCrafted()
                    }
                }
            }
        }
    }

    BackHandler(
        enabled = !sceneState.transitionLocked &&
            !NewPlayerOnboardingInteractionPolicy.allowsCraftingBack(onboardingStep),
    ) {
    }

    BackHandler(
        enabled = !sceneState.transitionLocked &&
            NewPlayerOnboardingInteractionPolicy.allowsCraftingBack(onboardingStep),
    ) {
        if (uiState.selectedMode != null) {
            appContainer.audioController.play(SoundCue.UiNavigate)
            craftingViewModel.backToModeSelection()
        } else {
            appContainer.audioController.play(SoundCue.UiNavigate)
            scope.launch { transitions.animateCraftingToHome() }
        }
    }

    CraftingScreen(
        state = uiState,
        onRefresh = craftingViewModel::refresh,
        onSelectMode = { mode ->
            if (NewPlayerOnboardingInteractionPolicy.allowsCraftingModeSelection(onboardingStep, mode)) {
                appContainer.audioController.play(SoundCue.UiNavigate)
                craftingViewModel.selectMode(mode)
            }
        },
        onBackHome = {
            if (NewPlayerOnboardingInteractionPolicy.allowsCraftingBack(onboardingStep)) {
                appContainer.audioController.play(SoundCue.UiNavigate)
                scope.launch { transitions.animateCraftingToHome() }
            }
        },
        onBackToModes = {
            appContainer.audioController.play(SoundCue.UiNavigate)
            craftingViewModel.backToModeSelection()
        },
        onApplyCrafting = { candidate ->
            if (
                NewPlayerOnboardingInteractionPolicy.allowsCraftingApplication(
                    step = onboardingStep,
                    mode = uiState.selectedMode,
                )
            ) {
                craftingViewModel.applyCrafting(candidate)
            }
        },
        onApplyAllDarkenSky = {
            if (
                NewPlayerOnboardingInteractionPolicy.allowsCraftingApplication(
                    step = onboardingStep,
                    mode = uiState.selectedMode,
                )
            ) {
                craftingViewModel.applyAllVisibleDarkenSkyCandidates()
            }
        },
        contentVisible = sceneState.craftingContentVisible,
        onCoachmarkTargetBoundsChanged = { target, bounds ->
            updateSceneState { it.withCoachmarkTargetBounds(target, bounds) }
        },
    )

    if (blockingModalSpec?.kind == NewPlayerBlockingModalKind.CraftingTools) {
        CraftingOnboardingToolsWalkthrough(
            onCompleted = {
                scope.launch { onboardingCoordinator.onCraftingToolsWalkthroughCompleted() }
            },
        )
    }
}
