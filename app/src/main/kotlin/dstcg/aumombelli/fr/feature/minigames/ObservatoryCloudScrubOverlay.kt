package fr.aumombelli.dstcg.feature.minigames

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntOffset
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

@Composable
internal fun ObservatoryCloudScrubOverlay(
    onScrubCloud: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .testTag("observatory-cloud-scrub-overlay"),
    ) {
        val sceneSize = with(density) {
            Size(
                width = maxWidth.toPx(),
                height = maxHeight.toPx(),
            )
        }
        val unit = min(sceneSize.width, sceneSize.height * 0.82f)
        val band = observatoryCloudBand(
            sceneSize = sceneSize,
            unit = unit,
        )
        val interactionSource = remember { MutableInteractionSource() }

        Box(
            modifier = Modifier
                .offset {
                    IntOffset(
                        x = band.topLeft.x.roundToInt(),
                        y = band.topLeft.y.roundToInt(),
                    )
                }
                .requiredSize(
                    width = with(density) { band.size.width.toDp() },
                    height = with(density) { band.size.height.toDp() },
                )
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = { onScrubCloud(ObservatoryCloudTapScrubAmount) },
                )
                .pointerInput(onScrubCloud) {
                    detectDragGestures(
                        onDragStart = {
                            onScrubCloud(ObservatoryCloudTapScrubAmount)
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            onScrubCloud(observatoryCloudScrubAmountForDrag(dragAmount.distance()))
                        },
                    )
                }
                .testTag("observatory-cloud-scrub-zone"),
        )
    }
}

private fun androidx.compose.ui.geometry.Offset.distance(): Float =
    sqrt(x * x + y * y)
