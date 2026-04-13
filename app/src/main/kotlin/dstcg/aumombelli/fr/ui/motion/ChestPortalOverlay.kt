package fr.aumombelli.dstcg.ui.motion

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import kotlin.math.max

@Composable
fun ChestPortalOverlay(
    progress: Float,
    overlayAlpha: Float = 1f,
    modifier: Modifier = Modifier,
) {
    val visibleProgress = progress.coerceIn(0f, 1f)
    val travelProgress = calculateBadgeChestTravelProgress(visibleProgress)
    if (visibleProgress <= 0f) return
    val visibleAlpha = overlayAlpha.coerceIn(0f, 1f)
    if (visibleAlpha <= 0f) return

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("app-transition-chest"),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = (24 + 280 * travelProgress).dp)
                .graphicsLayer {
                    alpha = visibleProgress * visibleAlpha
                }
                .width(236.dp)
                .height(284.dp),
        ) {
            ChestPortalFigure(
                progress = visibleProgress,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .width(188.dp)
                    .height(236.dp),
            )
        }
    }
}

@Composable
private fun ChestPortalFigure(
    progress: Float,
    modifier: Modifier = Modifier,
) {
    val pose = remember(progress) { calculateBadgeChestPose(progress) }

    Canvas(modifier = modifier) {
        drawTransitionChest(pose)
    }
}

private fun DrawScope.drawTransitionChest(pose: BookPose) {
    val openness = (pose.openAngle / 142f).coerceIn(0f, 1f)
    val lidAngle = pose.openAngle * 0.42f
    val topHeadroom = size.height * 0.22f
    val contentHeight = size.height - topHeadroom
    val center = Offset(
        x = size.width * scalarLerp(0.5f, 0.53f, openness),
        y = topHeadroom + contentHeight * (0.78f - pose.lift * 0.08f),
    )
    val bodyWidth = size.width * scalarLerp(0.56f, 0.70f, openness)
    val bodyHeight = contentHeight * scalarLerp(0.24f, 0.30f, openness)
    val lidHeight = bodyHeight * 0.58f
    val bodyRect = Rect(
        left = center.x - bodyWidth / 2f,
        top = center.y - bodyHeight,
        right = center.x + bodyWidth / 2f,
        bottom = center.y,
    )
    val lidRect = Rect(
        left = bodyRect.left,
        top = bodyRect.top - lidHeight * 0.84f,
        right = bodyRect.right,
        bottom = bodyRect.top + lidHeight * 0.18f,
    )
    val hingePivot = lidRect.bottomLeft
    val shadowWidth = bodyWidth * scalarLerp(0.94f, 1.18f, openness)
    val shadowHeight = bodyHeight * 0.26f
    val shadowCenter = Offset(
        x = center.x + size.width * 0.02f * openness,
        y = center.y + bodyHeight * 0.18f,
    )

    drawOval(
        brush = Brush.radialGradient(
            colors = listOf(
                Color.Black.copy(alpha = pose.shadowAlpha * 0.58f),
                Color.Black.copy(alpha = pose.shadowAlpha * 0.2f),
                Color.Transparent,
            ),
            center = shadowCenter,
            radius = shadowWidth * 0.65f,
        ),
        topLeft = Offset(shadowCenter.x - shadowWidth / 2f, shadowCenter.y - shadowHeight / 2f),
        size = Size(shadowWidth, shadowHeight),
    )

    val glowAlpha = scalarLerp(0f, 0.9f, openness)
    drawOval(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFFFFD07A).copy(alpha = glowAlpha * 0.42f),
                Color(0x66E08735).copy(alpha = glowAlpha * 0.2f),
                Color.Transparent,
            ),
            center = Offset(center.x, bodyRect.top + bodyHeight * 0.28f),
            radius = max(bodyWidth, bodyHeight) * scalarLerp(0.34f, 0.88f, openness),
        ),
        topLeft = Offset(
            center.x - bodyWidth * 0.44f,
            bodyRect.top - bodyHeight * 0.18f,
        ),
        size = Size(bodyWidth * 0.88f, bodyHeight * 0.62f),
    )

    drawRoundRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                Color(0xFF1B0E06),
                Color(0xFF0D0603),
            ),
        ),
        topLeft = Offset(bodyRect.left + bodyWidth * 0.08f, bodyRect.top + bodyHeight * 0.08f),
        size = Size(bodyWidth * 0.84f, bodyHeight * 0.36f),
        cornerRadius = CornerRadius(bodyHeight * 0.12f),
    )

    drawChestPanel(
        rect = bodyRect,
        accentAlpha = 1f,
    )

    val lidBrush = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF8B5428),
            Color(0xFF5A3216),
            Color(0xFF32170A),
        ),
    )
    rotate(
        degrees = -lidAngle,
        pivot = hingePivot,
    ) {
        drawRoundRect(
            brush = lidBrush,
            topLeft = lidRect.topLeft,
            size = lidRect.size,
            cornerRadius = CornerRadius(lidHeight * 0.32f),
        )
        drawRoundRect(
            color = Color(0xFFD9A667).copy(alpha = 0.16f),
            topLeft = Offset(lidRect.left, lidRect.top),
            size = Size(lidRect.width, lidRect.height * 0.38f),
            cornerRadius = CornerRadius(lidHeight * 0.32f),
        )
        drawStraps(
            rect = lidRect,
            alpha = 0.96f,
        )
        drawRoundRect(
            color = Color(0xFF2C1C13),
            topLeft = Offset(lidRect.left, lidRect.bottom - lidHeight * 0.18f),
            size = Size(lidRect.width, lidHeight * 0.16f),
        )
        drawRoundRect(
            color = Color(0xFFE3C598).copy(alpha = 0.38f),
            topLeft = Offset(lidRect.left + lidRect.width * 0.12f, lidRect.top + lidRect.height * 0.16f),
            size = Size(lidRect.width * 0.76f, lidRect.height * 0.14f),
            cornerRadius = CornerRadius(lidHeight * 0.12f),
        )
    }
}

