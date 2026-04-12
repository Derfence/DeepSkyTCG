package fr.aumombelli.dstcg.ui.motion

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

@Composable
fun BookPortalOverlay(
    progress: Float,
    overlayAlpha: Float = 1f,
    modifier: Modifier = Modifier,
) {
    val visibleProgress = progress.coerceIn(0f, 1f)
    val travelProgress = calculateLibraryBookTravelProgress(visibleProgress)
    if (visibleProgress <= 0f) return
    val visibleAlpha = overlayAlpha.coerceIn(0f, 1f)
    if (visibleAlpha <= 0f) return

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("app-transition-book"),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = (24 + 280 * travelProgress).dp)
                .graphicsLayer {
                    alpha = visibleProgress * visibleAlpha
                }
                .size(220.dp),
        ) {
            BookPortalFigure(
                progress = visibleProgress,
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(176.dp),
            )
        }
    }
}

@Composable
private fun BookPortalFigure(
    progress: Float,
    modifier: Modifier = Modifier,
) {
    val pose = remember(progress) { calculateLibraryBookPose(progress) }

    Canvas(modifier = modifier) {
        drawTransitionBook(pose)
    }
}

private data class BookFaceQuad(
    val outerTop: Offset,
    val innerTop: Offset,
    val innerBottom: Offset,
    val outerBottom: Offset,
) {
    fun asPath(): Path = Path().apply {
        moveTo(outerTop.x, outerTop.y)
        lineTo(innerTop.x, innerTop.y)
        lineTo(innerBottom.x, innerBottom.y)
        lineTo(outerBottom.x, outerBottom.y)
        close()
    }
}

private enum class CoverSurfaceStyle {
    Outer,
    Inner,
}

internal data class FlatBookPortalLayout(
    val openness: Float,
    val closedCoverRect: Rect,
    val backCoverRect: Rect,
    val frontOuterCoverRect: Rect,
    val frontInnerCoverRect: Rect,
    val leftPageRect: Rect,
    val rightPageRect: Rect,
    val spineRect: Rect,
    val shadowTopLeft: Offset,
    val shadowSize: Size,
    val shadowAlpha: Float,
    val frontCoverAlpha: Float,
    val frontInnerCoverAlpha: Float,
    val rightPagesAlpha: Float,
    val leftPagesAlpha: Float,
)

