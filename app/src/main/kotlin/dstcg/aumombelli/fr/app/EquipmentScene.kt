package fr.aumombelli.dstcg.app

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import fr.aumombelli.dstcg.AppContainer
import fr.aumombelli.dstcg.feature.equipment.EquipmentEvent
import fr.aumombelli.dstcg.feature.equipment.EquipmentScreen
import fr.aumombelli.dstcg.feature.equipment.EquipmentViewModel
import fr.aumombelli.dstcg.model.NewPlayerOnboardingStep
import fr.aumombelli.dstcg.ui.viewmodel.DstcgViewModelFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
internal fun EquipmentScene(
    appContainer: AppContainer,
    sceneState: AppSceneUiState,
    onboardingCoordinator: NewPlayerOnboardingCoordinator,
    onboardingStep: NewPlayerOnboardingStep?,
    transitions: AppSceneTransitionController,
    scope: CoroutineScope,
    updateSceneState: ((AppSceneUiState) -> AppSceneUiState) -> Unit,
) {
    val equipmentViewModel: EquipmentViewModel = viewModel(
        key = "equipment",
        factory = DstcgViewModelFactory {
            EquipmentViewModel(
                catalogRepository = appContainer.catalogRepository,
                equipmentRepository = appContainer.equipmentRepository,
            )
        },
    )
    val uiState by equipmentViewModel.uiState.collectAsState()

    LaunchedEffect(sceneState.equipmentRefreshSignal) {
        if (!uiState.isLoading || uiState.sections.isNotEmpty() || uiState.errorMessage != null) {
            equipmentViewModel.refresh()
        }
    }

    LaunchedEffect(equipmentViewModel) {
        equipmentViewModel.events.collect { event ->
            when (event) {
                is EquipmentEvent.Activated -> {
                    updateSceneState {
                        it.hideOnboardingHints()
                            .withEquipmentActivationScrollHintVisible(false)
                            .withCoachmarkTargetBounds(
                                NewPlayerOnboardingTarget.EquipmentActivation,
                                null,
                            )
                    }
                    onboardingCoordinator.onEquipmentActivated()
                }
            }
        }
    }

    val equipmentBackVisible = NewPlayerOnboardingInteractionPolicy.allowsEquipmentBack(onboardingStep)
    val equipmentBackAllowed = equipmentBackVisible && !sceneState.transitionLocked
    val navigateBackToHome: () -> Unit = {
        if (equipmentBackAllowed) {
            scope.launch { transitions.animateEquipmentToHome() }
        }
    }

    BackHandler(enabled = equipmentBackVisible) {
        navigateBackToHome()
    }

    EquipmentScreen(
        state = uiState,
        onRefresh = equipmentViewModel::refresh,
        onBack = if (equipmentBackVisible) navigateBackToHome else null,
        onActivateEquipment = { cardId ->
            if (NewPlayerOnboardingInteractionPolicy.allowsEquipmentActivation(onboardingStep)) {
                equipmentViewModel.activateEquipment(cardId)
            }
        },
        contentVisible = sceneState.equipmentContentVisible,
        onOnboardingActivationBoundsChanged = { bounds ->
            updateSceneState {
                it.withCoachmarkTargetBounds(
                    NewPlayerOnboardingTarget.EquipmentActivation,
                    bounds,
                )
            }
        },
        onOnboardingActivationScrollHintChanged = { visible ->
            updateSceneState { it.withEquipmentActivationScrollHintVisible(visible) }
        },
    )
}
