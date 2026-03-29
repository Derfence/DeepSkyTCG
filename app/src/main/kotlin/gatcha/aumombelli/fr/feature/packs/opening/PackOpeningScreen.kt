package fr.aumombelli.gatcha.feature.packs.opening

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import fr.aumombelli.gatcha.ui.motion.PackRevealBounds
import fr.aumombelli.gatcha.ui.motion.RarityBurstOverlay
import fr.aumombelli.gatcha.ui.screen.gatchaContentInsetsPadding
import kotlinx.coroutines.delay

@Composable
fun PackOpeningScreen(
    state: PackOpeningUiState,
    onDone: () -> Unit,
    initialBoosterBounds: PackRevealBounds? = null,
    modifier: Modifier = Modifier,
) {
    val packResult = state.packResult
    val displayCards = state.displayCards
    var cardsVisible by remember(packResult?.drawnAt) { mutableStateOf(false) }
    var fullscreenPage by remember(packResult?.drawnAt) { mutableStateOf<Int?>(null) }
    var swipeOffset by remember(packResult?.drawnAt) { mutableFloatStateOf(0f) }
    var dismissRequested by remember(packResult?.drawnAt) { mutableStateOf(false) }
    var verticalDragActive by remember(packResult?.drawnAt) { mutableStateOf(false) }
    var lastCardNudgeActive by remember(packResult?.drawnAt) { mutableStateOf(false) }
    var rootHeightPx by remember(packResult?.drawnAt) { mutableFloatStateOf(0f) }
    val scale = remember(packResult?.drawnAt) { Animatable(1f) }
    val burstProgress = remember(packResult?.drawnAt) { Animatable(0f) }
    val cardsEntranceProgress = remember(packResult?.drawnAt) { Animatable(0f) }
    val dismissProgress = remember(packResult?.drawnAt) { Animatable(0f) }
    val lastCardNudgeOffset = remember(packResult?.drawnAt) { Animatable(0f) }

    LaunchedEffect(packResult?.drawnAt) {
        if (packResult == null) return@LaunchedEffect
        cardsVisible = false
        fullscreenPage = null
        swipeOffset = 0f
        dismissRequested = false
        verticalDragActive = false
        lastCardNudgeActive = false
        scale.snapTo(1f)
        burstProgress.snapTo(0f)
        cardsEntranceProgress.snapTo(0f)
        dismissProgress.snapTo(0f)
        lastCardNudgeOffset.snapTo(0f)
        burstProgress.animateTo(1f, animationSpec = tween(durationMillis = 4800, easing = FastOutSlowInEasing))
        cardsVisible = true
        cardsEntranceProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 760, easing = FastOutSlowInEasing),
        )
    }

    LaunchedEffect(dismissRequested) {
        if (!dismissRequested) return@LaunchedEffect
        dismissProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 420, easing = FastOutSlowInEasing),
        )
        onDone()
    }

    val swipeModifier = if (displayCards.isNotEmpty() && state.errorMessage == null) {
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
                        swipeOffset = 0f
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
            BoosterCover(
                extensionId = packResult.extensionId,
                scale = scale.value,
                exitProgress = packExitProgress,
                initialBoosterBounds = initialBoosterBounds,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        alpha = 1f - dismissProgress.value
                        translationY = swipeOffset + (-720f * dismissProgress.value)
                    },
            )
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(18.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .gatchaContentInsetsPadding(includeBottom = true)
                    .graphicsLayer {
                        alpha = 1f - dismissProgress.value
                        translationY = swipeOffset + (-720f * dismissProgress.value)
                    }
                    .padding(20.dp),
            ) {
                androidx.compose.material3.Text(
                    text = "Pack Opening",
                    style = androidx.compose.material3.MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.testTag("pack-opening-title"),
                )
                androidx.compose.material3.Text(
                    text = "Extension: ${packOpeningExtensionLabel(displayCards, packResult)}",
                    color = Color(0xFFD8E6F8),
                )

                if (displayCards.isNotEmpty()) {
                    val pagerState = rememberPagerState(pageCount = { displayCards.size })
                    val currentPage = pagerState.currentPage
                    val settledPage = pagerState.settledPage
                    val currentCard = displayCards.getOrNull(currentPage)

                    LaunchedEffect(
                        packResult.drawnAt,
                        cardsVisible,
                        settledPage,
                        fullscreenPage,
                        dismissRequested,
                        verticalDragActive,
                    ) {
                        lastCardNudgeActive = false
                        lastCardNudgeOffset.snapTo(0f)
                        val shouldAnimateLastCard =
                            cardsVisible &&
                                settledPage == displayCards.lastIndex &&
                                fullscreenPage == null &&
                                !dismissRequested &&
                                !verticalDragActive
                        if (!shouldAnimateLastCard) return@LaunchedEffect

                        delay(2_000)
                        lastCardNudgeActive = true
                        while (true) {
                            lastCardNudgeOffset.animateTo(
                                targetValue = -26f,
                                animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing),
                            )
                            lastCardNudgeOffset.animateTo(
                                targetValue = 0f,
                                animationSpec = tween(durationMillis = 360, easing = FastOutSlowInEasing),
                            )
                            delay(1_150)
                        }
                    }

                    if (currentCard != null) {
                        androidx.compose.material3.Text(
                            text = currentCard.definition.id,
                            modifier = Modifier
                                .size(0.dp)
                                .testTag("pack-opening-current-card-id"),
                        )
                    }

                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier
                            .weight(1f)
                            .graphicsLayer {
                                alpha = cardsEntranceProgress.value
                                translationY = (1f - cardsEntranceProgress.value) *
                                    if (rootHeightPx > 0f) rootHeightPx else 960f
                            },
                    ) { page ->
                        RevealCard(
                            displayCard = displayCards[page],
                            page = page + 1,
                            total = displayCards.size,
                            isCurrentPage = page == currentPage,
                            showPreviousArrow = page == currentPage && page > 0,
                            showNextArrow = page == currentPage && page < displayCards.lastIndex,
                            cardTranslationY = if (page == currentPage) lastCardNudgeOffset.value else 0f,
                            nudgeActive = page == settledPage && settledPage == displayCards.lastIndex && lastCardNudgeActive,
                            onOpenFullscreen = {
                                lastCardNudgeActive = false
                                fullscreenPage = page
                            },
                        )
                    }
                }
            }
        }

        RarityBurstOverlay(
            rarityLabel = state.highestBurstRarity,
            hasHolographicBurst = state.hasHolographicBurst,
            progress = burstProgress.value,
            originBounds = initialBoosterBounds,
        )

        val fullscreenCard = fullscreenPage?.let(displayCards::getOrNull)
        if (fullscreenCard != null) {
            PackOpeningFullscreenDialog(
                displayCard = fullscreenCard,
                onDismiss = { fullscreenPage = null },
            )
        }
    }
}
