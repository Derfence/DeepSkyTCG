package fr.aumombelli.dstcg.feature.minigames

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import fr.aumombelli.dstcg.ui.component.AstroCardPreviewSurface
import fr.aumombelli.dstcg.ui.component.AstroCardSurfaceMode
import fr.aumombelli.dstcg.ui.component.SceneNavigationButton
import fr.aumombelli.dstcg.ui.component.SceneNavigationIcon
import fr.aumombelli.dstcg.ui.component.TRADING_CARD_WIDTH_OVER_HEIGHT
import fr.aumombelli.dstcg.ui.motion.SkyBackdropVariant
import fr.aumombelli.dstcg.ui.screen.dstcgContentInsetsPadding

private val TimelinePreferredCardWidth = 164.dp
private val TimelineMinimumCardWidth = 124.dp
private val TimelineCardGap = 18.dp
private val TimelineValidateButtonSlotHeight = 58.dp

@Composable
internal fun TimelineGameScreen(
    state: MiniGamesUiState,
    onBackToMenu: () -> Unit,
    onPlaceCard: (String, Int) -> Unit,
    onReturnCardToHand: (String, Int) -> Unit,
    onValidate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val resultScreen = state.screen as? MiniGamesScreenUiState.TimelineResult
    val playingCue = (state.screen as? MiniGamesScreenUiState.TimelinePlaying)?.feedbackEvent

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("timeline-screen"),
    ) {
        MiniGameSceneBackdrop(
            variant = SkyBackdropVariant.Rural,
            sparkleBoost = if (state.screen is MiniGamesScreenUiState.TimelinePlaying) 0.30f else 0.20f,
            modifier = Modifier.fillMaxSize(),
        )
        MiniGameFeedbackOverlay(
            cue = playingCue ?: resultScreen?.feedbackEvent,
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .dstcgContentInsetsPadding(includeBottom = true)
                .padding(horizontal = 18.dp, vertical = 18.dp),
        ) {
            SceneNavigationButton(
                icon = SceneNavigationIcon.Back,
                onClick = onBackToMenu,
                contentDescription = "Retour au menu des mini-jeux",
                testTag = "timeline-back",
                modifier = Modifier.align(Alignment.TopStart),
            )

            Text(
                text = "Timeline",
                color = Color.White,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 4.dp),
            )

            when (val screen = state.screen) {
                is MiniGamesScreenUiState.TimelinePlaying -> TimelinePlayingPanel(
                    playing = screen,
                    onPlaceCard = onPlaceCard,
                    onReturnCardToHand = onReturnCardToHand,
                    onValidate = onValidate,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .fillMaxSize(),
                )

                is MiniGamesScreenUiState.TimelineResult -> TimelineResultPanel(
                    result = screen,
                    onBackToMenu = onBackToMenu,
                    modifier = Modifier.align(Alignment.Center),
                )

                is MiniGamesScreenUiState.TimelineUnavailable -> TimelineUnavailablePanel(
                    message = screen.message,
                    onBackToMenu = onBackToMenu,
                    modifier = Modifier.align(Alignment.Center),
                )

                else -> Unit
            }
        }
    }
}

@Composable
private fun TimelinePlayingPanel(
    playing: MiniGamesScreenUiState.TimelinePlaying,
    onPlaceCard: (String, Int) -> Unit,
    onReturnCardToHand: (String, Int) -> Unit,
    onValidate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var slotBounds by remember { mutableStateOf<Map<Int, Rect>>(emptyMap()) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxSize()
            .testTag("timeline-playing"),
    ) {
        Spacer(modifier = Modifier.height(58.dp))
        TimelineHud(
            playing = playing,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp),
        )
        Text(
            text = playing.instruction,
            color = Color.White,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp)
                .testTag("timeline-instruction"),
        )
        MiniGameBoardSurface(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            TimelineHorizontalBoard(
                playing = playing,
                slotBounds = slotBounds,
                onSlotBoundsChanged = { index, bounds ->
                    slotBounds = slotBounds + (index to bounds)
                },
                onPlaceCard = onPlaceCard,
                onReturnCardToHand = onReturnCardToHand,
                enabled = !playing.feedbackEvent.isCompletion(),
                modifier = Modifier.fillMaxSize(),
            )
        }
        Box(
            contentAlignment = Alignment.TopCenter,
            modifier = Modifier
                .fillMaxWidth()
                .height(TimelineValidateButtonSlotHeight),
        ) {
            if (playing.canValidate) {
                Button(
                    onClick = onValidate,
                    modifier = Modifier
                        .padding(top = 10.dp)
                        .testTag("timeline-validate"),
                ) {
                    Text("Valider")
                }
            }
        }
    }
}

