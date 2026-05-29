package fr.aumombelli.dstcg.ui.motion

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.PI
import kotlin.math.atan2

internal data class ExtensionPatternDrawStyle(
    val pointCoreBaseRadiusFraction: Float,
    val pointCoreRevealRadiusFraction: Float,
    val pointHaloRadiusFraction: Float,
    val pointHaloAlphaMultiplier: Float,
    val lineStrokeWidthFraction: Float,
    val lineAlphaMultiplier: Float,
    val orbitStrokeWidthFraction: Float,
    val orbitAlphaMultiplier: Float,
    val pointColor: Color,
    val pointHaloColor: Color,
    val lineColor: Color,
    val orbitColor: Color,
)

internal fun DrawScope.drawExtensionPattern(
    spec: ExtensionAnimationSpec,
    lineProgress: Float,
    isReversing: Boolean,
    revealWindow: Float,
    style: ExtensionPatternDrawStyle,
) {
    val projection = projectExtensionPattern(
        spec = spec,
        canvasWidth = size.width,
        canvasHeight = size.height,
    )

    spec.circlePatterns.forEachIndexed { index, circle ->
        val reveal = extensionCircleReveal(
            lineProgress = lineProgress,
            circleIndex = index,
            circleCount = spec.circlePatterns.size,
            revealWindow = revealWindow,
        )
        if (reveal <= 0f) return@forEachIndexed

        val projectedCenter = projection.project(circle.center)
        val projectedPoint = projection.project(circle.point)
        val center = Offset(projectedCenter.x, projectedCenter.y)
        val point = Offset(projectedPoint.x, projectedPoint.y)
        val radius = fractionalDistance(projectedCenter, projectedPoint)
        if (radius <= 0f) return@forEachIndexed

        drawArc(
            color = style.orbitColor.copy(alpha = reveal * style.orbitAlphaMultiplier),
            startAngle = screenAngleDegrees(center = center, point = point),
            sweepAngle = 360f * reveal,
            useCenter = false,
            topLeft = Offset(center.x - radius, center.y - radius),
            size = Size(radius * 2f, radius * 2f),
            style = Stroke(width = size.minDimension * style.orbitStrokeWidthFraction),
        )
    }

    spec.lineConnections.forEachIndexed { index, connection ->
        val reveal = extensionLineReveal(
            lineProgress = lineProgress,
            lineIndex = index,
            lineCount = spec.lineConnections.size,
            revealWindow = revealWindow,
        )
        if (reveal <= 0f) return@forEachIndexed

        val start = spec.starPattern[connection.first]
        val end = spec.starPattern[connection.second]
        val projectedStart = projection.project(start)
        val projectedEnd = projection.project(end)
        val startOffset = Offset(projectedStart.x, projectedStart.y)
        val endOffset = Offset(projectedEnd.x, projectedEnd.y)
        val currentEnd = Offset(
            x = startOffset.x + (endOffset.x - startOffset.x) * reveal,
            y = startOffset.y + (endOffset.y - startOffset.y) * reveal,
        )
        drawLine(
            color = style.lineColor.copy(alpha = reveal * style.lineAlphaMultiplier),
            start = startOffset,
            end = currentEnd,
            strokeWidth = size.minDimension * style.lineStrokeWidthFraction,
        )
    }

    spec.starPattern.forEachIndexed { index, star ->
        val reveal = extensionPointReveal(
            spec = spec,
            pointIndex = index,
            lineProgress = lineProgress,
            isReversing = isReversing,
            revealWindow = revealWindow,
        )
        if (reveal <= 0f) return@forEachIndexed

        val projected = projection.project(star)
        drawExtensionPatternPoint(
            center = Offset(projected.x, projected.y),
            reveal = reveal,
            style = style,
        )
    }

    spec.circlePatterns.forEachIndexed { index, circle ->
        val reveal = extensionCircleReveal(
            lineProgress = lineProgress,
            circleIndex = index,
            circleCount = spec.circlePatterns.size,
            revealWindow = revealWindow,
        )
        if (reveal <= 0f) return@forEachIndexed

        val projected = projection.project(circle.point)
        drawExtensionPatternPoint(
            center = Offset(projected.x, projected.y),
            reveal = reveal,
            style = style,
        )
    }
}

private fun DrawScope.drawExtensionPatternPoint(
    center: Offset,
    reveal: Float,
    style: ExtensionPatternDrawStyle,
) {
    drawCircle(
        color = style.pointColor.copy(alpha = reveal),
        radius = size.minDimension * (
            style.pointCoreBaseRadiusFraction +
                reveal * style.pointCoreRevealRadiusFraction
            ),
        center = center,
    )
    drawCircle(
        color = style.pointHaloColor.copy(alpha = reveal * style.pointHaloAlphaMultiplier),
        radius = size.minDimension * style.pointHaloRadiusFraction,
        center = center,
    )
}

private fun screenAngleDegrees(
    center: Offset,
    point: Offset,
): Float = (atan2(
    y = point.y - center.y,
    x = point.x - center.x,
) * 180.0 / PI).toFloat()
