package fr.aumombelli.gatcha.ui.motion

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import fr.aumombelli.gatcha.ui.theme.EmberGold

@Composable
fun ExtensionConstellationOverlay(
    spec: ExtensionAnimationSpec,
    lineProgress: Float,
    isReversing: Boolean,
    modifier: Modifier = Modifier,
    tag: String? = "pack-extension-constellation",
) {
    if (spec.style == ExtensionAnimationStyle.NeutralSky) return
    if (lineProgress <= 0f) return

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .then(
                if (tag != null) {
                    Modifier.testTag(tag)
                } else {
                    Modifier
                },
            ),
    ) {
        val projection = projectExtensionPattern(
            spec = spec,
            canvasWidth = size.width,
            canvasHeight = size.height,
        )

        spec.starPattern.forEachIndexed { index, star ->
            val reveal = extensionPointReveal(
                spec = spec,
                pointIndex = index,
                lineProgress = lineProgress,
                isReversing = isReversing,
                revealWindow = 0.25f,
            )
            if (reveal <= 0f) return@forEachIndexed

            val projected = projection.project(star)
            val center = Offset(projected.x, projected.y)
            drawCircle(
                color = Color.White.copy(alpha = reveal),
                radius = size.minDimension * (0.006f + reveal * 0.003f),
                center = center,
            )
            drawCircle(
                color = EmberGold.copy(alpha = reveal * 0.35f),
                radius = size.minDimension * 0.018f,
                center = center,
            )
        }

        spec.lineConnections.forEachIndexed { index, connection ->
            val reveal = extensionLineReveal(
                lineProgress = lineProgress,
                lineIndex = index,
                lineCount = spec.lineConnections.size,
                revealWindow = 0.25f,
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
                color = Color(0xFFE2F0FF).copy(alpha = reveal * 0.9f),
                start = startOffset,
                end = currentEnd,
                strokeWidth = size.minDimension * 0.004f,
            )
        }
    }
}
