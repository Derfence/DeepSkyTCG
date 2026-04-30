package fr.aumombelli.dstcg.feature.crafting

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

@Composable
internal fun DarkenSkyKeogram(
    testTag: String,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(4.dp)
    Canvas(
        modifier = modifier
            .clip(shape)
            .testTag(testTag),
    ) {
        val panelCount = KeogramPanels.size
        val panelWidth = size.width / panelCount
        val cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
        drawRoundRect(
            color = Color.White.copy(alpha = 0.12f),
            cornerRadius = cornerRadius,
            style = Stroke(width = 1.dp.toPx()),
        )

        KeogramPanels.forEachIndexed { index, panel ->
            val left = panelWidth * index
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(panel.zenithColor, panel.midSkyColor, panel.horizonGlowColor),
                    startY = 0f,
                    endY = size.height,
                ),
                topLeft = Offset(left, 0f),
                size = androidx.compose.ui.geometry.Size(panelWidth + 1f, size.height),
            )
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        panel.lightPollutionColor,
                    ),
                    startY = 0f,
                    endY = size.height,
                ),
                topLeft = Offset(left, 0f),
                size = androidx.compose.ui.geometry.Size(panelWidth + 1f, size.height),
            )
            repeat(panel.starCount) { starIndex ->
                val star = KeogramStars[(starIndex * 7 + index * 11) % KeogramStars.size]
                val starCenter = Offset(
                    x = left + panelWidth * star.x,
                    y = size.height * star.y,
                )
                val radius = size.minDimension * star.radius
                drawCircle(
                    color = Color.White.copy(alpha = star.alpha),
                    radius = radius,
                    center = starCenter,
                )
            }
            if (index > 0) {
                drawLine(
                    color = Color.White.copy(alpha = 0.20f),
                    start = Offset(left, 0f),
                    end = Offset(left, size.height),
                    strokeWidth = 1.dp.toPx(),
                )
            }
        }
        drawRoundRect(
            color = Color.White.copy(alpha = 0.18f),
            cornerRadius = cornerRadius,
            style = Stroke(width = 1.dp.toPx()),
        )
    }
}

