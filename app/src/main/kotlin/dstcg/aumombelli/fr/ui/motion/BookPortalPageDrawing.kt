package fr.aumombelli.dstcg.ui.motion

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke

internal fun DrawScope.drawBookPageSlab(
    face: BookFaceQuad,
    mirroredHorizontally: Boolean = false,
    toneAlpha: Float = 1f,
) {
    val pagePath = face.asPath()
    val outerTop = if (mirroredHorizontally) face.innerTop else face.outerTop
    val innerTop = if (mirroredHorizontally) face.outerTop else face.innerTop
    val innerBottom = if (mirroredHorizontally) face.outerBottom else face.innerBottom
    val outerBottom = if (mirroredHorizontally) face.innerBottom else face.outerBottom

    drawPath(
        path = pagePath,
        brush = Brush.verticalGradient(
            colors = listOf(
                Color(0xFFFFFEF8).copy(alpha = toneAlpha),
                Color(0xFFF6ECD8).copy(alpha = toneAlpha),
                Color(0xFFE4CFA3).copy(alpha = toneAlpha),
            ),
        ),
        style = Fill,
    )
    drawPath(
        path = pagePath,
        brush = Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.30f * toneAlpha),
                Color.Transparent,
                Color(0x48A9814F).copy(alpha = toneAlpha),
            ),
            start = outerTop,
            end = innerBottom,
        ),
        style = Fill,
    )

    val topSheenPath = quadPath(
        topLeft = outerTop,
        topRight = innerTop,
        bottomRight = lerpOffset(innerTop, innerBottom, 0.24f),
        bottomLeft = lerpOffset(outerTop, outerBottom, 0.24f),
    )
    drawPath(
        path = topSheenPath,
        brush = Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.16f * toneAlpha),
                Color.Transparent,
            ),
        ),
        style = Fill,
    )

    val gutterOuterTop = lerpOffset(innerTop, outerTop, 0.22f)
    val gutterOuterBottom = lerpOffset(innerBottom, outerBottom, 0.22f)
    val gutterPath = Path().apply {
        moveTo(innerTop.x, innerTop.y)
        lineTo(gutterOuterTop.x, gutterOuterTop.y)
        lineTo(gutterOuterBottom.x, gutterOuterBottom.y)
        lineTo(innerBottom.x, innerBottom.y)
        close()
    }
    drawPath(
        path = gutterPath,
        brush = Brush.linearGradient(
            colors = listOf(
                Color(0x668A6334).copy(alpha = toneAlpha),
                Color(0x18000000).copy(alpha = toneAlpha),
            ),
            start = innerTop,
            end = gutterOuterBottom,
        ),
        style = Fill,
    )

    val edgeInnerTop = lerpOffset(outerTop, innerTop, 0.16f)
    val edgeInnerBottom = lerpOffset(outerBottom, innerBottom, 0.16f)
    val edgePath = Path().apply {
        moveTo(outerTop.x, outerTop.y)
        lineTo(edgeInnerTop.x, edgeInnerTop.y)
        lineTo(edgeInnerBottom.x, edgeInnerBottom.y)
        lineTo(outerBottom.x, outerBottom.y)
        close()
    }
    drawPath(
        path = edgePath,
        brush = Brush.linearGradient(
            colors = listOf(
                Color(0x8BFFF4DC).copy(alpha = toneAlpha),
                Color(0x44D4BD8E).copy(alpha = toneAlpha),
                Color(0x12D2C29D).copy(alpha = toneAlpha),
            ),
            start = outerTop,
            end = edgeInnerBottom,
        ),
        style = Fill,
    )

    val bottomEdgePath = quadPath(
        topLeft = lerpOffset(outerBottom, outerTop, 0.08f),
        topRight = lerpOffset(innerBottom, innerTop, 0.08f),
        bottomRight = innerBottom,
        bottomLeft = outerBottom,
    )
    drawPath(
        path = bottomEdgePath,
        brush = Brush.verticalGradient(
            colors = listOf(
                Color(0x35D6C091).copy(alpha = toneAlpha),
                Color(0x129B7A4C).copy(alpha = toneAlpha),
                Color.Transparent,
            ),
        ),
        style = Fill,
    )

    repeat(10) { index ->
        val stripeFraction = 0.06f + index * 0.08f
        val stripeStart = lerpOffset(outerTop, outerBottom, stripeFraction)
        val stripeEnd = lerpOffset(edgeInnerTop, edgeInnerBottom, stripeFraction)
        drawLine(
            color = Color(0xC0B08E5F).copy(alpha = 0.1f + index * 0.012f),
            start = stripeStart,
            end = stripeEnd,
            strokeWidth = size.minDimension * 0.0016f,
        )
    }

    repeat(6) { index ->
        val bandFraction = 0.12f + index * 0.12f
        val bandStart = lerpOffset(outerTop, outerBottom, bandFraction)
        val bandEnd = lerpOffset(innerTop, innerBottom, bandFraction)
        drawLine(
            color = Color.White.copy(alpha = (0.024f + index * 0.008f) * toneAlpha),
            start = bandStart,
            end = bandEnd,
            strokeWidth = size.minDimension * 0.0018f,
        )
    }

    drawPath(
        path = pagePath,
        color = Color(0x7AB39565).copy(alpha = toneAlpha),
        style = Stroke(width = size.minDimension * 0.0036f),
    )
}