private fun DrawScope.drawTransitionBook(pose: BookPose) {
    val layout = calculateFlatBookPortalLayout(
        canvasSize = size,
        pose = pose,
    )
    val openness = layout.openness
    val shadowCenter = Offset(
        x = layout.shadowTopLeft.x + layout.shadowSize.width / 2f,
        y = layout.shadowTopLeft.y + layout.shadowSize.height / 2f,
    )
    drawOval(
        brush = Brush.radialGradient(
            colors = listOf(
                Color.Black.copy(alpha = layout.shadowAlpha * 0.52f),
                Color.Black.copy(alpha = layout.shadowAlpha * 0.16f),
                Color.Transparent,
            ),
            center = shadowCenter,
            radius = layout.shadowSize.width * 0.72f,
        ),
        topLeft = layout.shadowTopLeft,
        size = layout.shadowSize,
    )

    drawBookCoverSlab(
        face = layout.backCoverRect.toBookFaceQuad(),
        surfaceStyle = CoverSurfaceStyle.Inner,
        accentAlpha = 1f,
    )

    if (layout.frontInnerCoverAlpha > 0.01f) {
        drawBookCoverSlab(
            face = layout.frontInnerCoverRect.toBookFaceQuad(),
            surfaceStyle = CoverSurfaceStyle.Inner,
            accentAlpha = layout.frontInnerCoverAlpha,
        )
    }

    if (layout.rightPagesAlpha > 0.01f || layout.leftPagesAlpha > 0.01f) {
        val leftPageFace = layout.leftPageRect.toBookFaceQuad()
        val rightPageFace = layout.rightPageRect.toBookFaceQuad()
        if (layout.rightPagesAlpha > 0.01f) {
            drawBookPageSlab(
                face = rightPageFace,
                mirroredHorizontally = true,
                toneAlpha = 0.96f * layout.rightPagesAlpha,
            )
        }
        if (layout.leftPagesAlpha > 0.01f) {
            drawBookPageSlab(
                face = leftPageFace,
                toneAlpha = 0.96f * layout.leftPagesAlpha,
            )
        }

        if (openness > 0.12f && (layout.rightPagesAlpha > 0.01f || layout.leftPagesAlpha > 0.01f)) {
            repeat(3) { index ->
                val fraction = index / 2f
                val horizontalInset = scalarLerp(0.05f, 0.17f, fraction)
                val verticalInset = scalarLerp(0.025f, 0.07f, fraction)
                val baseLeafAlpha = scalarLerp(0.18f, 0.44f, openness) *
                    scalarLerp(1f, 0.72f, fraction)
                if (layout.rightPagesAlpha > 0.01f) {
                    drawBookPageLeaf(
                        face = insetFaceAnchoredToBottom(
                            face = rightPageFace,
                            horizontalInset = horizontalInset,
                            topInset = verticalInset * 0.55f,
                        ),
                        mirroredHorizontally = true,
                        alpha = baseLeafAlpha * layout.rightPagesAlpha,
                    )
                }
                if (layout.leftPagesAlpha > 0.01f) {
                    drawBookPageLeaf(
                        face = insetFaceAnchoredToBottom(
                            face = leftPageFace,
                            horizontalInset = horizontalInset,
                            topInset = verticalInset * 0.55f,
                        ),
                        alpha = baseLeafAlpha * layout.leftPagesAlpha,
                    )
                }
            }
        }

        if (layout.rightPagesAlpha > 0.01f) {
            drawBookPageStarField(
                outerTop = rightPageFace.innerTop,
                innerTop = rightPageFace.outerTop,
                innerBottom = rightPageFace.outerBottom,
                outerBottom = rightPageFace.innerBottom,
                rotatedHalfTurn = true,
                toneAlpha = layout.rightPagesAlpha,
            )
        }
        if (layout.leftPagesAlpha > 0.01f) {
            drawBookPageStarField(
                outerTop = leftPageFace.outerTop,
                innerTop = leftPageFace.innerTop,
                innerBottom = leftPageFace.innerBottom,
                outerBottom = leftPageFace.outerBottom,
                toneAlpha = layout.leftPagesAlpha,
            )
        }
    }

    drawBookBinding(
        topCenter = Offset(
            x = (layout.spineRect.left + layout.spineRect.right) / 2f,
            y = layout.spineRect.top,
        ),
        bottomCenter = Offset(
            x = (layout.spineRect.left + layout.spineRect.right) / 2f,
            y = layout.spineRect.bottom,
        ),
        width = layout.spineRect.width,
    )

    if (layout.frontCoverAlpha > 0.01f) {
        drawBookCoverSlab(
            face = layout.frontOuterCoverRect.toBookFaceQuad(),
            surfaceStyle = CoverSurfaceStyle.Outer,
            accentAlpha = layout.frontCoverAlpha,
        )
    }
}

