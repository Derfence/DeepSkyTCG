package fr.aumombelli.gatcha.app

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import fr.aumombelli.gatcha.AppContainer
import fr.aumombelli.gatcha.ui.motion.SkyBackdropVariant
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
    private val readState: () -> AppSceneUiState,
    private val writeState: (AppSceneUiState) -> Unit,
    private val awaitNextFrame: suspend () -> Unit,
) {
    suspend fun finishPackOpeningToMenu() {
        val state = readState()
        if (state.transitionLocked) return

        writeState(state.lockTransitions().preparePackOpeningReturnToMenu())
        awaitNextFrame()
        appContainer.packRepository.clearCurrentPackResult()
        writeState(readState().showMenuContent())
        if (readState().pendingBadgeCelebration.isEmpty()) {
            writeState(readState().unlockTransitions())
        }
    }

    fun completeBadgeCelebration() {
        writeState(
            readState()
                .clearPendingBadgeCelebration()
                .unlockTransitions(),
        )
    }

    suspend fun runLaunchSequence() {
        writeState(readState().resetLaunchSequence())
        delay(120)
        writeState(readState().showLaunchLogo())
        delay(900)
        writeState(readState().raiseLaunchLogo())
        delay(720)
        writeState(readState().showStartCard())
    }

    suspend fun animateStartToMenu() {
        val state = readState()
        if (state.transitionLocked) return

        writeState(
            state.lockTransitions()
                .hideMenuContent()
                .hideLaunchLogo(),
        )
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
        writeState(
            readState()
                .enterMainMenu()
                .showMenuContent()
                .unlockTransitions(),
        )
    }

    suspend fun animateMenuToLibrary() {
        val state = readState()
        if (state.transitionLocked) return

        writeState(
            state.lockTransitions()
                .hideMenuContent()
                .hideLibraryContent(),
        )
        delay(520)
        writeState(readState().prepareLibraryEntry(nextLibraryViewModelKey = state.libraryViewModelKey + 1))
        bookProgress.snapTo(0f)
        bookOverlayAlpha.snapTo(1f)
        bookProgress.animateTo(1f, animationSpec = tween(durationMillis = 980, easing = FastOutSlowInEasing))
        writeState(readState().enterLibrary())
        awaitNextFrame()
        writeState(readState().showLibraryContent())
        bookOverlayAlpha.animateTo(0f, animationSpec = tween(durationMillis = 960, easing = FastOutSlowInEasing))
        bookProgress.snapTo(0f)
        bookOverlayAlpha.snapTo(1f)
        writeState(readState().unlockTransitions())
    }

    suspend fun animateMenuToPackSelection() {
        val state = readState()
        if (state.transitionLocked) return

        writeState(state.lockTransitions().hideMenuContent().hidePackSelectionScene())
        delay(520)
        appContainer.packRepository.clearCurrentPackResult()
        writeState(readState().preparePackSelection(nextPackFlowKey = state.packFlowKey + 1))
        awaitNextFrame()
        writeState(readState().showPackScene())
        delay(460)
        writeState(readState().showPackExtensionList())
        delay(760)
        writeState(readState().unlockTransitions())
    }

    suspend fun animateMenuToBadgeBook() {
        val state = readState()
        if (state.transitionLocked) return

        writeState(
            state.lockTransitions()
                .hideMenuContent()
                .hideBadgeBookContent(),
        )
        delay(520)
        writeState(readState().prepareBadgeBookEntry(nextBadgeBookViewModelKey = state.badgeBookViewModelKey + 1))
        chestProgress.snapTo(0f)
        chestOverlayAlpha.snapTo(1f)
        chestProgress.animateTo(1f, animationSpec = tween(durationMillis = 980, easing = FastOutSlowInEasing))
        writeState(readState().enterBadgeBook())
        awaitNextFrame()
        writeState(readState().showBadgeBookContent())
        chestOverlayAlpha.animateTo(0f, animationSpec = tween(durationMillis = 960, easing = FastOutSlowInEasing))
        chestProgress.snapTo(0f)
        chestOverlayAlpha.snapTo(1f)
        writeState(readState().unlockTransitions())
    }

    suspend fun animatePackSelectionToMenu() {
        val state = readState()
        if (state.transitionLocked) return

        writeState(
            state.lockTransitions()
                .hideMenuContent()
                .hidePackExtensionList(),
        )
        delay(760)
        writeState(readState().hidePackSelectionScene())
        delay(420)
        appContainer.packRepository.clearCurrentPackResult()
        writeState(readState().switchPackSelectionToMenu())
        awaitNextFrame()
        writeState(
            readState()
                .showMenuContent()
                .hidePackSelectionScene(),
        )
        delay(520)
        writeState(readState().unlockTransitions())
    }

    suspend fun animateLibraryToMenu() {
        val state = readState()
        if (state.transitionLocked) return

        writeState(
            state.lockTransitions()
                .hideMenuContent()
                .hideLibraryContent(),
        )
        bookProgress.snapTo(1f)
        bookOverlayAlpha.snapTo(0f)
        bookOverlayAlpha.animateTo(1f, animationSpec = tween(durationMillis = 960, easing = FastOutSlowInEasing))
        writeState(readState().enterMainMenu())
        bookProgress.animateTo(0f, animationSpec = tween(durationMillis = 980, easing = FastOutSlowInEasing))
        writeState(
            readState()
                .showMenuContent()
                .unlockTransitions(),
        )
    }

    suspend fun animateBadgeBookToMenu() {
        val state = readState()
        if (state.transitionLocked) return

        writeState(
            state.lockTransitions()
                .hideMenuContent()
                .hideBadgeBookContent(),
        )
        chestProgress.snapTo(1f)
        chestOverlayAlpha.snapTo(0f)
        chestOverlayAlpha.animateTo(1f, animationSpec = tween(durationMillis = 960, easing = FastOutSlowInEasing))
        writeState(readState().enterMainMenu())
        chestProgress.animateTo(0f, animationSpec = tween(durationMillis = 980, easing = FastOutSlowInEasing))
        writeState(
            readState()
                .showMenuContent()
                .unlockTransitions(),
        )
    }
}
