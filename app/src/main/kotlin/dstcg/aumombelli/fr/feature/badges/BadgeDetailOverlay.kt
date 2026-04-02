package fr.aumombelli.dstcg.feature.badges

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.math.max
import kotlin.math.min

internal const val BadgeDetailAnimationDurationMillis: Int = 480

internal data class ActiveBadgeDetail(
    val badge: BadgeItem,
    val sourceBounds: Rect,
    val isExpanded: Boolean = false,
    val hasEntered: Boolean = false,
)

@Composable
internal fun BadgeDetailOverlay(
    detail: ActiveBadgeDetail,
    rootSize: IntSize,
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit,
) {
    if (rootSize == IntSize.Zero) return

    val density = LocalDensity.current
    val transition = updateTransition(
        targetState = detail.isExpanded,
        label = "badge-detail-visibility",
    )
    val progress by transition.animateFloat(
        transitionSpec = {
            tween(
                durationMillis = BadgeDetailAnimationDurationMillis,
                easing = FastOutSlowInEasing,
            )
        },
        label = "badge-detail-progress",
    ) { isVisible ->
        if (isVisible) 1f else 0f
    }

    val sourceCenter = detail.sourceBounds.center
    val targetCenterX = rootSize.width / 2f
    val targetCenterY = rootSize.height * 0.30f
    val sourceSizePx = max(detail.sourceBounds.width, detail.sourceBounds.height)
    val targetSizePx = min(
        rootSize.width * 0.46f,
        with(density) { 220.dp.toPx() },
    )
    val currentCenterX = lerpFloat(sourceCenter.x, targetCenterX, progress)
    val currentCenterY = lerpFloat(sourceCenter.y, targetCenterY, progress)
    val currentSizePx = lerpFloat(sourceSizePx, targetSizePx, progress)
    val currentTopLeftX = currentCenterX - currentSizePx / 2f
    val currentTopLeftY = currentCenterY - currentSizePx / 2f
    val currentLogoSize = with(density) { (currentSizePx * 0.6f).toDp() }.coerceIn(56.dp, 96.dp)
    val textAlpha = ((progress - 0.56f) / 0.34f).coerceIn(0f, 1f)
    val scrimAlpha = progress * 0.72f
    val targetTopPadding = with(density) {
        (targetCenterY + targetSizePx / 2f + 24f).toDp()
    }
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("badge-detail"),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = scrimAlpha))
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onDismiss,
                )
                .testTag("badge-detail-scrim"),
        )

        BadgeCoinFace(
            badge = detail.badge,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(
                    start = with(density) { currentTopLeftX.toDp() },
                    top = with(density) { currentTopLeftY.toDp() },
                )
                .size(with(density) { currentSizePx.toDp() }),
            logoSize = currentLogoSize,
        )

        Surface(
            shape = androidx.compose.foundation.shape.RoundedCornerShape(28.dp),
            color = Color.Black.copy(alpha = 0.38f),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(start = 20.dp, top = targetTopPadding, end = 20.dp)
                .fillMaxWidth()
                .alpha(textAlpha),
        ) {
            Box(
                modifier = Modifier
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xE5121B2A),
                                Color(0xD9080D16),
                            ),
                        ),
                    )
                    .padding(20.dp),
            ) {
                androidx.compose.foundation.layout.Column(
                    verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        text = "${detail.badge.extensionName} · ${detail.badge.title}",
                        color = Color.White,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.testTag("badge-detail-title"),
                    )
                    Text(
                        text = detail.badge.progress.label,
                        color = Color(0xFFF6D88A),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.testTag("badge-detail-progress"),
                    )
                    Text(
                        text = detail.badge.description,
                        color = Color(0xFFD5E4F7),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.testTag("badge-detail-description"),
                    )
                }
            }
        }
    }
}

private fun lerpFloat(
    start: Float,
    stop: Float,
    fraction: Float,
): Float = start + (stop - start) * fraction.coerceIn(0f, 1f)
