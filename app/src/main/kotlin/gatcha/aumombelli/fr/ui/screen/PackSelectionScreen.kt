package fr.aumombelli.gatcha.ui.screen

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import fr.aumombelli.gatcha.model.ExtensionDefinition
import fr.aumombelli.gatcha.ui.motion.ExtensionAnimationStyle
import fr.aumombelli.gatcha.ui.motion.AnimatedExtensionPackCard
import fr.aumombelli.gatcha.ui.motion.LaunchLogoMark
import fr.aumombelli.gatcha.ui.motion.MotionCard
import fr.aumombelli.gatcha.ui.motion.PACK_REVEAL_WIDTH_FRACTION
import fr.aumombelli.gatcha.ui.motion.PackRevealBounds
import fr.aumombelli.gatcha.ui.motion.extensionLineReveal
import fr.aumombelli.gatcha.ui.motion.extensionPointReveal
import fr.aumombelli.gatcha.ui.motion.extensionAnimationSpec
import fr.aumombelli.gatcha.ui.motion.projectExtensionPattern
import fr.aumombelli.gatcha.ui.component.TRADING_CARD_WIDTH_OVER_HEIGHT
import fr.aumombelli.gatcha.ui.viewmodel.PackSelectionUiState
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.roundToLong
import kotlinx.coroutines.delay

