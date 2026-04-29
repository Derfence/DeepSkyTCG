package fr.aumombelli.dstcg.ui.component

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate

internal data class EquipmentMountGlyphColors(
    val headColor: Color,
    val bodyColor: Color,
    val telescopeColor: Color,
    val legColor: Color,
) {
    companion object {
        fun monochrome(color: Color): EquipmentMountGlyphColors = EquipmentMountGlyphColors(
            headColor = color,
            bodyColor = color,
            telescopeColor = color,
            legColor = color,
        )
    }
}

internal fun DrawScope.drawEquipmentObservatoryGlyph(
    strokeColor: Color,
    strokeWidth: Float,
) {
    drawRoundRect(
        color = strokeColor,
        topLeft = Offset(size.width * 0.2f, size.height * 0.62f),
        size = Size(size.width * 0.6f, size.height * 0.14f),
        cornerRadius = CornerRadius(size.width * 0.08f, size.width * 0.08f),
    )
    drawArc(
        color = strokeColor,
        startAngle = 180f,
        sweepAngle = 180f,
        useCenter = false,
        topLeft = Offset(size.width * 0.2f, size.height * 0.26f),
        size = Size(size.width * 0.6f, size.height * 0.52f),
        style = Stroke(width = strokeWidth),
    )
    drawLine(
        color = strokeColor,
        start = Offset(size.width * 0.75f, size.height * 0.2f),
        end = Offset(size.width * 0.7f, size.height * 0.25f),
        strokeWidth = strokeWidth * 1.5f,
        cap = StrokeCap.Square,
    )
    drawCircle(
        color = strokeColor,
        radius = strokeWidth * 0.55f,
        center = Offset(size.width * 0.74f, size.height * 0.22f),
    )
}

internal fun DrawScope.drawEquipmentTelescopeGlyph(
    strokeColor: Color,
    strokeWidth: Float,
) {
    val bodyCenter = Offset(size.width * 0.48f, size.height * 0.46f)
    rotate(
        degrees = -45f,
        pivot = bodyCenter,
    ) {
        val bodyWidth = size.width
        val bodyHeight = size.height * 0.42f
        val bodyTopLeft = Offset(
            bodyCenter.x - bodyWidth / 2f,
            bodyCenter.y - bodyHeight / 2f,
        )
        val bodyCorner = CornerRadius(bodyHeight * 0.48f, bodyHeight * 0.48f)
        drawRoundRect(
            color = strokeColor,
            topLeft = bodyTopLeft,
            size = Size(bodyWidth, bodyHeight),
            cornerRadius = bodyCorner,
        )
        val apertureWidth = bodyWidth * 0.22f
        val apertureHeight = bodyHeight * 0.72f
        val apertureTopLeft = Offset(
            bodyTopLeft.x + bodyWidth - apertureWidth - bodyWidth * 0.05f,
            bodyTopLeft.y + (bodyHeight - apertureHeight) / 2f,
        )
        drawOval(
            color = Color.Black.copy(alpha = 0.34f),
            topLeft = apertureTopLeft,
            size = Size(apertureWidth, apertureHeight),
        )
        val spiderCenter = Offset(
            apertureTopLeft.x + apertureWidth / 2f,
            bodyCenter.y,
        )
        val spiderStroke = strokeWidth * 0.55f
        val spiderHorizontalStart = Offset(spiderCenter.x - apertureWidth / 2f, spiderCenter.y)
        val spiderHorizontalEnd = Offset(spiderCenter.x + apertureWidth / 2f, spiderCenter.y)
        val spiderVerticalStart = Offset(spiderCenter.x, spiderCenter.y - apertureHeight / 2f)
        val spiderVerticalEnd = Offset(spiderCenter.x, spiderCenter.y + apertureHeight / 2f)
        drawLine(
            color = strokeColor,
            start = spiderHorizontalStart,
            end = spiderHorizontalEnd,
            strokeWidth = spiderStroke,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = strokeColor,
            start = spiderVerticalStart,
            end = spiderVerticalEnd,
            strokeWidth = spiderStroke,
            cap = StrokeCap.Round,
        )
        val secondaryWidth = bodyHeight * 0.16f
        val secondaryHeight = bodyHeight * 0.11f
        val secondaryTopLeft = Offset(
            spiderCenter.x - secondaryWidth / 2f,
            spiderCenter.y - secondaryHeight / 2f,
        )
        drawOval(
            color = strokeColor,
            topLeft = secondaryTopLeft,
            size = Size(secondaryWidth, secondaryHeight),
        )
        val focuserWidth = bodyWidth * 0.10f
        val focuserHeight = bodyHeight * 0.22f
        val focuserTopLeft = Offset(
            spiderCenter.x - bodyWidth * 0.3f,
            bodyTopLeft.y + (bodyHeight - focuserHeight) * 0.5f,
        )
        val focuserCorner = CornerRadius(focuserWidth * 0.25f, focuserWidth * 0.25f)
        drawRoundRect(
            color = Color.Black.copy(alpha = 0.34f),
            topLeft = focuserTopLeft,
            size = Size(focuserWidth, focuserHeight),
            cornerRadius = focuserCorner,
        )
        val finderShoeWidth = bodyWidth * 0.10f
        val finderShoeHeight = bodyHeight * 0.22f
        val finderShoeTopLeft = Offset(
            spiderCenter.x - bodyWidth * 0.3f,
            bodyTopLeft.y - finderShoeHeight * 0.9f,
        )
        val finderShoeCorner = CornerRadius(finderShoeWidth * 0.25f, finderShoeWidth * 0.25f)
        drawRoundRect(
            color = strokeColor,
            topLeft = finderShoeTopLeft,
            size = Size(finderShoeWidth, finderShoeHeight),
            cornerRadius = finderShoeCorner,
        )
        val finderWidth = bodyWidth * 0.3f
        val finderHeight = bodyHeight * 0.22f
        val finderTopLeft = Offset(
            spiderCenter.x - bodyWidth * 0.3f - finderWidth * 0.3f,
            bodyTopLeft.y - finderHeight * 0.9f - finderHeight * 0.8f,
        )
        val finderCorner = CornerRadius(finderWidth * 0.25f, finderWidth * 0.25f)
        drawRoundRect(
            color = strokeColor,
            topLeft = finderTopLeft,
            size = Size(finderWidth, finderHeight),
            cornerRadius = finderCorner,
        )
    }
}

