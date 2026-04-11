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
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import fr.aumombelli.dstcg.ui.motion.PackRevealBounds
import fr.aumombelli.dstcg.ui.motion.RarityBurstOverlay
import fr.aumombelli.dstcg.ui.component.TRADING_CARD_WIDTH_OVER_HEIGHT
import fr.aumombelli.dstcg.ui.screen.dstcgContentInsetsPadding
import kotlin.math.abs
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect

@Composable
fun PackOpeningScreen(
    state: PackOpeningUiState,
    onDone: () -> Unit,
    dismissSignal: Int = 0,
    showPersistentDismissHint: Boolean = false,
    initialBoosterBounds: PackRevealBounds? = null,
    modifier: Modifier = Modifier,
) {
    val packResult = state.packResult
    val displayCards = state.displayCards
    val revealItems = if (state.revealItems.isNotEmpty()) {
        state.revealItems
    } else {
        displayCards.map(::AstroPackRevealUiItem)
    }
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
    val scale = remember(packResult?.drawnAt) { Animatable(1f) }
    val burstProgress = remember(packResult?.drawnAt) { Animatable(0f) }
    val cardsEntranceProgress = remember(packResult?.drawnAt) { Animatable(0f) }
    val dismissProgress = remember(packResult?.drawnAt) { Animatable(0f) }
    val swipeHintOffset = remember(packResult?.drawnAt) { Animatable(0f) }

    LaunchedEffect(packResult?.drawnAt) {
        if (packResult == null) return@LaunchedEffect
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
        scale.snapTo(1f)
        burstProgress.snapTo(0f)
        cardsEntranceProgress.snapTo(0f)
        dismissProgress.snapTo(0f)
        swipeHintOffset.snapTo(0f)
        burstProgress.animateTo(1f, animationSpec = tween(durationMillis = 4800, easing = FastOutSlowInEasing))
        cardsVisible = true
        cardsEntranceProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 760, easing = FastOutSlowInEasing),
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

    val swipeModifier = if (revealItems.isNotEmpty() && state.errorMessage == null) {
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
	                        val currentItem = revealItems.getOrNull(currentPage)
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

	                        LaunchedEffect(packResult.drawnAt, settledPage) {
	                            if (settledPage == revealItems.lastIndex && !hasReachedLastCardOnce) {
	                                hasReachedLastCardOnce = true
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
	                        ) {
	                            swipeHintOffset.snapTo(0f)
	                            if (!shouldAnimateSwipeHint) return@LaunchedEffect
	                            delay(450)

	                            while (true) {
	                                swipeHintOffset.animateTo(
	                                    targetValue = -26f,
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
	                                RevealCard(
	                                    item = revealItems[page],
	                                    isCurrentPage = page == currentPage,
	                                    showPreviousArrow = page == currentPage && page > 0,
	                                    showNextArrow = page == currentPage && page < revealItems.lastIndex,
	                                    cardTranslationY = if (page == currentPage) swipeHintOffset.value else 0f,
	                                    nudgeActive = page == settledPage && shouldAnimateSwipeHint,
	                                    onOpenFullscreen = {
	                                        if (revealItems[page] is AstroPackRevealUiItem) {
	                                            fullscreenPage = page
	                                        }
	                                    },
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

        RarityBurstOverlay(
            rarityLabel = state.highestBurstRarity,
            hasHolographicBurst = state.hasHolographicBurst,
            progress = burstProgress.value,
            originBounds = transitionBoosterBounds ?: initialBoosterBounds,
        )

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

@Composable
private fun PackOpeningProgressIndicator(
    currentPage: Int,
    total: Int,
    alpha: Float = 1f,
) {
    androidx.compose.material3.Text(
        text = "${currentPage + 1} / $total",
        color = Color.White.copy(alpha = 0.9f),
        style = androidx.compose.material3.MaterialTheme.typography.labelLarge,
        modifier = Modifier
            .graphicsLayer {
                this.alpha = alpha
            }
            .testTag("pack-opening-progress"),
    )
}

@Composable
internal fun PackOpeningRevealSlotProbe(
    extensionLabel: String,
    totalItems: Int,
    onBoundsChanged: (PackRevealBounds?) -> Unit,
    onCoordinatesChanged: (LayoutCoordinates?) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(
        modifier = modifier
            .dstcgContentInsetsPadding(includeBottom = true)
            .clearAndSetSemantics {},
    ) {
        val availableWidth = maxWidth
        val availableHeight = maxHeight

        androidx.compose.foundation.layout.Box(
            modifier = Modifier.fillMaxSize(),
        ) {
        val sceneLayout = calculatePackOpeningSceneLayout(
            availableWidth = availableWidth,
            availableHeight = availableHeight,
        )
        val progressBelowPager = shouldPlacePackOpeningProgressBelowPager(
            availableWidth = availableWidth,
            availableHeight = availableHeight,
            sceneLayout = sceneLayout,
        )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(sceneLayout.sectionSpacing),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        horizontal = sceneLayout.horizontalContentPadding,
                        vertical = sceneLayout.verticalContentPadding,
                    ),
            ) {
                androidx.compose.material3.Text(
                    text = "Ouverture du pack",
                    style = if (sceneLayout.compactHeader) {
                        androidx.compose.material3.MaterialTheme.typography.titleLarge
                    } else {
                        androidx.compose.material3.MaterialTheme.typography.headlineMedium
                    },
                    fontWeight = FontWeight.Bold,
                    color = Color.Transparent,
                )
                androidx.compose.material3.Text(
                    text = "Extension : $extensionLabel",
                    color = Color.Transparent,
                    style = if (sceneLayout.compactHeader) {
                        androidx.compose.material3.MaterialTheme.typography.bodySmall
                    } else {
                        androidx.compose.material3.MaterialTheme.typography.bodyMedium
                    },
                )

                if (!progressBelowPager) {
                    ProbePackOpeningProgressIndicator(total = totalItems.coerceAtLeast(1))
                }

                Box(
                    modifier = Modifier.weight(1f),
                ) {
                    PackOpeningRevealCardFrame(
                        modifier = Modifier.fillMaxSize(),
                        onCardBoundsChanged = onBoundsChanged,
                        onCardCoordinatesChanged = onCoordinatesChanged,
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(TRADING_CARD_WIDTH_OVER_HEIGHT),
                        )
                    }
                }

                if (progressBelowPager) {
                    ProbePackOpeningProgressIndicator(total = totalItems.coerceAtLeast(1))
                }
            }
        }
    }
}

@Composable
private fun ProbePackOpeningProgressIndicator(total: Int) {
    androidx.compose.material3.Text(
        text = "1 / $total",
        color = Color.Transparent,
        style = androidx.compose.material3.MaterialTheme.typography.labelLarge,
    )
}

@Composable
private fun PackOpeningRevealStageScaffold(
    extensionLabel: String,
    currentPage: Int,
    totalItems: Int,
    sceneLayout: PackOpeningSceneLayout,
    progressBelowPager: Boolean,
    headerAlpha: Float = 1f,
    modifier: Modifier = Modifier,
    stageContent: @Composable BoxScope.() -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(sceneLayout.sectionSpacing),
        modifier = modifier
            .fillMaxSize()
            .padding(
                horizontal = sceneLayout.horizontalContentPadding,
                vertical = sceneLayout.verticalContentPadding,
            ),
    ) {
        androidx.compose.material3.Text(
            text = "Ouverture du pack",
            style = if (sceneLayout.compactHeader) {
                androidx.compose.material3.MaterialTheme.typography.titleLarge
            } else {
                androidx.compose.material3.MaterialTheme.typography.headlineMedium
            },
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier
                .graphicsLayer {
                    alpha = headerAlpha
                }
                .testTag("pack-opening-title"),
        )
        androidx.compose.material3.Text(
            text = "Extension : $extensionLabel",
            color = Color(0xFFD8E6F8),
            style = if (sceneLayout.compactHeader) {
                androidx.compose.material3.MaterialTheme.typography.bodySmall
            } else {
                androidx.compose.material3.MaterialTheme.typography.bodyMedium
            },
            modifier = Modifier.graphicsLayer {
                alpha = headerAlpha
            },
        )

        if (!progressBelowPager) {
            PackOpeningProgressIndicator(
                currentPage = currentPage,
                total = totalItems,
                alpha = headerAlpha,
            )
        }

        Box(
            modifier = Modifier.weight(1f),
            content = stageContent,
        )

        if (progressBelowPager) {
            PackOpeningProgressIndicator(
                currentPage = currentPage,
                total = totalItems,
                alpha = headerAlpha,
            )
        }
    }
}

private fun shouldPlacePackOpeningProgressBelowPager(
    availableWidth: Dp,
    availableHeight: Dp,
    sceneLayout: PackOpeningSceneLayout,
): Boolean {
    val estimatedCardWidth = (
        availableWidth -
            (sceneLayout.horizontalContentPadding * 2f) -
            PackOpeningRevealHorizontalChrome
        ).coerceAtLeast(0.dp)
    val estimatedCardHeight = estimatedCardWidth / TRADING_CARD_WIDTH_OVER_HEIGHT
    val estimatedPagerBudget = (
        availableHeight -
            (sceneLayout.verticalContentPadding * 2f) -
            PackOpeningRevealHeaderEstimate -
            (sceneLayout.sectionSpacing * 2f)
        ).coerceAtLeast(0.dp)
    return estimatedCardHeight > estimatedPagerBudget - PackOpeningProgressReserve
}

private val PackOpeningRevealHorizontalChrome = 72.dp
private val PackOpeningRevealHeaderEstimate = 96.dp
private val PackOpeningProgressReserve = 32.dp

private data class PagerScrollSnapshot(
    val isScrollInProgress: Boolean,
    val currentPage: Int,
    val offsetFraction: Float,
)