@Composable
fun PackSelectionScreen(
    state: PackSelectionUiState,
    onRefresh: () -> Unit,
    onSelectExtension: (String) -> Unit,
    onSelectBooster: (Int) -> Unit,
    onOpenPack: (String) -> Unit,
    onPackRevealReady: () -> Unit,
    onSelectedBoosterBoundsChanged: (PackRevealBounds?) -> Unit = {},
    packReadySignal: Int,
    modifier: Modifier = Modifier,
    showBackground: Boolean = true,
    sceneVisible: Boolean = true,
    extensionListVisible: Boolean = true,
    interactionsEnabled: Boolean = true,
) {
    val nextDrawAtText = formatNextDrawAt(state.nextDrawAt)
    val drawLocked = state.nextDrawAt?.let { runCatching { Instant.parse(it).isAfter(Instant.now()) }.getOrDefault(false) } ?: false
    val selectedExtension = state.extensions.firstOrNull { it.id == state.selectedExtensionId }
    var displayedExtensionId by remember(state.extensions) { mutableStateOf<String?>(selectedExtension?.id) }
    val displayedExtension = state.extensions.firstOrNull { it.id == displayedExtensionId }
    val displayedExtensionIndex = displayedExtension?.let { extension ->
        state.extensions.indexOfFirst { it.id == extension.id }.coerceAtLeast(0)
    } ?: 0
    val sceneAlpha by animateFloatAsState(
        targetValue = if (sceneVisible) 1f else 0f,
        animationSpec = tween(durationMillis = 420, easing = FastOutSlowInEasing),
        label = "pack-selection-scene-alpha",
    )
    val extensionListAlpha by animateFloatAsState(
        targetValue = if (extensionListVisible) 1f else 0f,
        animationSpec = tween(durationMillis = 520, easing = FastOutSlowInEasing),
        label = "pack-selection-extension-list-alpha",
    )
    val extensionListTranslationY by animateFloatAsState(
        targetValue = if (extensionListVisible) 0f else 280f,
        animationSpec = tween(durationMillis = 760, easing = FastOutSlowInEasing),
        label = "pack-selection-extension-list-translation",
    )

    val heroProgress = remember { Animatable(0f) }
    val boosterIntroProgress = remember { Animatable(0f) }
    val boosterSelectionProgress = remember { Animatable(0f) }
    var handledPackSignal by remember(displayedExtension?.id) { mutableIntStateOf(packReadySignal) }

    LaunchedEffect(selectedExtension?.id) {
        if (selectedExtension != null) {
            if (displayedExtensionId == selectedExtension.id && heroProgress.value >= 0.99f) return@LaunchedEffect
            displayedExtensionId = selectedExtension.id
            heroProgress.snapTo(0f)
            boosterIntroProgress.snapTo(0f)
            boosterSelectionProgress.snapTo(0f)
            heroProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 820, easing = FastOutSlowInEasing),
            )
            boosterIntroProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 760, easing = FastOutSlowInEasing),
            )
        } else if (displayedExtensionId != null) {
            boosterIntroProgress.snapTo(0f)
            boosterSelectionProgress.snapTo(0f)
            heroProgress.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 760, easing = FastOutSlowInEasing),
            )
            displayedExtensionId = null
        }
    }

    LaunchedEffect(state.selectedBoosterIndex, displayedExtension?.id) {
        if (displayedExtension == null || state.selectedBoosterIndex == null) {
            boosterSelectionProgress.snapTo(0f)
            return@LaunchedEffect
        }
        boosterSelectionProgress.snapTo(0f)
        boosterSelectionProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 560, easing = FastOutSlowInEasing),
        )
    }

    LaunchedEffect(packReadySignal, state.selectedBoosterIndex, displayedExtension?.id) {
        if (displayedExtension == null) return@LaunchedEffect
        if (state.selectedBoosterIndex == null) return@LaunchedEffect
        if (packReadySignal == handledPackSignal) return@LaunchedEffect
        handledPackSignal = packReadySignal
        val remainingSelectionMillis = (560f * (1f - boosterSelectionProgress.value))
            .coerceAtLeast(0f)
            .roundToLong()
        delay(remainingSelectionMillis + 80L)
        onPackRevealReady()
    }
    val listFadeProgress = if (displayedExtension != null) {
        (1f - heroProgress.value).coerceIn(0f, 1f)
    } else {
        1f
    }
    val introTextAlpha = if (displayedExtension != null) {
        (1f - heroProgress.value * 1.1f).coerceIn(0f, 1f)
    } else {
        1f
    }
    val stageTextAlpha = if (state.selectedBoosterIndex != null) {
        (1f - boosterSelectionProgress.value).coerceIn(0f, 1f)
    } else {
        1f
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer {
                alpha = sceneAlpha
            }
            .background(
                if (showBackground) {
                    Brush.verticalGradient(listOf(Color(0x5509111E), Color(0x88060B12)))
                } else {
                    Brush.verticalGradient(listOf(Color.Transparent, Color.Transparent))
                },
            )
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxSize()
                .gatchaContentInsetsPadding(includeBottom = true)
                .padding(16.dp),
        ) {
            Text(
                text = "Open Pack",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.graphicsLayer {
                    alpha = stageTextAlpha
                },
            )

            Text(
                text = if (nextDrawAtText == null) {
                    "Aucun cooldown actif. Choisis une extension puis un booster."
                } else {
                    "Prochain tirage disponible : $nextDrawAtText"
                },
                color = Color(0xFFD6E4F5),
                modifier = Modifier
                    .graphicsLayer {
                        alpha = stageTextAlpha
                    }
                    .testTag("pack-status"),
            )

            state.errorMessage?.let { error ->
                MotionCard(
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(18.dp),
                    ) {
                        Text(
                            text = error,
                            color = Color(0xFFFFB1B1),
                            modifier = Modifier.testTag("pack-error"),
                        )
                        Button(
                            onClick = onRefresh,
                            enabled = interactionsEnabled,
                            modifier = Modifier.testTag("pack-refresh"),
                        ) {
                            Text("Retry")
                        }
                    }
                }
            }

            if (state.isLoading && state.extensions.isEmpty()) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp),
                ) {
                    CircularProgressIndicator()
                }
            } else {
                BoxWithConstraints(
                    modifier = Modifier.weight(1f, fill = true),
                ) {
                    Text(
                        text = "Choisis l'extension a contempler.",
                        color = Color(0xFFEAF4FF),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.graphicsLayer {
                            alpha = introTextAlpha
                        },
                    )
                    ExtensionList(
                        extensions = state.extensions,
                        drawLocked = drawLocked,
                        onSelectExtension = onSelectExtension,
                        interactionsEnabled = interactionsEnabled && displayedExtension == null,
                        highlightedExtensionId = displayedExtensionId,
                        highlightProgress = heroProgress.value,
                        badgeAnimationsEnabled = extensionListVisible && displayedExtension == null,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = EXTENSION_LIST_TOP_PADDING)
                            .graphicsLayer {
                                alpha = extensionListAlpha * listFadeProgress
                                translationY = extensionListTranslationY
                            },
                    )
                    displayedExtension?.let { extension ->
                        ExtensionBoosterStage(
                            extension = extension,
                            extensionIndex = displayedExtensionIndex,
                            heroProgress = heroProgress.value,
                            boosterIntroProgress = boosterIntroProgress.value,
                            boosterSelectionProgress = boosterSelectionProgress.value,
                            drawLocked = drawLocked,
                            selectedBoosterIndex = state.selectedBoosterIndex,
                            isAwaitingPackResult = state.isAwaitingPackResult,
                            onSelectBooster = { boosterIndex ->
                                onSelectBooster(boosterIndex)
                                onOpenPack(extension.id)
                            },
                            onSelectedBoosterBoundsChanged = onSelectedBoosterBoundsChanged,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ExtensionList(
    extensions: List<ExtensionDefinition>,
    drawLocked: Boolean,
    onSelectExtension: (String) -> Unit,
    interactionsEnabled: Boolean,
    highlightedExtensionId: String?,
    highlightProgress: Float,
    badgeAnimationsEnabled: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier,
    ) {
        extensions.forEachIndexed { index, extension ->
            val alpha = when {
                highlightedExtensionId == null -> 1f
                extension.id == highlightedExtensionId -> (1f - highlightProgress * 4f).coerceIn(0f, 1f)
                else -> (1f - highlightProgress).coerceIn(0f, 1f)
            }
            MotionCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(EXTENSION_CARD_HEIGHT)
                    .graphicsLayer {
                        this.alpha = alpha
                    },
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.Start,
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxSize()
                        .padding(18.dp)
                        .testTag("pack-extension-${extension.id}"),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = extension.name,
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f),
                        )
                        ExtensionAnimatedBadge(
                            extensionId = extension.id,
                            animationsEnabled = badgeAnimationsEnabled,
                            startDelayMillis = 760 + index * 90,
                            modifier = Modifier
                                .padding(start = 12.dp)
                                .width(78.dp)
                                .height(54.dp),
                        )
                    }
                    Button(
                        onClick = { onSelectExtension(extension.id) },
                        enabled = !drawLocked && interactionsEnabled,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("pack-extension-enter-${extension.id}"),
                    ) {
                        Text(if (drawLocked) "Verrouillé" else "Observer")
                    }
                }
            }
        }
    }
}

