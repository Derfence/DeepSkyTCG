package fr.aumombelli.dstcg.feature.packs.selection

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import fr.aumombelli.dstcg.ui.motion.BrandLogoVariant
import fr.aumombelli.dstcg.ui.motion.ExtensionAnimationStyle
import fr.aumombelli.dstcg.ui.motion.LaunchLogoMark
import fr.aumombelli.dstcg.ui.motion.extensionAnimationSpec
import fr.aumombelli.dstcg.ui.motion.extensionLineReveal
import fr.aumombelli.dstcg.ui.motion.extensionPointReveal
import fr.aumombelli.dstcg.ui.motion.projectExtensionPattern
import kotlinx.coroutines.delay

@Composable
internal fun ExtensionAnimatedBadge(
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
                variant = BrandLogoVariant.Badge17,
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
