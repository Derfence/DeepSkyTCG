package fr.aumombelli.dstcg.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import fr.aumombelli.dstcg.data.ProgressGateway
import fr.aumombelli.dstcg.data.requireUsableProgress
import fr.aumombelli.dstcg.model.NewPlayerOnboardingStep
import fr.aumombelli.dstcg.ui.motion.AppScene

internal data class NewPlayerOnboardingUiState(
    val currentStep: NewPlayerOnboardingStep? = null,
    val libraryCardHintVisible: Boolean = false,
)

internal data class NewPlayerCoachmarkSpec(
    val target: NewPlayerOnboardingTarget,
    val title: String,
    val message: String,
)

internal class NewPlayerOnboardingCoordinator(
    private val progressRepository: ProgressGateway,
) {
    var uiState by mutableStateOf(NewPlayerOnboardingUiState())
        private set

    suspend fun syncFromProgress() {
        val progress = progressRepository.loadProgress().requireUsableProgress().progress
        uiState = uiState.copy(currentStep = progress.newPlayerOnboardingStep)
    }

    suspend fun onHomeOpenPackSelected() {
        advanceTo(NewPlayerOnboardingStep.SelectFirstExtension) {
            it == NewPlayerOnboardingStep.OpenFirstPackMenu
        }
    }

    suspend fun onExtensionSelected() {
        advanceTo(NewPlayerOnboardingStep.SelectFirstBooster) {
            it == NewPlayerOnboardingStep.SelectFirstExtension
        }
    }

    suspend fun onPackOpened(): Boolean {
        val currentStep = uiState.currentStep ?: return false
        return when (currentStep) {
            NewPlayerOnboardingStep.OpenFirstPackMenu,
            NewPlayerOnboardingStep.SelectFirstExtension,
            NewPlayerOnboardingStep.SelectFirstBooster,
            -> {
                persistStep(NewPlayerOnboardingStep.ViewLibrary)
                true
            }

            NewPlayerOnboardingStep.OpenSecondPackMenu -> {
                persistStep(NewPlayerOnboardingStep.ViewEquipmentMenu)
                true
            }

            NewPlayerOnboardingStep.ViewEquipmentMenu,
            NewPlayerOnboardingStep.ActivateFirstEquipment,
            -> true

            NewPlayerOnboardingStep.ViewLibrary,
            NewPlayerOnboardingStep.ViewBadges,
            NewPlayerOnboardingStep.Completed,
            -> false
        }
    }

    suspend fun onLibraryOpened(): Boolean {
        val currentStep = uiState.currentStep ?: return false
        if (currentStep != NewPlayerOnboardingStep.ViewLibrary) return false

        persistStep(NewPlayerOnboardingStep.ViewBadges)
        uiState = uiState.copy(libraryCardHintVisible = true)
        return true
    }

    suspend fun onBadgeBookOpened() {
        advanceTo(NewPlayerOnboardingStep.OpenSecondPackMenu) {
            it == NewPlayerOnboardingStep.ViewBadges
        }
        uiState = uiState.copy(libraryCardHintVisible = false)
    }

    suspend fun onEquipmentOpened() {
        advanceTo(NewPlayerOnboardingStep.ActivateFirstEquipment) {
            it == NewPlayerOnboardingStep.ViewEquipmentMenu
        }
    }

    suspend fun onEquipmentActivated() {
        advanceTo(NewPlayerOnboardingStep.Completed) {
            it == NewPlayerOnboardingStep.ActivateFirstEquipment
        }
    }

    fun onLibraryCardHintConsumed() {
        if (!uiState.libraryCardHintVisible) return
        uiState = uiState.copy(libraryCardHintVisible = false)
    }

    fun activeCoachmark(
        currentScene: AppScene,
        sceneState: AppSceneUiState,
        badgeCelebrationVisible: Boolean,
    ): NewPlayerCoachmarkSpec? {
        val step = uiState.currentStep ?: return null
        if (step == NewPlayerOnboardingStep.Completed) return null

        return when (step) {
            NewPlayerOnboardingStep.OpenFirstPackMenu -> {
                if (
                    currentScene == AppScene.Home &&
                    sceneState.homeContentVisible &&
                    sceneState.onboardingHintsVisible &&
                    !sceneState.transitionLocked &&
                    sceneState.coachmarkTargetBounds.containsKey(NewPlayerOnboardingTarget.HomeOpenPack)
                ) {
                    NewPlayerCoachmarkSpec(
                        target = NewPlayerOnboardingTarget.HomeOpenPack,
                        title = "Premières cartes",
                        message = "Commençons ta collection de cartes d'objets célestes !",
                    )
                } else {
                    null
                }
            }

            NewPlayerOnboardingStep.SelectFirstExtension -> {
                if (
                    currentScene == AppScene.PackSelection &&
                    sceneState.packSceneVisible &&
                    sceneState.packExtensionListVisible &&
                    sceneState.onboardingHintsVisible &&
                    sceneState.coachmarkTargetBounds.containsKey(NewPlayerOnboardingTarget.PackSelectionExtension)
                ) {
                    NewPlayerCoachmarkSpec(
                        target = NewPlayerOnboardingTarget.PackSelectionExtension,
                        title = "Choisis une extension",
                        message = "Choisissons cette collection pour commencer.",
                    )
                } else {
                    null
                }
            }

            NewPlayerOnboardingStep.SelectFirstBooster -> {
                if (
                    currentScene == AppScene.PackSelection &&
                    sceneState.packSceneVisible &&
                    sceneState.onboardingHintsVisible &&
                    sceneState.coachmarkTargetBounds.containsKey(NewPlayerOnboardingTarget.PackSelectionBooster)
                ) {
                    NewPlayerCoachmarkSpec(
                        target = NewPlayerOnboardingTarget.PackSelectionBooster,
                        title = "Choisis un booster",
                        message = "Touche le booster de ton choix pour en révéler le contenu ⭐",
                    )
                } else {
                    null
                }
            }

            NewPlayerOnboardingStep.ViewLibrary -> {
                if (
                    currentScene == AppScene.Home &&
                    sceneState.homeContentVisible &&
                    sceneState.onboardingHintsVisible &&
                    !sceneState.transitionLocked &&
                    sceneState.coachmarkTargetBounds.containsKey(NewPlayerOnboardingTarget.HomeLibrary)
                ) {
                    NewPlayerCoachmarkSpec(
                        target = NewPlayerOnboardingTarget.HomeLibrary,
                        title = "Retrouve tes cartes",
                        message = "Ouvre la bibliotheque pour revoir les cartes que tu as obtenues.",
                    )
                } else {
                    null
                }
            }

            NewPlayerOnboardingStep.ViewBadges -> {
                if (
                    currentScene == AppScene.Home &&
                    sceneState.homeContentVisible &&
                    sceneState.onboardingHintsVisible &&
                    !sceneState.transitionLocked &&
                    !badgeCelebrationVisible &&
                    sceneState.coachmarkTargetBounds.containsKey(NewPlayerOnboardingTarget.HomeBadges)
                ) {
                    NewPlayerCoachmarkSpec(
                        target = NewPlayerOnboardingTarget.HomeBadges,
                        title = "Badges d'astronome",
                        message = "Ton premier badge t'attend ici !",
                    )
                } else {
                    null
                }
            }

            NewPlayerOnboardingStep.OpenSecondPackMenu -> null

            NewPlayerOnboardingStep.ViewEquipmentMenu -> {
                if (
                    currentScene == AppScene.Home &&
                    sceneState.homeContentVisible &&
                    sceneState.onboardingHintsVisible &&
                    !sceneState.transitionLocked &&
                    !badgeCelebrationVisible &&
                    sceneState.coachmarkTargetBounds.containsKey(NewPlayerOnboardingTarget.HomeEquipment)
                ) {
                    NewPlayerCoachmarkSpec(
                        target = NewPlayerOnboardingTarget.HomeEquipment,
                        title = "Menu equipement",
                        message = "Ton nouvel equipement est accessible ici depuis l'accueil.",
                    )
                } else {
                    null
                }
            }

            NewPlayerOnboardingStep.ActivateFirstEquipment -> {
                if (
                    currentScene == AppScene.Equipment &&
                    sceneState.equipmentContentVisible &&
                    sceneState.onboardingHintsVisible &&
                    !sceneState.transitionLocked &&
                    sceneState.coachmarkTargetBounds.containsKey(NewPlayerOnboardingTarget.EquipmentActivation)
                ) {
                    NewPlayerCoachmarkSpec(
                        target = NewPlayerOnboardingTarget.EquipmentActivation,
                        title = "Active ta carte",
                        message = "Active cet equipement pour ameliorer les prochains packs.",
                    )
                } else {
                    null
                }
            }

            NewPlayerOnboardingStep.Completed -> null
        }
    }

    private suspend fun advanceTo(
        nextStep: NewPlayerOnboardingStep,
        shouldAdvance: (NewPlayerOnboardingStep) -> Boolean,
    ) {
        val currentStep = uiState.currentStep ?: return
        if (!shouldAdvance(currentStep)) return
        persistStep(nextStep)
    }

    private suspend fun persistStep(nextStep: NewPlayerOnboardingStep) {
        val loadedProgress = progressRepository.loadProgress().requireUsableProgress().progress
        if (loadedProgress.newPlayerOnboardingStep == nextStep) {
            uiState = uiState.copy(currentStep = nextStep)
            return
        }

        progressRepository.saveProgress(
            loadedProgress.copy(newPlayerOnboardingStep = nextStep),
        )
        uiState = uiState.copy(currentStep = nextStep)
    }
}
