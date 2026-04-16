package fr.aumombelli.dstcg.ui.motion

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

@Composable
fun EquipmentPortalOverlay(
    progress: Float,
    overlayAlpha: Float = 1f,
    modifier: Modifier = Modifier,
) {
    val visibleProgress = progress.coerceIn(0f, 1f)
    val travelProgress = calculateEquipmentPortalTravelProgress(visibleProgress)
    if (visibleProgress <= 0f) return
    val visibleAlpha = overlayAlpha.coerceIn(0f, 1f)
    if (visibleAlpha <= 0f) return

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("app-transition-equipment"),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = (24 + 272 * travelProgress).dp)
                .graphicsLayer {
                    alpha = visibleProgress * visibleAlpha
                }
                .width(244.dp)
                .height(220.dp),
        ) {
            EquipmentPortalFigure(
                progress = visibleProgress,
                modifier = Modifier
                    .align(Alignment.Center)
                    .width(196.dp)
                    .height(168.dp),
            )
        }
    }
}

@Composable
private fun EquipmentPortalFigure(
    progress: Float,
    modifier: Modifier = Modifier,
) {
    val pose = remember(progress) { calculateEquipmentPortalPose(progress) }

    Canvas(modifier = modifier) {
        drawTransitionEquipmentPanel(pose)
    }
}

internal data class EquipmentPortalPose(
    val lift: Float,
    val opening: Float,
    val shadowAlpha: Float,
    val glowAlpha: Float,
)

private fun DrawScope.drawTransitionEquipmentPanel(
    pose: EquipmentPortalPose,
) {
    val opening = pose.opening
    val panelCenter = Offset(
        x = size.width * 0.5f,
        y = size.height * scalarLerp(0.70f, 0.50f, pose.lift),
    )
    val panelWidth = size.width * scalarLerp(0.28f, 0.9f, opening)
    val panelHeight = size.height * scalarLerp(0.18f, 0.66f, opening)
    val panelRect = Rect(
        left = panelCenter.x - panelWidth / 2f,
        top = panelCenter.y - panelHeight / 2f,
        right = panelCenter.x + panelWidth / 2f,
        bottom = panelCenter.y + panelHeight / 2f,
    )
    val shadowWidth = panelWidth * scalarLerp(0.92f, 1.12f, opening)
    val shadowHeight = panelHeight * scalarLerp(0.30f, 0.16f, opening)
    val shadowCenter = Offset(
        x = panelCenter.x,
        y = panelRect.bottom + panelHeight * 0.15f,
    )
    val innerInset = panelWidth * scalarLerp(0.06f, 0.08f, opening)
    val innerRect = Rect(
        left = panelRect.left + innerInset,
        top = panelRect.top + panelHeight * 0.14f,
        right = panelRect.right - innerInset,
        bottom = panelRect.bottom - panelHeight * 0.12f,
    )
    val moduleGap = innerRect.width * 0.05f
    val fullModuleWidth = ((innerRect.width - moduleGap * 4f) / 3f).coerceAtLeast(0f)
    val visibleModuleWidth = fullModuleWidth * opening
    val moduleHeight = innerRect.height * 0.44f
    val moduleTop = innerRect.top + innerRect.height * 0.34f
    val headerWidth = innerRect.width * scalarLerp(0.16f, 0.62f, opening)
    val headerHeight = innerRect.height * 0.09f
    val headerLeft = panelCenter.x - headerWidth / 2f
    val accentLineWidth = innerRect.width * scalarLerp(0f, 0.38f, opening)
    val accentLineHeight = innerRect.height * 0.05f
    val accentLineLeft = panelCenter.x - accentLineWidth / 2f
    val indicatorRadius = innerRect.height * 0.055f * opening
    val moduleAccentColors = listOf(
        Color(0xFF63E0D7),
        Color(0xFFF0CC6A),
        Color(0xFFFF9B7A),
    )

    drawOval(
        brush = Brush.radialGradient(
            colors = listOf(
                Color.Black.copy(alpha = pose.shadowAlpha * 0.52f),
                Color.Black.copy(alpha = pose.shadowAlpha * 0.16f),
                Color.Transparent,
            ),
            center = shadowCenter,
            radius = shadowWidth * 0.72f,
        ),
        topLeft = Offset(
            shadowCenter.x - shadowWidth / 2f,
            shadowCenter.y - shadowHeight / 2f,
        ),
        size = Size(shadowWidth, shadowHeight),
    )

    drawRoundRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                Color(0xFF203245),
                Color(0xFF132130),
                Color(0xFF0C1724),
            ),
        ),
        topLeft = panelRect.topLeft,
        size = panelRect.size,
        cornerRadius = CornerRadius(panelHeight * 0.16f),
    )
    drawRoundRect(
        color = Color(0xFF9AD4FF).copy(alpha = 0.12f + pose.glowAlpha * 0.08f),
        topLeft = panelRect.topLeft,
        size = Size(panelRect.width, panelRect.height * 0.16f),
        cornerRadius = CornerRadius(panelHeight * 0.16f),
        style = Fill,
    )
    drawRoundRect(
        color = Color(0xFF08111A).copy(alpha = 0.82f),
        topLeft = innerRect.topLeft,
        size = innerRect.size,
        cornerRadius = CornerRadius(panelHeight * 0.11f),
    )
    drawRoundRect(
        color = Color(0xFFEAF6FF).copy(alpha = 0.16f + pose.glowAlpha * 0.10f),
        topLeft = panelRect.topLeft,
        size = panelRect.size,
        cornerRadius = CornerRadius(panelHeight * 0.16f),
        style = Stroke(width = panelHeight * 0.028f),
    )

    drawRoundRect(
        color = Color(0xFFBCE5FF).copy(alpha = 0.22f + opening * 0.28f),
        topLeft = Offset(headerLeft, innerRect.top + innerRect.height * 0.10f),
        size = Size(headerWidth, headerHeight),
        cornerRadius = CornerRadius(headerHeight * 0.6f),
    )
    drawRoundRect(
        color = Color(0xFFF0F7FF).copy(alpha = 0.16f + opening * 0.22f),
        topLeft = Offset(accentLineLeft, innerRect.top + innerRect.height * 0.22f),
        size = Size(accentLineWidth, accentLineHeight),
        cornerRadius = CornerRadius(accentLineHeight * 0.6f),
    )

    moduleAccentColors.forEachIndexed { index, accent ->
        val fullLeft = innerRect.left + moduleGap + index * (fullModuleWidth + moduleGap)
        val moduleLeft = fullLeft + (fullModuleWidth - visibleModuleWidth) / 2f
        val moduleRect = Rect(
            left = moduleLeft,
            top = moduleTop,
            right = moduleLeft + visibleModuleWidth,
            bottom = moduleTop + moduleHeight,
        )
        if (moduleRect.width <= 0f) return@forEachIndexed

        drawRoundRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    accent.copy(alpha = 0.28f + opening * 0.16f),
                    accent.copy(alpha = 0.12f + opening * 0.10f),
                ),
            ),
            topLeft = moduleRect.topLeft,
            size = moduleRect.size,
            cornerRadius = CornerRadius(moduleHeight * 0.24f),
        )
        drawRoundRect(
            color = accent.copy(alpha = 0.52f + opening * 0.18f),
            topLeft = Offset(
                moduleRect.left + moduleRect.width * 0.16f,
                moduleRect.top + moduleRect.height * 0.22f,
            ),
            size = Size(
                moduleRect.width * 0.68f,
                moduleRect.height * 0.10f,
            ),
            cornerRadius = CornerRadius(moduleHeight * 0.07f),
        )
        drawRoundRect(
            color = Color.White.copy(alpha = 0.10f + opening * 0.14f),
            topLeft = Offset(
                moduleRect.left + moduleRect.width * 0.16f,
                moduleRect.top + moduleRect.height * 0.42f,
            ),
            size = Size(
                moduleRect.width * 0.42f,
                moduleRect.height * 0.08f,
            ),
            cornerRadius = CornerRadius(moduleHeight * 0.05f),
        )
        drawCircle(
            color = accent.copy(alpha = 0.78f),
            radius = indicatorRadius,
            center = Offset(
                x = moduleRect.center.x,
                y = moduleRect.top - innerRect.height * 0.06f,
            ),
        )
    }
}

