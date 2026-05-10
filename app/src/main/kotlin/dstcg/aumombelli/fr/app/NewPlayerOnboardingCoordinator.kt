package fr.aumombelli.dstcg.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import fr.aumombelli.dstcg.data.CraftingGateway
import fr.aumombelli.dstcg.data.ProgressGateway
import fr.aumombelli.dstcg.data.requireUsableProgress
import fr.aumombelli.dstcg.model.HomeMenuNoveltyState
import fr.aumombelli.dstcg.model.NewPlayerOnboardingStep
import fr.aumombelli.dstcg.model.StandaloneProgress
import fr.aumombelli.dstcg.model.or
import fr.aumombelli.dstcg.ui.motion.AppScene

internal data class NewPlayerOnboardingUiState(
    val currentStep: NewPlayerOnboardingStep? = null,
    val libraryCardHintVisible: Boolean = false,
    val blockingModalStep: NewPlayerOnboardingStep? = null,
)

internal data class NewPlayerCoachmarkSpec(
    val target: NewPlayerOnboardingTarget,
    val title: String,
    val message: String,
    val placement: NewPlayerCoachmarkPlacement = NewPlayerCoachmarkPlacement.AroundTarget,
    val targetEffect: NewPlayerCoachmarkTargetEffect = NewPlayerCoachmarkTargetEffect.Highlight,
)

internal enum class NewPlayerCoachmarkPlacement {
    AroundTarget,
    BelowTarget,
    CenteredOnTarget,
    OverHomeCardText,
    OverlapTargetBottom,
}

internal enum class NewPlayerCoachmarkTargetEffect {
    Highlight,
    None,
    TouchZone,
}

internal enum class NewPlayerBlockingModalKind {
    WelcomeIntro,
    LibraryVariants,
    CraftingTools,
    Conclusion,
}

internal data class NewPlayerBlockingModalSpec(
    val kind: NewPlayerBlockingModalKind,
)

