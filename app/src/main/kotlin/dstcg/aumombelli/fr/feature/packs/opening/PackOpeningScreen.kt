package fr.aumombelli.dstcg.feature.packs.opening

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import fr.aumombelli.dstcg.audio.LocalAudioController
import fr.aumombelli.dstcg.audio.SoundCue
import fr.aumombelli.dstcg.performance.LocalAppPerformanceProfile
import fr.aumombelli.dstcg.ui.component.SceneNavigationButton
import fr.aumombelli.dstcg.ui.component.SceneNavigationIcon
import fr.aumombelli.dstcg.ui.motion.PackRevealBounds
import fr.aumombelli.dstcg.ui.motion.RarityBurstOverlay
import fr.aumombelli.dstcg.ui.motion.HolographicArrivalCelebrationOverlay
import fr.aumombelli.dstcg.ui.motion.packOpeningHolographicMotion
import fr.aumombelli.dstcg.ui.motion.relativeTo
import fr.aumombelli.dstcg.ui.component.TRADING_CARD_WIDTH_OVER_HEIGHT
import fr.aumombelli.dstcg.ui.screen.dstcgContentInsetsPadding
import kotlin.math.abs
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

private const val PACK_OPENING_BURST_DURATION_MS = 4_800
private const val PACK_OPENING_REVEAL_DELAY_MS = 3_200
private const val PACK_OPENING_CARDS_ENTRANCE_DURATION_MS = 760
private const val PACK_OPENING_HOLOGRAPHIC_CUE_PREWARM_PROGRESS = 0.08f
private val PackOpeningSwipeHintNudgeDistance = 10.dp

