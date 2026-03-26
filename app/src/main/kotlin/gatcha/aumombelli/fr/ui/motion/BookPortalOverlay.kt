package fr.aumombelli.gatcha.ui.motion

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
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun BookPortalOverlay(
    progress: Float,
    overlayAlpha: Float = 1f,
    modifier: Modifier = Modifier,
) {
    val visibleProgress = progress.coerceIn(0f, 1f)
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
                .padding(bottom = (24 + 280 * visibleProgress).dp)
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
    val pose = remember(progress) { calculateBookPose(progress) }

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

private data class ProjectedBookFace(
    val quad: BookFaceQuad,
    val averageDepth: Float,
)

private data class BookPoint3(
    val x: Float,
    val y: Float,
    val z: Float,
)

private data class DrawLayer(
    val depth: Float,
    val render: DrawScope.() -> Unit,
)

private enum class CoverSurfaceStyle {
    Outer,
    Inner,
}

private fun DrawScope.drawTransitionBook(pose: BookPose) {
    val openness = (pose.openAngle / 142f).coerceIn(0f, 1f)
    val bookHeight = 1.34f
    val coverWidth = 0.94f
    val pageWidth = 0.88f
    val backCoverDepth = -0.12f
    val pageBlockDepth = -0.02f
    val frontCoverDepth = 0.09f
    val center = Offset(
        x = size.width * scalarLerp(0.50f, 0.54f, openness),
        y = size.height * (0.78f - pose.lift * 0.06f),
    )
    val scale = size.minDimension * 0.58f / bookHeight
    val cameraDistance = 7.4f
    val layers = mutableListOf<DrawLayer>()

    val shadowWidth = size.minDimension * (0.26f + pose.lift * 0.12f + openness * 0.16f)
    val shadowHeight = size.minDimension * (0.055f + openness * 0.024f)
    val shadowCenter = Offset(
        x = center.x + size.minDimension * scalarLerp(0.0f, 0.03f, openness),
        y = center.y + size.minDimension * (0.085f - pose.lift * 0.018f),
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
        topLeft = Offset(shadowCenter.x - shadowWidth / 2f, shadowCenter.y - shadowHeight / 2f),
        size = Size(shadowWidth, shadowHeight),
    )

    fun projectPoint(point: BookPoint3): Pair<Offset, Float> {
        val spreadPoint = point.copy(x = point.x * pose.spreadWidth)
        val pitched = rotateAroundX(spreadPoint, pose.pitchX)
        val yawed = rotateAroundY(pitched, pose.yawY)
        val perspective = cameraDistance / (cameraDistance - yawed.z).coerceAtLeast(1.4f)
        return Offset(
            x = center.x + yawed.x * scale * perspective,
            y = center.y - yawed.y * scale * perspective,
        ) to yawed.z
    }

    fun buildFace(
        hingeX: Float,
        outerX: Float,
        depthZ: Float,
        hingeRotation: Float = 0f,
        hingePivotX: Float = hingeX,
    ): ProjectedBookFace {
        val outerTop = rotateAroundY(
            point = BookPoint3(outerX, bookHeight, depthZ),
            degrees = hingeRotation,
            pivotX = hingePivotX,
        )
        val innerTop = rotateAroundY(
            point = BookPoint3(hingeX, bookHeight, depthZ),
            degrees = hingeRotation,
            pivotX = hingePivotX,
        )
        val innerBottom = rotateAroundY(
            point = BookPoint3(hingeX, 0f, depthZ),
            degrees = hingeRotation,
            pivotX = hingePivotX,
        )
        val outerBottom = rotateAroundY(
            point = BookPoint3(outerX, 0f, depthZ),
            degrees = hingeRotation,
            pivotX = hingePivotX,
        )

        val projectedOuterTop = projectPoint(outerTop)
        val projectedInnerTop = projectPoint(innerTop)
        val projectedInnerBottom = projectPoint(innerBottom)
        val projectedOuterBottom = projectPoint(outerBottom)

        return ProjectedBookFace(
            quad = BookFaceQuad(
                outerTop = projectedOuterTop.first,
                innerTop = projectedInnerTop.first,
                innerBottom = projectedInnerBottom.first,
                outerBottom = projectedOuterBottom.first,
            ),
            averageDepth = listOf(
                projectedOuterTop.second,
                projectedInnerTop.second,
                projectedInnerBottom.second,
                projectedOuterBottom.second,
            ).average().toFloat(),
        )
    }

    val backCover = buildFace(
        hingeX = 0f,
        outerX = coverWidth,
        depthZ = backCoverDepth,
    )
    val pageBlock = buildFace(
        hingeX = 0.035f,
        outerX = pageWidth,
        depthZ = pageBlockDepth,
    )
    val frontCover = buildFace(
        hingeX = 0f,
        outerX = coverWidth,
        depthZ = frontCoverDepth,
        hingeRotation = pose.openAngle,
    )
    val frontPageBlock = buildFace(
        hingeX = 0.028f,
        outerX = pageWidth * 0.92f,
        depthZ = frontCoverDepth - 0.036f,
        hingeRotation = pose.openAngle * 0.94f + pose.pageFan * 0.10f,
        hingePivotX = 0f,
    )
    val frontCoverRectangularity = ((openness - 0.42f) / 0.58f).coerceIn(0f, 1f)
    val frontCoverFace = rectangularizeFace(
        face = frontCover.quad,
        blend = frontCoverRectangularity * 0.72f,
    )
    val frontPageBlockFace = rectangularizeFace(
        face = frontPageBlock.quad,
        blend = frontCoverRectangularity * 0.54f,
    )

    layers += DrawLayer(backCover.averageDepth) {
        drawBookCoverSlab(
            face = backCover.quad,
            open = openness,
            surfaceStyle = CoverSurfaceStyle.Outer,
            accentAlpha = 0.72f,
        )
    }
    layers += DrawLayer(pageBlock.averageDepth) {
        drawBookPageSlab(
            face = pageBlock.quad,
            open = openness,
            toneAlpha = 0.96f,
        )
    }
    layers += DrawLayer(frontPageBlock.averageDepth) {
        drawBookPageSlab(
            face = frontPageBlockFace,
            open = openness,
            toneAlpha = scalarLerp(0.82f, 0.94f, openness),
        )
    }

    if (openness > 0.02f) {
        val pageCount = 4
        repeat(pageCount) { index ->
            val fraction = index / (pageCount - 1).toFloat()
            val pageAngle = pose.openAngle * scalarLerp(0.38f, 0.82f, fraction) - pose.pageFan * (1f - fraction) * 0.42f
            val leaf = buildFace(
                hingeX = 0.018f + fraction * 0.024f,
                outerX = pageWidth * scalarLerp(0.98f, 0.82f, fraction),
                depthZ = scalarLerp(0.005f, 0.055f, fraction),
                hingeRotation = pageAngle,
                hingePivotX = 0f,
            )
            layers += DrawLayer(leaf.averageDepth) {
                drawBookPageLeaf(
                    face = leaf.quad,
                    alpha = scalarLerp(0.28f, 0.7f, openness) * scalarLerp(1f, 0.72f, fraction),
                )
            }
        }
    }

    val bindingTop = projectPoint(BookPoint3(0f, bookHeight, -0.018f))
    val bindingBottom = projectPoint(BookPoint3(0f, 0f, -0.018f))
    layers += DrawLayer((bindingTop.second + bindingBottom.second) / 2f) {
        drawBookBinding(
            topCenter = bindingTop.first,
            bottomCenter = bindingBottom.first,
            width = size.minDimension * scalarLerp(0.068f, 0.084f, openness),
            open = openness,
        )
    }

    layers += DrawLayer(frontCover.averageDepth + pose.frontCoverDominance * 0.1f) {
        drawBookCoverSlab(
            face = frontCoverFace,
            open = openness,
            surfaceStyle = if (pose.openAngle < 88f) {
                CoverSurfaceStyle.Outer
            } else {
                CoverSurfaceStyle.Inner
            },
            accentAlpha = scalarLerp(1f, 0.84f, openness),
        )
    }

    layers
        .sortedBy { it.depth }
        .forEach { layer -> layer.render(this) }
}

private fun DrawScope.drawBookPageSlab(
    face: BookFaceQuad,
    open: Float,
    toneAlpha: Float = 1f,
) {
    val pagePath = face.asPath()
    val outerTop = face.outerTop
    val innerTop = face.innerTop
    val innerBottom = face.innerBottom
    val outerBottom = face.outerBottom

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

private fun DrawScope.drawBookBinding(
    topCenter: Offset,
    bottomCenter: Offset,
    width: Float,
    open: Float,
) {
    val axis = bottomCenter - topCenter
    val axisLengthSquared = ((axis.x * axis.x) + (axis.y * axis.y)).coerceAtLeast(0.0001f)
    val inverseLength = 1f / kotlin.math.sqrt(axisLengthSquared)
    val axisUnit = Offset(axis.x * inverseLength, axis.y * inverseLength)
    val normal = Offset(-axisUnit.y, axisUnit.x)
    val widthScale = scalarLerp(0.9f, 1.1f, open)
    val halfWidth = width * widthScale / 2f
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
    open: Float,
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
    val emblemRadius = size.minDimension * scalarLerp(0.014f, 0.019f, open)
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
    alpha: Float,
) {
    val leafPath = face.asPath()
    drawPath(
        path = leafPath,
        brush = Brush.linearGradient(
            colors = listOf(
                Color(0xFFFFFEFA).copy(alpha = alpha),
                Color(0xFFF3E8CC).copy(alpha = alpha),
                Color(0xFFE1CFA0).copy(alpha = alpha * 0.9f),
            ),
            start = face.outerTop,
            end = face.innerBottom,
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

private fun rectangularizeFace(
    face: BookFaceQuad,
    blend: Float,
): BookFaceQuad {
    val clampedBlend = blend.coerceIn(0f, 1f)
    if (clampedBlend <= 0f) return face

    val averagedSpan = Offset(
        x = ((face.outerTop.x - face.innerTop.x) + (face.outerBottom.x - face.innerBottom.x)) / 2f,
        y = ((face.outerTop.y - face.innerTop.y) + (face.outerBottom.y - face.innerBottom.y)) / 2f,
    )
    val rectangularTarget = BookFaceQuad(
        outerTop = face.innerTop + averagedSpan,
        innerTop = face.innerTop,
        innerBottom = face.innerBottom,
        outerBottom = face.innerBottom + averagedSpan,
    )

    return BookFaceQuad(
        outerTop = lerpOffset(face.outerTop, rectangularTarget.outerTop, clampedBlend),
        innerTop = face.innerTop,
        innerBottom = face.innerBottom,
        outerBottom = lerpOffset(face.outerBottom, rectangularTarget.outerBottom, clampedBlend),
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

private fun rotateAroundX(
    point: BookPoint3,
    degrees: Float,
): BookPoint3 {
    val radians = degrees / 180f * PI.toFloat()
    val cosine = cos(radians)
    val sine = sin(radians)
    return BookPoint3(
        x = point.x,
        y = point.y * cosine - point.z * sine,
        z = point.y * sine + point.z * cosine,
    )
}

private fun rotateAroundY(
    point: BookPoint3,
    degrees: Float,
    pivotX: Float = 0f,
): BookPoint3 {
    val translatedX = point.x - pivotX
    val radians = degrees / 180f * PI.toFloat()
    val cosine = cos(radians)
    val sine = sin(radians)
    val rotatedX = translatedX * cosine + point.z * sine
    val rotatedZ = -translatedX * sine + point.z * cosine
    return BookPoint3(
        x = rotatedX + pivotX,
        y = point.y,
        z = rotatedZ,
    )
}