@Composable
private fun ExtensionAnimatedBadge(
    extensionId: String,
    animationsEnabled: Boolean,
    startDelayMillis: Int,
    modifier: Modifier = Modifier,
) {
    val spec = remember(extensionId) { extensionAnimationSpec(extensionId) }
    val lineProgress = remember(extensionId) { Animatable(0f) }
    val emblemAlpha = remember(extensionId) { Animatable(0f) }

    LaunchedEffect(extensionId, animationsEnabled) {
        lineProgress.snapTo(0f)
        emblemAlpha.snapTo(0f)
        if (!animationsEnabled) return@LaunchedEffect

        delay(startDelayMillis.toLong())
        when (spec.style) {
            ExtensionAnimationStyle.BigDipper -> {
                lineProgress.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 520, easing = FastOutSlowInEasing),
                )
            }
            ExtensionAnimationStyle.NeutralSky -> {
                emblemAlpha.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 360, easing = FastOutSlowInEasing),
                )
            }
        }
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier,
    ) {
        if (spec.style == ExtensionAnimationStyle.BigDipper) {
            Canvas(
                modifier = Modifier.fillMaxSize(),
            ) {
                val projection = projectExtensionPattern(
                    spec = spec,
                    canvasWidth = size.width,
                    canvasHeight = size.height,
                )

                spec.starPattern.forEachIndexed { index, star ->
                    val reveal = extensionPointReveal(
                        spec = spec,
                        pointIndex = index,
                        lineProgress = lineProgress.value,
                        isReversing = false,
                        revealWindow = 0.28f,
                    )
                    if (reveal <= 0f) return@forEachIndexed
                    val projected = projection.project(star)
                    val center = Offset(projected.x, projected.y)
                    drawCircle(
                        color = Color.White.copy(alpha = reveal),
                        radius = size.minDimension * (0.026f + reveal * 0.012f),
                        center = center,
                    )
                    drawCircle(
                        color = Color(0xFFFFC85A).copy(alpha = reveal * 0.32f),
                        radius = size.minDimension * 0.074f,
                        center = center,
                    )
                }

                spec.lineConnections.forEachIndexed { index, connection ->
                    val reveal = extensionLineReveal(
                        lineProgress = lineProgress.value,
                        lineIndex = index,
                        lineCount = spec.lineConnections.size,
                        revealWindow = 0.28f,
                    )
                    if (reveal <= 0f) return@forEachIndexed

                    val start = spec.starPattern[connection.first]
                    val end = spec.starPattern[connection.second]
                    val projectedStart = projection.project(start)
                    val projectedEnd = projection.project(end)
                    val startOffset = Offset(projectedStart.x, projectedStart.y)
                    val endOffset = Offset(projectedEnd.x, projectedEnd.y)
                    val currentEnd = Offset(
                        x = startOffset.x + (endOffset.x - startOffset.x) * reveal,
                        y = startOffset.y + (endOffset.y - startOffset.y) * reveal,
                    )
                    drawLine(
                        color = Color(0xFFE2F0FF).copy(alpha = reveal * 0.92f),
                        start = startOffset,
                        end = currentEnd,
                        strokeWidth = size.minDimension * 0.038f,
                    )
                }
            }
        } else {
            LaunchLogoMark(
                showWordmark = false,
                emblemSize = 42.dp,
                modifier = Modifier.graphicsLayer {
                    alpha = emblemAlpha.value
                    scaleX = 0.82f + emblemAlpha.value * 0.12f
                    scaleY = 0.82f + emblemAlpha.value * 0.12f
                },
            )
        }
    }
}

