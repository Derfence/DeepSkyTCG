package fr.aumombelli.dstcg.feature.minigames

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate

internal fun DrawScope.drawObservatoryTerrace(geometry: ObservatoryGeometry) {
    val unit = geometry.unit
    val y = geometry.baseY + unit * 0.12f
    val x = geometry.center.x

    val upper = Path().apply {
        moveTo(x - unit * 0.23f, y - unit * 0.08f)
        lineTo(x + unit * 0.28f, y - unit * 0.08f)
        lineTo(x + unit * 0.34f, y)
        lineTo(x - unit * 0.30f, y)
        close()
    }
    val lower = Path().apply {
        moveTo(x - unit * 0.30f, y)
        lineTo(x + unit * 0.34f, y)
        lineTo(x + unit * 0.42f, y + unit * 0.06f)
        lineTo(x - unit * 0.38f, y + unit * 0.06f)
        close()
    }
    drawPath(path = lower, color = Color(0xFF070A11))
    drawPath(path = upper, color = Color(0xFF0A1019))

    repeat(10) { index ->
        val railX = x - unit * 0.19f + index * unit * 0.046f
        drawLine(
            color = Color(0x6B566477),
            start = Offset(railX, y - unit * 0.13f),
            end = Offset(railX, y - unit * 0.07f),
            strokeWidth = unit * 0.004f,
        )
    }
    drawLine(
        color = Color(0x7A566477),
        start = Offset(x - unit * 0.20f, y - unit * 0.13f),
        end = Offset(x + unit * 0.26f, y - unit * 0.13f),
        strokeWidth = unit * 0.006f,
    )
}

internal fun DrawScope.drawObservatoryBuilding(geometry: ObservatoryGeometry) {
    val left = geometry.center.x - geometry.buildingWidth * 0.50f
    val top = geometry.baseY - geometry.buildingHeight
    val right = left + geometry.buildingWidth

    drawRect(
        color = Color(0xFF151F2F),
        topLeft = Offset(left, top),
        size = Size(geometry.buildingWidth, geometry.buildingHeight),
    )

    val side = Path().apply {
        moveTo(right, top)
        lineTo(right + geometry.unit * 0.10f, top + geometry.unit * 0.04f)
        lineTo(right + geometry.unit * 0.10f, top + geometry.buildingHeight * 0.96f)
        lineTo(right, top + geometry.buildingHeight)
        close()
    }
    drawPath(path = side, color = Color(0xFF0E1622))

    drawRect(
        color = Color(0xCCE9B96A),
        topLeft = Offset(left + geometry.buildingWidth * 0.50f, top + geometry.buildingHeight * 0.18f),
        size = Size(geometry.unit * 0.028f, geometry.buildingHeight * 0.72f),
    )
    drawRect(
        color = Color(0xA8D2A558),
        topLeft = Offset(left + geometry.buildingWidth * 0.76f, top + geometry.buildingHeight * 0.50f),
        size = Size(geometry.unit * 0.024f, geometry.buildingHeight * 0.42f),
    )
}