@Composable
fun PackOpeningScreen(
    state: PackOpeningUiState,
    onDone: () -> Unit,
    dismissSignal: Int = 0,
    showPersistentDismissHint: Boolean = false,
    initialBoosterBounds: PackRevealBounds? = null,
    initialBoosterDecorSeed: Any? = Unit,
    modifier: Modifier = Modifier,
    onDismissRequest: (() -> Unit)? = null,
) {
    val packResult = state.packResult
    val displayCards = state.displayCards
    val performanceProfile = LocalAppPerformanceProfile.current
    val audioController = LocalAudioController.current
    val density = LocalDensity.current
    val swipeHintNudgeDistancePx = with(density) { PackOpeningSwipeHintNudgeDistance.toPx() }
    val revealItems = if (state.revealItems.isNotEmpty()) {
        state.revealItems
    } else {
        displayCards.map(::AstroPackRevealUiItem)
    }
    val boosterDecorSeed = remember(packResult?.drawnAt) { initialBoosterDecorSeed }
    var cardsVisible by remember(packResult?.drawnAt) { mutableStateOf(false) }
    var fullscreenPage by remember(packResult?.drawnAt) { mutableStateOf<Int?>(null) }
    var dismissStartOffset by remember(packResult?.drawnAt) { mutableFloatStateOf(0f) }
    var hasReachedLastCardOnce by remember(packResult?.drawnAt) { mutableStateOf(false) }
    var swipeHintUnlocked by remember(packResult?.drawnAt) { mutableStateOf(false) }
    var swipeHintLabelActivated by remember(packResult?.drawnAt) { mutableStateOf(false) }
    var swipeOffset by remember(packResult?.drawnAt) { mutableFloatStateOf(0f) }
    var dismissRequested by remember(packResult?.drawnAt) { mutableStateOf(false) }
    var handledDismissSignal by remember(packResult?.drawnAt) { mutableIntStateOf(dismissSignal) }
    var verticalDragActive by remember(packResult?.drawnAt) { mutableStateOf(false) }
    var rootHeightPx by remember(packResult?.drawnAt) { mutableFloatStateOf(0f) }
    var transitionBoosterBounds by remember(packResult?.drawnAt) { mutableStateOf<PackRevealBounds?>(null) }
    var currentRevealCardBounds by remember(packResult?.drawnAt) { mutableStateOf<PackRevealBounds?>(null) }
    var revealCelebrationLayerBounds by remember(packResult?.drawnAt) { mutableStateOf<PackRevealBounds?>(null) }
    var playedHolographicCuePages by remember(packResult?.drawnAt) { mutableStateOf(setOf<Int>()) }
    var lastObservedCuePage by remember(packResult?.drawnAt) { mutableIntStateOf(0) }
    var activeHolographicCuePage by remember(packResult?.drawnAt) { mutableStateOf<Int?>(null) }
    val scale = remember(packResult?.drawnAt) { Animatable(1f) }
    val burstProgress = remember(packResult?.drawnAt) { Animatable(0f) }
    val cardsEntranceProgress = remember(packResult?.drawnAt) { Animatable(0f) }
    val dismissProgress = remember(packResult?.drawnAt) { Animatable(0f) }
    val swipeHintOffset = remember(packResult?.drawnAt) { Animatable(0f) }
    val holographicCueProgress = remember(packResult?.drawnAt) { Animatable(0f) }

    LaunchedEffect(packResult?.drawnAt) {
        if (packResult == null) return@LaunchedEffect
        audioController.play(SoundCue.PackBurst)
        cardsVisible = false
        fullscreenPage = null
        dismissStartOffset = 0f
        hasReachedLastCardOnce = false
        swipeHintUnlocked = false
        swipeHintLabelActivated = false
        swipeOffset = 0f
        dismissRequested = false
        verticalDragActive = false
        transitionBoosterBounds = null
        currentRevealCardBounds = null
        revealCelebrationLayerBounds = null
        playedHolographicCuePages = emptySet()
        lastObservedCuePage = 0
        activeHolographicCuePage = null
        scale.snapTo(1f)
        burstProgress.snapTo(0f)
        cardsEntranceProgress.snapTo(0f)
        dismissProgress.snapTo(0f)
        swipeHintOffset.snapTo(0f)
        holographicCueProgress.snapTo(0f)
        launch {
            burstProgress.animateTo(
                1f,
                animationSpec = tween(
                    durationMillis = PACK_OPENING_BURST_DURATION_MS,
                    easing = FastOutSlowInEasing,
                ),
            )
        }
        delay(PACK_OPENING_REVEAL_DELAY_MS.toLong())
        audioController.play(SoundCue.PackReveal)
        cardsVisible = true
        cardsEntranceProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = PACK_OPENING_CARDS_ENTRANCE_DURATION_MS,
                easing = FastOutSlowInEasing,
            ),
        )
    }

    LaunchedEffect(dismissRequested) {
        if (!dismissRequested) return@LaunchedEffect
        verticalDragActive = false
        swipeOffset = 0f
        dismissProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 420, easing = FastOutSlowInEasing),
        )
        onDone()
    }

    LaunchedEffect(dismissSignal) {
        if (dismissSignal == handledDismissSignal || dismissRequested) return@LaunchedEffect
        handledDismissSignal = dismissSignal
        dismissStartOffset = swipeOffset
        dismissRequested = true
    }

    val swipeModifier = if (cardsVisible && revealItems.isNotEmpty() && state.errorMessage == null) {
        Modifier.pointerInput(packResult?.drawnAt) {
            detectVerticalDragGestures(
                onDragStart = {
                    verticalDragActive = true
                },
                onVerticalDrag = { change, dragAmount ->
                    change.consume()
                    if (!verticalDragActive) {
                        verticalDragActive = true
                    }
                    swipeOffset = (swipeOffset + dragAmount).coerceAtMost(0f)
                },
                onDragCancel = {
                    verticalDragActive = false
                    swipeOffset = 0f
                },
                onDragEnd = {
                    verticalDragActive = false
                    if (swipeOffset < -220f) {
                        dismissStartOffset = swipeOffset
                        dismissRequested = true
                    } else {
                        swipeOffset = 0f
                    }
                },
            )
        }
    } else {
        Modifier
    }
    val packExitProgress = if (cardsVisible) {
        1f
    } else {
        normalizedPhase(
            progress = burstProgress.value,
            start = 0.18f,
            end = 0.84f,
        )
    }
    val dismissBaseOffset = if (dismissRequested) dismissStartOffset else swipeOffset

    androidx.compose.foundation.layout.Box(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { rootHeightPx = it.height.toFloat() }
            .background(Brush.verticalGradient(listOf(Color(0xE0060B14), Color(0xFF162840))))
            .then(swipeModifier),
    ) {
        if (packResult == null) {
            EmptyPackState(onDone = onDone)
            return@Box
        }

        if (state.errorMessage != null && displayCards.isEmpty()) {
            PackOpeningErrorState(
                message = state.errorMessage,
                onDone = onDone,
            )
            return@Box
        }

        if (!cardsVisible) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .dstcgContentInsetsPadding(includeBottom = true)
                    .graphicsLayer {
                        alpha = 1f - dismissProgress.value
                        translationY = dismissBaseOffset + (-720f * dismissProgress.value)
                    },
            ) {
                val sceneLayout = calculatePackOpeningSceneLayout(
                    availableWidth = maxWidth,
                    availableHeight = maxHeight,
                )
                val progressBelowPager = shouldPlacePackOpeningProgressBelowPager(
                    availableWidth = maxWidth,
                    availableHeight = maxHeight,
                    sceneLayout = sceneLayout,
                )

                PackOpeningRevealStageScaffold(
                    extensionLabel = packOpeningExtensionLabel(displayCards, packResult),
                    currentPage = 0,
                    totalItems = revealItems.size.coerceAtLeast(1),
                    sceneLayout = sceneLayout,
                    progressBelowPager = progressBelowPager,
                    headerAlpha = 0f,
                ) {
                    BoosterCover(
                        extensionId = packResult.extensionId,
                        scale = scale.value,
                        exitProgress = packExitProgress,
                        decorSeed = boosterDecorSeed,
                        isEpicBoosted = packResult.isEpicBoosted,
                        onBoundsChanged = { bounds ->
                            transitionBoosterBounds = bounds
                        },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        } else {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .dstcgContentInsetsPadding(includeBottom = true)
                    .graphicsLayer {
                        alpha = 1f - dismissProgress.value
                        translationY = dismissBaseOffset + (-720f * dismissProgress.value)
                    },
            ) {
                val sceneLayout = calculatePackOpeningSceneLayout(
                    availableWidth = maxWidth,
                    availableHeight = maxHeight,
                )
                val progressBelowPager = shouldPlacePackOpeningProgressBelowPager(
                    availableWidth = maxWidth,
                    availableHeight = maxHeight,
                    sceneLayout = sceneLayout,
                )
                if (revealItems.isNotEmpty()) {
                    key(packResult.drawnAt) {
                        val pagerState = rememberPagerState(pageCount = { revealItems.size })
                        val currentPage = pagerState.currentPage
                        val settledPage = pagerState.settledPage
                        val currentItem = revealItems.getOrNull(settledPage)
                        val currentAstroItem = revealItems.getOrNull(currentPage) as? AstroPackRevealUiItem
                        val pendingHolographicCuePage = currentPage.takeIf {
                            shouldTriggerPackOpeningHolographicCue(
                                cardsVisible = cardsVisible,
                                currentPage = currentPage,
                                lastObservedPage = lastObservedCuePage,
                                isHolographic = currentAstroItem
                                    ?.displayCard
                                    ?.activeVariant
                                    ?.isHolographic == true,
                                alreadyPlayed = currentPage in playedHolographicCuePages,
                            )
                        }
                        val pagerIsFullySettled =
                            !pagerState.isScrollInProgress &&
                                currentPage == settledPage &&
                                abs(pagerState.currentPageOffsetFraction) < 0.001f
                        val shouldAnimateSwipeHint =
                            cardsVisible &&
                                swipeHintUnlocked &&
                                pagerIsFullySettled &&
                                fullscreenPage == null &&
                                !dismissRequested &&
                                !verticalDragActive
                        val showSwipeHintLabel =
                            cardsVisible &&
                                !dismissRequested &&
                                showPersistentDismissHint &&
                                swipeHintLabelActivated
                        val animatedHolographicCueProgress = if (activeHolographicCuePage != null) {
                            1f - holographicCueProgress.value
                        } else {
                            0f
                        }
                        val holographicCueOverlayProgress = if (animatedHolographicCueProgress > 0f) {
                            animatedHolographicCueProgress
                        } else if (pendingHolographicCuePage != null) {
                            PACK_OPENING_HOLOGRAPHIC_CUE_PREWARM_PROGRESS
                        } else {
                            0f
                        }

                        LaunchedEffect(packResult.drawnAt, settledPage) {
                            if (settledPage == revealItems.lastIndex && !hasReachedLastCardOnce) {
                                hasReachedLastCardOnce = true
                            }
                        }

                        LaunchedEffect(packResult.drawnAt, currentPage, cardsVisible) {
                            val shouldTriggerCue = shouldTriggerPackOpeningHolographicCue(
                                cardsVisible = cardsVisible,
                                currentPage = currentPage,
                                lastObservedPage = lastObservedCuePage,
                                isHolographic = currentAstroItem
                                    ?.displayCard
                                    ?.activeVariant
                                    ?.isHolographic == true,
                                alreadyPlayed = currentPage in playedHolographicCuePages,
                            )
                            if (currentPage != lastObservedCuePage) {
                                lastObservedCuePage = currentPage
                            }
                            if (!shouldTriggerCue) return@LaunchedEffect

                            playedHolographicCuePages = playedHolographicCuePages + currentPage
                            activeHolographicCuePage = currentPage
                            audioController.play(SoundCue.HolographicReveal)
                            holographicCueProgress.snapTo(1f - PACK_OPENING_HOLOGRAPHIC_CUE_PREWARM_PROGRESS)
                            holographicCueProgress.animateTo(
                                targetValue = 0f,
                                animationSpec = tween(durationMillis = 1_450, easing = FastOutSlowInEasing),
                            )
                            if (activeHolographicCuePage == currentPage) {
                                activeHolographicCuePage = null
                            }
                        }

                        LaunchedEffect(packResult.drawnAt, hasReachedLastCardOnce) {
                            if (!hasReachedLastCardOnce || swipeHintUnlocked) return@LaunchedEffect
                            delay(2_000)
                            swipeHintUnlocked = true
                        }

                        LaunchedEffect(
                            packResult.drawnAt,
                            shouldAnimateSwipeHint,
                            settledPage,
                            swipeHintNudgeDistancePx,
                        ) {
                            swipeHintOffset.snapTo(0f)
                            if (!shouldAnimateSwipeHint) return@LaunchedEffect
                            delay(450)

                            while (true) {
                                swipeHintOffset.animateTo(
                                    targetValue = -swipeHintNudgeDistancePx,
                                    animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing),
                                )
                                swipeHintOffset.animateTo(
                                    targetValue = 0f,
                                    animationSpec = tween(durationMillis = 360, easing = FastOutSlowInEasing),
                                )
                                delay(1_150)
                            }
                        }

                        LaunchedEffect(
                            packResult.drawnAt,
                            shouldAnimateSwipeHint,
                            showPersistentDismissHint,
                        ) {
                            if (!showPersistentDismissHint || !shouldAnimateSwipeHint) return@LaunchedEffect
                            delay(450)
                            swipeHintLabelActivated = true
                        }

                        LaunchedEffect(packResult.drawnAt) {
                            snapshotFlow {
                                PagerScrollSnapshot(
                                    isScrollInProgress = pagerState.isScrollInProgress,
                                    currentPage = pagerState.currentPage,
                                    offsetFraction = pagerState.currentPageOffsetFraction,
                                )
                            }.collect { snapshot ->
                                if (
                                    !snapshot.isScrollInProgress &&
                                    abs(snapshot.offsetFraction) > 0.001f
                                ) {
                                    pagerState.animateScrollToPage(snapshot.currentPage)
                                }
                            }
                        }

                        PackOpeningRevealStageScaffold(
                            extensionLabel = packOpeningExtensionLabel(displayCards, packResult),
                            currentPage = currentPage,
                            totalItems = revealItems.size,
                            sceneLayout = sceneLayout,
                            progressBelowPager = progressBelowPager,
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .onGloballyPositioned { coordinates ->
                                        val bounds = coordinates.boundsInRoot()
                                        revealCelebrationLayerBounds = PackRevealBounds(
                                            leftPx = bounds.left,
                                            topPx = bounds.top,
                                            widthPx = bounds.width,
                                            heightPx = bounds.height,
                                        )
                                    },
                            ) {
                                if (currentItem != null) {
                                    androidx.compose.material3.Text(
                                        text = currentItem.id,
                                        modifier = Modifier
                                            .size(0.dp)
                                            .testTag("pack-opening-current-card-id"),
                                    )
                                }

                                HorizontalPager(
                                    state = pagerState,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .testTag("pack-opening-pager")
                                        .graphicsLayer {
                                            alpha = cardsEntranceProgress.value
                                            translationY = (1f - cardsEntranceProgress.value) *
                                                if (rootHeightPx > 0f) rootHeightPx else 960f
                                        },
                                ) { page ->
                                    val relativePageOffset = (page - currentPage) + pagerState.currentPageOffsetFraction
                                    val settleCueProgress = if (page == activeHolographicCuePage) {
                                        holographicCueProgress.value
                                    } else {
                                        0f
                                    }
                                    val arrivalCueProgress = if (page == activeHolographicCuePage) {
                                        animatedHolographicCueProgress
                                    } else if (page == pendingHolographicCuePage) {
                                        PACK_OPENING_HOLOGRAPHIC_CUE_PREWARM_PROGRESS
                                    } else {
                                        0f
                                    }
                                    val holographicMotion = if (
                                        revealItems[page] is AstroPackRevealUiItem &&
                                        (revealItems[page] as AstroPackRevealUiItem)
                                            .displayCard.activeVariant.isHolographic
                                    ) {
                                        packOpeningHolographicMotion(
                                            relativePageOffset = relativePageOffset,
                                            settleCueProgress = settleCueProgress,
                                            interactiveEffectsEnabled =
                                                performanceProfile.enableInteractiveHolographicEffects,
                                        )
                                    } else {
                                        null
                                    }

                                    RevealCard(
                                        item = revealItems[page],
                                        isCurrentPage = page == currentPage,
                                        showPreviousArrow = page == currentPage && page > 0,
                                        showNextArrow = page == currentPage && page < revealItems.lastIndex,
                                        cardTranslationY = if (page == currentPage) swipeHintOffset.value else 0f,
                                        nudgeActive = page == settledPage && shouldAnimateSwipeHint,
                                        holographicArrivalProgress = arrivalCueProgress,
                                        holographicMotion = holographicMotion,
                                        onCardBoundsChanged = if (page == currentPage) {
                                            { bounds -> currentRevealCardBounds = bounds }
                                        } else {
                                            null
                                        },
                                        onOpenFullscreen = {
                                            if (revealItems[page] is AstroPackRevealUiItem) {
                                                fullscreenPage = page
                                            }
                                        },
                                    )
                                }

                                if (holographicCueOverlayProgress > 0f) {
                                    HolographicArrivalCelebrationOverlay(
                                        progress = holographicCueOverlayProgress,
                                        originBounds = currentRevealCardBounds
                                            ?.takeIf { revealCelebrationLayerBounds != null }
                                            ?.relativeTo(revealCelebrationLayerBounds!!),
                                        modifier = Modifier.fillMaxSize(),
                                    )
                                }

                                if (showSwipeHintLabel) {
                                    androidx.compose.material3.Text(
                                        text = "Glisse vers le haut pour revenir au menu.",
                                        color = Color(0xFFF8D98D),
                                        style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier
                                            .align(Alignment.TopCenter)
                                            .padding(top = sceneLayout.hintLabelTopPadding)
                                            .testTag("pack-opening-swipe-hint-label"),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        RarityBurstOverlay(
            rarityLabel = state.highestBurstRarity,
            hasHolographicBurst = state.hasHolographicBurst,
            progress = burstProgress.value,
            originBounds = null,
        )

        if (cardsVisible && revealItems.isNotEmpty()) {
            onDismissRequest?.let { dismiss ->
                SceneNavigationButton(
                    icon = SceneNavigationIcon.Close,
                    onClick = dismiss,
                    contentDescription = "Fermer",
                    testTag = "pack-opening-close",
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .dstcgContentInsetsPadding()
                        .padding(top = 8.dp, end = 8.dp),
                )
            }
        }

        val fullscreenCard = fullscreenPage
            ?.let(revealItems::getOrNull)
            ?.let { item -> (item as? AstroPackRevealUiItem)?.displayCard }
        if (fullscreenCard != null) {
            PackOpeningFullscreenDialog(
                displayCard = fullscreenCard,
                onDismiss = { fullscreenPage = null },
            )
        }
    }
}