internal fun DrawScope.drawBookPageStarField(
    outerTop: Offset,
    innerTop: Offset,
    innerBottom: Offset,
    outerBottom: Offset,
    rotatedHalfTurn: Boolean = false,
    toneAlpha: Float,
) {
    val pointColor = Color(0xFF181411).copy(alpha = 0.74f * toneAlpha)
    val glowColor = Color(0x66181411).copy(alpha = 0.14f * toneAlpha)
    val dotRadius = size.minDimension * 0.0048f
    val glowRadius = dotRadius * 2.8f

    val starDots = listOf(
        0.50f to 0.24f,
        0.22f to 0.12f,
        0.46f to 0.31f,
        0.34f to 0.71f,
        0.63f to 0.45f,
    )

    starDots.forEachIndexed { index, (horizontalFraction, verticalFraction) ->
        val center = pointOnBookFace(
            outerTop = outerTop,
            innerTop = innerTop,
            innerBottom = innerBottom,
            outerBottom = outerBottom,
            horizontalFraction = normalizePageHorizontalFraction(
                horizontalFraction = horizontalFraction,
                rotatedHalfTurn = rotatedHalfTurn,
            ),
            verticalFraction = normalizePageVerticalFraction(
                verticalFraction = verticalFraction,
                rotatedHalfTurn = rotatedHalfTurn,
            ),
        )
        val radiusScale = if (index % 2 == 0) 1.2f else 0.9f
        drawCircle(
            color = glowColor,
            radius = glowRadius * radiusScale,
            center = center,
        )
        drawCircle(
            color = pointColor,
            radius = dotRadius * radiusScale,
            center = center,
        )
    }

    listOf(
        0.72f to 0.27f,
        0.38f to 0.8f,
        0.29f to 0.80f,
    ).forEach { (horizontalFraction, verticalFraction) ->
        val center = pointOnBookFace(
            outerTop = outerTop,
            innerTop = innerTop,
            innerBottom = innerBottom,
            outerBottom = outerBottom,
            horizontalFraction = normalizePageHorizontalFraction(
                horizontalFraction = horizontalFraction,
                rotatedHalfTurn = rotatedHalfTurn,
            ),
            verticalFraction = normalizePageVerticalFraction(
                verticalFraction = verticalFraction,
                rotatedHalfTurn = rotatedHalfTurn,
            ),
        )
        drawPath(
            path = starPath(
                center = center,
                points = 4,
                outerRadius = dotRadius * 3.8f,
                innerRadius = dotRadius * 1.05f,
            ),
            color = Color(0xFF181411).copy(alpha = 0.84f * toneAlpha),
            style = Fill,
        )
    }
}

private fun normalizePageHorizontalFraction(
    horizontalFraction: Float,
    rotatedHalfTurn: Boolean,
): Float = if (rotatedHalfTurn) 1f - horizontalFraction else horizontalFraction

private fun normalizePageVerticalFraction(
    verticalFraction: Float,
    rotatedHalfTurn: Boolean,
): Float = if (rotatedHalfTurn) 1f - verticalFraction else verticalFraction

private fun pointOnBookFace(
    outerTop: Offset,
    innerTop: Offset,
    innerBottom: Offset,
    outerBottom: Offset,
    horizontalFraction: Float,
    verticalFraction: Float,
): Offset {
    val top = lerpOffset(outerTop, innerTop, horizontalFraction)
    val bottom = lerpOffset(outerBottom, innerBottom, horizontalFraction)
    return lerpOffset(top, bottom, verticalFraction)
}

internal fun DrawScope.drawBookPageLeaf(
    face: BookFaceQuad,
    mirroredHorizontally: Boolean = false,
    alpha: Float,
) {
    val leafPath = face.asPath()
    val outerTop = if (mirroredHorizontally) face.innerTop else face.outerTop
    val innerBottom = if (mirroredHorizontally) face.outerBottom else face.innerBottom
    drawPath(
        path = leafPath,
        brush = Brush.linearGradient(
            colors = listOf(
                Color(0xFFFFFEFA).copy(alpha = alpha),
                Color(0xFFF3E8CC).copy(alpha = alpha),
                Color(0xFFE1CFA0).copy(alpha = alpha * 0.9f),
            ),
            start = outerTop,
            end = innerBottom,
        ),
        style = Fill,
    )
    drawPath(
        path = leafPath,
        brush = Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = alpha * 0.14f),
                Color.Transparent,
            ),
        ),
        style = Fill,
    )
    drawPath(
        path = leafPath,
        color = Color(0x56B69466).copy(alpha = alpha),
        style = Stroke(width = size.minDimension * 0.0026f),
    )
}
