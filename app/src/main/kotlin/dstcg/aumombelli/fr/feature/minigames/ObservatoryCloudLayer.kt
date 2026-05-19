package fr.aumombelli.dstcg.feature.minigames

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope

internal const val ObservatoryCloudBandCenterYRatio: Float = 0.30f

private const val ObservatoryCloudBandHorizontalOverflowRatio: Float = 0.12f
private const val ObservatoryCloudBandHeightToUnitRatio: Float = 0.22f

internal data class ObservatoryCloudBand(
    val topLeft: Offset,
    val size: Size,
) {
    val centerY: Float = topLeft.y + size.height / 2f
}

internal fun observatoryCloudBand(
    sceneSize: Size,
    unit: Float,
): ObservatoryCloudBand {
    val overflow = sceneSize.width * ObservatoryCloudBandHorizontalOverflowRatio
    val height = unit * ObservatoryCloudBandHeightToUnitRatio
    val centerY = sceneSize.height * ObservatoryCloudBandCenterYRatio
    return ObservatoryCloudBand(
        topLeft = Offset(
            x = -overflow,
            y = centerY - height / 2f,
        ),
        size = Size(
            width = sceneSize.width + overflow * 2f,
            height = height,
        ),
    )
}

internal fun DrawScope.drawCloudPassage(
    geometry: ObservatoryGeometry,
    alpha: Float,
) {
    if (alpha <= 0.01f) return

    val band = observatoryCloudBand(
        sceneSize = size,
        unit = geometry.unit,
    )
    val cloudColor = Color(0xFFD8E7F9)
    val shadowColor = Color(0xFF7890AA).copy(alpha = alpha * 0.20f)
    val mistBrush = Brush.verticalGradient(
        colors = listOf(
            Color.Transparent,
            cloudColor.copy(alpha = alpha * 0.22f),
            cloudColor.copy(alpha = alpha * 0.50f),
            cloudColor.copy(alpha = alpha * 0.30f),
            Color.Transparent,
        ),
        startY = band.topLeft.y,
        endY = band.topLeft.y + band.size.height,
    )

    drawRect(
        brush = mistBrush,
        topLeft = Offset(0f, band.topLeft.y),
        size = Size(size.width, band.size.height),
    )
    drawOval(
        color = shadowColor,
        topLeft = Offset(
            x = band.topLeft.x,
            y = band.centerY + band.size.height * 0.03f,
        ),
        size = Size(
            width = band.size.width,
            height = band.size.height * 0.42f,
        ),
    )

    ObservatoryCloudLobes.forEach { lobe ->
        val lobeSize = Size(
            width = geometry.unit * lobe.widthToUnitRatio,
            height = geometry.unit * lobe.heightToUnitRatio,
        )
        drawOval(
            color = cloudColor.copy(alpha = alpha * lobe.alphaRatio),
            topLeft = Offset(
                x = size.width * lobe.xRatio - lobeSize.width / 2f,
                y = band.centerY + band.size.height * lobe.yOffsetRatio - lobeSize.height / 2f,
            ),
            size = lobeSize,
        )
    }
}

private data class ObservatoryCloudLobe(
    val xRatio: Float,
    val yOffsetRatio: Float,
    val widthToUnitRatio: Float,
    val heightToUnitRatio: Float,
    val alphaRatio: Float,
)

private val ObservatoryCloudLobes = listOf(
    ObservatoryCloudLobe(-0.08f, 0.02f, 0.34f, 0.12f, 0.44f),
    ObservatoryCloudLobe(0.05f, -0.08f, 0.28f, 0.18f, 0.50f),
    ObservatoryCloudLobe(0.19f, -0.20f, 0.34f, 0.22f, 0.58f),
    ObservatoryCloudLobe(0.36f, -0.07f, 0.32f, 0.18f, 0.48f),
    ObservatoryCloudLobe(0.52f, -0.19f, 0.36f, 0.23f, 0.60f),
    ObservatoryCloudLobe(0.69f, -0.06f, 0.33f, 0.18f, 0.50f),
    ObservatoryCloudLobe(0.86f, -0.17f, 0.35f, 0.22f, 0.58f),
    ObservatoryCloudLobe(1.04f, -0.05f, 0.30f, 0.17f, 0.48f),
    ObservatoryCloudLobe(1.16f, -0.14f, 0.26f, 0.18f, 0.44f),
)
