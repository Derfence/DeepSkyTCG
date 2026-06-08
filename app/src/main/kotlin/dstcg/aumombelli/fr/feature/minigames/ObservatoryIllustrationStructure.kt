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
    val panel = observatoryDomePanelGeometry(geometry, state.azimuth)
    val openingProgress = state.domeProgress.coerceIn(0f, 1f)

    val dome = Path().apply {
        moveTo(cx - rx, baseY)
        cubicTo(cx - rx, baseY - ry * 0.82f, cx - rx * 0.52f, baseY - ry, cx, baseY - ry)
        cubicTo(cx + rx * 0.52f, baseY - ry, cx + rx, baseY - ry * 0.82f, cx + rx, baseY)
        close()
    }
    drawPath(path = dome, color = Color(0xFF1F2D40))

    val opening = trapezoidPath(
        centerX = panel.centerX,
        topWidth = observatoryLerp(panel.topWidth * 0.22f, panel.topWidth * 1.12f, openingProgress),
        bottomWidth = observatoryLerp(panel.bottomWidth * 0.22f, panel.bottomWidth * 1.12f, openingProgress),
        topY = panel.topY,
        bottomY = panel.bottomY,
    )
    drawPath(path = opening, color = Color(0xFF07101B))

    val panelCenterX = panel.centerX + openingProgress * rx * 0.28f
    val panelPath = trapezoidPath(
        centerX = panelCenterX,
        topWidth = panel.topWidth,
        bottomWidth = panel.bottomWidth,
        topY = panel.topY,
        bottomY = panel.bottomY,
    )
    drawPath(path = panelPath, color = Color(0xFF0E1622))
    drawPath(
        path = panelPath,
        color = Color(0x59E5C170),
        style = Stroke(width = geometry.unit * 0.004f),
    )

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
    val tubeGeometry = observatoryTelescopeTubeGeometry(geometry)
    val pivot = tubeGeometry.rearEdge
    val unit = geometry.unit
    val mountColor = Color(0xFF263955).copy(alpha = alpha)
    val detailColor = Color(0xFF8BE8E0).copy(alpha = alpha)

    val bracket = Path().apply {
        moveTo(pivot.x - unit * 0.030f, geometry.domeBaseY)
        lineTo(pivot.x + unit * 0.030f, geometry.domeBaseY)
        lineTo(pivot.x + unit * 0.018f, pivot.y + unit * 0.030f)
        lineTo(pivot.x - unit * 0.018f, pivot.y + unit * 0.030f)
        close()
    }
    drawPath(path = bracket, color = Color(0xFF111C2A).copy(alpha = alpha))
    drawPath(
        path = bracket,
        color = detailColor.copy(alpha = alpha * 0.22f),
        style = Stroke(width = unit * 0.004f),
    )

    rotate(degrees = geometry.tubeAngleDeg, pivot = pivot) {
        val tube = Path().apply {
            moveTo(pivot.x, pivot.y - geometry.tubeHeight * 0.36f)
            lineTo(pivot.x + geometry.tubeLength * 0.76f, pivot.y - geometry.tubeHeight * 0.50f)
            lineTo(pivot.x + geometry.tubeLength, pivot.y - geometry.tubeHeight * 0.42f)
            lineTo(pivot.x + geometry.tubeLength, pivot.y + geometry.tubeHeight * 0.42f)
            lineTo(pivot.x + geometry.tubeLength * 0.76f, pivot.y + geometry.tubeHeight * 0.50f)
            lineTo(pivot.x, pivot.y + geometry.tubeHeight * 0.36f)
            close()
        }
        drawPath(path = tube, color = Color(0xFF22334B).copy(alpha = alpha))
        drawPath(
            path = tube,
            color = Color(0x4D8BE8E0).copy(alpha = alpha * 0.30f),
            style = Stroke(width = unit * 0.004f),
        )
        drawRoundRect(
            color = Color(0xFF1A2940).copy(alpha = alpha),
            topLeft = Offset(pivot.x + geometry.tubeLength * 0.18f, pivot.y - geometry.tubeHeight * 0.47f),
            size = Size(geometry.tubeLength * 0.08f, geometry.tubeHeight * 0.94f),
            cornerRadius = CornerRadius(geometry.tubeHeight * 0.16f, geometry.tubeHeight * 0.16f),
        )
        drawRoundRect(
            color = Color(0xFF111A28).copy(alpha = alpha),
            topLeft = Offset(pivot.x + geometry.tubeLength * 0.78f, pivot.y - geometry.tubeHeight * 0.64f),
            size = Size(geometry.tubeLength * 0.22f, geometry.tubeHeight * 1.28f),
            cornerRadius = CornerRadius(geometry.tubeHeight * 0.18f, geometry.tubeHeight * 0.18f),
        )
        drawLine(
            color = detailColor.copy(alpha = alpha * 0.42f),
            start = Offset(pivot.x + geometry.tubeLength * 0.12f, pivot.y - geometry.tubeHeight * 0.20f),
            end = Offset(pivot.x + geometry.tubeLength * 0.72f, pivot.y - geometry.tubeHeight * 0.24f),
            strokeWidth = unit * 0.006f,
            cap = StrokeCap.Round,
        )
    }

    drawCircle(
        color = mountColor,
        radius = unit * 0.032f,
        center = pivot,
    )
    drawCircle(
        color = Color(0xFF0A1019).copy(alpha = alpha),
        radius = unit * 0.020f,
        center = pivot,
    )
    drawCircle(
        color = detailColor.copy(alpha = alpha * 0.70f),
        radius = unit * 0.010f,
        center = pivot,
    )
}

private fun trapezoidPath(
    centerX: Float,
    topWidth: Float,
    bottomWidth: Float,
    topY: Float,
    bottomY: Float,
): Path = Path().apply {
    moveTo(centerX - topWidth * 0.50f, topY)
    lineTo(centerX + topWidth * 0.50f, topY)
    lineTo(centerX + bottomWidth * 0.50f, bottomY)
    lineTo(centerX - bottomWidth * 0.50f, bottomY)
    close()
}
