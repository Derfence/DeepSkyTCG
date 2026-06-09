package fr.aumombelli.dstcg.feature.minigames

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

internal fun DrawScope.drawObservatoryIllustration(state: ObservatoryIllustrationState) {
    val geometry = ObservatoryGeometry.from(size, state)

    drawSceneSky()
    drawSceneMountains()
    drawObservationBeam(geometry, state)
    drawObjectiveMarker(geometry, state)
    drawTargetReticle(geometry, state)
    drawObservatoryTerrace(geometry)
    drawObservatoryBuilding(geometry)
    drawObservatoryDome(geometry, state)
    drawTelescope(geometry, state)
    drawCloudPassage(geometry, state.cloudAlpha)
    drawSceneVignette()
}

internal data class ObservatoryGeometry(
    val unit: Float,
    val center: Offset,
    val baseY: Float,
    val buildingWidth: Float,
    val buildingHeight: Float,
    val domeBaseY: Float,
    val domeRadiusX: Float,
    val domeRadiusY: Float,
    val pivot: Offset,
    val tubeLength: Float,
    val tubeHeight: Float,
    val tubeAngleDeg: Float,
    val lens: Offset,
    val target: Offset,
    val objective: Offset,
) {
    companion object {
        fun from(size: Size, state: ObservatoryIllustrationState): ObservatoryGeometry {
            val unit = min(size.width, size.height * 0.82f)
            val center = Offset(
                x = size.width * 0.56f,
                y = size.height * 0.70f,
            )
            val buildingWidth = unit * 0.28f
            val buildingHeight = unit * 0.23f
            val domeBaseY = center.y - buildingHeight * 0.88f
            val domeRadiusX = unit * 0.17f
            val domeRadiusY = unit * 0.12f
            val azimuthOffset = (state.azimuth - 0.5f) * unit * 0.09f
            val pivot = Offset(
                x = center.x + azimuthOffset,
                y = domeBaseY - domeRadiusY * 0.38f,
            )
            val tubeLength = unit * 0.36f
            val tubeHeight = unit * 0.055f
            val target = targetFor(
                size = size,
                unit = unit,
                domeBaseY = domeBaseY,
                domeRadiusY = domeRadiusY,
                azimuth = state.azimuth,
                altitude = state.altitude,
            )
            val objective = targetFor(
                size = size,
                unit = unit,
                domeBaseY = domeBaseY,
                domeRadiusY = domeRadiusY,
                azimuth = state.targetAzimuth,
                altitude = state.targetAltitude,
            )
            val tubeAngleDeg = atan2(
                y = target.y - pivot.y,
                x = target.x - pivot.x,
            ).toDegrees()
            val lens = Offset(
                x = pivot.x + tubeLength,
                y = pivot.y,
            ).rotateAround(pivot, tubeAngleDeg)
            return ObservatoryGeometry(
                unit = unit,
                center = center,
                baseY = center.y,
                buildingWidth = buildingWidth,
                buildingHeight = buildingHeight,
                domeBaseY = domeBaseY,
                domeRadiusX = domeRadiusX,
                domeRadiusY = domeRadiusY,
                pivot = pivot,
                tubeLength = tubeLength,
                tubeHeight = tubeHeight,
                tubeAngleDeg = tubeAngleDeg,
                lens = lens,
                target = target,
                objective = objective,
            )
        }
    }
}

internal data class ObservatoryDomePanelGeometry(
    val centerX: Float,
    val topWidth: Float,
    val bottomWidth: Float,
    val topY: Float,
    val bottomY: Float,
)

internal data class ObservatoryTelescopeTubeGeometry(
    val rearEdge: Offset,
    val frontEdge: Offset,
)

internal fun observatoryDomePanelGeometry(
    geometry: ObservatoryGeometry,
    azimuth: Float,
): ObservatoryDomePanelGeometry {
    val progress = azimuth.coerceIn(0f, 1f)
    val centerX = observatoryLerp(
        start = geometry.center.x - geometry.unit * 0.045f,
        end = geometry.center.x + geometry.unit * 0.045f,
        fraction = progress,
    )
    return ObservatoryDomePanelGeometry(
        centerX = centerX,
        topWidth = geometry.domeRadiusX * 0.34f,
        bottomWidth = geometry.domeRadiusX * 0.54f,
        topY = geometry.domeBaseY - geometry.domeRadiusY * 0.96f,
        bottomY = geometry.domeBaseY - geometry.domeRadiusY * 0.16f,
    )
}

internal fun observatoryTelescopeTubeGeometry(
    geometry: ObservatoryGeometry,
): ObservatoryTelescopeTubeGeometry = ObservatoryTelescopeTubeGeometry(
    rearEdge = geometry.pivot,
    frontEdge = geometry.lens,
)

internal fun targetFor(
    size: Size,
    unit: Float,
    domeBaseY: Float,
    domeRadiusY: Float,
    azimuth: Float,
    altitude: Float,
): Offset {
    val edgeMargin = unit * 0.07f
    val topY = edgeMargin
    val observatoryTopY = domeBaseY - domeRadiusY * 1.05f
    return Offset(
        x = observatoryLerp(edgeMargin, size.width - edgeMargin, azimuth),
        y = observatoryLerp(observatoryTopY, topY, altitude),
    )
}

internal fun Offset.rotateAround(pivot: Offset, degrees: Float): Offset {
    val radians = degrees.toRadians()
    val translatedX = x - pivot.x
    val translatedY = y - pivot.y
    return Offset(
        x = pivot.x + translatedX * cos(radians) - translatedY * sin(radians),
        y = pivot.y + translatedX * sin(radians) + translatedY * cos(radians),
    )
}

internal fun Float.toRadians(): Float = (this / 180f * PI).toFloat()

internal fun Float.toDegrees(): Float = (this * 180f / PI).toFloat()

internal fun observatoryLerp(start: Float, end: Float, fraction: Float): Float =
    start + (end - start) * fraction.coerceIn(0f, 1f)
