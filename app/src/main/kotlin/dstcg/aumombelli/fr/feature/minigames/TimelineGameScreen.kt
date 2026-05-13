package fr.aumombelli.dstcg.feature.minigames

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
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
import fr.aumombelli.dstcg.ui.motion.SkyBackdropVariant
import fr.aumombelli.dstcg.ui.screen.dstcgContentInsetsPadding

@Composable
internal fun TimelineGameScreen(
    state: MiniGamesUiState,
    onBackToMenu: () -> Unit,
    onPlaceCard: (String, Int) -> Unit,
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
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                playing.slots.forEach { slot ->
                    TimelineDropSlot(
                        slot = slot,
                        slotBounds = slotBounds,
                        onSlotBoundsChanged = { index, bounds ->
                            slotBounds = slotBounds + (index to bounds)
                        },
                        onPlaceCard = onPlaceCard,
                        enabled = !playing.feedbackEvent.isCompletion(),
                    )
                }
            }
        }
        Text(
            text = "Main",
            color = Color(0xFFD6E7F7),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 10.dp, bottom = 6.dp),
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 118.dp, max = 160.dp)
                .testTag("timeline-hand"),
        ) {
            playing.handCards.forEach { card ->
                TimelineDraggableCard(
                    card = card,
                    slotBounds = slotBounds,
                    onPlaceCard = onPlaceCard,
                    enabled = !playing.feedbackEvent.isCompletion(),
                    compact = false,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                )
            }
            repeat(playing.slots.size - playing.handCards.size) {
                Spacer(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                )
            }
        }
        Button(
            onClick = onValidate,
            enabled = playing.canValidate,
            modifier = Modifier
                .padding(top = 10.dp)
                .testTag("timeline-validate"),
        ) {
            Text("Valider")
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
private fun TimelineDropSlot(
    slot: TimelineSlotUi,
    slotBounds: Map<Int, Rect>,
    onSlotBoundsChanged: (Int, Rect) -> Unit,
    onPlaceCard: (String, Int) -> Unit,
    enabled: Boolean,
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xAA07111A),
        contentColor = Color.White,
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
            .onGloballyPositioned { coordinates ->
                onSlotBoundsChanged(slot.index, coordinates.boundsInRoot())
            }
            .testTag(slot.testTag),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            Text(
                text = (slot.index + 1).toString(),
                color = Color(0xFFF6C75D),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.widthIn(min = 24.dp),
            )
            val placedCard = slot.placedCard
            if (placedCard == null) {
                Text(
                    text = "Dépose une carte ici",
                    color = Color(0xFFAFC4D6),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
            } else {
                TimelineDraggableCard(
                    card = placedCard,
                    slotBounds = slotBounds,
                    onPlaceCard = onPlaceCard,
                    enabled = enabled,
                    compact = true,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun TimelineDraggableCard(
    card: TimelineCardUi,
    slotBounds: Map<Int, Rect>,
    onPlaceCard: (String, Int) -> Unit,
    enabled: Boolean,
    compact: Boolean,
    modifier: Modifier = Modifier,
) {
    var dragOffset by remember(card.id) { mutableStateOf(Offset.Zero) }
    var bounds by remember(card.id) { mutableStateOf<Rect?>(null) }
    val isDragging = dragOffset != Offset.Zero

    Surface(
        shape = RoundedCornerShape(if (compact) 10.dp else 12.dp),
        color = if (compact) Color(0xCC153B4C) else Color(0xDD10283A),
        contentColor = Color.White,
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
                    Modifier.pointerInput(card.id, slotBounds) {
                        detectDragGestures(
                            onDrag = { change, dragAmount ->
                                change.consume()
                                dragOffset += dragAmount
                            },
                            onDragEnd = {
                                val center = bounds?.center?.let { it + dragOffset }
                                val targetSlot = center?.let { dropCenter ->
                                    slotBounds.entries.firstOrNull { (_, slotRect) ->
                                        slotRect.contains(dropCenter)
                                    }?.key
                                }
                                if (targetSlot != null) {
                                    onPlaceCard(card.id, targetSlot)
                                }
                                dragOffset = Offset.Zero
                            },
                            onDragCancel = {
                                dragOffset = Offset.Zero
                            },
                        )
                    }
                } else {
                    Modifier
                },
            )
            .testTag(card.testTag),
    ) {
        if (compact) {
            TimelineCompactCardContent(card)
        } else {
            TimelineHandCardContent(card)
        }
    }
}

@Composable
private fun TimelineHandCardContent(card: TimelineCardUi) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp),
        modifier = Modifier.padding(6.dp),
    ) {
        AstroCardPreviewSurface(
            displayCard = card.displayCard,
            mode = AstroCardSurfaceMode.Thumbnail,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        )
        Text(
            text = card.displayCard.definition.name,
            color = Color.White,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun TimelineCompactCardContent(card: TimelineCardUi) {
    Column(
        verticalArrangement = Arrangement.spacedBy(2.dp),
        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
    ) {
        Text(
            text = card.displayCard.definition.name,
            color = Color.White,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = card.valueLabel,
            color = Color(0xFFD6E7F7),
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
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