internal fun DrawScope.drawObservatoryDome(
    geometry: ObservatoryGeometry,
    state: ObservatoryIllustrationState,
) {
    val cx = geometry.center.x
    val baseY = geometry.domeBaseY
    val rx = geometry.domeRadiusX
    val ry = geometry.domeRadiusY

    val dome = Path().apply {
        moveTo(cx - rx, baseY)
        cubicTo(cx - rx, baseY - ry * 0.82f, cx - rx * 0.52f, baseY - ry, cx, baseY - ry)
        cubicTo(cx + rx * 0.52f, baseY - ry, cx + rx, baseY - ry * 0.82f, cx + rx, baseY)
        close()
    }
    drawPath(path = dome, color = Color(0xFF1F2D40))

    val openingWidth = observatoryLerp(rx * 0.10f, rx * 0.95f, state.domeProgress)
    val opening = Path().apply {
        moveTo(cx - openingWidth * 0.45f, baseY)
        cubicTo(
            cx - openingWidth * 0.42f,
            baseY - ry * 0.46f,
            cx - openingWidth * 0.18f,
            baseY - ry * 0.90f,
            cx,
            baseY - ry * 0.94f,
        )
        cubicTo(
            cx + openingWidth * 0.18f,
            baseY - ry * 0.90f,
            cx + openingWidth * 0.42f,
            baseY - ry * 0.46f,
            cx + openingWidth * 0.45f,
            baseY,
        )
        close()
    }
    drawPath(path = opening, color = Color(0xFF07101B))

    val panelShift = state.domeProgress * rx * 0.78f
    val panel = Path().apply {
        moveTo(cx - rx * 0.06f + panelShift, baseY - ry * 0.96f)
        lineTo(cx + rx * 0.72f + panelShift, baseY - ry * 0.68f)
        lineTo(cx + rx * 0.50f + panelShift, baseY - ry * 0.20f)
        lineTo(cx - rx * 0.18f + panelShift, baseY - ry * 0.54f)
        close()
    }
    rotate(degrees = state.domeProgress * 10f, pivot = Offset(cx, baseY - ry * 0.60f)) {
        drawPath(path = panel, color = Color(0xFF0E1622))
    }

    val rimColor = if (state.domeReady) Color(0xFFE5C170) else Color(0xB8E5C170)
    drawPath(
        path = dome,
        color = rimColor,
        style = Stroke(width = geometry.unit * 0.006f),
    )
}

internal fun DrawScope.drawTelescope(
    geometry: ObservatoryGeometry,
    state: ObservatoryIllustrationState,
) {
    val alpha = observatoryLerp(0.06f, 1f, state.domeProgress.coerceIn(0f, 1f))
    val pivot = geometry.pivot
    val unit = geometry.unit
    val baseColor = Color(0xFF263955).copy(alpha = alpha)
    val detailColor = Color(0xFF8BE8E0).copy(alpha = alpha)

    drawOval(
        color = Color(0xFF111C2A).copy(alpha = alpha),
        topLeft = Offset(pivot.x - unit * 0.075f, geometry.domeBaseY - unit * 0.015f),
        size = Size(unit * 0.15f, unit * 0.035f),
    )
    drawLine(
        color = baseColor,
        start = Offset(pivot.x - unit * 0.035f, geometry.domeBaseY - unit * 0.012f),
        end = pivot,
        strokeWidth = unit * 0.018f,
        cap = StrokeCap.Round,
    )
    drawLine(
        color = baseColor,
        start = Offset(pivot.x + unit * 0.035f, geometry.domeBaseY - unit * 0.012f),
        end = pivot,
        strokeWidth = unit * 0.018f,
        cap = StrokeCap.Round,
    )

    rotate(degrees = geometry.tubeAngleDeg, pivot = pivot) {
        drawRoundRect(
            color = Color(0xFF22334B).copy(alpha = alpha),
            topLeft = Offset(pivot.x - geometry.tubeHeight * 0.30f, pivot.y - geometry.tubeHeight * 0.50f),
            size = Size(geometry.tubeLength, geometry.tubeHeight),
            cornerRadius = CornerRadius(geometry.tubeHeight * 0.50f, geometry.tubeHeight * 0.50f),
        )
        drawRoundRect(
            color = Color(0xFF111A28).copy(alpha = alpha),
            topLeft = Offset(pivot.x + geometry.tubeLength * 0.76f, pivot.y - geometry.tubeHeight * 0.62f),
            size = Size(geometry.tubeLength * 0.25f, geometry.tubeHeight * 1.24f),
            cornerRadius = CornerRadius(geometry.tubeHeight * 0.56f, geometry.tubeHeight * 0.56f),
        )
        drawLine(
            color = detailColor.copy(alpha = alpha * 0.42f),
            start = Offset(pivot.x + geometry.tubeLength * 0.12f, pivot.y - geometry.tubeHeight * 0.18f),
            end = Offset(pivot.x + geometry.tubeLength * 0.78f, pivot.y - geometry.tubeHeight * 0.18f),
            strokeWidth = unit * 0.006f,
            cap = StrokeCap.Round,
        )
    }

    drawCircle(
        color = Color(0xFF0A1019).copy(alpha = alpha),
        radius = unit * 0.034f,
        center = pivot,
    )
    drawCircle(
        color = detailColor.copy(alpha = alpha * 0.72f),
        radius = unit * 0.016f,
        center = pivot,
    )
}
