package fr.aumombelli.dstcg.ui.motion

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke

internal fun DrawScope.drawBookBinding(
    topCenter: Offset,
    bottomCenter: Offset,
    width: Float,
) {
    val axis = bottomCenter - topCenter
    val axisLengthSquared = ((axis.x * axis.x) + (axis.y * axis.y)).coerceAtLeast(0.0001f)
    val inverseLength = 1f / kotlin.math.sqrt(axisLengthSquared)
    val axisUnit = Offset(axis.x * inverseLength, axis.y * inverseLength)
    val normal = Offset(-axisUnit.y, axisUnit.x)
    val halfWidth = width / 2f
    val outerTop = topCenter + normal * halfWidth
    val innerTop = topCenter - normal * halfWidth
    val innerBottom = bottomCenter - normal * halfWidth
    val outerBottom = bottomCenter + normal * halfWidth
    val face = BookFaceQuad(
        outerTop = outerTop,
        innerTop = innerTop,
        innerBottom = innerBottom,
        outerBottom = outerBottom,
    )
    val bindingPath = face.asPath()

    drawPath(
        path = bindingPath,
        brush = Brush.linearGradient(
            colors = listOf(
                Color(0xFF07111E),
                Color(0xFF2A4871),
                Color(0xFF091423),
            ),
            start = outerTop,
            end = innerBottom,
        ),
        style = Fill,
    )
    drawPath(
        path = bindingPath,
        brush = Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.14f),
                Color.Transparent,
                Color.Black.copy(alpha = 0.24f),
            ),
            start = topCenter,
            end = bottomCenter,
        ),
        style = Fill,
    )

    repeat(4) { index ->
        val fraction = 0.18f + index * 0.18f
        val bandCenter = lerpOffset(topCenter, bottomCenter, fraction)
        val bandHalfWidth = halfWidth * 0.58f
        drawLine(
            color = Color(0xB7E0C27A),
            start = bandCenter - normal * bandHalfWidth,
            end = bandCenter + normal * bandHalfWidth,
            strokeWidth = size.minDimension * 0.010f,
        )
    }

    val labelCenter = lerpOffset(topCenter, bottomCenter, 0.34f)
    val labelHalfWidth = halfWidth * 0.48f
    val labelHeight = size.minDimension * 0.030f
    val labelTopCenter = labelCenter - axisUnit * (labelHeight * 0.16f)
    drawPath(
        path = quadPath(
            topLeft = labelTopCenter - normal * labelHalfWidth - axisUnit * (labelHeight * 0.5f),
            topRight = labelTopCenter + normal * labelHalfWidth - axisUnit * (labelHeight * 0.5f),
            bottomRight = labelTopCenter + normal * labelHalfWidth + axisUnit * (labelHeight * 0.5f),
            bottomLeft = labelTopCenter - normal * labelHalfWidth + axisUnit * (labelHeight * 0.5f),
        ),
        color = Color(0x36E3C897),
        style = Fill,
    )
    drawPath(
        path = starPath(
            center = labelCenter,
            points = 4,
            outerRadius = halfWidth * 0.22f,
            innerRadius = halfWidth * 0.1f,
        ),
        color = Color(0xD8E9CD90),
        style = Fill,
    )
    drawPath(
        path = bindingPath,
        color = Color(0xD107101B),
        style = Stroke(width = size.minDimension * 0.0048f),
    )
}

