package fr.aumombelli.dstcg.ui.motion

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke

internal enum class CoverSurfaceStyle {
    Outer,
    Inner,
}

internal fun DrawScope.drawTransitionBook(pose: BookPose) {
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
