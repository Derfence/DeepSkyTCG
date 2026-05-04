package fr.aumombelli.dstcg.ui.motion

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import fr.aumombelli.dstcg.ui.theme.rarityBadgeStyle
import kotlinx.coroutines.delay

@Composable
fun AnimatedExtensionPackCard(
    extensionId: String,
    modifier: Modifier = Modifier,
    animationDelayMillis: Int = 0,
    animationKey: Any? = Unit,
    animationsEnabled: Boolean = true,
    revealProgressOverride: Float? = null,
    decorSeed: Any? = Unit,
    showContainerChrome: Boolean = true,
    isEpicBoosted: Boolean = false,
) {
    val spec = remember(extensionId) { extensionAnimationSpec(extensionId) }
    val decorSpec = remember(extensionId, decorSeed, isEpicBoosted) {
        packCardDecorSpec(
            seed = extensionId.hashCode() * 31 + (decorSeed?.hashCode() ?: 0),
            isEpicBoosted = isEpicBoosted,
        )
    }
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

            ExtensionAnimationStyle.Planet -> {
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

    val revealProgress = revealProgressOverride ?: when (spec.style) {
        ExtensionAnimationStyle.BigDipper -> lineProgress.value
        ExtensionAnimationStyle.Planet -> lineProgress.value
        ExtensionAnimationStyle.NeutralSky -> neutralProgress.value
    }

    if (showContainerChrome) {
        MotionCard(modifier = modifier) {
            AnimatedExtensionPackCardContent(
                spec = spec,
                decorSpec = decorSpec,
                revealProgress = revealProgress,
                lineProgress = revealProgressOverride ?: lineProgress.value,
                neutralProgress = revealProgressOverride ?: neutralProgress.value,
            )
        }
    } else {
        Box(modifier = modifier) {
            AnimatedExtensionPackCardContent(
                spec = spec,
                decorSpec = decorSpec,
                revealProgress = revealProgress,
                lineProgress = revealProgressOverride ?: lineProgress.value,
                neutralProgress = revealProgressOverride ?: neutralProgress.value,
            )
        }
    }
}

@Composable
internal fun GenericPackCardShell(
    decorSeed: Any? = Unit,
    revealProgress: Float = 1f,
    modifier: Modifier = Modifier,
    contentPadding: Dp = 14.dp,
    isEpicBoosted: Boolean = false,
) {
    val decorSpec = remember(decorSeed, isEpicBoosted) {
        packCardDecorSpec(
            seed = decorSeed?.hashCode() ?: 0,
            isEpicBoosted = isEpicBoosted,
        )
    }

    PackCardShellCanvas(
        decorSpec = decorSpec,
        revealProgress = revealProgress,
        modifier = modifier
            .fillMaxSize()
            .padding(contentPadding),
    )
}

@Composable
private fun AnimatedExtensionPackCardContent(
    spec: ExtensionAnimationSpec,
    decorSpec: PackCardDecorSpec,
    revealProgress: Float,
    lineProgress: Float,
    neutralProgress: Float,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp),
    ) {
        PackCardShellCanvas(
            decorSpec = decorSpec,
            revealProgress = revealProgress,
            modifier = Modifier.fillMaxSize(),
        )

        if (spec.style == ExtensionAnimationStyle.BigDipper || spec.style == ExtensionAnimationStyle.Planet) {
            ExtensionConstellationOverlay(
                spec = spec,
                lineProgress = lineProgress,
                isReversing = false,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 18.dp, vertical = 24.dp),
                tag = null,
            )
        } else {
            NeutralPackGlyph(
                progress = neutralProgress,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 18.dp, vertical = 22.dp),
            )
        }
    }
}