internal class NewPlayerOnboardingCoordinator(
    private val progressRepository: ProgressGateway,
    private val craftingRepository: CraftingGateway? = null,
) {
    var uiState by mutableStateOf(NewPlayerOnboardingUiState())
        private set

    suspend fun syncFromProgress() {
        val progress = progressRepository.loadProgress().requireUsableProgress().progress
        uiState = uiState.withStep(resolveCraftingEligibilityStep(progress))
    }

    suspend fun onWelcomeIntroAcknowledged() {
        advanceTo(NewPlayerOnboardingStep.OpenFirstPackMenu) {
            it == NewPlayerOnboardingStep.ShowWelcomeIntro
        }
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
            NewPlayerOnboardingStep.ShowWelcomeIntro,
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

            NewPlayerOnboardingStep.AwaitCraftingEligibility -> {
                syncCraftingEligibility()
                false
            }

            NewPlayerOnboardingStep.ViewLibrary,
            NewPlayerOnboardingStep.LearnLibraryVariants,
            NewPlayerOnboardingStep.ViewBadges,
            NewPlayerOnboardingStep.ViewCraftingMenu,
            NewPlayerOnboardingStep.LearnCraftingTools,
            NewPlayerOnboardingStep.UseSkyDarkening,
            NewPlayerOnboardingStep.DiscoverMiniGames,
            NewPlayerOnboardingStep.ShowConclusion,
            NewPlayerOnboardingStep.Completed,
            -> false
        }
    }

    suspend fun onLibraryOpened(): Boolean {
        val currentStep = uiState.currentStep ?: return false
        return when (currentStep) {
            NewPlayerOnboardingStep.ViewLibrary -> {
                persistStep(NewPlayerOnboardingStep.LearnLibraryVariants)
                false
            }

            NewPlayerOnboardingStep.LearnLibraryVariants -> false
            else -> false
        }
    }

    suspend fun onLibraryVariantWalkthroughCompleted(): Boolean {
        val currentStep = uiState.currentStep ?: return false
        if (currentStep != NewPlayerOnboardingStep.LearnLibraryVariants) return false

        persistStep(
            nextStep = NewPlayerOnboardingStep.ViewBadges,
            libraryCardHintVisible = true,
        )
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
        advanceTo(NewPlayerOnboardingStep.AwaitCraftingEligibility) {
            it == NewPlayerOnboardingStep.ActivateFirstEquipment
        }
        syncCraftingEligibility()
    }

    suspend fun onCraftingOpened() {
        advanceTo(NewPlayerOnboardingStep.LearnCraftingTools) {
            it == NewPlayerOnboardingStep.ViewCraftingMenu
        }
    }

    suspend fun onCraftingToolsWalkthroughCompleted() {
        advanceTo(NewPlayerOnboardingStep.UseSkyDarkening) {
            it == NewPlayerOnboardingStep.LearnCraftingTools
        }
    }

    suspend fun onSkyDarkeningCrafted() {
        val currentStep = uiState.currentStep ?: return
        if (currentStep != NewPlayerOnboardingStep.UseSkyDarkening) return
        persistMiniGamesDiscovery()
    }

    suspend fun onMiniGamesMenuOpened() {
        advanceTo(NewPlayerOnboardingStep.ShowConclusion) {
            it == NewPlayerOnboardingStep.DiscoverMiniGames
        }
    }

    suspend fun onConclusionAcknowledged() {
        advanceTo(NewPlayerOnboardingStep.Completed) {
            it == NewPlayerOnboardingStep.ShowConclusion
        }
    }

    fun onLibraryCardHintConsumed() {
        if (!uiState.libraryCardHintVisible) return
        uiState = uiState.copy(libraryCardHintVisible = false)
    }

    fun isBlockingStep(step: NewPlayerOnboardingStep? = uiState.currentStep): Boolean =
        NewPlayerOnboardingInteractionPolicy.isBlockingStep(step)

    fun activeBlockingModal(
        currentScene: AppScene,
        sceneState: AppSceneUiState,
    ): NewPlayerBlockingModalSpec? {
        if (sceneState.transitionLocked || !sceneState.onboardingHintsVisible) return null

        return when (uiState.blockingModalStep) {
            NewPlayerOnboardingStep.ShowWelcomeIntro ->
                if (currentScene == AppScene.Home && sceneState.homeContentVisible) {
                    NewPlayerBlockingModalSpec(kind = NewPlayerBlockingModalKind.WelcomeIntro)
                } else {
                    null
                }

            NewPlayerOnboardingStep.LearnLibraryVariants ->
                if (currentScene == AppScene.Library && sceneState.libraryContentVisible) {
                    NewPlayerBlockingModalSpec(kind = NewPlayerBlockingModalKind.LibraryVariants)
                } else {
                    null
                }

            NewPlayerOnboardingStep.LearnCraftingTools ->
                if (currentScene == AppScene.Crafting && sceneState.craftingContentVisible) {
                    NewPlayerBlockingModalSpec(kind = NewPlayerBlockingModalKind.CraftingTools)
                } else {
                    null
                }

            NewPlayerOnboardingStep.ShowConclusion ->
                if (currentScene == AppScene.Home && sceneState.homeContentVisible) {
                    NewPlayerBlockingModalSpec(kind = NewPlayerBlockingModalKind.Conclusion)
                } else {
                    null
                }

            else -> null
        }
    }

    fun activeCoachmark(
        currentScene: AppScene,
        sceneState: AppSceneUiState,
        badgeCelebrationVisible: Boolean,
    ): NewPlayerCoachmarkSpec? {
        val step = uiState.currentStep ?: return null
        if (
            step == NewPlayerOnboardingStep.ShowConclusion ||
            step == NewPlayerOnboardingStep.Completed
        ) {
            return null
        }

        return when (step) {
            NewPlayerOnboardingStep.ShowWelcomeIntro -> null

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
                        placement = NewPlayerCoachmarkPlacement.OverHomeCardText,
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
                        placement = NewPlayerCoachmarkPlacement.BelowTarget,
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
                        placement = NewPlayerCoachmarkPlacement.CenteredOnTarget,
                        targetEffect = NewPlayerCoachmarkTargetEffect.None,
                    )
                } else {
                    null
                }
            }

            NewPlayerOnboardingStep.ViewLibrary,
            NewPlayerOnboardingStep.LearnLibraryVariants,
            -> {
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
                    (
                        sceneState.coachmarkTargetBounds.containsKey(NewPlayerOnboardingTarget.EquipmentActivation) ||
                            sceneState.equipmentActivationScrollHintVisible
                        )
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

            NewPlayerOnboardingStep.AwaitCraftingEligibility -> null

            NewPlayerOnboardingStep.ViewCraftingMenu,
            NewPlayerOnboardingStep.LearnCraftingTools,
            NewPlayerOnboardingStep.UseSkyDarkening,
            -> activeCraftingCoachmark(
                currentScene = currentScene,
                sceneState = sceneState,
                badgeCelebrationVisible = badgeCelebrationVisible,
            )

            NewPlayerOnboardingStep.DiscoverMiniGames -> activeMiniGamesDiscoveryCoachmark(
                currentScene = currentScene,
                sceneState = sceneState,
                badgeCelebrationVisible = badgeCelebrationVisible,
            )

            NewPlayerOnboardingStep.ShowConclusion,
            NewPlayerOnboardingStep.Completed -> null
        }
    }

    private fun activeMiniGamesDiscoveryCoachmark(
        currentScene: AppScene,
        sceneState: AppSceneUiState,
        badgeCelebrationVisible: Boolean,
    ): NewPlayerCoachmarkSpec? {
        if (
            currentScene != AppScene.Home ||
            !sceneState.homeContentVisible ||
            !sceneState.onboardingHintsVisible ||
            sceneState.transitionLocked ||
            badgeCelebrationVisible ||
            !sceneState.coachmarkTargetBounds.containsKey(NewPlayerOnboardingTarget.HomeMiniGames)
        ) {
            return null
        }
        return NewPlayerCoachmarkSpec(
            target = NewPlayerOnboardingTarget.HomeMiniGames,
            title = "Mini-jeux quotidiens",
            message = "La carte d'accueil a maintenant un verso Mini-jeux : retourne-la avec le bouton ou un swipe, puis touche la carte pour ouvrir le menu. Les quatre jeux peuvent réduire la recharge d'un pack jusqu'à 4 h par jour.",
            placement = NewPlayerCoachmarkPlacement.OverHomeCardText,
        )
    }

    private fun activeCraftingCoachmark(
        currentScene: AppScene,
        sceneState: AppSceneUiState,
        badgeCelebrationVisible: Boolean,
    ): NewPlayerCoachmarkSpec? {
        if (
            currentScene == AppScene.Home &&
            sceneState.homeContentVisible &&
            sceneState.onboardingHintsVisible &&
            !sceneState.transitionLocked &&
            !badgeCelebrationVisible &&
            sceneState.coachmarkTargetBounds.containsKey(NewPlayerOnboardingTarget.HomeCrafting)
        ) {
            return NewPlayerCoachmarkSpec(
                target = NewPlayerOnboardingTarget.HomeCrafting,
                title = "Atelier de fabrication",
                message = "Une carte peut être amélioré grâce à ses doublons. Ouvre l'atelier pour découvrir comment faire.",
            )
        }
        if (
            currentScene != AppScene.Crafting ||
            !sceneState.craftingContentVisible ||
            !sceneState.onboardingHintsVisible ||
            sceneState.transitionLocked
        ) {
            return null
        }
        return when {
            sceneState.coachmarkTargetBounds.containsKey(NewPlayerOnboardingTarget.CraftingConfirm) ->
                NewPlayerCoachmarkSpec(
                    target = NewPlayerOnboardingTarget.CraftingConfirm,
                    title = "Confirme l'amélioration",
                    message = "Valide pour consommer les doublons et créer la carte au ciel suivant.",
                )

            sceneState.coachmarkTargetBounds.containsKey(NewPlayerOnboardingTarget.CraftingCandidate) ->
                NewPlayerCoachmarkSpec(
                    target = NewPlayerOnboardingTarget.CraftingCandidate,
                    title = "Passons à l'amélioration",
                    message = "Cette carte est eligible, sélectionne-la pour voir ce qui sera consommé.",
                )

            sceneState.coachmarkTargetBounds.containsKey(NewPlayerOnboardingTarget.CraftingDarkenSkyMode) ->
                NewPlayerCoachmarkSpec(
                    target = NewPlayerOnboardingTarget.CraftingDarkenSkyMode,
                    title = "Assombrir le ciel",
                    message = "L'une des deux améliorations permet d'assombrir le ciel d'une carte.",
                    placement = NewPlayerCoachmarkPlacement.OverlapTargetBottom,
                    targetEffect = NewPlayerCoachmarkTargetEffect.TouchZone,
                )

            else -> null
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

    private suspend fun persistStep(
        nextStep: NewPlayerOnboardingStep,
        libraryCardHintVisible: Boolean = false,
    ) {
        val loadedProgress = progressRepository.loadProgress().requireUsableProgress().progress
        if (loadedProgress.newPlayerOnboardingStep == nextStep) {
            uiState = uiState.withStep(
                nextStep = nextStep,
                libraryCardHintVisible = libraryCardHintVisible,
            )
            return
        }

        progressRepository.updateProgress { currentProgress ->
            if (currentProgress.newPlayerOnboardingStep.ordinal >= nextStep.ordinal) {
                currentProgress
            } else {
                currentProgress.copy(newPlayerOnboardingStep = nextStep)
            }
        }
        uiState = uiState.withStep(
            nextStep = nextStep,
            libraryCardHintVisible = libraryCardHintVisible,
        )
    }

    private suspend fun persistMiniGamesDiscovery() {
        val nextStep = NewPlayerOnboardingStep.DiscoverMiniGames
        val loadedProgress = progressRepository.loadProgress().requireUsableProgress().progress
        if (loadedProgress.newPlayerOnboardingStep == nextStep) {
            uiState = uiState.withStep(nextStep = nextStep)
            return
        }

        progressRepository.updateProgress { currentProgress ->
            if (currentProgress.newPlayerOnboardingStep.ordinal >= nextStep.ordinal) {
                currentProgress
            } else {
                currentProgress.copy(
                    newPlayerOnboardingStep = nextStep,
                    miniGamesMenuUnlocked = true,
                    homeMenuNoveltyState = currentProgress.homeMenuNoveltyState.or(
                        HomeMenuNoveltyState(miniGames = true),
                    ),
                )
            }
        }
        uiState = uiState.withStep(nextStep = nextStep)
    }

    private suspend fun syncCraftingEligibility() {
        val progress = progressRepository.loadProgress().requireUsableProgress().progress
        uiState = uiState.withStep(resolveCraftingEligibilityStep(progress))
    }

    private suspend fun resolveCraftingEligibilityStep(
        progress: StandaloneProgress,
    ): NewPlayerOnboardingStep {
        if (
            progress.newPlayerOnboardingStep != NewPlayerOnboardingStep.AwaitCraftingEligibility ||
            progress.openedPackCount < CraftingOnboardingMinOpenedPackCount ||
            !hasDarkenSkyCandidate()
        ) {
            return progress.newPlayerOnboardingStep
        }
        progressRepository.updateProgress { currentProgress ->
            if (
                currentProgress.newPlayerOnboardingStep == NewPlayerOnboardingStep.AwaitCraftingEligibility &&
                currentProgress.openedPackCount >= CraftingOnboardingMinOpenedPackCount
            ) {
                currentProgress.copy(newPlayerOnboardingStep = NewPlayerOnboardingStep.ViewCraftingMenu)
            } else {
                currentProgress
            }
        }
        return NewPlayerOnboardingStep.ViewCraftingMenu
    }

    private suspend fun hasDarkenSkyCandidate(): Boolean =
        craftingRepository?.let { repository ->
            runCatching { repository.hasDarkenSkyCandidates() }.getOrDefault(false)
        } ?: false
}

private const val CraftingOnboardingMinOpenedPackCount = 3

private fun NewPlayerOnboardingUiState.withStep(
    nextStep: NewPlayerOnboardingStep?,
    libraryCardHintVisible: Boolean = false,
): NewPlayerOnboardingUiState = copy(
    currentStep = nextStep,
    libraryCardHintVisible = libraryCardHintVisible,
    blockingModalStep = when (nextStep) {
        NewPlayerOnboardingStep.ShowWelcomeIntro,
        NewPlayerOnboardingStep.LearnLibraryVariants,
        NewPlayerOnboardingStep.LearnCraftingTools,
        NewPlayerOnboardingStep.ShowConclusion,
        -> nextStep

        else -> null
    },
)
