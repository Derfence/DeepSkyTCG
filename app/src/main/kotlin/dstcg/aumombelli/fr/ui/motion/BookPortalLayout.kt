package fr.aumombelli.dstcg.ui.motion

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size

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
