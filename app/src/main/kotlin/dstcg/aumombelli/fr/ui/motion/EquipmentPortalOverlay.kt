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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.inset
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.lerp
import fr.aumombelli.dstcg.ui.component.EquipmentMountGlyphColors
import fr.aumombelli.dstcg.ui.component.drawEquipmentMountGlyph
import fr.aumombelli.dstcg.model.EquipmentType
import fr.aumombelli.dstcg.ui.component.equipmentCategoryColorTokens
import kotlin.math.sqrt

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
                .width(264.dp)
                .height(372.dp),
        ) {
            EquipmentPortalFigure(
                progress = visibleProgress,
                modifier = Modifier
                    .align(Alignment.Center)
                    .width(220.dp)
                    .height(324.dp),
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
    val roofOpening: Float,
    val instrumentReveal: Float,
    val shadowAlpha: Float,
    val glowAlpha: Float,
)

private fun DrawScope.drawTransitionEquipmentPanel(
    pose: EquipmentPortalPose,
) {
    val observatoryColors = equipmentCategoryColorTokens(EquipmentType.Observatory)
    val telescopeColors = equipmentCategoryColorTokens(EquipmentType.Telescope)
    val mountColors = equipmentCategoryColorTokens(EquipmentType.Mount)
    val houseCenter = Offset(
        x = size.width * 0.5f,
        y = size.height * scalarLerp(0.84f, 0.80f, pose.lift),
    )
    val bodyWidth = size.width * scalarLerp(0.34f, 0.56f, pose.lift)
    val bodyHeight = size.height * scalarLerp(0.24f, 0.40f, pose.lift)
    val bodyRect = Rect(
        left = houseCenter.x - bodyWidth / 2f,
        top = houseCenter.y - bodyHeight * 0.56f,
        right = houseCenter.x + bodyWidth / 2f,
        bottom = houseCenter.y + bodyHeight * 0.44f,
    )
    val roofThickness = bodyHeight * 0.18f
    val roofOverhang = bodyWidth * 0.09f
    val roofHingeY = bodyRect.top + roofThickness * 0.18f
    val closedRoofHeight = bodyHeight * 0.386f
    val leftRoofHinge = Offset(
        x = bodyRect.left - roofOverhang,
        y = roofHingeY,
    )
    val rightRoofHinge = Offset(
        x = bodyRect.right + roofOverhang,
        y = roofHingeY,
    )
    val roofRidge = Offset(
        x = bodyRect.center.x,
        y = roofHingeY - closedRoofHeight,
    )
    val shadowWidth = bodyWidth * scalarLerp(1.16f, 1.42f, pose.lift)
    val shadowHeight = bodyHeight * scalarLerp(0.28f, 0.18f, pose.roofOpening)
    val shadowCenter = Offset(
        x = bodyRect.center.x,
        y = bodyRect.bottom + bodyHeight * 0.18f,
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

    if (pose.roofOpening > 0f || pose.instrumentReveal > 0f) {
        val openingGlowCenter = Offset(
            x = bodyRect.center.x,
            y = bodyRect.top - bodyHeight * 0.06f,
        )
        val openingGlowRadius = bodyWidth * scalarLerp(
            0.22f,
            0.72f,
            pose.roofOpening * 0.55f + pose.instrumentReveal * 0.45f,
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    telescopeColors.accent.copy(alpha = pose.glowAlpha * 0.28f),
                    observatoryColors.accent.copy(alpha = pose.glowAlpha * 0.20f),
                    Color.Transparent,
                ),
                center = openingGlowCenter,
                radius = openingGlowRadius,
            ),
            radius = openingGlowRadius,
            center = openingGlowCenter,
        )
    }

    drawInstrumentBehindHouse(
        pose = pose,
        bodyRect = bodyRect,
    )

    drawRoundRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                Color(0xFF325B73),
                Color(0xFF22475D),
                Color(0xFF132B3E),
            ),
        ),
        topLeft = bodyRect.topLeft,
        size = bodyRect.size,
        cornerRadius = CornerRadius(bodyWidth * 0.11f),
    )
    drawRoundRect(
        color = observatoryColors.accentText.copy(alpha = 0.07f + pose.glowAlpha * 0.05f),
        topLeft = bodyRect.topLeft,
        size = Size(bodyRect.width, bodyRect.height * 0.22f),
        cornerRadius = CornerRadius(bodyWidth * 0.11f),
    )
    drawRoundRect(
        color = observatoryColors.iconStroke.copy(alpha = 0.14f + pose.glowAlpha * 0.10f),
        topLeft = bodyRect.topLeft,
        size = bodyRect.size,
        cornerRadius = CornerRadius(bodyWidth * 0.11f),
        style = Stroke(width = bodyHeight * 0.05f),
    )

    val atticWidth = bodyWidth * 0.52f
    val atticHeight = bodyHeight * 0.15f
    drawRoundRect(
        color = Color(0xFF102635).copy(alpha = 0.18f + pose.roofOpening * 0.28f),
        topLeft = Offset(
            x = bodyRect.center.x - atticWidth / 2f,
            y = bodyRect.top + bodyHeight * 0.04f,
        ),
        size = Size(atticWidth, atticHeight),
        cornerRadius = CornerRadius(atticHeight * 0.45f),
    )

    val windowWidth = bodyWidth * 0.16f
    val windowHeight = bodyHeight * 0.20f
    val windowTop = bodyRect.top + bodyHeight * 0.24f
    val leftWindowLeft = bodyRect.left + bodyWidth * 0.17f
    val rightWindowLeft = bodyRect.right - bodyWidth * 0.17f - windowWidth
    val windowColor = telescopeColors.accent.copy(alpha = 0.72f + pose.glowAlpha * 0.16f)
    drawRoundRect(
        color = windowColor,
        topLeft = Offset(leftWindowLeft, windowTop),
        size = Size(windowWidth, windowHeight),
        cornerRadius = CornerRadius(windowWidth * 0.18f),
    )
    drawRoundRect(
        color = windowColor,
        topLeft = Offset(rightWindowLeft, windowTop),
        size = Size(windowWidth, windowHeight),
        cornerRadius = CornerRadius(windowWidth * 0.18f),
    )

    val doorWidth = bodyWidth * 0.20f
    val doorHeight = bodyHeight * 0.46f
    val doorRect = Rect(
        left = bodyRect.center.x - doorWidth / 2f,
        top = bodyRect.bottom - doorHeight,
        right = bodyRect.center.x + doorWidth / 2f,
        bottom = bodyRect.bottom,
    )
    drawRoundRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                Color(0xFF17394D),
                Color(0xFF0D2231),
            ),
        ),
        topLeft = doorRect.topLeft,
        size = doorRect.size,
        cornerRadius = CornerRadius(doorWidth * 0.22f),
    )
    drawCircle(
        color = telescopeColors.accentText.copy(alpha = 0.84f),
        radius = doorWidth * 0.06f,
        center = Offset(
            x = doorRect.right - doorWidth * 0.20f,
            y = doorRect.center.y,
        ),
    )

    drawRoofPanel(
        hinge = leftRoofHinge,
        ridge = roofRidge,
        thickness = roofThickness,
        openingProgress = pose.roofOpening,
        rotationDegrees = -70f * pose.roofOpening,
        startColor = Color(0xFF8B5428).copy(alpha = 0.96f),
        endColor = Color(0xFF5A3216).copy(alpha = 0.92f),
        outlineColor = Color(0xFFE3C598).copy(alpha = 0.22f),
    )
    drawRoofPanel(
        hinge = rightRoofHinge,
        ridge = roofRidge,
        thickness = roofThickness,
        openingProgress = pose.roofOpening,
        rotationDegrees = 70f * pose.roofOpening,
        startColor = Color(0xFF8B5428).copy(alpha = 0.96f),
        endColor = Color(0xFF5A3216).copy(alpha = 0.92f),
        outlineColor = Color(0xFFE3C598).copy(alpha = 0.22f),
    )
    drawCircle(
        color = telescopeColors.accent.copy(alpha = 0.88f),
        radius = roofThickness * 0.16f,
        center = leftRoofHinge,
    )
    drawCircle(
        color = telescopeColors.accent.copy(alpha = 0.88f),
        radius = roofThickness * 0.16f,
        center = rightRoofHinge,
    )
}