@Composable
internal fun SpaceAgencyLaunchTower(
    testTag: String,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(4.dp)
    Canvas(
        modifier = modifier
            .clip(shape)
            .testTag(testTag),
    ) {
        val cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF07111B),
                    Color(0xFF102033),
                    Color(0xFF050910),
                ),
                startY = 0f,
                endY = size.height,
            ),
        )
        drawCircle(
            color = Color(0x226CA7FF),
            radius = size.minDimension * 0.44f,
            center = Offset(size.width * 0.62f, size.height * 0.22f),
        )
        drawCircle(
            color = Color(0x18FFFFFF),
            radius = size.minDimension * 0.08f,
            center = Offset(size.width * 0.82f, size.height * 0.18f),
        )

        val groundY = size.height * 0.88f
        drawAgencyBuilding(
            left = size.width * 0.08f,
            bottom = groundY,
            width = size.width * 0.15f,
            height = size.height * 0.16f,
            bodyColor = Color(0xFF162C43),
            windowColumns = 2,
        )
        drawAgencyBuilding(
            left = size.width * 0.22f,
            bottom = groundY,
            width = size.width * 0.12f,
            height = size.height * 0.24f,
            bodyColor = Color(0xFF1C3854),
            windowColumns = 1,
        )
        drawAgencyBuilding(
            left = size.width * 0.66f,
            bottom = groundY,
            width = size.width * 0.13f,
            height = size.height * 0.22f,
            bodyColor = Color(0xFF18324D),
            windowColumns = 2,
        )
        drawAgencyBuilding(
            left = size.width * 0.79f,
            bottom = groundY,
            width = size.width * 0.14f,
            height = size.height * 0.13f,
            bodyColor = Color(0xFF13283D),
            windowColumns = 2,
        )
        drawRect(
            color = Color(0xFF09131E).copy(alpha = 0.82f),
            topLeft = Offset(0f, groundY),
            size = Size(size.width, size.height - groundY),
        )

        val towerTop = size.height * 0.12f
        val towerBottom = size.height * 0.87f
        val towerHeight = towerBottom - towerTop
        val towerWidth = size.width * 0.30f
        val towerLeft = (size.width - towerWidth) / 2f
        val towerRight = towerLeft + towerWidth
        val towerCenterX = (towerLeft + towerRight) / 2f
        val towerStroke = size.minDimension * 0.018f
        val towerColor = Color(0xFFE4EDF7).copy(alpha = 0.82f)
        val towerFill = Color(0xFF294865).copy(alpha = 0.82f)

        drawRoundRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF416B8E).copy(alpha = 0.72f),
                    towerFill,
                    Color(0xFF142A40).copy(alpha = 0.92f),
                ),
                startY = towerTop,
                endY = towerBottom,
            ),
            topLeft = Offset(towerLeft, towerTop),
            size = Size(towerWidth, towerHeight),
            cornerRadius = CornerRadius(towerStroke * 1.2f, towerStroke * 1.2f),
        )
        drawRoundRect(
            color = towerColor.copy(alpha = 0.58f),
            topLeft = Offset(towerLeft, towerTop),
            size = Size(towerWidth, towerHeight),
            cornerRadius = CornerRadius(towerStroke * 1.2f, towerStroke * 1.2f),
            style = Stroke(width = towerStroke * 0.82f),
        )
        listOf(0.32f, 0.55f, 0.76f).forEach { fraction ->
            val y = towerTop + towerHeight * fraction
            drawRoundRect(
                color = towerColor.copy(alpha = 0.64f),
                topLeft = Offset(towerLeft - towerWidth * 0.08f, y),
                size = Size(towerWidth * 1.16f, towerStroke * 0.72f),
                cornerRadius = CornerRadius(towerStroke, towerStroke),
            )
        }

        drawRoundRect(
            color = towerColor.copy(alpha = 0.78f),
            topLeft = Offset(towerCenterX - towerWidth * 0.12f, towerTop - towerHeight * 0.045f),
            size = Size(towerWidth * 0.24f, towerHeight * 0.06f),
            cornerRadius = CornerRadius(towerStroke, towerStroke),
        )
        drawLine(
            color = towerColor.copy(alpha = 0.72f),
            start = Offset(towerCenterX, towerTop - towerHeight * 0.045f),
            end = Offset(towerCenterX, towerTop - towerHeight * 0.12f),
            strokeWidth = towerStroke * 0.42f,
        )

        val logoRadius = towerWidth * 0.15f
        val logoCenter = Offset(towerCenterX, towerTop + towerHeight * 0.20f)
        drawCircle(
            color = Color(0xFF07111B).copy(alpha = 0.84f),
            radius = logoRadius,
            center = logoCenter,
        )
        drawCircle(
            color = Color(0xFF9EE7FF).copy(alpha = 0.30f),
            radius = logoRadius,
            center = logoCenter,
            style = Stroke(width = towerStroke * 0.86f),
        )
        drawCircle(
            color = Color(0x336CA7FF),
            radius = logoRadius * 0.72f,
            center = Offset(logoCenter.x, logoCenter.y + logoRadius * 0.10f),
        )

        drawRocketLogo(
            center = Offset(logoCenter.x, logoCenter.y - logoRadius * 0.06f),
            radius = logoRadius * 0.82f,
        )

        drawRoundRect(
            color = Color.White.copy(alpha = 0.18f),
            cornerRadius = cornerRadius,
            style = Stroke(width = 1.dp.toPx()),
        )
    }
}