private fun DrawScope.drawBookPageSlab(
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

private fun DrawScope.drawBookPageStarField(
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

internal fun calculateLibraryBookPose(progress: Float): BookPose {
    val clampedProgress = progress.coerceIn(0f, 1f)
    val travelProgress = calculateLibraryBookTravelProgress(clampedProgress)
    val openingProgress = smoothLibraryBookPhase(
        calculateLibraryBookOpeningProgress(clampedProgress),
    )
    return BookPose(
        lift = travelProgress,
        pitchX = 0f,
        yawY = 0f,
        openAngle = 142f * openingProgress,
        pageFan = scalarLerp(0f, 12f, openingProgress),
        spreadWidth = 1f,
        shadowAlpha = scalarLerp(0.18f, 0.34f, travelProgress * 0.7f + openingProgress * 0.3f),
        frontCoverDominance = scalarLerp(1f, 0.42f, openingProgress).coerceIn(0.3f, 1f),
    )
}

internal fun calculateLibraryBookTravelProgress(progress: Float): Float {
    val clampedProgress = progress.coerceIn(0f, 1f)
    return (clampedProgress / LIBRARY_BOOK_TRAVEL_END_PROGRESS).coerceIn(0f, 1f)
}

internal fun calculateLibraryBookOpeningProgress(progress: Float): Float =
    (
        (progress.coerceIn(0f, 1f) - LIBRARY_BOOK_OPENING_START_PROGRESS) /
            LIBRARY_BOOK_OPENING_DURATION_PROGRESS
        ).coerceIn(0f, 1f)

private fun DrawScope.drawBookBinding(
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

private fun DrawScope.drawBookCoverSlab(
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

private fun DrawScope.drawBookPageLeaf(
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

internal fun calculateFlatBookPortalLayout(
    canvasSize: Size,
    pose: BookPose,
): FlatBookPortalLayout {
    val openness = (pose.openAngle / 142f).coerceIn(0f, 1f)
    val coverMotionProgress = openness
    val coverAspectRatio = 0.94f / 1.34f
    val spineWidth = (canvasSize.minDimension * 0.046f).coerceAtLeast(4f)
    val maxSpreadWidth = canvasSize.width * 0.84f
    val maxCoverHeight = canvasSize.height * 0.62f
    val coverWidth = minOf(
        maxCoverHeight * coverAspectRatio,
        ((maxSpreadWidth - spineWidth) / 2f).coerceAtLeast(1f),
    )
    val coverHeight = coverWidth / coverAspectRatio
    val pageWidth = coverWidth * 0.94f
    val pageHeight = coverHeight * 0.94f
    val closedCoverRight = canvasSize.width * 0.92f
    val closedCoverLeft = closedCoverRight - coverWidth
    val centerY = canvasSize.height * (0.78f - pose.lift * 0.05f)
    val top = centerY - coverHeight / 2f
    val bottom = centerY + coverHeight / 2f
    val closedCoverRect = snapRectToPixel(
        left = closedCoverLeft,
        top = top,
        right = closedCoverRight,
        bottom = bottom,
    )
    val pageBottomInset = coverHeight * 0.015f
    val pageBottom = closedCoverRect.bottom - pageBottomInset
    val pageTop = pageBottom - pageHeight
    val spineRect = snapRectToPixel(
        left = closedCoverRect.left - spineWidth,
        top = closedCoverRect.top,
        right = closedCoverRect.left,
        bottom = closedCoverRect.bottom,
    )
    val finalLeftPageRect = snapRectToPixel(
        left = spineRect.left - pageWidth,
        top = pageTop,
        right = spineRect.left,
        bottom = pageBottom,
    )
    val rightPageRect = snapRectToPixel(
        left = spineRect.right,
        top = pageTop,
        right = spineRect.right + pageWidth,
        bottom = pageBottom,
    )
    val backCoverRect = snapRectToPixel(
        left = spineRect.right,
        top = closedCoverRect.top,
        right = spineRect.right + coverWidth,
        bottom = closedCoverRect.bottom,
    )
    val rectoCollapseProgress = (coverMotionProgress / 0.5f).coerceIn(0f, 1f)
    val versoExpandProgress = ((coverMotionProgress - 0.5f) / 0.5f).coerceIn(0f, 1f)
    val pageOpenProgress = versoExpandProgress
    val leftPageRect = snapRectToPixel(
        left = scalarLerp(spineRect.left, finalLeftPageRect.left, pageOpenProgress),
        top = pageTop,
        right = scalarLerp(spineRect.left, finalLeftPageRect.right, pageOpenProgress),
        bottom = pageBottom,
    )
    val frontOuterLeft = scalarLerp(closedCoverRect.left, spineRect.left, rectoCollapseProgress)
    val frontOuterWidth = coverWidth * (1f - rectoCollapseProgress)
    val frontOuterCoverRect = snapRectToPixel(
        left = frontOuterLeft,
        top = closedCoverRect.top,
        right = frontOuterLeft + frontOuterWidth,
        bottom = closedCoverRect.bottom,
    )
    val frontInnerWidth = coverWidth * versoExpandProgress
    val frontInnerCoverRect = snapRectToPixel(
        left = spineRect.left - frontInnerWidth,
        top = closedCoverRect.top,
        right = spineRect.left,
        bottom = closedCoverRect.bottom,
    )
    val shadowWidth =
        scalarLerp(
            (coverWidth + spineWidth) * 1.06f,
            (coverWidth * 2f + spineWidth) * 1.06f,
            coverMotionProgress,
        )
    val shadowHeight = canvasSize.minDimension * scalarLerp(0.06f, 0.09f, openness)
    val bookLeft = minOf(frontOuterCoverRect.left, frontInnerCoverRect.left, spineRect.left)
    val bookRight = maxOf(backCoverRect.right, frontOuterCoverRect.right, frontInnerCoverRect.right)
    val shadowCenter = Offset(
        x = (bookLeft + bookRight) / 2f,
        y = bottom + canvasSize.minDimension * (0.065f - pose.lift * 0.014f),
    )
    return FlatBookPortalLayout(
        openness = openness,
        closedCoverRect = closedCoverRect,
        backCoverRect = backCoverRect,
        frontOuterCoverRect = frontOuterCoverRect,
        frontInnerCoverRect = frontInnerCoverRect,
        leftPageRect = leftPageRect,
        rightPageRect = rightPageRect,
        spineRect = spineRect,
        shadowTopLeft = Offset(
            x = shadowCenter.x - shadowWidth / 2f,
            y = shadowCenter.y - shadowHeight / 2f,
        ),
        shadowSize = Size(shadowWidth, shadowHeight),
        shadowAlpha = pose.shadowAlpha,
        frontCoverAlpha = if (frontOuterWidth > 0.5f) 1f else 0f,
        frontInnerCoverAlpha = if (frontInnerWidth > 0.5f) 1f else 0f,
        rightPagesAlpha = 1f,
        leftPagesAlpha = (pageOpenProgress * 1.15f).coerceIn(0f, 1f),
    )
}

private fun insetFace(
    face: BookFaceQuad,
    horizontalInset: Float,
    verticalInset: Float,
): BookFaceQuad = BookFaceQuad(
    outerTop = lerpOffset(
        lerpOffset(face.outerTop, face.innerTop, horizontalInset),
        face.outerBottom,
        verticalInset,
    ),
    innerTop = lerpOffset(
        lerpOffset(face.innerTop, face.outerTop, horizontalInset),
        face.innerBottom,
        verticalInset,
    ),
    innerBottom = lerpOffset(
        lerpOffset(face.innerBottom, face.outerBottom, horizontalInset),
        face.innerTop,
        verticalInset,
    ),
    outerBottom = lerpOffset(
        lerpOffset(face.outerBottom, face.innerBottom, horizontalInset),
        face.outerTop,
        verticalInset,
    ),
)

private fun insetFaceAnchoredToBottom(
    face: BookFaceQuad,
    horizontalInset: Float,
    topInset: Float,
): BookFaceQuad = BookFaceQuad(
    outerTop = lerpOffset(
        lerpOffset(face.outerTop, face.innerTop, horizontalInset),
        face.outerBottom,
        topInset,
    ),
    innerTop = lerpOffset(
        lerpOffset(face.innerTop, face.outerTop, horizontalInset),
        face.innerBottom,
        topInset,
    ),
    innerBottom = lerpOffset(face.innerBottom, face.outerBottom, horizontalInset),
    outerBottom = lerpOffset(face.outerBottom, face.innerBottom, horizontalInset),
)

private fun Rect.toBookFaceQuad(): BookFaceQuad = BookFaceQuad(
    outerTop = Offset(left, top),
    innerTop = Offset(right, top),
    innerBottom = Offset(right, bottom),
    outerBottom = Offset(left, bottom),
)

private fun smoothLibraryBookPhase(progress: Float): Float {
    val clampedProgress = progress.coerceIn(0f, 1f)
    return clampedProgress * clampedProgress * (3f - 2f * clampedProgress)
}

private fun snapRectToPixel(
    left: Float,
    top: Float,
    right: Float,
    bottom: Float,
): Rect = Rect(
    left = left.roundToInt().toFloat(),
    top = top.roundToInt().toFloat(),
    right = right.roundToInt().toFloat(),
    bottom = bottom.roundToInt().toFloat(),
)

private const val LIBRARY_BOOK_OPENING_START_PROGRESS = 1f / 3f
private const val LIBRARY_BOOK_TRAVEL_END_PROGRESS = 2f / 3f
private const val LIBRARY_BOOK_OPENING_DURATION_PROGRESS =
    1f - LIBRARY_BOOK_OPENING_START_PROGRESS
