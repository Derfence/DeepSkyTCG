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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import fr.aumombelli.dstcg.ui.motion.BrandLogoVariant
import fr.aumombelli.dstcg.ui.motion.ExtensionPatternDrawStyle
import fr.aumombelli.dstcg.ui.motion.ExtensionAnimationStyle
import fr.aumombelli.dstcg.ui.motion.LaunchLogoMark
import fr.aumombelli.dstcg.ui.motion.drawExtensionPattern
import fr.aumombelli.dstcg.ui.motion.extensionAnimationSpec
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
            ExtensionAnimationStyle.Planet -> {
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
        if (spec.style == ExtensionAnimationStyle.BigDipper || spec.style == ExtensionAnimationStyle.Planet) {
            Canvas(
                modifier = Modifier.fillMaxSize(),
            ) {
                drawExtensionPattern(
                    spec = spec,
                    lineProgress = lineProgress.value,
                    isReversing = false,
                    revealWindow = 0.28f,
                    style = ExtensionPatternDrawStyle(
                        pointCoreBaseRadiusFraction = 0.026f,
                        pointCoreRevealRadiusFraction = 0.012f,
                        pointHaloRadiusFraction = 0.074f,
                        pointHaloAlphaMultiplier = 0.32f,
                        lineStrokeWidthFraction = 0.038f,
                        lineAlphaMultiplier = 0.92f,
                        orbitStrokeWidthFraction = 0.028f,
                        orbitAlphaMultiplier = 0.72f,
                        pointColor = Color.White,
                        pointHaloColor = Color(0xFFFFC85A),
                        lineColor = Color(0xFFE2F0FF),
                        orbitColor = Color(0xFFE2F0FF),
                    ),
                )
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