internal fun calculateEquipmentPortalPose(progress: Float): EquipmentPortalPose {
    val clampedProgress = progress.coerceIn(0f, 1f)
    val travelProgress = calculateEquipmentPortalTravelProgress(clampedProgress)
    val openingProgress = calculateEquipmentPortalOpeningProgress(clampedProgress)
    val instrumentProgress = calculateEquipmentPortalInstrumentRevealProgress(clampedProgress)

    return EquipmentPortalPose(
        lift = travelProgress,
        roofOpening = openingProgress,
        instrumentReveal = instrumentProgress,
        shadowAlpha = scalarLerp(
            0.16f,
            0.34f,
            travelProgress * 0.55f + openingProgress * 0.25f + instrumentProgress * 0.20f,
        ),
        glowAlpha = (openingProgress * 0.35f + instrumentProgress * 0.65f).coerceIn(0f, 1f),
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

        clampedProgress <= EQUIPMENT_PORTAL_OPENING_END_PROGRESS -> easeInOutBurst(
            (
                (clampedProgress - EQUIPMENT_PORTAL_OPENING_START_PROGRESS) /
                    (EQUIPMENT_PORTAL_OPENING_END_PROGRESS - EQUIPMENT_PORTAL_OPENING_START_PROGRESS)
                ).coerceIn(0f, 1f),
        )

        else -> 1f
    }
}

internal fun calculateEquipmentPortalInstrumentRevealProgress(progress: Float): Float {
    val clampedProgress = progress.coerceIn(0f, 1f)
    return when {
        clampedProgress <= EQUIPMENT_PORTAL_INSTRUMENT_REVEAL_START_PROGRESS -> 0f
        else -> easeInOutBurst(
            (
                (clampedProgress - EQUIPMENT_PORTAL_INSTRUMENT_REVEAL_START_PROGRESS) /
                    (1f - EQUIPMENT_PORTAL_INSTRUMENT_REVEAL_START_PROGRESS)
                ).coerceIn(0f, 1f),
        )
    }
}