@Composable
private fun PackCardShellCanvas(
    decorSpec: PackCardDecorSpec,
    revealProgress: Float,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val toothDepth = size.height * decorSpec.tearBand.toothDepthFraction
        val topEdge = buildPackSawtoothEdgePoints(
            width = size.width,
            baselineY = toothDepth,
            tipY = 0f,
            toothCount = decorSpec.tearBand.toothCount,
        )
        val bottomEdge = buildPackSawtoothEdgePoints(
            width = size.width,
            baselineY = size.height - toothDepth,
            tipY = size.height,
            toothCount = decorSpec.tearBand.toothCount,
        )
        val packOutline = buildPackSawtoothOutlinePath(
            topEdge = topEdge,
            bottomEdge = bottomEdge,
        )

        clipPath(packOutline) {
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF182B45),
                        Color(0xFF08111D),
                    ),
                ),
            )
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0x553B72B7),
                        Color.Transparent,
                    ),
                    center = Offset(size.width * 0.5f, size.height * 0.5f),
                    radius = size.minDimension * 0.72f,
                ),
            )
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.Black.copy(alpha = 0.18f),
                        Color.Transparent,
                        Color.White.copy(alpha = 0.08f),
                    ),
                ),
            )

            for (i in 0 until topEdge.size - 2 step 2) {
                val p1 = topEdge[i]
                val p2 = topEdge[i + 1]
                val p3 = topEdge[i + 2]
                drawPath(
                    path = Path().apply {
                        moveTo(p1.x, p1.y)
                        lineTo(p2.x, p2.y)
                        lineTo(p3.x, p3.y)
                        close()
                    },
                    color = Color.Black.copy(alpha = 0.2f),
                )
            }
            for (i in 0 until bottomEdge.size - 2 step 2) {
                val p1 = bottomEdge[i]
                val p2 = bottomEdge[i + 1]
                val p3 = bottomEdge[i + 2]
                drawPath(
                    path = Path().apply {
                        moveTo(p1.x, p1.y)
                        lineTo(p2.x, p2.y)
                        lineTo(p3.x, p3.y)
                        close()
                    },
                    color = Color.White.copy(alpha = 0.08f),
                )
            }

            decorSpec.rarityStars.forEachIndexed { index, star ->
                val style = rarityBadgeStyle(star.rarityLabel)
                val staggeredReveal = ((revealProgress - index * 0.025f) / 0.55f).coerceIn(0f, 1f)
                if (staggeredReveal <= 0f) return@forEachIndexed

                val center = Offset(
                    x = size.width * star.xFraction,
                    y = size.height * star.yFraction,
                )
                val outerRadius = size.minDimension * star.radiusFraction
                val alpha = staggeredReveal * 0.96f
                drawPath(
                    path = starPath(
                        center = center,
                        points = style.branchCount,
                        outerRadius = outerRadius,
                        innerRadius = outerRadius * 0.42f,
                    ),
                    color = style.color.copy(alpha = alpha),
                    style = Fill,
                )
            }

            drawLine(
                color = rarityBadgeStyle("Rare").color.copy(alpha = 0.24f),
                start = Offset(topEdge.first().x, topEdge.first().y + toothDepth * 0.44f),
                end = Offset(topEdge.last().x, topEdge.last().y + toothDepth * 0.44f),
                strokeWidth = size.minDimension * 0.01f,
            )
            drawLine(
                color = rarityBadgeStyle("Rare").color.copy(alpha = 0.24f),
                start = Offset(bottomEdge.first().x, bottomEdge.first().y - toothDepth * 0.44f),
                end = Offset(bottomEdge.last().x, bottomEdge.last().y - toothDepth * 0.44f),
                strokeWidth = size.minDimension * 0.01f,
            )
        }

        drawPath(
            path = packOutline,
            color = rarityBadgeStyle("Rare").color.copy(alpha = 0.24f),
            style = Stroke(
                width = size.minDimension * 0.01f,
            ),
        )
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
                color = fr.aumombelli.dstcg.ui.theme.EmberGold.copy(alpha = localAlpha * 0.2f),
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
