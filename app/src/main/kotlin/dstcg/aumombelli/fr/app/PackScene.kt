package fr.aumombelli.dstcg.app

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import fr.aumombelli.dstcg.AppContainer
import fr.aumombelli.dstcg.feature.packs.opening.PackOpeningScreen
import fr.aumombelli.dstcg.feature.packs.opening.PackOpeningViewModel
import fr.aumombelli.dstcg.feature.packs.selection.PackEvent
import fr.aumombelli.dstcg.feature.packs.selection.PackSelectionScreen
import fr.aumombelli.dstcg.feature.packs.selection.PackViewModel
import fr.aumombelli.dstcg.model.NewPlayerOnboardingStep
import fr.aumombelli.dstcg.ui.motion.AppScene
import fr.aumombelli.dstcg.ui.viewmodel.DstcgViewModelFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
internal fun PackScene(
    appContainer: AppContainer,
    sceneState: AppSceneUiState,
    onboardingCoordinator: NewPlayerOnboardingCoordinator,
    onboardingStep: NewPlayerOnboardingStep?,
    transitions: AppSceneTransitionController,
    scope: CoroutineScope,
    updateSceneState: ((AppSceneUiState) -> AppSceneUiState) -> Unit,
) {
    val packViewModel: PackViewModel = viewModel(
        key = "pack",
        factory = DstcgViewModelFactory {
            PackViewModel(
                catalogRepository = appContainer.catalogRepository,
                progressRepository = appContainer.progressRepository,
                packRepository = appContainer.packRepository,
                gameSettings = appContainer.gameSettings,
            )
        },
    )
    val uiState by packViewModel.uiState.collectAsState()

    LaunchedEffect(sceneState.packRefreshSignal) {
        if (
            !uiState.isLoading ||
            uiState.extensions.isNotEmpty() ||
            uiState.errorMessage != null ||
            uiState.selectedExtensionId != null
        ) {
            packViewModel.refresh()
        }
    }

    LaunchedEffect(packViewModel) {
        packViewModel.events.collect { event ->
            when (event) {
                is PackEvent.PackReadyForReveal -> {
                    val shouldDeferBadgeCelebration = onboardingCoordinator.onPackOpened()
                    updateSceneState {
                        it.registerPackReady(
                            event.newlyUnlockedBadges,
                            deferBadgeCelebration = shouldDeferBadgeCelebration,
                        )
                    }
                }
            }
        }
    }

    if (sceneState.currentScene == AppScene.PackSelection) {
        BackHandler(
            enabled = !sceneState.transitionLocked &&
                !uiState.isAwaitingPackResult &&
                NewPlayerOnboardingInteractionPolicy.allowsPackSelectionBack(onboardingStep),
        ) {
            if (uiState.selectedExtensionId != null) {
                packViewModel.clearExtensionSelection()
            } else {
                scope.launch { transitions.animatePackSelectionToHome() }
            }
        }
    } else {
        BackHandler(enabled = sceneState.packOpeningExitSignal == 0) {
            updateSceneState { it.requestPackOpeningExit() }
        }
    }

    if (sceneState.currentScene == AppScene.PackSelection || sceneState.packSceneVisible) {
        PackSelectionScreen(
            state = uiState,
            onRefresh = packViewModel::refresh,
            onSelectExtension = { extensionId ->
                if (NewPlayerOnboardingInteractionPolicy.allowsPackSelectionExtensionSelection(onboardingStep)) {
                    packViewModel.selectExtension(extensionId)
                    scope.launch { onboardingCoordinator.onExtensionSelected() }
                }
            },
            onSelectBooster = { boosterIndex ->
                if (NewPlayerOnboardingInteractionPolicy.allowsPackSelectionBoosterSelection(onboardingStep)) {
                    packViewModel.selectBooster(boosterIndex)
                }
            },
            onOpenPack = { extensionId ->
                if (NewPlayerOnboardingInteractionPolicy.allowsPackSelectionBoosterSelection(onboardingStep)) {
                    packViewModel.openPack(extensionId)
                }
            },
            onPackRevealReady = {
                updateSceneState { it.enterPackOpening() }
                scope.launch {
                    delay(PackSelectionOpeningCrossfadeDurationMillis)
                    updateSceneState { it.hidePackSelectionScene() }
                    packViewModel.clearExtensionSelection()
                }
            },
            onSelectedBoosterBoundsChanged = { bounds ->
                updateSceneState { it.withPackRevealBounds(bounds) }
            },
            onCoachmarkTargetBoundsChanged = { target, bounds ->
                updateSceneState { it.withCoachmarkTargetBounds(target, bounds) }
            },
            packReadySignal = sceneState.packReadySignal,
            showBackground = false,
            sceneVisible = sceneState.currentScene == AppScene.PackSelection && sceneState.packSceneVisible,
            extensionListVisible = sceneState.currentScene == AppScene.PackSelection &&
                sceneState.packExtensionListVisible,
            interactionsEnabled = !sceneState.transitionLocked && sceneState.currentScene == AppScene.PackSelection,
            backgroundOnly = sceneState.currentScene == AppScene.PackOpening,
        )
    }

    if (sceneState.currentScene == AppScene.PackOpening) {
        val openingViewModel: PackOpeningViewModel = viewModel(
            key = "pack-opening",
            factory = DstcgViewModelFactory {
                PackOpeningViewModel(
                    catalogRepository = appContainer.catalogRepository,
                    packRepository = appContainer.packRepository,
                )
            },
        )
        val openingUiState by openingViewModel.uiState.collectAsState()
        val showPersistentDismissHint =
            sceneState.onboardingHintsVisible &&
                NewPlayerOnboardingInteractionPolicy.showsPackOpeningDismissHint(
                    onboardingCoordinator.uiState.currentStep,
                )

        PackOpeningScreen(
            state = openingUiState,
            showPersistentDismissHint = showPersistentDismissHint,
            initialBoosterBounds = sceneState.selectedPackRevealBounds,
            initialBoosterDecorSeed = uiState.selectedBoosterIndex,
            dismissSignal = sceneState.packOpeningExitSignal,
            onDone = {
                scope.launch { transitions.finishPackOpeningToHome() }
            },
        )
    }
}

private const val PackSelectionOpeningCrossfadeDurationMillis = 420L