@Composable
private fun ExtensionBoosterStage(
    extension: ExtensionDefinition,
    extensionIndex: Int,
    heroProgress: Float,
    boosterIntroProgress: Float,
    boosterSelectionProgress: Float,
    drawLocked: Boolean,
    selectedBoosterIndex: Int?,
    isAwaitingPackResult: Boolean,
    onSelectBooster: (Int) -> Unit,
    onSelectedBoosterBoundsChanged: (PackRevealBounds?) -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val heroHeight = lerp(EXTENSION_CARD_HEIGHT, maxHeight, heroProgress)
        val heroTop = lerp(
            start = EXTENSION_LIST_TOP_PADDING +
                (EXTENSION_CARD_HEIGHT + EXTENSION_CARD_SPACING) * extensionIndex.toFloat(),
            stop = 0.dp,
            fraction = heroProgress,
        )

        MotionCard(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = heroTop)
                .fillMaxWidth()
                .height(heroHeight),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp),
            ) {
                Text(
                    text = extension.name,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .graphicsLayer {
                            alpha = (1f - boosterSelectionProgress).coerceIn(0f, 1f)
                            translationY = (1f - heroProgress) * 112f
                            translationX = (1f - heroProgress) * -76f
                        },
                )
                BoosterField(
                    extension = extension,
                    selectedBoosterIndex = selectedBoosterIndex,
                    drawLocked = drawLocked,
                    isAwaitingPackResult = isAwaitingPackResult,
                    onSelectBooster = onSelectBooster,
                    onSelectedBoosterBoundsChanged = onSelectedBoosterBoundsChanged,
                    introProgress = boosterIntroProgress,
                    selectionProgress = boosterSelectionProgress,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .fillMaxSize()
                        .padding(top = 88.dp, bottom = 18.dp),
                )
            }
        }
    }
}