internal fun DrawScope.drawEquipmentMountGlyph(
    strokeColor: Color,
    strokeWidth: Float,
) {
    drawEquipmentMountGlyph(
        colors = EquipmentMountGlyphColors.monochrome(strokeColor),
        strokeWidth = strokeWidth,
    )
}

internal fun DrawScope.drawEquipmentMountGlyph(
    colors: EquipmentMountGlyphColors,
    strokeWidth: Float,
) {
    val centerBase = Offset(size.width * 0.58f, size.height * 0.65f)
    val headSize = strokeWidth * 2f
    val headTopLeft = Offset(centerBase.x - headSize / 2f, centerBase.y - headSize)
    drawRect(
        color = colors.headColor,
        topLeft = headTopLeft,
        size = Size(headSize, headSize),
    )
    val circleRadius = headSize / 2f
    val circleCenter = Offset(
        headTopLeft.x + circleRadius,
        headTopLeft.y,
    )
    drawCircle(
        color = colors.headColor,
        radius = circleRadius,
        center = circleCenter,
    )
    val diamondCenter = Offset(
        circleCenter.x - circleRadius,
        circleCenter.y - circleRadius,
    )
    val telescopeLength = headSize * 5f
    val telescopeWidth = strokeWidth * 1.5f
    rotate(
        degrees = 45f,
        pivot = diamondCenter,
    ) {
        drawRect(
            color = colors.bodyColor,
            topLeft = Offset(
                diamondCenter.x - headSize / 2f,
                diamondCenter.y - headSize / 2f,
            ),
            size = Size(headSize, headSize),
        )
        drawRect(
            color = colors.bodyColor,
            topLeft = Offset(
                diamondCenter.x - headSize / 4f,
                diamondCenter.y - headSize,
            ),
            size = Size(headSize / 2f, headSize / 2f),
        )
        drawLine(
            color = colors.telescopeColor,
            start = Offset(
                diamondCenter.x - telescopeLength / 2f,
                diamondCenter.y - 3f * headSize / 2f + telescopeWidth / 2f,
            ),
            end = Offset(
                diamondCenter.x + telescopeLength / 2f,
                diamondCenter.y - 3f * headSize / 2f + telescopeWidth / 2f,
            ),
            strokeWidth = telescopeWidth,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = colors.bodyColor,
            start = diamondCenter,
            end = Offset(diamondCenter.x, diamondCenter.y + size.width * 0.3f),
            strokeWidth = strokeWidth,
        )
        drawCircle(
            color = colors.bodyColor,
            center = Offset(diamondCenter.x, diamondCenter.y + size.width * 0.2f),
            radius = strokeWidth,
        )
    }
    drawLine(
        color = colors.legColor,
        start = Offset(centerBase.x - strokeWidth / 2f, centerBase.y),
        end = Offset(centerBase.x - size.width * 0.14f, centerBase.y + size.width * 0.26f),
        strokeWidth = strokeWidth,
        cap = StrokeCap.Round,
    )
    drawLine(
        color = colors.legColor,
        start = centerBase,
        end = Offset(centerBase.x, centerBase.y + size.width * 0.28f),
        strokeWidth = strokeWidth,
        cap = StrokeCap.Round,
    )
    drawLine(
        color = colors.legColor,
        start = Offset(centerBase.x + strokeWidth / 2f, centerBase.y),
        end = Offset(centerBase.x + size.width * 0.14f, centerBase.y + size.width * 0.26f),
        strokeWidth = strokeWidth,
        cap = StrokeCap.Round,
    )
}
