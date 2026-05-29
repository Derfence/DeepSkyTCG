package fr.aumombelli.dstcg.ui.motion

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import kotlin.math.roundToInt

internal data class BookFaceQuad(
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

internal fun insetFace(
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

internal fun insetFaceAnchoredToBottom(
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

internal fun Rect.toBookFaceQuad(): BookFaceQuad = BookFaceQuad(
    outerTop = Offset(left, top),
    innerTop = Offset(right, top),
    innerBottom = Offset(right, bottom),
    outerBottom = Offset(left, bottom),
)

internal fun smoothLibraryBookPhase(progress: Float): Float {
    val clampedProgress = progress.coerceIn(0f, 1f)
    return clampedProgress * clampedProgress * (3f - 2f * clampedProgress)
}

internal fun snapRectToPixel(
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

internal const val LIBRARY_BOOK_OPENING_START_PROGRESS = 1f / 3f
internal const val LIBRARY_BOOK_TRAVEL_END_PROGRESS = 2f / 3f
internal const val LIBRARY_BOOK_OPENING_DURATION_PROGRESS =
    1f - LIBRARY_BOOK_OPENING_START_PROGRESS