internal fun calculateEquipmentPortalPose(progress: Float): EquipmentPortalPose {
    val clampedProgress = progress.coerceIn(0f, 1f)
    val travelProgress = calculateEquipmentPortalTravelProgress(clampedProgress)
    val openingProgress = calculateEquipmentPortalOpeningProgress(clampedProgress)

    return EquipmentPortalPose(
        lift = travelProgress,
        opening = openingProgress,
        shadowAlpha = scalarLerp(0.16f, 0.34f, travelProgress * 0.65f + openingProgress * 0.35f),
        glowAlpha = scalarLerp(0f, 1f, openingProgress),
    )
}

internal fun calculateEquipmentPortalTravelProgress(progress: Float): Float {
    val clampedProgress = progress.coerceIn(0f, 1f)
    return when {
        clampedProgress <= EQUIPMENT_PORTAL_TRAVEL_HALF_PROGRESS -> {
            0.5f * easeInOutBurst(
                clampedProgress / EQUIPMENT_PORTAL_TRAVEL_HALF_PROGRESS,
            )
        }

        clampedProgress <= EQUIPMENT_PORTAL_TRAVEL_END_PROGRESS -> {
            0.5f + 0.5f * easeInOutBurst(
                (
                    (clampedProgress - EQUIPMENT_PORTAL_TRAVEL_HALF_PROGRESS) /
                        (EQUIPMENT_PORTAL_TRAVEL_END_PROGRESS - EQUIPMENT_PORTAL_TRAVEL_HALF_PROGRESS)
                    ).coerceIn(0f, 1f),
            )
        }

        else -> 1f
    }
}

internal fun calculateEquipmentPortalOpeningProgress(progress: Float): Float {
    val clampedProgress = progress.coerceIn(0f, 1f)
    return when {
        clampedProgress <= EQUIPMENT_PORTAL_OPENING_START_PROGRESS -> 0f

        clampedProgress <= EQUIPMENT_PORTAL_TRAVEL_END_PROGRESS -> {
            0.5f * easeInOutBurst(
                (
                    (clampedProgress - EQUIPMENT_PORTAL_OPENING_START_PROGRESS) /
                        (EQUIPMENT_PORTAL_TRAVEL_END_PROGRESS - EQUIPMENT_PORTAL_OPENING_START_PROGRESS)
                    ).coerceIn(0f, 1f),
            )
        }

        else -> {
            0.5f + 0.5f * easeInOutBurst(
                (
                    (clampedProgress - EQUIPMENT_PORTAL_TRAVEL_END_PROGRESS) /
                        (1f - EQUIPMENT_PORTAL_TRAVEL_END_PROGRESS)
                    ).coerceIn(0f, 1f),
            )
        }
    }
}

private const val EQUIPMENT_PORTAL_TRAVEL_HALF_PROGRESS = 1f / 3f
private const val EQUIPMENT_PORTAL_OPENING_START_PROGRESS = EQUIPMENT_PORTAL_TRAVEL_HALF_PROGRESS
private const val EQUIPMENT_PORTAL_TRAVEL_END_PROGRESS = 2f / 3f