@Composable
private fun BoosterField(
    extension: ExtensionDefinition,
    selectedBoosterIndex: Int?,
    drawLocked: Boolean,
    isAwaitingPackResult: Boolean,
    onSelectBooster: (Int) -> Unit,
    onSelectedBoosterBoundsChanged: (PackRevealBounds?) -> Unit,
    introProgress: Float = 1f,
    selectionProgress: Float = 0f,
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(selectedBoosterIndex) {
        if (selectedBoosterIndex == null) {
            onSelectedBoosterBoundsChanged(null)
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val horizontalGap = 18.dp
        val verticalGap = 20.dp
        val revealWidth = maxWidth * PACK_REVEAL_WIDTH_FRACTION
        val revealHeight = revealWidth / TRADING_CARD_WIDTH_OVER_HEIGHT
        val gridPackWidth = minOf(
            (maxWidth - horizontalGap) / 2,
            ((maxHeight - verticalGap) / 2) * TRADING_CARD_WIDTH_OVER_HEIGHT,
        )
        val gridPackHeight = gridPackWidth / TRADING_CARD_WIDTH_OVER_HEIGHT
        val gridStartX = (maxWidth - (gridPackWidth * 2 + horizontalGap)) / 2
        val gridStartY = (maxHeight - (gridPackHeight * 2 + verticalGap)) / 2
        val revealCenterX = maxWidth / 2
        val revealCenterY = maxHeight / 2

        repeat(4) { index ->
            val isSelected = selectedBoosterIndex == index
            val introReveal = if (selectedBoosterIndex == null) {
                ((introProgress - index * 0.18f) / 0.22f).coerceIn(0f, 1f)
            } else {
                1f
            }
            val row = index / 2
            val column = index % 2
            val startCenterX = gridStartX +
                gridPackWidth / 2 +
                if (column == 1) gridPackWidth + horizontalGap else 0.dp
            val startCenterY = gridStartY +
                gridPackHeight / 2 +
                if (row == 1) gridPackHeight + verticalGap else 0.dp
            val currentWidth = if (isSelected) {
                lerp(gridPackWidth, revealWidth, selectionProgress)
            } else {
                gridPackWidth
            }
            val currentHeight = currentWidth / TRADING_CARD_WIDTH_OVER_HEIGHT
            val currentCenterX = if (isSelected) {
                lerp(startCenterX, revealCenterX, selectionProgress)
            } else {
                startCenterX
            }
            val currentCenterY = if (isSelected) {
                lerp(startCenterY, revealCenterY, selectionProgress)
            } else {
                startCenterY
            }
            val alpha = if (selectedBoosterIndex == null || isSelected) {
                introReveal
            } else {
                (1f - selectionProgress * 1.4f).coerceAtLeast(0f) * introReveal
            }
            val visible = alpha > 0.01f &&
                (selectedBoosterIndex == null || isSelected || selectionProgress < 0.98f)
            if (!visible) return@repeat
            val packEnabled = !drawLocked &&
                !isAwaitingPackResult &&
                selectedBoosterIndex == null &&
                introReveal >= 0.98f

            key(index) {
                AnimatedExtensionPackCard(
                    extensionId = extension.id,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(
                            start = currentCenterX - currentWidth / 2,
                            top = currentCenterY - currentHeight / 2,
                        )
                        .graphicsLayer {
                            this.alpha = alpha
                            translationY = (1f - introReveal) * 54f
                        }
                        .size(width = currentWidth, height = currentHeight)
                        .then(
                            if (isSelected) {
                                Modifier.onGloballyPositioned { coordinates ->
                                    val bounds = coordinates.boundsInRoot()
                                    onSelectedBoosterBoundsChanged(
                                        PackRevealBounds(
                                            leftPx = bounds.left,
                                            topPx = bounds.top,
                                            widthPx = bounds.width,
                                            heightPx = bounds.height,
                                        ),
                                    )
                                }
                            } else {
                                Modifier
                            },
                        )
                        .testTag("pack-booster-$index")
                        .clickable(
                            enabled = packEnabled,
                            onClick = { onSelectBooster(index) },
                        ),
                    animationDelayMillis = 180 + index * 120,
                    animationKey = "pack-$index",
                    animationsEnabled = introReveal > 0f,
                )
            }
        }
    }
}

private val EXTENSION_CARD_HEIGHT: Dp = 164.dp
private val EXTENSION_CARD_SPACING: Dp = 12.dp
private val EXTENSION_LIST_TOP_PADDING: Dp = 44.dp

private fun formatNextDrawAt(nextDrawAt: String?): String? {
    val instant = nextDrawAt?.let { runCatching { Instant.parse(it) }.getOrNull() } ?: return null
    if (!instant.isAfter(Instant.now())) return null
    return DateTimeFormatter.ofPattern("dd/MM HH:mm")
        .withZone(ZoneId.systemDefault())
        .format(instant)
}