internal fun DrawScope.drawBookCoverSlab(
    face: BookFaceQuad,
    surfaceStyle: CoverSurfaceStyle,
    accentAlpha: Float = 1f,
) {
    val outerPath = face.asPath()
    val outerTop = face.outerTop
    val innerTop = face.innerTop
    val innerBottom = face.innerBottom
    val outerBottom = face.outerBottom
    val baseColors = if (surfaceStyle == CoverSurfaceStyle.Outer) {
        listOf(
            Color(0xFF243A57),
            Color(0xFF14243A),
            Color(0xFF08111E),
        )
    } else {
        listOf(
            Color(0xFF3B4552),
            Color(0xFF27303E),
            Color(0xFF181E28),
        )
    }
    drawPath(
        path = outerPath,
        brush = Brush.verticalGradient(baseColors.map { it.copy(alpha = accentAlpha) }),
        style = Fill,
    )
    drawPath(
        path = outerPath,
        brush = Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.14f * accentAlpha),
                Color.Transparent,
                Color.Black.copy(alpha = 0.3f * accentAlpha),
            ),
            start = outerTop,
            end = innerBottom,
        ),
        style = Fill,
    )

    val boardEdgeTop = lerpOffset(outerTop, innerTop, 0.08f)
    val boardEdgeBottom = lerpOffset(outerBottom, innerBottom, 0.08f)
    val boardEdgePath = Path().apply {
        moveTo(outerTop.x, outerTop.y)
        lineTo(boardEdgeTop.x, boardEdgeTop.y)
        lineTo(boardEdgeBottom.x, boardEdgeBottom.y)
        lineTo(outerBottom.x, outerBottom.y)
        close()
    }
    drawPath(
        path = boardEdgePath,
        brush = Brush.linearGradient(
            colors = listOf(
                Color(0x86A7C5D8).copy(alpha = accentAlpha),
                Color(0x14000000).copy(alpha = accentAlpha),
            ),
            start = outerTop,
            end = boardEdgeBottom,
        ),
        style = Fill,
    )

    val hingeOuterTop = lerpOffset(innerTop, outerTop, 0.18f)
    val hingeOuterBottom = lerpOffset(innerBottom, outerBottom, 0.18f)
    val hingePath = Path().apply {
        moveTo(innerTop.x, innerTop.y)
        lineTo(hingeOuterTop.x, hingeOuterTop.y)
        lineTo(hingeOuterBottom.x, hingeOuterBottom.y)
        lineTo(innerBottom.x, innerBottom.y)
        close()
    }
    drawPath(
        path = hingePath,
        brush = Brush.verticalGradient(
            colors = listOf(
                if (surfaceStyle == CoverSurfaceStyle.Outer) {
                    Color(0x58D8C38B)
                } else {
                    Color(0x3ED3C6A3)
                }.copy(alpha = accentAlpha),
                Color(0x1805080D).copy(alpha = accentAlpha),
                Color(0x6E05080D).copy(alpha = accentAlpha),
            ),
        ),
        style = Fill,
    )

    val inset = insetFace(face, horizontalInset = 0.12f, verticalInset = 0.12f)
    val insetPath = inset.asPath()
    drawPath(
        path = insetPath,
        color = if (surfaceStyle == CoverSurfaceStyle.Outer) {
            Color(0x9EE0C17D)
        } else {
            Color(0x5DBFC8D6)
        }.copy(alpha = accentAlpha),
        style = Stroke(width = size.minDimension * 0.0038f),
    )
    drawPath(
        path = insetPath,
        color = Color.White.copy(alpha = 0.05f * accentAlpha),
        style = Stroke(width = size.minDimension * 0.0016f),
    )

    if (surfaceStyle == CoverSurfaceStyle.Outer) {
        val titleTopLeft = lerpOffset(inset.outerTop, inset.outerBottom, 0.18f)
        val titleTopRight = lerpOffset(inset.innerTop, inset.innerBottom, 0.18f)
        val titleBottomLeft = lerpOffset(inset.outerTop, inset.outerBottom, 0.3f)
        val titleBottomRight = lerpOffset(inset.innerTop, inset.innerBottom, 0.3f)
        val titlePlate = quadPath(titleTopLeft, titleTopRight, titleBottomRight, titleBottomLeft)
        drawPath(
            path = titlePlate,
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0x3AF2DFB6).copy(alpha = accentAlpha),
                    Color(0x13756540).copy(alpha = accentAlpha),
                ),
            ),
            style = Fill,
        )
        drawPath(
            path = titlePlate,
            color = Color(0xA8E2C783).copy(alpha = accentAlpha),
            style = Stroke(width = size.minDimension * 0.0028f),
        )
    }

    val topGlowPath = quadPath(
        topLeft = inset.outerTop,
        topRight = inset.innerTop,
        bottomRight = lerpOffset(inset.innerTop, inset.innerBottom, 0.2f),
        bottomLeft = lerpOffset(inset.outerTop, inset.outerBottom, 0.2f),
    )
    drawPath(
        path = topGlowPath,
        brush = Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.08f * accentAlpha),
                Color.Transparent,
            ),
        ),
        style = Fill,
    )

    val midTop = lerpOffset(inset.outerTop, inset.innerTop, 0.5f)
    val midBottom = lerpOffset(inset.outerBottom, inset.innerBottom, 0.5f)
    val emblemCenter = lerpOffset(midTop, midBottom, 0.54f)
    val emblemRadius = size.minDimension * 0.016f
    drawCircle(
        color = if (surfaceStyle == CoverSurfaceStyle.Outer) {
            Color(0x18F1D6A0)
        } else {
            Color(0x12DCE9F0)
        }.copy(alpha = accentAlpha),
        radius = emblemRadius * 1.7f,
        center = emblemCenter,
    )
    drawCircle(
        color = if (surfaceStyle == CoverSurfaceStyle.Outer) {
            Color(0xB7E6C47D)
        } else {
            Color(0x8BCAD6E0)
        }.copy(alpha = accentAlpha),
        radius = emblemRadius,
        center = emblemCenter,
        style = Stroke(width = size.minDimension * 0.0026f),
    )
    drawPath(
        path = starPath(
            center = emblemCenter,
            points = 4,
            outerRadius = emblemRadius * 0.72f,
            innerRadius = emblemRadius * 0.3f,
        ),
        color = if (surfaceStyle == CoverSurfaceStyle.Outer) {
            Color(0xD6F2D392)
        } else {
            Color(0xB7D9E7EF)
        }.copy(alpha = accentAlpha),
        style = Fill,
    )

    val cornerInset = 0.08f
    val topLeftCornerEndX = lerpOffset(inset.outerTop, inset.innerTop, cornerInset)
    val topLeftCornerEndY = lerpOffset(inset.outerTop, inset.outerBottom, cornerInset)
    val topRightCornerEndX = lerpOffset(inset.innerTop, inset.outerTop, cornerInset)
    val topRightCornerEndY = lerpOffset(inset.innerTop, inset.innerBottom, cornerInset)
    val bottomLeftCornerEndX = lerpOffset(inset.outerBottom, inset.innerBottom, cornerInset)
    val bottomLeftCornerEndY = lerpOffset(inset.outerBottom, inset.outerTop, cornerInset)
    val bottomRightCornerEndX = lerpOffset(inset.innerBottom, inset.outerBottom, cornerInset)
    val bottomRightCornerEndY = lerpOffset(inset.innerBottom, inset.innerTop, cornerInset)
    val filigreeColor = if (surfaceStyle == CoverSurfaceStyle.Outer) {
        Color(0x88E8CA84)
    } else {
        Color(0x58CBD6E0)
    }.copy(alpha = accentAlpha)
    drawLine(
        color = filigreeColor,
        start = inset.outerTop,
        end = topLeftCornerEndX,
        strokeWidth = size.minDimension * 0.002f,
    )
    drawLine(
        color = filigreeColor,
        start = inset.outerTop,
        end = topLeftCornerEndY,
        strokeWidth = size.minDimension * 0.002f,
    )
    drawLine(
        color = filigreeColor,
        start = inset.innerTop,
        end = topRightCornerEndX,
        strokeWidth = size.minDimension * 0.002f,
    )
    drawLine(
        color = filigreeColor,
        start = inset.innerTop,
        end = topRightCornerEndY,
        strokeWidth = size.minDimension * 0.002f,
    )
    drawLine(
        color = filigreeColor,
        start = inset.outerBottom,
        end = bottomLeftCornerEndX,
        strokeWidth = size.minDimension * 0.002f,
    )
    drawLine(
        color = filigreeColor,
        start = inset.outerBottom,
        end = bottomLeftCornerEndY,
        strokeWidth = size.minDimension * 0.002f,
    )
    drawLine(
        color = filigreeColor,
        start = inset.innerBottom,
        end = bottomRightCornerEndX,
        strokeWidth = size.minDimension * 0.002f,
    )
    drawLine(
        color = filigreeColor,
        start = inset.innerBottom,
        end = bottomRightCornerEndY,
        strokeWidth = size.minDimension * 0.002f,
    )

    val bottomBevelPath = quadPath(
        topLeft = lerpOffset(outerBottom, outerTop, 0.08f),
        topRight = lerpOffset(innerBottom, innerTop, 0.08f),
        bottomRight = innerBottom,
        bottomLeft = outerBottom,
    )
    drawPath(
        path = bottomBevelPath,
        brush = Brush.verticalGradient(
            colors = listOf(
                Color.Transparent,
                Color(0x6C04070D).copy(alpha = accentAlpha),
            ),
        ),
        style = Fill,
    )

    val grainSteps = 5
    repeat(grainSteps) { index ->
        val fraction = 0.14f + index * 0.13f
        val start = lerpOffset(outerTop, outerBottom, fraction)
        val end = lerpOffset(innerTop, innerBottom, fraction)
        drawLine(
            color = Color.White.copy(alpha = (0.018f + index * 0.008f) * accentAlpha),
            start = start,
            end = end,
            strokeWidth = size.minDimension * 0.002f,
        )
    }

    drawPath(
        path = outerPath,
        color = Color(0xD107101B),
        style = Stroke(width = size.minDimension * 0.005f),
    )
}