private fun DrawScope.drawAgencyBuilding(
    left: Float,
    bottom: Float,
    width: Float,
    height: Float,
    bodyColor: Color,
    windowColumns: Int,
) {
    val top = bottom - height
    val cornerRadius = CornerRadius(width * 0.08f, width * 0.08f)
    drawRoundRect(
        color = bodyColor.copy(alpha = 0.88f),
        topLeft = Offset(left, top),
        size = Size(width, height),
        cornerRadius = cornerRadius,
    )
    drawRoundRect(
        color = Color.White.copy(alpha = 0.12f),
        topLeft = Offset(left, top),
        size = Size(width, height),
        cornerRadius = cornerRadius,
        style = Stroke(width = width * 0.035f),
    )
    val windowSize = width * 0.13f
    val rowCount = 3
    repeat(windowColumns) { column ->
        repeat(rowCount) { row ->
            drawRoundRect(
                color = Color(0xFF9EE7FF).copy(alpha = 0.28f),
                topLeft = Offset(
                    x = left + width * (0.28f + column * 0.34f),
                    y = top + height * (0.28f + row * 0.18f),
                ),
                size = Size(windowSize, windowSize * 1.16f),
                cornerRadius = CornerRadius(windowSize * 0.22f, windowSize * 0.22f),
            )
        }
    }
}

private fun DrawScope.drawRocketLogo(
    center: Offset,
    radius: Float,
) {
    val rocketPath = Path().apply {
        moveTo(center.x, center.y - radius * 0.62f)
        cubicTo(
            center.x + radius * 0.36f,
            center.y - radius * 0.36f,
            center.x + radius * 0.32f,
            center.y + radius * 0.14f,
            center.x + radius * 0.10f,
            center.y + radius * 0.40f,
        )
        lineTo(center.x + radius * 0.04f, center.y + radius * 0.54f)
        lineTo(center.x - radius * 0.04f, center.y + radius * 0.54f)
        lineTo(center.x - radius * 0.10f, center.y + radius * 0.40f)
        cubicTo(
            center.x - radius * 0.32f,
            center.y + radius * 0.14f,
            center.x - radius * 0.36f,
            center.y - radius * 0.36f,
            center.x,
            center.y - radius * 0.62f,
        )
        close()
    }
    val leftFin = Path().apply {
        moveTo(center.x - radius * 0.10f, center.y + radius * 0.25f)
        lineTo(center.x - radius * 0.34f, center.y + radius * 0.56f)
        lineTo(center.x - radius * 0.08f, center.y + radius * 0.44f)
        close()
    }
    val rightFin = Path().apply {
        moveTo(center.x + radius * 0.10f, center.y + radius * 0.25f)
        lineTo(center.x + radius * 0.34f, center.y + radius * 0.56f)
        lineTo(center.x + radius * 0.08f, center.y + radius * 0.44f)
        close()
    }
    val flame = Path().apply {
        moveTo(center.x, center.y + radius * 0.52f)
        cubicTo(
            center.x + radius * 0.13f,
            center.y + radius * 0.76f,
            center.x + radius * 0.04f,
            center.y + radius * 0.96f,
            center.x,
            center.y + radius * 1.06f,
        )
        cubicTo(
            center.x - radius * 0.04f,
            center.y + radius * 0.96f,
            center.x - radius * 0.13f,
            center.y + radius * 0.76f,
            center.x,
            center.y + radius * 0.52f,
        )
        close()
    }

    listOf(-0.28f, 0f, 0.28f).forEach { offset ->
        drawLine(
            color = Color.White.copy(alpha = 0.20f),
            start = Offset(center.x + radius * offset, center.y + radius * 0.72f),
            end = Offset(center.x + radius * offset, center.y + radius * 1.34f),
            strokeWidth = radius * 0.018f,
        )
    }
    drawPath(
        path = leftFin,
        color = Color(0xFF74D9FF).copy(alpha = 0.88f),
    )
    drawPath(
        path = rightFin,
        color = Color(0xFF74D9FF).copy(alpha = 0.88f),
    )
    drawPath(
        path = flame,
        color = Color(0xFFFFB35C).copy(alpha = 0.86f),
    )
    drawPath(
        path = rocketPath,
        color = Color.White.copy(alpha = 0.94f),
    )
    drawPath(
        path = rocketPath,
        color = Color(0xFF9EE7FF).copy(alpha = 0.34f),
        style = Stroke(width = radius * 0.045f),
    )
    drawCircle(
        color = Color(0xFF07111B).copy(alpha = 0.72f),
        radius = radius * 0.11f,
        center = Offset(center.x, center.y - radius * 0.18f),
    )
    drawCircle(
        color = Color(0xFF9EE7FF).copy(alpha = 0.88f),
        radius = radius * 0.065f,
        center = Offset(center.x, center.y - radius * 0.18f),
    )
}

