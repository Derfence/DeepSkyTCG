package fr.aumombelli.dstcg.app

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import fr.aumombelli.dstcg.AppContainer
import fr.aumombelli.dstcg.feature.home.HomeScreen
import fr.aumombelli.dstcg.feature.home.HomeViewModel
import fr.aumombelli.dstcg.model.NewPlayerOnboardingStep
import fr.aumombelli.dstcg.ui.motion.BrandLogoVariant
import fr.aumombelli.dstcg.ui.viewmodel.DstcgViewModelFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
internal fun HomeScene(
    appContainer: AppContainer,
    activity: Activity?,
    sceneState: AppSceneUiState,
    onboardingCoordinator: NewPlayerOnboardingCoordinator,
    onboardingStep: NewPlayerOnboardingStep?,
    hasEnteredHomeOnce: MutableState<Boolean>,
    homeContentEntranceSettled: MutableState<Boolean>,
    homeLogoVariant: BrandLogoVariant,
    transitions: AppSceneTransitionController,
    scope: CoroutineScope,
    updateSceneState: ((AppSceneUiState) -> AppSceneUiState) -> Unit,
) {
    val homeViewModel: HomeViewModel = viewModel(
        key = "home",
        factory = DstcgViewModelFactory {
            HomeViewModel(
                progressRepository = appContainer.progressRepository,
                craftingRepository = appContainer.craftingRepository,
            )
        },
    )
    val uiState by homeViewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        if (hasEnteredHomeOnce.value) {
            homeViewModel.refresh()
        } else {
            hasEnteredHomeOnce.value = true
        }
    }

    BackHandler(
        enabled = !sceneState.transitionLocked &&
            NewPlayerOnboardingInteractionPolicy.allowsHomeExit(onboardingStep),
    ) {
        activity?.finish()
    }

    HomeScreen(
        state = uiState,
        onOpenPack = {
            if (
                !sceneState.transitionLocked &&
                NewPlayerOnboardingInteractionPolicy.allowsHomeOpenPack(onboardingStep)
            ) {
                scope.launch {
                    onboardingCoordinator.onHomeOpenPackSelected()
                    transitions.animateHomeToPackSelection()
                }
            }
        },
        onOpenLibrary = {
            if (
                !sceneState.transitionLocked &&
                uiState.isLibraryMenuVisible &&
                NewPlayerOnboardingInteractionPolicy.allowsHomeLibrary(onboardingStep)
            ) {
                homeViewModel.markLibrarySeen()
                scope.launch {
                    val shouldResumeBadgeCelebration = onboardingCoordinator.onLibraryOpened()
                    if (shouldResumeBadgeCelebration) {
                        updateSceneState { it.resumePendingBadgeCelebration() }
                    }
                    transitions.animateHomeToLibrary()
                }
            }
        },
        onOpenCrafting = {
            if (
                !sceneState.transitionLocked &&
                uiState.isCraftingMenuAvailable &&
                NewPlayerOnboardingInteractionPolicy.allowsHomeCrafting(onboardingStep)
            ) {
                scope.launch {
                    onboardingCoordinator.onCraftingOpened()
                    transitions.animateHomeToCrafting()
                }
            }
        },
        onOpenEquipment = {
            if (
                !sceneState.transitionLocked &&
                NewPlayerOnboardingInteractionPolicy.allowsHomeEquipment(onboardingStep)
            ) {
                homeViewModel.markEquipmentSeen()
                scope.launch {
                    onboardingCoordinator.onEquipmentOpened()
                    transitions.animateHomeToEquipment()
                }
            }
        },
        onOpenBadgeBook = {
            if (
                !sceneState.transitionLocked &&
                uiState.isBadgeBookMenuVisible &&
                NewPlayerOnboardingInteractionPolicy.allowsHomeBadgeBook(onboardingStep)
            ) {
                homeViewModel.markBadgeBookSeen()
                scope.launch {
                    onboardingCoordinator.onBadgeBookOpened()
                    transitions.animateHomeToBadgeBook()
                }
            }
        },
        onOpenMiniGamesMenu = {
            if (
                !sceneState.transitionLocked &&
                uiState.isMiniGamesMenuVisible &&
                NewPlayerOnboardingInteractionPolicy.allowsHomeMiniGames(onboardingStep)
            ) {
                homeViewModel.markMiniGamesSeen()
                scope.launch {
                    transitions.animateHomeToMiniGamesMenu()
                }
            }
        },
        onResetProgress = homeViewModel::resetProgress,
        showBackground = false,
        contentVisible = sceneState.homeContentVisible,
        interactionsEnabled = !sceneState.transitionLocked,
        allowAuxiliaryActions = NewPlayerOnboardingInteractionPolicy
            .allowsHomeAuxiliaryActions(onboardingStep),
        homeLogoVariant = homeLogoVariant,
        onHomeLogoLayoutChanged = { badgeCenterYInRootPx, landingSizePx ->
            updateSceneState {
                it.withHomeLogoBadgeLayout(
                    centerYPx = badgeCenterYInRootPx,
                    landingSizePx = landingSizePx,
                )
            }
        },
        onContentEntranceSettledChanged = { settled ->
            homeContentEntranceSettled.value = settled
        },
        onCoachmarkTargetBoundsChanged = { target, bounds ->
            updateSceneState { it.withCoachmarkTargetBounds(target, bounds) }
        },
    )
}
