package fr.aumombelli.dstcg.feature.minigames

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke

internal fun DrawScope.drawSceneSky() {
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                Color(0xFF0B1626),
                Color(0xFF061323),
                Color(0xFF04080F),
            ),
        ),
    )

    drawCircle(
        color = Color(0x22C2DCFF),
        radius = size.minDimension * 0.22f,
        center = Offset(size.width * 0.72f, size.height * 0.20f),
    )
    drawCircle(
        color = Color(0x20FFE2AA),
        radius = size.minDimension * 0.08f,
        center = Offset(size.width * 0.72f, size.height * 0.20f),
    )

    val constellation = listOf(
        Offset(size.width * 0.16f, size.height * 0.20f),
        Offset(size.width * 0.29f, size.height * 0.15f),
        Offset(size.width * 0.43f, size.height * 0.18f),
        Offset(size.width * 0.59f, size.height * 0.13f),
        Offset(size.width * 0.78f, size.height * 0.20f),
    )
    constellation.zipWithNext().forEach { (start, end) ->
        drawLine(
            color = Color(0x5CD2E6FF),
            start = start,
            end = end,
            strokeWidth = size.minDimension * 0.004f,
            cap = StrokeCap.Round,
        )
    }
    constellation.forEachIndexed { index, point ->
        val radius = size.minDimension * if (index == 3) 0.008f else 0.006f
        drawCircle(color = Color(0xEAF3F7FF), radius = radius, center = point)
        drawCircle(
            color = Color(0x36FFD684),
            radius = radius * 2.3f,
            center = point,
            style = Stroke(width = radius * 0.45f),
        )
    }

    val stars = listOf(
        0.10f to 0.36f,
        0.18f to 0.10f,
        0.24f to 0.30f,
        0.38f to 0.08f,
        0.50f to 0.28f,
        0.66f to 0.09f,
        0.82f to 0.32f,
        0.90f to 0.16f,
    )
    stars.forEachIndexed { index, (x, y) ->
        drawCircle(
            color = if (index % 3 == 0) Color(0xDFFFDF9A) else Color(0xC8EBF5FF),
            radius = size.minDimension * (0.0038f + (index % 2) * 0.002f),
            center = Offset(size.width * x, size.height * y),
        )
    }
}

internal fun DrawScope.drawSceneMountains() {
    val farRidge = Path().apply {
        moveTo(0f, size.height * 0.70f)
        lineTo(size.width * 0.18f, size.height * 0.61f)
        lineTo(size.width * 0.36f, size.height * 0.67f)
        lineTo(size.width * 0.57f, size.height * 0.55f)
        lineTo(size.width * 0.76f, size.height * 0.64f)
        lineTo(size.width, size.height * 0.54f)
        lineTo(size.width, size.height)
        lineTo(0f, size.height)
        close()
    }
    drawPath(path = farRidge, color = Color(0xFF0F1B2A))

    val nearRidge = Path().apply {
        moveTo(0f, size.height * 0.81f)
        lineTo(size.width * 0.22f, size.height * 0.68f)
        lineTo(size.width * 0.43f, size.height * 0.76f)
        lineTo(size.width * 0.62f, size.height * 0.62f)
        lineTo(size.width * 0.80f, size.height * 0.73f)
        lineTo(size.width, size.height * 0.66f)
        lineTo(size.width, size.height)
        lineTo(0f, size.height)
        close()
    }
    drawPath(path = nearRidge, color = Color(0xFF081018))
}

internal fun DrawScope.drawSceneVignette() {
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                Color.Transparent,
                Color.Transparent,
                Color(0x9904080F),
            ),
        ),
    )
}