private fun DrawScope.drawChestPanel(
    rect: Rect,
    accentAlpha: Float,
) {
    drawRoundRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                Color(0xFF7C4A24),
                Color(0xFF562C12),
                Color(0xFF2C1208),
            ),
        ),
        topLeft = rect.topLeft,
        size = rect.size,
        cornerRadius = CornerRadius(rect.height * 0.12f),
    )
    drawRoundRect(
        color = Color(0xFFD4A16B).copy(alpha = 0.18f * accentAlpha),
        topLeft = rect.topLeft,
        size = Size(rect.width, rect.height * 0.24f),
        cornerRadius = CornerRadius(rect.height * 0.12f),
    )
    drawStraps(
        rect = rect,
        alpha = 1f,
    )
    drawRoundRect(
        color = Color(0xFF4C260F),
        topLeft = Offset(rect.left, rect.top + rect.height * 0.48f),
        size = Size(rect.width, rect.height * 0.1f),
    )
    drawRoundRect(
        color = Color(0xFFB88E59),
        topLeft = Offset(rect.center.x - rect.width * 0.08f, rect.top + rect.height * 0.3f),
        size = Size(rect.width * 0.16f, rect.height * 0.22f),
        cornerRadius = CornerRadius(rect.height * 0.08f),
        style = Fill,
    )
    drawRoundRect(
        color = Color(0xFF4D341E),
        topLeft = Offset(rect.center.x - rect.width * 0.04f, rect.top + rect.height * 0.36f),
        size = Size(rect.width * 0.08f, rect.height * 0.12f),
        cornerRadius = CornerRadius(rect.height * 0.04f),
    )
    drawRoundRect(
        color = Color(0xFFF2D5A9).copy(alpha = 0.14f),
        topLeft = Offset(rect.left + rect.width * 0.14f, rect.top + rect.height * 0.12f),
        size = Size(rect.width * 0.72f, rect.height * 0.08f),
        cornerRadius = CornerRadius(rect.height * 0.04f),
    )
}

