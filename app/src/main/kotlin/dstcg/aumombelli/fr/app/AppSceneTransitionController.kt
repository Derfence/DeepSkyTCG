package fr.aumombelli.dstcg.app

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import fr.aumombelli.dstcg.AppContainer
import fr.aumombelli.dstcg.audio.SoundCue
import fr.aumombelli.dstcg.ui.motion.SkyBackdropVariant
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal class AppSceneTransitionController(
    private val appContainer: AppContainer,
    private val skyVariant: SkyBackdropVariant,
    private val cameraTilt: Animatable<Float, AnimationVector1D>,
    private val mountainSkyBlend: Animatable<Float, AnimationVector1D>,
    private val horizonLights: Animatable<Float, AnimationVector1D>,
    private val bookProgress: Animatable<Float, AnimationVector1D>,
    private val bookOverlayAlpha: Animatable<Float, AnimationVector1D>,
    private val chestProgress: Animatable<Float, AnimationVector1D>,
    private val chestOverlayAlpha: Animatable<Float, AnimationVector1D>,
    private val equipmentProgress: Animatable<Float, AnimationVector1D>,
    private val equipmentOverlayAlpha: Animatable<Float, AnimationVector1D>,
    private val readState: () -> AppSceneUiState,
    private val writeState: (AppSceneUiState) -> Unit,
    private val awaitNextFrame: suspend () -> Unit,
) {
    suspend fun finishPackOpeningToHome() {
        val state = readState()
        if (state.transitionLocked) return
        playNavigationCue()

        writeState(state.lockTransitions().preparePackOpeningReturnToHome())
        awaitNextFrame()
        appContainer.packRepository.clearCurrentPackResult()
        animateBackdropToHome()
        val nextState = readState().showHomeContent()
        writeState(
            if (nextState.pendingBadgeCelebration.isNotEmpty() && !nextState.badgeCelebrationDeferred) {
                nextState
            } else {
                nextState.unlockTransitions()
            },
        )
        if (nextState.pendingBadgeCelebration.isEmpty() || nextState.badgeCelebrationDeferred) {
            revealOnboardingHintsAfterTransition()
        }
    }

    suspend fun finishPackOpeningToPackSelection() {
        val state = readState()
        if (state.transitionLocked) return
        playNavigationCue()

        writeState(state.lockTransitions().preparePackOpeningReturnToPackSelection())
        awaitNextFrame()
        appContainer.packRepository.clearCurrentPackResult()
        writeState(
            readState()
                .showPackScene()
                .showPackExtensionList(),
        )
        unlockTransitionsAndRevealOnboardingHints()
    }

    fun completeBadgeCelebration() {
        writeState(
            readState()
                .clearPendingBadgeCelebration()
                .unlockTransitions()
                .showOnboardingHints(),
        )
    }

    suspend fun runLaunchSequence() {
        writeState(readState().resetLaunchSequence())
        delay(120)
        writeState(readState().showLaunchLogo())
        delay(900)
        writeState(readState().raiseLaunchLogo())
        delay(720)
        writeState(readState().showHomeContent())
        delay(260)
        writeState(readState().hideLaunchLogo())
    }

    suspend fun animateHomeToPackSelection() {
        val state = readState()
        if (state.transitionLocked) return
        playNavigationCue(SoundCue.PackSelectionOpen)

        writeState(
            state.lockTransitions()
                .hideHomeContent(),
        )
        delay(560)
        writeState(readState().hideLaunchLogo().hidePackSelectionScene())
        animateBackdropToPackSelection()
        appContainer.packRepository.clearCurrentPackResult()
        writeState(readState().preparePackSelection(nextPackRefreshSignal = state.packRefreshSignal + 1))
        awaitNextFrame()
        writeState(readState().showPackScene())
        delay(460)
        writeState(readState().showPackExtensionList())
        delay(760)
        unlockTransitionsAndRevealOnboardingHints()
    }

    suspend fun animateHomeToLibrary() {
        val state = readState()
        if (state.transitionLocked) return

        coroutineScope {
            writeState(
                state.lockTransitions()
                    .hideHomeContent()
                    .hideLaunchLogo()
                    .hideLibraryContent(),
            )
        }
        delay(520)
        writeState(readState().prepareLibraryEntry(nextLibraryRefreshSignal = state.libraryRefreshSignal + 1))
        bookProgress.snapTo(0f)
        bookOverlayAlpha.snapTo(1f)
        awaitNextFrame()
        playNavigationCue(SoundCue.LibraryOpen)
        bookProgress.animateTo(1f, animationSpec = tween(durationMillis = 980, easing = FastOutSlowInEasing))
        writeState(readState().enterLibrary())
        awaitNextFrame()
        writeState(readState().showLibraryContent())
        bookOverlayAlpha.animateTo(0f, animationSpec = tween(durationMillis = 960, easing = FastOutSlowInEasing))
        bookProgress.snapTo(0f)
        bookOverlayAlpha.snapTo(1f)
        unlockTransitionsAndRevealOnboardingHints()
    }

    suspend fun animateHomeToCrafting() {
        val state = readState()
        if (state.transitionLocked) return
        playNavigationCue(SoundCue.CraftingOpen)

        writeState(
            state.lockTransitions()
                .hideLaunchLogo()
                .hideHomeContent()
                .hideCraftingContent()
                .prepareCraftingEntry(nextCraftingRefreshSignal = state.craftingRefreshSignal + 1),
        )
        delay(520)
        writeState(readState().enterCrafting())
        awaitNextFrame()
        writeState(readState().showCraftingContent())
        delay(420)
        unlockTransitionsAndRevealOnboardingHints()
    }

    suspend fun animateHomeToEquipment() {
        val state = readState()
        if (state.transitionLocked) return
        playNavigationCue(SoundCue.EquipmentOpen)

        writeState(
            state.lockTransitions()
                .hideLaunchLogo()
                .hideHomeContent()
                .hideEquipmentContent()
                .prepareEquipmentEntry(nextEquipmentRefreshSignal = state.equipmentRefreshSignal + 1),
        )
        equipmentProgress.snapTo(0f)
        equipmentOverlayAlpha.snapTo(1f)
        equipmentProgress.animateTo(
            1f,
            animationSpec = tween(
                durationMillis = EquipmentPortalTravelDurationMillis,
                easing = FastOutSlowInEasing,
            ),
        )
        awaitNextFrame()
        writeState(readState().enterEquipment())
        awaitNextFrame()
        writeState(readState().showEquipmentContent())
        equipmentOverlayAlpha.animateTo(
            0f,
            animationSpec = tween(
                durationMillis = EquipmentPortalFadeDurationMillis,
                easing = FastOutSlowInEasing,
            ),
        )
        equipmentProgress.snapTo(0f)
        equipmentOverlayAlpha.snapTo(1f)
        unlockTransitionsAndRevealOnboardingHints()
    }

    suspend fun animateHomeToBadgeBook() {
        val state = readState()
        if (state.transitionLocked) return

        writeState(
            state.lockTransitions()
                .hideHomeContent()
                .hideLaunchLogo()
                .hideBadgeBookContent(),
        )
        delay(520)
        writeState(readState().prepareBadgeBookEntry(nextBadgeBookRefreshSignal = state.badgeBookRefreshSignal + 1))
        chestProgress.snapTo(0f)
        chestOverlayAlpha.snapTo(1f)
        awaitNextFrame()
        playNavigationCue(SoundCue.BadgeBookOpen)
        chestProgress.animateTo(1f, animationSpec = tween(durationMillis = 980, easing = FastOutSlowInEasing))
        writeState(readState().enterBadgeBook())
        awaitNextFrame()
        writeState(readState().showBadgeBookContent())
        chestOverlayAlpha.animateTo(0f, animationSpec = tween(durationMillis = 960, easing = FastOutSlowInEasing))
        chestProgress.snapTo(0f)
        chestOverlayAlpha.snapTo(1f)
        unlockTransitionsAndRevealOnboardingHints()
    }

    suspend fun animateHomeToMiniGamesMenu() {
        val state = readState()
        if (state.transitionLocked) return
        playNavigationCue(SoundCue.MiniGamesOpen)

        writeState(
            state.lockTransitions()
                .hideLaunchLogo()
                .hideHomeContent()
                .hideMiniGamesMenuContent()
                .prepareMiniGamesMenuEntry(),
        )
        delay(520)
        writeState(readState().enterMiniGamesMenu())
        awaitNextFrame()
        writeState(readState().showMiniGamesMenuContent())
        delay(420)
        unlockTransitionsAndRevealOnboardingHints()
    }

    suspend fun animatePackSelectionToHome() {
        val state = readState()
        if (state.transitionLocked) return
        playNavigationCue(SoundCue.PackSelectionClose)

        writeState(
            state.lockTransitions()
                .hidePackExtensionList(),
        )
        delay(760)
        writeState(readState().hidePackSelectionScene())
        delay(420)
        appContainer.packRepository.clearCurrentPackResult()
        writeState(readState().switchPackSelectionToHome())
        awaitNextFrame()
        animateBackdropToHome()
        val nextState = readState().showHomeContent().hidePackSelectionScene()
        writeState(nextState)
        unlockTransitionsAndRevealOnboardingHints()
    }

    suspend fun animateLibraryToHome() {
        val state = readState()
        if (state.transitionLocked) return

        writeState(
            state.lockTransitions()
                .hideLibraryContent(),
        )
        bookProgress.snapTo(1f)
        bookOverlayAlpha.snapTo(0f)
        awaitNextFrame()
        bookOverlayAlpha.animateTo(1f, animationSpec = tween(durationMillis = 960, easing = FastOutSlowInEasing))
        writeState(readState().enterHome())
        playNavigationCue(SoundCue.LibraryClose)
        bookProgress.animateTo(0f, animationSpec = tween(durationMillis = 980, easing = FastOutSlowInEasing))
        val nextState = readState().showHomeContent()
        writeState(
            if (nextState.pendingBadgeCelebration.isNotEmpty() && !nextState.badgeCelebrationDeferred) {
                nextState
            } else {
                nextState.unlockTransitions()
            },
        )
        if (nextState.pendingBadgeCelebration.isEmpty() || nextState.badgeCelebrationDeferred) {
            revealOnboardingHintsAfterTransition()
        }
    }

    suspend fun animateCraftingToHome() {
        val state = readState()
        if (state.transitionLocked) return
        playNavigationCue(SoundCue.CraftingClose)

        writeState(
            state.lockTransitions()
                .hideCraftingContent(),
        )
        delay(420)
        writeState(readState().enterHome())
        awaitNextFrame()
        val nextState = readState().showHomeContent()
        writeState(
            if (nextState.pendingBadgeCelebration.isNotEmpty() && !nextState.badgeCelebrationDeferred) {
                nextState
            } else {
                nextState.unlockTransitions()
            },
        )
        if (nextState.pendingBadgeCelebration.isEmpty() || nextState.badgeCelebrationDeferred) {
            revealOnboardingHintsAfterTransition()
        }
    }

    suspend fun animateBadgeBookToHome() {
        val state = readState()
        if (state.transitionLocked) return

        writeState(
            state.lockTransitions()
                .hideBadgeBookContent(),
        )
        chestProgress.snapTo(1f)
        chestOverlayAlpha.snapTo(0f)
        awaitNextFrame()
        chestOverlayAlpha.animateTo(1f, animationSpec = tween(durationMillis = 960, easing = FastOutSlowInEasing))
        writeState(readState().enterHome())
        playNavigationCue(SoundCue.BadgeBookClose)
        chestProgress.animateTo(0f, animationSpec = tween(durationMillis = 980, easing = FastOutSlowInEasing))
        val nextState = readState().showHomeContent()
        writeState(
            if (nextState.pendingBadgeCelebration.isNotEmpty() && !nextState.badgeCelebrationDeferred) {
                nextState
            } else {
                nextState.unlockTransitions()
            },
        )
        if (nextState.pendingBadgeCelebration.isEmpty() || nextState.badgeCelebrationDeferred) {
            revealOnboardingHintsAfterTransition()
        }
    }

    suspend fun animateEquipmentToHome() {
        val state = readState()
        if (state.transitionLocked) return
        playNavigationCue(SoundCue.EquipmentClose)

        writeState(
            state.lockTransitions()
                .hideEquipmentContent(),
        )
        equipmentProgress.snapTo(1f)
        equipmentOverlayAlpha.snapTo(0f)
        writeState(readState().enterHome())
        coroutineScope {
            launch {
                equipmentOverlayAlpha.animateTo(
                    1f,
                    animationSpec = tween(
                        durationMillis = EquipmentPortalFadeDurationMillis,
                        easing = FastOutSlowInEasing,
                    ),
                )
            }
            launch {
                equipmentProgress.animateTo(
                    0f,
                    animationSpec = tween(
                        durationMillis = EquipmentPortalTravelDurationMillis,
                        easing = FastOutSlowInEasing,
                    ),
                )
            }
        }
        equipmentProgress.snapTo(0f)
        equipmentOverlayAlpha.snapTo(1f)
        writeState(readState().enterHome())
        val nextState = readState().showHomeContent()
        writeState(
            if (nextState.pendingBadgeCelebration.isNotEmpty() && !nextState.badgeCelebrationDeferred) {
                nextState
            } else {
                nextState.unlockTransitions()
            },
        )
        if (nextState.pendingBadgeCelebration.isEmpty() || nextState.badgeCelebrationDeferred) {
            revealOnboardingHintsAfterTransition()
        }
    }

    suspend fun animateMiniGamesMenuToHome() {
        val state = readState()
        if (state.transitionLocked) return
        playNavigationCue(SoundCue.MiniGamesClose)

        writeState(
            state.lockTransitions()
                .hideMiniGamesMenuContent(),
        )
        delay(420)
        writeState(readState().enterHome())
        awaitNextFrame()
        val nextState = readState().showHomeContent()
        writeState(
            if (nextState.pendingBadgeCelebration.isNotEmpty() && !nextState.badgeCelebrationDeferred) {
                nextState
            } else {
                nextState.unlockTransitions()
            },
        )
        if (nextState.pendingBadgeCelebration.isEmpty() || nextState.badgeCelebrationDeferred) {
            revealOnboardingHintsAfterTransition()
        }
    }

    private suspend fun animateBackdropToPackSelection() {
        coroutineScope {
            launch {
                cameraTilt.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 760, easing = FastOutSlowInEasing),
                )
            }
            launch {
                mountainSkyBlend.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 760, easing = FastOutSlowInEasing),
                )
            }
            if (skyVariant.hasHorizonLights) {
                launch {
                    horizonLights.animateTo(
                        targetValue = 0f,
                        animationSpec = tween(durationMillis = 760, easing = FastOutSlowInEasing),
                    )
                }
            }
        }
    }

    private suspend fun animateBackdropToHome() {
        coroutineScope {
            launch {
                cameraTilt.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(durationMillis = 760, easing = FastOutSlowInEasing),
                )
            }
            launch {
                mountainSkyBlend.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(durationMillis = 760, easing = FastOutSlowInEasing),
                )
            }
            launch {
                horizonLights.animateTo(
                    targetValue = if (skyVariant.hasHorizonLights) 1f else 0f,
                    animationSpec = tween(durationMillis = 760, easing = FastOutSlowInEasing),
                )
            }
        }
    }

    private suspend fun unlockTransitionsAndRevealOnboardingHints() {
        writeState(readState().unlockTransitions())
        revealOnboardingHintsAfterTransition()
    }

    private fun playNavigationCue(cue: SoundCue = SoundCue.UiNavigate) {
        appContainer.audioController.play(cue)
    }

    private suspend fun revealOnboardingHintsAfterTransition(
        delayMillis: Long = OnboardingHintRevealDelayMillis,
    ) {
        delay(delayMillis)
        if (!readState().transitionLocked) {
            writeState(readState().showOnboardingHints())
        }
    }

    private companion object {
        const val OnboardingHintRevealDelayMillis: Long = 220L
        const val EquipmentPortalTravelDurationMillis: Int = 1440
        const val EquipmentPortalFadeDurationMillis: Int = 320
    }
}