private fun DrawScope.drawInstrumentBehindHouse(
    pose: EquipmentPortalPose,
    bodyRect: Rect,
) {
    if (pose.instrumentReveal <= 0f) return

    val instrumentWidth = bodyRect.width * 0.84f
    val instrumentHeight = size.height * 0.30f
    val hiddenTop = bodyRect.top + instrumentHeight * 0.32f
    val revealTravel = instrumentHeight * 2.05f
    val instrumentLeft = bodyRect.center.x - instrumentWidth * 0.56f
    val instrumentTop = hiddenTop - revealTravel * pose.instrumentReveal
    val instrumentRight = instrumentLeft + instrumentWidth
    val instrumentBottom = instrumentTop + instrumentHeight
    val glyphAlpha = 0.20f + pose.instrumentReveal * 0.80f
    val telescopeColors = equipmentCategoryColorTokens(EquipmentType.Telescope)
    val mountColors = equipmentCategoryColorTokens(EquipmentType.Mount)
    val observatoryColors = equipmentCategoryColorTokens(EquipmentType.Observatory)
    val glyphColors = EquipmentMountGlyphColors(
        headColor = mountColors.accent.copy(alpha = glyphAlpha),
        bodyColor = mountColors.accent.copy(alpha = glyphAlpha),
        telescopeColor = telescopeColors.accent.copy(alpha = glyphAlpha),
        legColor = mountColors.accent.copy(alpha = glyphAlpha),
    )

    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                telescopeColors.accent.copy(alpha = pose.glowAlpha * 0.22f),
                observatoryColors.accent.copy(alpha = pose.glowAlpha * 0.14f),
                Color.Transparent,
            ),
            center = Offset(bodyRect.center.x, bodyRect.top + bodyRect.height * 0.08f),
            radius = instrumentWidth * 0.96f,
        ),
        radius = instrumentWidth * 0.96f,
        center = Offset(bodyRect.center.x, bodyRect.top + bodyRect.height * 0.08f),
    )

    inset(
        left = instrumentLeft,
        top = instrumentTop,
        right = size.width - instrumentRight,
        bottom = size.height - instrumentBottom,
    ) {
        drawEquipmentMountGlyph(
            colors = glyphColors,
            strokeWidth = minOf(size.width, size.height) * 0.08f,
        )
    }
}

private fun DrawScope.drawRoofPanel(
    hinge: Offset,
    ridge: Offset,
    thickness: Float,
    openingProgress: Float,
    rotationDegrees: Float,
    startColor: Color,
    endColor: Color,
    outlineColor: Color,
) {
    val roofColor = lerp(
        start = startColor,
        stop = endColor,
        fraction = (openingProgress * 0.82f).coerceIn(0f, 1f),
    )
    val roofOutline = outlineColor.copy(alpha = 0.16f + openingProgress * 0.08f)
    withTransform({
        rotate(
            degrees = rotationDegrees,
            pivot = hinge,
        )
    }) {
        val roofPath = roofPanelPath(
            start = hinge,
            end = ridge,
            thickness = thickness,
        )
        drawPath(
            path = roofPath,
            brush = Brush.linearGradient(
                colors = listOf(
                    roofColor,
                    roofColor.copy(alpha = 0.90f),
                    roofColor.copy(alpha = 0.84f),
                ),
                start = hinge,
                end = ridge,
            ),
        )
        drawPath(
            path = roofPath,
            color = roofOutline,
            style = Stroke(width = thickness * 0.10f),
        )
    }
}

private fun roofPanelPath(
    start: Offset,
    end: Offset,
    thickness: Float,
): Path {
    val dx = end.x - start.x
    val dy = end.y - start.y
    val length = sqrt(dx * dx + dy * dy).coerceAtLeast(0.0001f)
    val offsetX = -dy / length * thickness / 2f
    val offsetY = dx / length * thickness / 2f

    return Path().apply {
        moveTo(start.x + offsetX, start.y + offsetY)
        lineTo(end.x + offsetX, end.y + offsetY)
        lineTo(end.x - offsetX, end.y - offsetY)
        lineTo(start.x - offsetX, start.y - offsetY)
        close()
    }
}

private const val EQUIPMENT_PORTAL_TRAVEL_HALF_PROGRESS = 1f / 3f
private const val EQUIPMENT_PORTAL_OPENING_START_PROGRESS = EQUIPMENT_PORTAL_TRAVEL_HALF_PROGRESS
private const val EQUIPMENT_PORTAL_OPENING_END_PROGRESS = 2f / 3f
private const val EQUIPMENT_PORTAL_TRAVEL_END_PROGRESS = 2f / 3f
private const val EQUIPMENT_PORTAL_INSTRUMENT_REVEAL_START_PROGRESS = EQUIPMENT_PORTAL_TRAVEL_END_PROGRESS