@Composable
private fun TimelineHud(
    playing: MiniGamesScreenUiState.TimelinePlaying,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.testTag("timeline-hud"),
    ) {
        MiniGameHudPill(
            label = "Critère",
            value = playing.criterionTitle,
            tint = Color(0xFF9AEAFF),
            modifier = Modifier.weight(1f),
        )
        MiniGameHudPill(
            label = "Gain max",
            value = playing.rewardLabel,
            tint = Color(0xFFF6C75D),
            modifier = Modifier.weight(1f),
        )
        MiniGameHudPill(
            label = "Cartes",
            value = "${playing.slots.count { it.placedCard != null }}/${playing.slots.size}",
            tint = Color(0xFF75E0C2),
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun TimelineHorizontalBoard(
    playing: MiniGamesScreenUiState.TimelinePlaying,
    slotBounds: Map<Int, Rect>,
    onSlotBoundsChanged: (Int, Rect) -> Unit,
    onPlaceCard: (String, Int) -> Unit,
    onReturnCardToHand: (String, Int) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()
    val visibleHandRatio = 0.54f
    var draggingCardId by remember { mutableStateOf<String?>(null) }
    var handSlotBounds by remember { mutableStateOf<Map<Int, Rect>>(emptyMap()) }

    BoxWithConstraints(
        modifier = modifier
            .clipToBounds()
            .testTag("timeline-horizontal-scroll"),
    ) {
        val viewportWidth = maxWidth
        val viewportHeight = maxHeight
        val cardGap = TimelineCardGap
        val sidePadding = 10.dp
        val verticalGap = 14.dp
        val cardHeightBudget = (viewportHeight - verticalGap) / (1f + visibleHandRatio)
        val cardWidth = minOf(TimelinePreferredCardWidth, cardHeightBudget * TRADING_CARD_WIDTH_OVER_HEIGHT)
            .coerceAtLeast(TimelineMinimumCardWidth)
        val cardHeight = cardWidth / TRADING_CARD_WIDTH_OVER_HEIGHT
        val fullStackHeight = (cardHeight * 2f) + verticalGap
        val baseContentWidth = (cardWidth * playing.slots.size) +
            (cardGap * (playing.slots.size - 1).coerceAtLeast(0)) +
            (sidePadding * 2)
        val contentWidth = maxOf(baseContentWidth, viewportWidth + 1.dp)
        val slotTop = if (viewportHeight >= fullStackHeight) {
            (viewportHeight - fullStackHeight) / 2f
        } else {
            0.dp
        }
        val handTop = slotTop + cardHeight + verticalGap
        val isDraggingPlacedCard = playing.slots.any { it.placedCard?.id == draggingCardId }
        val isDraggingHandCard = playing.handSlots.any { it?.id == draggingCardId }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .horizontalScroll(scrollState),
        ) {
            Box(
                modifier = Modifier
                    .width(contentWidth)
                    .height(viewportHeight)
                    .padding(horizontal = sidePadding),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(cardGap),
                    modifier = Modifier
                        .zIndex(if (isDraggingPlacedCard) 10f else 0f)
                        .padding(top = slotTop),
                ) {
                    playing.slots.forEach { slot ->
                        val placedCard = slot.placedCard
                        TimelineCardSlot(
                            slot = slot,
                            slotBounds = slotBounds,
                            onSlotBoundsChanged = onSlotBoundsChanged,
                            onPlaceCard = onPlaceCard,
                            handSlotBounds = handSlotBounds,
                            emptyHandSlotIndexes = playing.emptyHandSlotIndexes(),
                            onReturnCardToHand = onReturnCardToHand,
                            onDragStateChange = { isDragging ->
                                draggingCardId = placedCard?.id?.takeIf { isDragging }
                            },
                            enabled = enabled,
                            modifier = Modifier
                                .zIndex(if (placedCard?.id == draggingCardId) 8f else 0f)
                                .width(cardWidth)
                                .height(cardHeight),
                        )
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(cardGap),
                    modifier = Modifier
                        .zIndex(if (isDraggingHandCard) 10f else 0f)
                        .padding(top = handTop)
                        .testTag("timeline-hand"),
                ) {
                    playing.handSlots.forEachIndexed { index, card ->
                        TimelineHandSlot(
                            index = index,
                            card = card,
                            slotBounds = slotBounds,
                            handSlotBounds = handSlotBounds,
                            onHandSlotBoundsChanged = { slotIndex, bounds ->
                                handSlotBounds = handSlotBounds + (slotIndex to bounds)
                            },
                            emptyHandSlotIndexes = playing.emptyHandSlotIndexes(),
                            onPlaceCard = onPlaceCard,
                            onReturnCardToHand = onReturnCardToHand,
                            onDragStateChange = { isDragging ->
                                draggingCardId = card?.id?.takeIf { isDragging }
                            },
                            enabled = enabled,
                            modifier = Modifier
                                .zIndex(if (card?.id == draggingCardId) 8f else 0f)
                                .width(cardWidth)
                                .height(cardHeight),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TimelineHandSlot(
    index: Int,
    card: TimelineCardUi?,
    slotBounds: Map<Int, Rect>,
    handSlotBounds: Map<Int, Rect>,
    onHandSlotBoundsChanged: (Int, Rect) -> Unit,
    emptyHandSlotIndexes: Set<Int>,
    onPlaceCard: (String, Int) -> Unit,
    onReturnCardToHand: (String, Int) -> Unit,
    onDragStateChange: (Boolean) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .onGloballyPositioned { coordinates ->
                onHandSlotBoundsChanged(index, coordinates.boundsInRoot())
            }
            .testTag("timeline-hand-slot-$index"),
    ) {
        if (card != null) {
            TimelineDraggableCard(
                card = card,
                slotBounds = slotBounds,
                onPlaceCard = onPlaceCard,
                handSlotBounds = handSlotBounds,
                emptyHandSlotIndexes = emptyHandSlotIndexes,
                onReturnCardToHand = onReturnCardToHand,
                onDragStateChange = onDragStateChange,
                enabled = enabled,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun TimelineCardSlot(
    slot: TimelineSlotUi,
    slotBounds: Map<Int, Rect>,
    onSlotBoundsChanged: (Int, Rect) -> Unit,
    onPlaceCard: (String, Int) -> Unit,
    handSlotBounds: Map<Int, Rect>,
    emptyHandSlotIndexes: Set<Int>,
    onReturnCardToHand: (String, Int) -> Unit,
    onDragStateChange: (Boolean) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(14.dp)
    val placedCard = slot.placedCard
    val emptySlotDecoration = if (placedCard == null) {
        Modifier
            .background(Color(0xA807111A), shape)
            .border(1.dp, Color(0x88BBDFF2), shape)
    } else {
        Modifier
    }
    Box(
        modifier = modifier
            .then(emptySlotDecoration)
            .onGloballyPositioned { coordinates ->
                onSlotBoundsChanged(slot.index, coordinates.boundsInRoot())
            }
            .testTag(slot.testTag),
    ) {
        if (placedCard == null) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
            ) {
                val emptyLabel = slot.emptyLabel
                if (emptyLabel != null) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = emptyLabel,
                            color = Color(0xFFAFC4D6),
                            style = MaterialTheme.typography.labelMedium,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        } else {
            TimelineDraggableCard(
                card = placedCard,
                slotBounds = slotBounds,
                onPlaceCard = onPlaceCard,
                handSlotBounds = handSlotBounds,
                emptyHandSlotIndexes = emptyHandSlotIndexes,
                onReturnCardToHand = onReturnCardToHand,
                onDragStateChange = onDragStateChange,
                enabled = enabled,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun TimelineDraggableCard(
    card: TimelineCardUi,
    slotBounds: Map<Int, Rect>,
    onPlaceCard: (String, Int) -> Unit,
    handSlotBounds: Map<Int, Rect>,
    emptyHandSlotIndexes: Set<Int>,
    onReturnCardToHand: (String, Int) -> Unit,
    onDragStateChange: (Boolean) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    var dragOffset by remember(card.id) { mutableStateOf(Offset.Zero) }
    var dragStartBounds by remember(card.id) { mutableStateOf<Rect?>(null) }
    var bounds by remember(card.id) { mutableStateOf<Rect?>(null) }
    val isDragging = dragOffset != Offset.Zero

    Box(
        modifier = modifier
            .zIndex(if (isDragging) 4f else 0f)
            .graphicsLayer {
                translationX = dragOffset.x
                translationY = dragOffset.y
                scaleX = if (isDragging) 1.04f else 1f
                scaleY = if (isDragging) 1.04f else 1f
            }
            .onGloballyPositioned { coordinates ->
                bounds = coordinates.boundsInRoot()
            }
            .then(
                if (enabled) {
                    Modifier.pointerInput(card.id, slotBounds, handSlotBounds, emptyHandSlotIndexes) {
                        detectDragGestures(
                            onDragStart = {
                                dragStartBounds = bounds
                                onDragStateChange(true)
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                dragOffset += dragAmount
                            },
                            onDragEnd = {
                                val targetSlot = resolveTimelineDropTarget(
                                    startBounds = dragStartBounds ?: bounds,
                                    dragOffset = dragOffset,
                                    slotBounds = slotBounds,
                                )
                                val targetHandSlot = if (targetSlot == null) {
                                    resolveTimelineHandDropTarget(
                                        startBounds = dragStartBounds ?: bounds,
                                        dragOffset = dragOffset,
                                        handSlotBounds = handSlotBounds,
                                        emptyHandSlotIndexes = emptyHandSlotIndexes,
                                    )
                                } else {
                                    null
                                }
                                dragOffset = Offset.Zero
                                dragStartBounds = null
                                onDragStateChange(false)
                                if (targetSlot != null) {
                                    onPlaceCard(card.id, targetSlot)
                                } else if (targetHandSlot != null) {
                                    onReturnCardToHand(card.id, targetHandSlot)
                                }
                            },
                            onDragCancel = {
                                dragOffset = Offset.Zero
                                dragStartBounds = null
                                onDragStateChange(false)
                            },
                        )
                    }
                } else {
                    Modifier
                },
            )
            .testTag(card.testTag),
    ) {
        TimelinePlayableCardContent(
            card = card,
            modifier = Modifier
                .fillMaxSize()
                .testTag("timeline-card-preview-${card.id}"),
        )
    }
}

private fun resolveTimelineDropTarget(
    startBounds: Rect?,
    dragOffset: Offset,
    slotBounds: Map<Int, Rect>,
): Int? {
    val cardCenter = movedCardCenter(
        startBounds = startBounds,
        dragOffset = dragOffset,
    ) ?: return null
    return slotBounds.entries.firstOrNull { (_, slotRect) ->
        slotRect.contains(cardCenter)
    }?.key
}

private fun resolveTimelineHandDropTarget(
    startBounds: Rect?,
    dragOffset: Offset,
    handSlotBounds: Map<Int, Rect>,
    emptyHandSlotIndexes: Set<Int>,
): Int? {
    val cardCenter = movedCardCenter(
        startBounds = startBounds,
        dragOffset = dragOffset,
    ) ?: return null
    return handSlotBounds.entries.firstOrNull { (index, bounds) ->
        index in emptyHandSlotIndexes && bounds.contains(cardCenter)
    }?.key
}

private fun movedCardCenter(
    startBounds: Rect?,
    dragOffset: Offset,
): Offset? = startBounds?.let { bounds ->
    Offset(
        x = bounds.center.x + dragOffset.x,
        y = bounds.center.y + dragOffset.y,
    )
}

private fun MiniGamesScreenUiState.TimelinePlaying.emptyHandSlotIndexes(): Set<Int> =
    handSlots.mapIndexedNotNullTo(mutableSetOf()) { index, card ->
        index.takeIf { card == null }
    }

@Composable
private fun TimelinePlayableCardContent(
    card: TimelineCardUi,
    modifier: Modifier = Modifier,
) {
    AstroCardPreviewSurface(
        displayCard = card.displayCard,
        mode = AstroCardSurfaceMode.Thumbnail,
        modifier = modifier,
    )
}

@Composable
private fun TimelineResultPanel(
    result: MiniGamesScreenUiState.TimelineResult,
    onBackToMenu: () -> Unit,
    modifier: Modifier = Modifier,
) {
    MiniGameBoardSurface(
        modifier = modifier
            .widthIn(max = 560.dp)
            .fillMaxWidth()
            .testTag("timeline-result"),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 520.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Text(
                text = "Timeline terminée",
                color = Color.White,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                MiniGameHudPill(
                    label = result.criterionTitle,
                    value = result.scoreLabel,
                    tint = Color(0xFF75E0C2),
                    modifier = Modifier.weight(1f),
                )
                MiniGameHudPill(
                    label = "Gain",
                    value = result.rewardLabel,
                    tint = Color(0xFFF6C75D),
                    modifier = Modifier.weight(1f),
                )
            }
            result.slotResults.forEach { slotResult ->
                TimelineResultRow(slotResult)
            }
            if (result.showCorrectOrder) {
                Text(
                    text = "Ordre correct",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("timeline-correct-order-title"),
                )
                result.correctOrder.forEachIndexed { index, card ->
                    Text(
                        text = "${index + 1}. ${card.displayCard.definition.name} - ${card.valueLabel}",
                        color = Color(0xFFD6E7F7),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("timeline-correct-order-$index"),
                    )
                }
            }
            Button(
                onClick = onBackToMenu,
                modifier = Modifier.testTag("timeline-result-back"),
            ) {
                Text("Retour au menu")
            }
        }
    }
}

@Composable
private fun TimelineResultRow(slotResult: TimelineSlotResultUi) {
    val tint = if (slotResult.isCorrect) Color(0xFF88E6D2) else Color(0xFFFFC4BD)
    val label = if (slotResult.isCorrect) {
        slotResult.placedCard.displayCard.definition.name
    } else {
        "${slotResult.placedCard.displayCard.definition.name} → ${slotResult.correctCard.displayCard.definition.name}"
    }
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (slotResult.isCorrect) Color(0xAA164E42) else Color(0xAA672A2E),
        contentColor = Color.White,
        modifier = Modifier
            .fillMaxWidth()
            .testTag(slotResult.testTag),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(3.dp),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
        ) {
            Text(
                text = "${slotResult.index + 1}. $label",
                color = tint,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "Valeur attendue : ${slotResult.correctCard.valueLabel}",
                color = Color(0xFFD8E8F1),
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

@Composable
private fun TimelineUnavailablePanel(
    message: String,
    onBackToMenu: () -> Unit,
    modifier: Modifier = Modifier,
) {
    MiniGameBoardSurface(
        modifier = modifier
            .widthIn(max = 460.dp)
            .fillMaxWidth()
            .testTag("timeline-unavailable"),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = message,
                color = Color.White,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleMedium,
            )
            Button(
                onClick = onBackToMenu,
                modifier = Modifier.testTag("timeline-unavailable-back"),
            ) {
                Text("Retour au menu")
            }
        }
    }
}

private fun MiniGameFeedbackEvent?.isCompletion(): Boolean =
    this?.tone == MiniGameFeedbackTone.Completion
