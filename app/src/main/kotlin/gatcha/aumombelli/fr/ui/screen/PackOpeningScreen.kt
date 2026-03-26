package fr.aumombelli.gatcha.ui.screen

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import fr.aumombelli.gatcha.model.DisplayCard
import fr.aumombelli.gatcha.ui.component.AstroCardDetailsSurface
import fr.aumombelli.gatcha.ui.component.AstroCardPreviewSurface
import fr.aumombelli.gatcha.ui.component.AstroCardSurfaceMode
import fr.aumombelli.gatcha.ui.component.TRADING_CARD_WIDTH_OVER_HEIGHT
import fr.aumombelli.gatcha.ui.motion.AnimatedExtensionPackCard
import fr.aumombelli.gatcha.ui.motion.PACK_REVEAL_WIDTH_FRACTION
import fr.aumombelli.gatcha.ui.motion.PackRevealBounds
import fr.aumombelli.gatcha.ui.motion.RarityBurstOverlay
import fr.aumombelli.gatcha.ui.viewmodel.PackOpeningUiState
import kotlin.math.roundToInt
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
    var rootHeightPx by remember(packResult?.drawnAt) { mutableFloatStateOf(0f) }
    val scale = remember(packResult?.drawnAt) { Animatable(1f) }
    val burstProgress = remember(packResult?.drawnAt) { Animatable(0f) }
    val cardsEntranceProgress = remember(packResult?.drawnAt) { Animatable(0f) }
    val dismissProgress = remember(packResult?.drawnAt) { Animatable(0f) }

    LaunchedEffect(packResult?.drawnAt) {
        if (packResult == null) return@LaunchedEffect
        cardsVisible = false
        fullscreenPage = null
        swipeOffset = 0f
        dismissRequested = false
        scale.snapTo(1f)
        burstProgress.snapTo(0f)
        cardsEntranceProgress.snapTo(0f)
        dismissProgress.snapTo(0f)
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
                onVerticalDrag = { change, dragAmount ->
                    change.consume()
                    swipeOffset = (swipeOffset + dragAmount).coerceAtMost(0f)
                },
                onDragEnd = {
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

    Box(
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
                    .graphicsLayer {
                        alpha = 1f - dismissProgress.value
                        translationY = swipeOffset + (-720f * dismissProgress.value)
                    }
                    .padding(20.dp),
            ) {
                Text(
                    text = "Pack Opening",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.testTag("pack-opening-title"),
                )
                Text(
                    text = "Extension: ${displayCards.firstOrNull()?.extensionName ?: packResult.extensionId}",
                    color = Color(0xFFD8E6F8),
                )

                if (displayCards.isNotEmpty()) {
                    val pagerState = rememberPagerState(pageCount = { displayCards.size })
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
                            onOpenFullscreen = { fullscreenPage = page },
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

        if (cardsVisible && displayCards.isNotEmpty() && state.errorMessage == null) {
            Text(
                text = "Glisse vers le haut pour revenir au menu",
                color = Color.White.copy(
                    alpha = 0.78f * cardsEntranceProgress.value * (1f - dismissProgress.value),
                ),
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 28.dp)
                    .testTag("pack-opening-swipe-hint"),
            )
        }

        val fullscreenCard = fullscreenPage?.let(displayCards::getOrNull)
        if (fullscreenCard != null) {
            PackOpeningFullscreenDialog(
                displayCard = fullscreenCard,
                onDismiss = { fullscreenPage = null },
            )
        }
    }
}

@Composable
private fun EmptyPackState(
    onDone: () -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
    ) {
        Text("No opened pack available.", color = Color.White)
        Button(
            onClick = onDone,
            modifier = Modifier.testTag("pack-opening-done"),
        ) {
            Text("Back to menu")
        }
    }
}

@Composable
private fun PackOpeningErrorState(
    message: String,
    onDone: () -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
    ) {
        Text(message, color = Color(0xFFFFA3A3))
        Button(
            onClick = onDone,
            modifier = Modifier.testTag("pack-opening-done"),
        ) {
            Text("Back to menu")
        }
    }
}

@Composable
private fun BoosterCover(
    extensionId: String,
    scale: Float,
    exitProgress: Float,
    initialBoosterBounds: PackRevealBounds?,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val easedExitProgress = easeInDownwardMotion(exitProgress)

    BoxWithConstraints(modifier = modifier) {
        val viewportHeightPx = with(density) { maxHeight.toPx() }
        if (initialBoosterBounds != null) {
            val width = with(density) { initialBoosterBounds.widthPx.toDp() }
            val height = with(density) { initialBoosterBounds.heightPx.toDp() }
            val exitTranslationY = (
                (viewportHeightPx - initialBoosterBounds.topPx) +
                    initialBoosterBounds.heightPx * 1.18f
                ) * easedExitProgress

            AnimatedExtensionPackCard(
                extensionId = extensionId,
                modifier = Modifier
                    .offset {
                        IntOffset(
                            x = initialBoosterBounds.leftPx.roundToInt(),
                            y = initialBoosterBounds.topPx.roundToInt(),
                        )
                    }
                    .size(width = width, height = height)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        translationY = exitTranslationY
                    }
                    .testTag("pack-opening-booster"),
                revealProgressOverride = 1f,
            )
        } else {
            val exitTranslationY = viewportHeightPx * 1.04f * easedExitProgress
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize(),
            ) {
                AnimatedExtensionPackCard(
                    extensionId = extensionId,
                    modifier = Modifier
                        .fillMaxWidth(PACK_REVEAL_WIDTH_FRACTION)
                        .aspectRatio(TRADING_CARD_WIDTH_OVER_HEIGHT)
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            translationY = exitTranslationY
                        }
                        .testTag("pack-opening-booster"),
                    revealProgressOverride = 1f,
                )
            }
        }
    }
}

private fun normalizedPhase(
    progress: Float,
    start: Float,
    end: Float,
): Float = ((progress - start) / (end - start).coerceAtLeast(0.0001f)).coerceIn(0f, 1f)

private fun easeInDownwardMotion(progress: Float): Float {
    val clamped = progress.coerceIn(0f, 1f)
    return clamped * clamped
}

@Composable
private fun RevealCard(
    displayCard: DisplayCard,
    page: Int,
    total: Int,
    onOpenFullscreen: () -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 18.dp, horizontal = 12.dp),
    ) {
        Text(
            text = "$page / $total",
            color = Color.White.copy(alpha = 0.9f),
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.testTag("pack-opening-progress"),
        )
        Text(
            text = displayCard.definition.id,
            color = Color(0xFFEAF4FF),
            modifier = Modifier.testTag("pack-opening-card-id"),
        )
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.weight(1f),
        ) {
            AstroCardPreviewSurface(
                displayCard = displayCard,
                mode = AstroCardSurfaceMode.PackReveal,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("pack-opening-card-surface"),
                onClick = onOpenFullscreen,
            )
        }
    }
}

@Composable
private fun PackOpeningFullscreenDialog(
    displayCard: DisplayCard,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false,
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xE608101A))
                .padding(14.dp)
                .testTag("astro-card-fullscreen"),
        ) {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .testTag("astro-card-fullscreen-close"),
            ) {
                Text("Fermer")
            }
            AstroCardDetailsSurface(
                displayCard = displayCard,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 42.dp),
            )
        }
    }
}