private data class KeogramPanel(
    val zenithColor: Color,
    val midSkyColor: Color,
    val horizonGlowColor: Color,
    val lightPollutionColor: Color,
    val starCount: Int,
)

private data class KeogramStar(
    val x: Float,
    val y: Float,
    val radius: Float,
    val alpha: Float,
)

private val KeogramPanels = listOf(
    KeogramPanel(
        zenithColor = Color(0xFF312C49),
        midSkyColor = Color(0xFF5F5148),
        horizonGlowColor = Color(0xFFD0A35B),
        lightPollutionColor = Color(0x55F7CE78),
        starCount = 5,
    ),
    KeogramPanel(
        zenithColor = Color(0xFF151B36),
        midSkyColor = Color(0xFF363650),
        horizonGlowColor = Color(0xFF9B7158),
        lightPollutionColor = Color(0x3FD49A6A),
        starCount = 10,
    ),
    KeogramPanel(
        zenithColor = Color(0xFF061222),
        midSkyColor = Color(0xFF0D3156),
        horizonGlowColor = Color(0xFF255F79),
        lightPollutionColor = Color(0x2A6FBAC9),
        starCount = 17,
    ),
    KeogramPanel(
        zenithColor = Color(0xFF01030A),
        midSkyColor = Color(0xFF06142B),
        horizonGlowColor = Color(0xFF102F4A),
        lightPollutionColor = Color(0x1F6AA9FF),
        starCount = 28,
    ),
)

private val KeogramStars = listOf(
    KeogramStar(0.17f, 0.11f, 0.005f, 0.74f),
    KeogramStar(0.83f, 0.64f, 0.004f, 0.58f),
    KeogramStar(0.46f, 0.27f, 0.006f, 0.80f),
    KeogramStar(0.29f, 0.79f, 0.004f, 0.60f),
    KeogramStar(0.72f, 0.39f, 0.005f, 0.70f),
    KeogramStar(0.09f, 0.52f, 0.003f, 0.48f),
    KeogramStar(0.61f, 0.93f, 0.003f, 0.46f),
    KeogramStar(0.37f, 0.72f, 0.004f, 0.66f),
    KeogramStar(0.91f, 0.19f, 0.005f, 0.68f),
    KeogramStar(0.53f, 0.58f, 0.004f, 0.62f),
    KeogramStar(0.22f, 0.35f, 0.004f, 0.64f),
    KeogramStar(0.78f, 0.86f, 0.003f, 0.50f),
    KeogramStar(0.34f, 0.05f, 0.005f, 0.76f),
    KeogramStar(0.66f, 0.70f, 0.004f, 0.58f),
    KeogramStar(0.12f, 0.42f, 0.004f, 0.56f),
    KeogramStar(0.88f, 0.96f, 0.003f, 0.42f),
    KeogramStar(0.48f, 0.83f, 0.004f, 0.56f),
    KeogramStar(0.74f, 0.08f, 0.006f, 0.82f),
    KeogramStar(0.19f, 0.90f, 0.003f, 0.44f),
    KeogramStar(0.58f, 0.18f, 0.004f, 0.68f),
    KeogramStar(0.95f, 0.48f, 0.003f, 0.52f),
    KeogramStar(0.41f, 0.90f, 0.003f, 0.48f),
    KeogramStar(0.06f, 0.24f, 0.004f, 0.60f),
    KeogramStar(0.69f, 0.54f, 0.004f, 0.64f),
    KeogramStar(0.31f, 0.60f, 0.003f, 0.52f),
    KeogramStar(0.86f, 0.31f, 0.004f, 0.62f),
    KeogramStar(0.15f, 0.68f, 0.003f, 0.50f),
    KeogramStar(0.55f, 0.43f, 0.005f, 0.72f),
    KeogramStar(0.76f, 0.58f, 0.004f, 0.58f),
    KeogramStar(0.25f, 0.15f, 0.004f, 0.66f),
    KeogramStar(0.63f, 0.76f, 0.003f, 0.50f),
)
