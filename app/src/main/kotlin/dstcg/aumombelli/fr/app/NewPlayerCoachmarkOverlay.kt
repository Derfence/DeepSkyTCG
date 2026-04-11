package fr.aumombelli.dstcg.app

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import kotlin.math.max
import kotlin.math.min

@Composable
internal fun NewPlayerCoachmarkOverlay(
    spec: NewPlayerCoachmarkSpec,
    targetBounds: Rect?,
    forceScrollDownHint: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val pulseTransition = rememberInfiniteTransition(label = "onboarding-coachmark-pulse")
    val pulseProgress = pulseTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "onboarding-coachmark-progress",
    )

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .testTag("new-player-coachmark-overlay"),
    ) {
        val bubbleWidthPx = with(density) { 244.dp.toPx() }
        val bubbleHeightPx = with(density) { 108.dp.toPx() }
        val haloPaddingPx = with(density) { 12.dp.toPx() }
        val rootWidthPx = with(density) { maxWidth.toPx() }
        val rootHeightPx = with(density) { maxHeight.toPx() }
        val horizontalMarginPx = with(density) { 16.dp.toPx() }
        val verticalMarginPx = with(density) { 20.dp.toPx() }
        val showScrollDownHint = forceScrollDownHint ||
            (targetBounds != null && targetBounds.top >= rootHeightPx)

        if (showScrollDownHint) {
            ScrollDownCoachmarkHint(
                pulseProgress = pulseProgress.value,
                modifier = Modifier.align(Alignment.Center),
            )
        } else if (targetBounds != null) {
            val targetWidthPx = targetBounds.width + haloPaddingPx * 2f
            val targetHeightPx = targetBounds.height + haloPaddingPx * 2f
            val targetCenterX = targetBounds.left + targetBounds.width / 2f
            val bubbleX = (targetCenterX - bubbleWidthPx / 2f)
                .coerceIn(
                    horizontalMarginPx,
                    max(horizontalMarginPx, rootWidthPx - bubbleWidthPx - horizontalMarginPx),
                )
            val bubbleAbove = targetBounds.top > rootHeightPx * 0.42f
            val bubbleY = if (bubbleAbove) {
                max(verticalMarginPx, targetBounds.top - bubbleHeightPx - verticalMarginPx)
            } else {
                min(rootHeightPx - bubbleHeightPx - verticalMarginPx, targetBounds.bottom + verticalMarginPx)
            }

            Box(
                modifier = Modifier
                    .size(
                        width = with(density) { targetWidthPx.toDp() },
                        height = with(density) { targetHeightPx.toDp() },
                    )
                    .graphicsLayer {
                        translationX = targetBounds.left - haloPaddingPx
                        translationY = targetBounds.top - haloPaddingPx
                        alpha = 0.32f + (1f - pulseProgress.value) * 0.42f
                        scaleX = 1f + pulseProgress.value * 0.08f
                        scaleY = 1f + pulseProgress.value * 0.08f
                    }
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0x66FFF1A8),
                                Color(0x2264C9FF),
                                Color.Transparent,
                            ),
                        ),
                    )
                    .border(
                        width = 2.dp,
                        color = Color(0xFFFFE79A),
                        shape = MaterialTheme.shapes.large,
                    )
                    .testTag("new-player-coachmark-target-${spec.target.name}"),
            )

            Surface(
                color = Color(0xEE0A1524),
                contentColor = Color.White,
                shadowElevation = 10.dp,
                shape = MaterialTheme.shapes.large,
                modifier = Modifier
                    .size(width = 244.dp, height = 108.dp)
                    .graphicsLayer {
                        translationX = bubbleX
                        translationY = bubbleY
                    }
                    .testTag("new-player-coachmark-${spec.target.name}"),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                ) {
                    Text(
                        text = spec.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = spec.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFD8E7F9),
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun ScrollDownCoachmarkHint(
    pulseProgress: Float,
    modifier: Modifier = Modifier,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(144.dp)
            .testTag("new-player-coachmark-scroll-down"),
    ) {
        Canvas(
            modifier = Modifier
                .size(84.dp)
                .graphicsLayer {
                    translationY = pulseProgress * 18f
                },
        ) {
            val strokeWidth = size.minDimension * 0.11f
            val leftX = size.width * 0.24f
            val centerX = size.width * 0.5f
            val rightX = size.width * 0.76f
            val topY = size.height * 0.18f
            val midY = size.height * 0.50f
            val bottomY = size.height * 0.82f
            drawLine(
                color = Color(0xFFFFE79A),
                start = androidx.compose.ui.geometry.Offset(leftX, topY),
                end = androidx.compose.ui.geometry.Offset(centerX, midY),
                strokeWidth = strokeWidth,
                cap = androidx.compose.ui.graphics.StrokeCap.Round,
            )
            drawLine(
                color = Color(0xFFFFE79A),
                start = androidx.compose.ui.geometry.Offset(centerX, midY),
                end = androidx.compose.ui.geometry.Offset(rightX, topY),
                strokeWidth = strokeWidth,
                cap = androidx.compose.ui.graphics.StrokeCap.Round,
            )
            drawLine(
                color = Color(0xFFFFE79A),
                start = androidx.compose.ui.geometry.Offset(leftX, midY - size.height * 0.08f),
                end = androidx.compose.ui.geometry.Offset(centerX, bottomY),
                strokeWidth = strokeWidth,
                cap = androidx.compose.ui.graphics.StrokeCap.Round,
            )
            drawLine(
                color = Color(0xFFFFE79A),
                start = androidx.compose.ui.geometry.Offset(centerX, bottomY),
                end = androidx.compose.ui.geometry.Offset(rightX, midY - size.height * 0.08f),
                strokeWidth = strokeWidth,
                cap = androidx.compose.ui.graphics.StrokeCap.Round,
            )
            drawCircle(
                color = Color(0x44FFF1A8),
                radius = size.minDimension * (0.50f + pulseProgress * 0.06f),
                style = Stroke(width = size.minDimension * 0.035f),
            )
        }
    }
}

private fun androidx.compose.ui.unit.Dp.toPx(density: Density): Float = with(density) { toPx() }