private fun DrawScope.drawStraps(
    rect: Rect,
    alpha: Float,
) {
    val strapColor = Color(0xFF7B8595).copy(alpha = alpha)
    val edgeColor = Color(0xFFE4EAF6).copy(alpha = alpha * 0.3f)
    val strapWidth = rect.width * 0.12f
    listOf(
        rect.left + rect.width * 0.16f,
        rect.right - rect.width * 0.28f,
    ).forEach { left ->
        drawRoundRect(
            color = strapColor,
            topLeft = Offset(left, rect.top),
            size = Size(strapWidth, rect.height),
            cornerRadius = CornerRadius(rect.height * 0.08f),
        )
        drawRoundRect(
            color = edgeColor,
            topLeft = Offset(left, rect.top),
            size = Size(strapWidth, rect.height * 0.16f),
            cornerRadius = CornerRadius(rect.height * 0.08f),
        )
    }
    drawRoundRect(
        color = Color(0x883A4150).copy(alpha = alpha),
        topLeft = Offset(rect.left, rect.center.y - rect.height * 0.05f),
        size = Size(rect.width, rect.height * 0.1f),
    )
    drawRoundRect(
        color = Color(0xFFE6D4B7).copy(alpha = alpha * 0.12f),
        topLeft = Offset(rect.left, rect.top),
        size = Size(rect.width, rect.height * 0.06f),
    )
    drawRoundRect(
        color = Color(0xFF14181D).copy(alpha = alpha * 0.5f),
        topLeft = Offset(rect.left, rect.top),
        size = rect.size,
        cornerRadius = CornerRadius(rect.height * 0.12f),
        style = Stroke(width = rect.height * 0.03f),
    )
}

internal fun calculateBadgeChestPose(progress: Float): BookPose {
    val clampedProgress = progress.coerceIn(0f, 1f)
    val travelProgress = calculateBadgeChestTravelProgress(clampedProgress)
    val openingProgress = smoothBadgeChestPhase(
        calculateBadgeChestOpeningProgress(clampedProgress),
    )

    return BookPose(
        lift = travelProgress,
        pitchX = scalarLerp(12f, 10f, openingProgress),
        yawY = scalarLerp(-14f, -8f, openingProgress),
        openAngle = 142f * openingProgress,
        pageFan = scalarLerp(0f, 12f, openingProgress),
        spreadWidth = scalarLerp(0.78f, 1.18f, openingProgress),
        shadowAlpha = scalarLerp(0.18f, 0.34f, travelProgress * 0.7f + openingProgress * 0.3f),
        frontCoverDominance = scalarLerp(1f, 0.42f, openingProgress).coerceIn(0.3f, 1f),
    )
}

internal fun calculateBadgeChestTravelProgress(progress: Float): Float {
    val clampedProgress = progress.coerceIn(0f, 1f)
    return when {
        clampedProgress <= BADGE_CHEST_TRAVEL_HALF_PROGRESS -> {
            0.5f * smoothBadgeChestPhase(
                clampedProgress / BADGE_CHEST_TRAVEL_HALF_PROGRESS,
            )
        }

        clampedProgress <= BADGE_CHEST_TRAVEL_END_PROGRESS -> {
            0.5f + 0.5f * smoothBadgeChestPhase(
                (
                    (clampedProgress - BADGE_CHEST_TRAVEL_HALF_PROGRESS) /
                        (BADGE_CHEST_TRAVEL_END_PROGRESS - BADGE_CHEST_TRAVEL_HALF_PROGRESS)
                    ).coerceIn(0f, 1f),
            )
        }

        else -> 1f
    }
}

internal fun calculateBadgeChestOpeningProgress(progress: Float): Float {
    val clampedProgress = progress.coerceIn(0f, 1f)
    return when {
        clampedProgress <= BADGE_CHEST_OPENING_START_PROGRESS -> 0f

        clampedProgress <= BADGE_CHEST_TRAVEL_END_PROGRESS -> {
            0.5f * smoothBadgeChestPhase(
                (
                    (clampedProgress - BADGE_CHEST_OPENING_START_PROGRESS) /
                        (BADGE_CHEST_TRAVEL_END_PROGRESS - BADGE_CHEST_OPENING_START_PROGRESS)
                    ).coerceIn(0f, 1f),
            )
        }

        else -> {
            0.5f + 0.5f * smoothBadgeChestPhase(
                (
                    (clampedProgress - BADGE_CHEST_TRAVEL_END_PROGRESS) /
                        (1f - BADGE_CHEST_TRAVEL_END_PROGRESS)
                    ).coerceIn(0f, 1f),
            )
        }
    }
}

private fun smoothBadgeChestPhase(progress: Float): Float {
    val clampedProgress = progress.coerceIn(0f, 1f)
    return clampedProgress * clampedProgress * (3f - 2f * clampedProgress)
}

private const val BADGE_CHEST_TRAVEL_HALF_PROGRESS = 1f / 3f
private const val BADGE_CHEST_OPENING_START_PROGRESS = BADGE_CHEST_TRAVEL_HALF_PROGRESS
private const val BADGE_CHEST_TRAVEL_END_PROGRESS = 2f / 3f
