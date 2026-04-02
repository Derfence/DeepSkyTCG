package fr.aumombelli.dstcg.feature.packs.selection

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import fr.aumombelli.dstcg.model.ExtensionDefinition
import fr.aumombelli.dstcg.ui.component.TRADING_CARD_WIDTH_OVER_HEIGHT
import fr.aumombelli.dstcg.ui.motion.AnimatedExtensionPackCard
import fr.aumombelli.dstcg.ui.motion.MotionCard
import fr.aumombelli.dstcg.ui.motion.PACK_REVEAL_WIDTH_FRACTION
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
                androidx.compose.material3.Text(
                    text = extension.name,
                    style = androidx.compose.material3.MaterialTheme.typography.headlineSmall,
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
        val revealWidth = minOf(
            maxWidth * PACK_REVEAL_WIDTH_FRACTION,
            maxHeight * TRADING_CARD_WIDTH_OVER_HEIGHT,
        )
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
                        .absoluteOffset(
                            x = currentCenterX - currentWidth / 2,
                            y = currentCenterY - currentHeight / 2,
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
