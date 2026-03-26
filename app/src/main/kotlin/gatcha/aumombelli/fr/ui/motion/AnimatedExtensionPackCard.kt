package fr.aumombelli.gatcha.ui.motion

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@Composable
fun AnimatedExtensionPackCard(
    extensionId: String,
    modifier: Modifier = Modifier,
    animationDelayMillis: Int = 0,
    animationKey: Any? = Unit,
    animationsEnabled: Boolean = true,
    revealProgressOverride: Float? = null,
) {
    val spec = remember(extensionId) { extensionAnimationSpec(extensionId) }
    val lineProgress = remember(extensionId, animationKey) { Animatable(0f) }
    val neutralProgress = remember(extensionId, animationKey) { Animatable(0f) }
    var animationStarted by remember(extensionId, animationKey) { mutableStateOf(false) }

    LaunchedEffect(extensionId, animationDelayMillis, animationKey, animationsEnabled, revealProgressOverride) {
        if (revealProgressOverride != null) return@LaunchedEffect
        if (!animationsEnabled) return@LaunchedEffect
        if (animationStarted) return@LaunchedEffect

        animationStarted = true
        lineProgress.snapTo(0f)
        neutralProgress.snapTo(0f)
        delay(animationDelayMillis.toLong())
        when (spec.style) {
            ExtensionAnimationStyle.BigDipper -> {
                lineProgress.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 760, easing = FastOutSlowInEasing),
                )
            }

            ExtensionAnimationStyle.NeutralSky -> {
                neutralProgress.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 520, easing = FastOutSlowInEasing),
                )
            }
        }
    }

    MotionCard(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF152841),
                            Color(0xFF0D1827),
                            Color(0xFF050912),
                        ),
                    ),
                )
                .padding(14.dp),
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.12f),
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.18f),
                        ),
                    ),
                    cornerRadius = CornerRadius(size.minDimension * 0.12f, size.minDimension * 0.12f),
                )
                drawRoundRect(
                    color = Color(0x55F3D59F),
                    cornerRadius = CornerRadius(size.minDimension * 0.12f, size.minDimension * 0.12f),
                    style = Stroke(width = size.minDimension * 0.018f),
                )
                drawRoundRect(
                    color = Color.White.copy(alpha = 0.08f),
                    topLeft = Offset(size.width * 0.08f, size.height * 0.08f),
                    size = Size(size.width * 0.84f, size.height * 0.16f),
                    cornerRadius = CornerRadius(size.minDimension, size.minDimension),
                )
            }

            if (spec.style == ExtensionAnimationStyle.BigDipper) {
                ExtensionConstellationOverlay(
                    spec = spec,
                    lineProgress = revealProgressOverride ?: lineProgress.value,
                    isReversing = false,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 18.dp, vertical = 24.dp),
                    tag = null,
                )
            } else {
                NeutralPackGlyph(
                    progress = revealProgressOverride ?: neutralProgress.value,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 18.dp, vertical = 22.dp),
                )
            }
        }
    }
}

@Composable
private fun NeutralPackGlyph(
    progress: Float,
    modifier: Modifier = Modifier,
) {
    if (progress <= 0f) return

    Canvas(modifier = modifier) {
        val stars = listOf(
            Triple(0.28f, 0.28f, 0.048f),
            Triple(0.74f, 0.24f, 0.038f),
            Triple(0.36f, 0.58f, 0.032f),
            Triple(0.66f, 0.64f, 0.042f),
        )
        val alpha = progress.coerceIn(0f, 1f)
        stars.forEachIndexed { index, (x, y, radius) ->
            val localAlpha = ((alpha - index * 0.12f) / 0.5f).coerceIn(0f, 1f)
            if (localAlpha <= 0f) return@forEachIndexed
            val center = Offset(size.width * x, size.height * y)
            drawPath(
                path = starPath(
                    center = center,
                    points = 4,
                    outerRadius = size.minDimension * radius,
                    innerRadius = size.minDimension * radius * 0.42f,
                ),
                color = Color(0xFFEAF5FF).copy(alpha = localAlpha),
                style = Fill,
            )
            drawCircle(
                color = fr.aumombelli.gatcha.ui.theme.EmberGold.copy(alpha = localAlpha * 0.2f),
                radius = size.minDimension * radius * 1.8f,
                center = center,
            )
        }
        drawLine(
            color = Color(0x88D7E8FF).copy(alpha = alpha * 0.74f),
            start = Offset(size.width * 0.24f, size.height * 0.72f),
            end = Offset(size.width * 0.76f, size.height * 0.72f),
            strokeWidth = size.minDimension * 0.014f,
        )
        drawLine(
            color = Color(0x66D7E8FF).copy(alpha = alpha * 0.74f),
            start = Offset(size.width * 0.34f, size.height * 0.78f),
            end = Offset(size.width * 0.66f, size.height * 0.78f),
            strokeWidth = size.minDimension * 0.01f,
        )
    }
}
