package fr.aumombelli.dstcg.feature.badges

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.min
import kotlin.math.sin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first

@Composable
internal fun BadgeUnlockCelebrationOverlay(
    badges: List<BadgeItem>,
    targetBounds: Rect?,
    visible: Boolean,
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!visible || badges.isEmpty()) return

    val sortedBadges = remember(badges) { sortBadgeCelebrationItems(badges) }
    val density = LocalDensity.current
    val glowSizePx = with(density) { 220.dp.toPx() }
    val coinSizePx = with(density) { 92.dp.toPx() }
    val landingGapPx = with(density) { 10.dp.toPx() }
    val titleGapPx = with(density) { 12.dp.toPx() }
    val latestTargetBounds by rememberUpdatedState(targetBounds)
    var rootSize by remember(sortedBadges) { mutableStateOf(IntSize.Zero) }
    var titleSize by remember(sortedBadges) { mutableStateOf(IntSize.Zero) }
    var resolvedTargetBounds by remember(sortedBadges) { mutableStateOf<Rect?>(null) }
    var animationStarted by remember(sortedBadges) { mutableStateOf(false) }
    val overlayAlpha = remember(sortedBadges) { Animatable(0f) }
    val flightProgress = remember(sortedBadges) { Animatable(0f) }
    val fadeOutProgress = remember(sortedBadges) { Animatable(0f) }

    LaunchedEffect(sortedBadges, visible) {
        if (!visible || animationStarted) return@LaunchedEffect
        if (rootSize == IntSize.Zero) {
            snapshotFlow { rootSize }
                .filter { it != IntSize.Zero }
                .first()
        }

        resolvedTargetBounds = latestTargetBounds ?: fallbackBadgeCelebrationBounds(rootSize)
        animationStarted = true
        overlayAlpha.snapTo(0f)
        flightProgress.snapTo(0f)
        fadeOutProgress.snapTo(0f)

        overlayAlpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
        )
        flightProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 860, easing = FastOutSlowInEasing),
        )
        delay(80)
        fadeOutProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing),
        )
        onFinished()
    }

    val target = resolvedTargetBounds ?: targetBounds ?: fallbackBadgeCelebrationBounds(rootSize)
    val overlayVisibility = overlayAlpha.value * (1f - fadeOutProgress.value)
    val travel = FastOutSlowInEasing.transform(flightProgress.value.coerceIn(0f, 1f))
    val bounce = celebrationBounce(flightProgress.value)
    val startCenter = badgeCelebrationStartCenter(rootSize)
    val endScale = badgeCelebrationEndScale(sortedBadges.size)
    val endCenter = badgeCelebrationEndCenter(
        targetBounds = target,
        displayedCoinSizePx = coinSizePx * endScale,
        gapPx = landingGapPx,
    )
    val glowCenterX = lerpFloat(startCenter.x, endCenter.x, travel)
    val glowCenterY = lerpFloat(startCenter.y, endCenter.y, travel)
    val startRadius = 132f
    val targetWidth = target.right - target.left
    val endRadius = min(targetWidth * 0.24f, 42f).coerceAtLeast(0f)
    val titleText = if (sortedBadges.size == 1) "Badge obtenu !" else "${sortedBadges.size} badges obtenus !"
    val initialCoinCenters = badgeCelebrationInitialCoinCenters(
        badgeCount = sortedBadges.size,
        startCenter = startCenter,
        radius = startRadius,
    )
    val coinCenters = sortedBadges.indices.map { index ->
        val endOffset = badgeCelebrationFanOffset(index = index, radius = endRadius)
        Offset(
            x = lerpFloat(
                start = initialCoinCenters[index].x,
                stop = endCenter.x + endOffset.x,
                fraction = travel,
            ),
            y = lerpFloat(
                start = initialCoinCenters[index].y,
                stop = endCenter.y + endOffset.y,
                fraction = travel,
            ),
        )
    }
    val coinScale = lerpFloat(
        start = 1.04f,
        stop = endScale,
        fraction = travel,
    ) * (1f + (bounce * 0.06f))
    val titleTopLeft = badgeCelebrationStaticTitleTopLeft(
        titleSize = titleSize,
        startCenter = startCenter,
        badgeCount = sortedBadges.size,
        radius = startRadius,
        displayedCoinSizePx = coinSizePx * 1.04f,
        gapPx = titleGapPx,
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { rootSize = it }
            .testTag("badge-unlock-celebration"),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.14f * overlayVisibility))
                .testTag("badge-unlock-celebration-scrim"),
        )

        Box(
            modifier = Modifier
                .size(220.dp)
                .align(Alignment.TopStart)
                .graphicsLayer {
                    this.alpha = 0.78f * overlayVisibility
                    translationX = glowCenterX - glowSizePx / 2f
                    translationY = glowCenterY - glowSizePx / 2f
                    scaleX = 0.92f + 0.14f * travel
                    scaleY = 0.92f + 0.14f * travel
                }
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0x55FFE9AE),
                            Color(0x22A6C9FF),
                            Color.Transparent,
                        ),
                    ),
                ),
        )

        Text(
            text = titleText,
            color = Color.White.copy(alpha = overlayVisibility),
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.3.sp,
            ),
            modifier = Modifier
                .align(Alignment.TopStart)
                .onSizeChanged { titleSize = it }
                .graphicsLayer {
                    this.alpha = overlayVisibility
                    translationX = titleTopLeft.x
                    translationY = titleTopLeft.y
                }
                .testTag("badge-unlock-celebration-title"),
        )

        sortedBadges.forEachIndexed { index, badge ->
            val coinCenter = coinCenters[index]
            Box(
                modifier = Modifier
                    .size(92.dp)
                    .align(Alignment.TopStart)
                    .graphicsLayer {
                        this.alpha = overlayVisibility
                        translationX = coinCenter.x - coinSizePx / 2f
                        translationY = coinCenter.y - coinSizePx / 2f
                        scaleX = coinScale
                        scaleY = coinScale
                    }
                    .testTag("badge-unlock-celebration-coin-${badge.id}"),
            ) {
                BadgeCoinFace(
                    badge = badge,
                    modifier = Modifier.fillMaxSize(),
                    logoSize = 58.dp,
                )
            }
        }
    }
}

private fun fallbackBadgeCelebrationBounds(rootSize: IntSize): Rect {
    val width = 180f
    val height = 60f
    val left = (rootSize.width - width) / 2f
    val top = rootSize.height * 0.56f
    return Rect(
        left = left,
        top = top,
        right = left + width,
        bottom = top + height,
    )
}

private fun celebrationBounce(progress: Float): Float {
    if (progress < 0.78f) return 0f
    val settleProgress = ((progress - 0.78f) / 0.22f).coerceIn(0f, 1f)
    return sin(settleProgress * PI.toFloat()).coerceAtLeast(0f) * (1f - settleProgress)
}

private fun lerpFloat(start: Float, stop: Float, fraction: Float): Float =
    start + (stop - start) * fraction
