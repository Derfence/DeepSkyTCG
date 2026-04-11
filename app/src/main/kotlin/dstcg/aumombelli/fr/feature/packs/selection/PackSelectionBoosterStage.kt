package fr.aumombelli.dstcg.feature.packs.selection

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import fr.aumombelli.dstcg.model.ExtensionDefinition
import fr.aumombelli.dstcg.ui.component.TRADING_CARD_WIDTH_OVER_HEIGHT
import fr.aumombelli.dstcg.ui.motion.AnimatedExtensionPackCard
import fr.aumombelli.dstcg.ui.motion.MotionCard
import fr.aumombelli.dstcg.ui.motion.PackRevealBounds

@Composable
internal fun ExtensionBoosterStage(
    extension: ExtensionDefinition,
    extensionIndex: Int,
    heroProgress: Float,
    boosterIntroProgress: Float,
    boosterSelectionProgress: Float,
    drawLocked: Boolean,
    selectedBoosterIndex: Int?,
    isAwaitingPackResult: Boolean,
    screenBounds: Rect? = null,
    selectedBoosterTargetBounds: PackRevealBounds? = null,
    onSelectBooster: (Int) -> Unit,
    onSelectedBoosterBoundsChanged: (PackRevealBounds?) -> Unit,
    onFirstBoosterBoundsChanged: (Rect?) -> Unit = {},
    backgroundOnly: Boolean = false,
    modifier: Modifier = Modifier,
) {
    var stageLeftInsetPx by remember { mutableFloatStateOf(0f) }
    var stageRightInsetPx by remember { mutableFloatStateOf(0f) }
    var stageTopInsetPx by remember { mutableFloatStateOf(0f) }
    var stageBottomInsetPx by remember { mutableFloatStateOf(0f) }
    var boosterBoundsByIndex by remember(extension.id) { mutableStateOf<Map<Int, PackRevealBounds>>(emptyMap()) }
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .testTag("pack-extension-stage")
            .onGloballyPositioned { coordinates ->
                val stageBounds = coordinates.boundsInRoot()
                val currentScreenBounds = screenBounds
                stageLeftInsetPx = if (currentScreenBounds != null) {
                    (stageBounds.left - currentScreenBounds.left).coerceAtLeast(0f)
                } else {
                    0f
                }
                stageRightInsetPx = if (currentScreenBounds != null) {
                    (currentScreenBounds.right - stageBounds.right).coerceAtLeast(0f)
                } else {
                    0f
                }
                stageTopInsetPx = if (currentScreenBounds != null) {
                    (stageBounds.top - currentScreenBounds.top).coerceAtLeast(0f)
                } else {
                    0f
                }
                stageBottomInsetPx = if (currentScreenBounds != null) {
                    (currentScreenBounds.bottom - stageBounds.bottom).coerceAtLeast(0f)
                } else {
                    0f
                }
            },
    ) {
        val stageLeftInset = with(LocalDensity.current) { stageLeftInsetPx.toDp() }
        val stageRightInset = with(LocalDensity.current) { stageRightInsetPx.toDp() }
        val stageTopInset = with(LocalDensity.current) { stageTopInsetPx.toDp() }
        val stageBottomInset = with(LocalDensity.current) { stageBottomInsetPx.toDp() }
        val layoutDirection = LocalLayoutDirection.current
        val safeInsets = WindowInsets.safeDrawing.asPaddingValues()
        val safeStartInset = safeInsets.calculateLeftPadding(layoutDirection)
        val safeTopInset = safeInsets.calculateTopPadding()
        val safeEndInset = safeInsets.calculateRightPadding(layoutDirection)
        val safeBottomInset = safeInsets.calculateBottomPadding()
        val screenWidth = maxWidth + stageLeftInset + stageRightInset
        val screenHeight = maxHeight + stageTopInset + stageBottomInset
        val stageChrome = calculatePackSelectionBoosterStageChrome(
            screenWidth = screenWidth,
            screenHeight = screenHeight,
            safeStartInset = safeStartInset,
            safeTopInset = safeTopInset,
            safeEndInset = safeEndInset,
            safeBottomInset = safeBottomInset,
            stageStartInset = stageLeftInset,
            stageTopInset = stageTopInset,
            stageEndInset = stageRightInset,
            stageBottomInset = stageBottomInset,
            heroProgress = heroProgress,
        )
        val heroWidth = lerp(
            maxWidth,
            maxWidth + stageLeftInset + stageRightInset,
            heroProgress,
        )
        val heroHeight = lerp(
            EXTENSION_CARD_HEIGHT,
            maxHeight + stageTopInset + stageBottomInset,
            heroProgress,
        )
        val heroLeft = lerp(
            start = 0.dp,
            stop = -stageLeftInset,
            fraction = heroProgress,
        )
        val heroTop = lerp(
            start = EXTENSION_LIST_TOP_PADDING +
                (EXTENSION_CARD_HEIGHT + EXTENSION_CARD_SPACING) * extensionIndex.toFloat(),
            stop = -stageTopInset,
            fraction = heroProgress,
        )

        OverflowTopAlignedHero(
            modifier = Modifier
                .align(Alignment.TopStart)
                .absoluteOffset(x = heroLeft, y = heroTop)
                .fillMaxWidth(),
            width = heroWidth,
            height = heroHeight,
        ) {
            MotionCard(
                modifier = Modifier.fillMaxSize(),
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                ) {
                    if (!backgroundOnly) {
                        androidx.compose.material3.Text(
                            text = extension.name,
                            style = androidx.compose.material3.MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .absoluteOffset(y = stageChrome.titleTopPadding)
                                .testTag("pack-extension-title")
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
                            onBoosterBoundsChanged = { index, bounds ->
                                boosterBoundsByIndex = boosterBoundsByIndex.toMutableMap().also { current ->
                                    if (bounds == null) {
                                        current.remove(index)
                                    } else {
                                        current[index] = bounds
                                    }
                                }
                            },
                            onFirstBoosterBoundsChanged = onFirstBoosterBoundsChanged,
                            introProgress = boosterIntroProgress,
                            selectionProgress = boosterSelectionProgress,
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .fillMaxSize()
                                .padding(
                                    start = stageChrome.fieldStartPadding,
                                    top = stageChrome.fieldTopPadding,
                                    end = stageChrome.fieldEndPadding,
                                    bottom = stageChrome.fieldBottomPadding,
                                ),
                        )
                        val selectedStartBounds = selectedBoosterIndex?.let(boosterBoundsByIndex::get)
                        if (
                            selectedBoosterIndex != null &&
                            selectedStartBounds != null &&
                            selectedBoosterTargetBounds != null
                        ) {
                            SelectedBoosterOverlay(
                                extensionId = extension.id,
                                boosterIndex = selectedBoosterIndex,
                                startBounds = selectedStartBounds,
                                targetBounds = selectedBoosterTargetBounds,
                                selectionProgress = boosterSelectionProgress,
                                onBoundsChanged = onSelectedBoosterBoundsChanged,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OverflowTopAlignedHero(
    width: androidx.compose.ui.unit.Dp,
    height: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    Layout(
        modifier = modifier,
        content = {
            Box(
                modifier = Modifier
                    .width(width)
                    .height(height)
                    .testTag("pack-extension-hero"),
                content = content,
            )
        },
    ) { measurables, constraints ->
        val childConstraints = constraints.copy(
            minWidth = width.roundToPx(),
            maxWidth = width.roundToPx(),
            minHeight = height.roundToPx(),
            maxHeight = height.roundToPx(),
        )
        val placeable = measurables.single().measure(childConstraints)
        layout(
            width = constraints.maxWidth,
            height = constraints.maxHeight,
        ) {
            placeable.placeRelative(0, 0)
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
    onBoosterBoundsChanged: (Int, PackRevealBounds?) -> Unit,
    onFirstBoosterBoundsChanged: (Rect?) -> Unit,
    introProgress: Float = 1f,
    selectionProgress: Float = 0f,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(
        modifier = modifier.fillMaxSize(),
    ) {
        val introSequenceComplete = introProgress >= 0.99f
        var firstBoosterCoachmarkReady by remember(extension.id) { mutableStateOf(false) }
        var firstBoosterBounds by remember(extension.id) { mutableStateOf<Rect?>(null) }
        val gridMetrics = calculatePackSelectionBoosterGridMetrics(
            availableWidth = maxWidth,
            availableHeight = maxHeight,
        )

        LaunchedEffect(introSequenceComplete, selectedBoosterIndex, drawLocked, isAwaitingPackResult) {
            val canShowCoachmark = introSequenceComplete &&
                selectedBoosterIndex == null &&
                !drawLocked &&
                !isAwaitingPackResult
            firstBoosterCoachmarkReady = canShowCoachmark
        }

        LaunchedEffect(firstBoosterCoachmarkReady, firstBoosterBounds) {
            onFirstBoosterBoundsChanged(
                if (firstBoosterCoachmarkReady) firstBoosterBounds else null,
            )
        }

        repeat(4) { index ->
            val isSelected = selectedBoosterIndex == index
            val introReveal = if (selectedBoosterIndex == null) {
                ((introProgress - index * 0.18f) / 0.22f).coerceIn(0f, 1f)
            } else {
                1f
            }
            val row = index / 2
            val column = index % 2
            val startCenterX = gridMetrics.gridStartX +
                gridMetrics.gridPackWidth / 2 +
                if (column == 1) gridMetrics.gridPackWidth + gridMetrics.horizontalGap else 0.dp
            val startCenterY = gridMetrics.gridStartY +
                gridMetrics.gridPackHeight / 2 +
                if (row == 1) gridMetrics.gridPackHeight + gridMetrics.verticalGap else 0.dp
            val currentWidth = gridMetrics.gridPackWidth
            val currentHeight = currentWidth / TRADING_CARD_WIDTH_OVER_HEIGHT
            val currentCenterX = startCenterX
            val currentCenterY = startCenterY
            val alpha = if (selectedBoosterIndex == null || isSelected) {
                introReveal
            } else {
                (1f - selectionProgress * 1.4f).coerceAtLeast(0f) * introReveal
            }
            val visible = alpha > 0.01f &&
                (selectedBoosterIndex == null || !isSelected || selectionProgress < 0.02f)
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
                        .absoluteOffset(
                            x = currentCenterX - currentWidth / 2,
                            y = currentCenterY - currentHeight / 2,
                        )
                        .graphicsLayer {
                            this.alpha = alpha
                            translationY = (1f - introReveal) * 54f
                        }
                        .size(width = currentWidth, height = currentHeight)
                        .onGloballyPositioned { coordinates ->
                            val bounds = coordinates.boundsInRoot()
                            if (index == 0) {
                                firstBoosterBounds = bounds
                            }
                            onBoosterBoundsChanged(
                                index,
                                PackRevealBounds(
                                    leftPx = bounds.left,
                                    topPx = bounds.top,
                                    widthPx = bounds.width,
                                    heightPx = bounds.height,
                                ),
                            )
                        }
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

@Composable
private fun SelectedBoosterOverlay(
    extensionId: String,
    boosterIndex: Int,
    startBounds: PackRevealBounds,
    targetBounds: PackRevealBounds,
    selectionProgress: Float,
    onBoundsChanged: (PackRevealBounds?) -> Unit,
) {
    val density = LocalDensity.current
    val currentLeft = with(density) {
        lerp(startBounds.leftPx.toDp(), targetBounds.leftPx.toDp(), selectionProgress)
    }
    val currentTop = with(density) {
        lerp(startBounds.topPx.toDp(), targetBounds.topPx.toDp(), selectionProgress)
    }
    val currentWidth = with(density) {
        lerp(startBounds.widthPx.toDp(), targetBounds.widthPx.toDp(), selectionProgress)
    }
    val currentHeight = currentWidth / TRADING_CARD_WIDTH_OVER_HEIGHT

    AnimatedExtensionPackCard(
        extensionId = extensionId,
        modifier = Modifier
            .absoluteOffset(x = currentLeft, y = currentTop)
            .size(width = currentWidth, height = currentHeight)
            .onGloballyPositioned { coordinates ->
                val bounds = coordinates.boundsInRoot()
                onBoundsChanged(
                    PackRevealBounds(
                        leftPx = bounds.left,
                        topPx = bounds.top,
                        widthPx = bounds.width,
                        heightPx = bounds.height,
                    ),
                )
            }
            .testTag("pack-booster-$boosterIndex"),
        animationKey = "selected-pack-overlay-$boosterIndex",
        revealProgressOverride = 1f,
    )
}
