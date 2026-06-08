package fr.aumombelli.dstcg.feature.minigames

import androidx.compose.ui.geometry.Size
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.sqrt

class ObservatoryIllustrationGeometryTest {
    @Test
    fun `azimuth spans the whole observation width`() {
        val size = Size(width = 1000f, height = 1000f)
        val leftTarget = targetFor(
            size = size,
            unit = 820f,
            domeBaseY = 534f,
            domeRadiusY = 98f,
            azimuth = 0f,
            altitude = 0.5f,
        )
        val rightTarget = targetFor(
            size = size,
            unit = 820f,
            domeBaseY = 534f,
            domeRadiusY = 98f,
            azimuth = 1f,
            altitude = 0.5f,
        )

        assertTrue(leftTarget.x < size.width * 0.08f)
        assertTrue(rightTarget.x > size.width * 0.92f)
    }

    @Test
    fun `altitude spans from observatory top to scene top`() {
        val size = Size(width = 1000f, height = 1000f)
        val lowTarget = targetFor(
            size = size,
            unit = 820f,
            domeBaseY = 534f,
            domeRadiusY = 98f,
            azimuth = 0.5f,
            altitude = 0f,
        )
        val highTarget = targetFor(
            size = size,
            unit = 820f,
            domeBaseY = 534f,
            domeRadiusY = 98f,
            azimuth = 0.5f,
            altitude = 1f,
        )

        assertTrue(lowTarget.y > size.height * 0.40f)
        assertTrue(highTarget.y < size.height * 0.08f)
    }

    @Test
    fun `telescope tube pivots from its rear edge at dome center`() {
        val size = Size(width = 1000f, height = 1000f)
        val geometry = ObservatoryGeometry.from(
            size = size,
            state = observatoryState(azimuth = 0.5f, altitude = 0.68f),
        )
        val tube = observatoryTelescopeTubeGeometry(geometry)
        val frontDistance = sqrt(
            (tube.frontEdge.x - tube.rearEdge.x) * (tube.frontEdge.x - tube.rearEdge.x) +
                (tube.frontEdge.y - tube.rearEdge.y) * (tube.frontEdge.y - tube.rearEdge.y),
        )

        assertEquals(geometry.center.x, tube.rearEdge.x, 0.001f)
        assertEquals(geometry.domeBaseY - geometry.domeRadiusY * 0.38f, tube.rearEdge.y, 0.001f)
        assertEquals(geometry.pivot.x, tube.rearEdge.x, 0.001f)
        assertEquals(geometry.pivot.y, tube.rearEdge.y, 0.001f)
        assertEquals(geometry.tubeLength, frontDistance, 0.001f)
    }

    @Test
    fun `dome panel tracks telescope azimuth`() {
        val size = Size(width = 1000f, height = 1000f)
        val leftGeometry = ObservatoryGeometry.from(
            size = size,
            state = observatoryState(azimuth = 0f, altitude = 0.5f),
        )
        val rightGeometry = ObservatoryGeometry.from(
            size = size,
            state = observatoryState(azimuth = 1f, altitude = 0.5f),
        )
        val leftPanel = observatoryDomePanelGeometry(leftGeometry, azimuth = 0f)
        val rightPanel = observatoryDomePanelGeometry(rightGeometry, azimuth = 1f)

        assertEquals(leftGeometry.pivot.x, leftPanel.centerX, 0.001f)
        assertEquals(rightGeometry.pivot.x, rightPanel.centerX, 0.001f)
        assertTrue(leftPanel.centerX < rightPanel.centerX)
    }

    @Test
    fun `dome panel does not track tube altitude`() {
        val size = Size(width = 1000f, height = 1000f)
        val lowGeometry = ObservatoryGeometry.from(
            size = size,
            state = observatoryState(azimuth = 0.72f, altitude = 0.12f),
        )
        val highGeometry = ObservatoryGeometry.from(
            size = size,
            state = observatoryState(azimuth = 0.72f, altitude = 0.92f),
        )
        val lowPanel = observatoryDomePanelGeometry(lowGeometry, azimuth = 0.72f)
        val highPanel = observatoryDomePanelGeometry(highGeometry, azimuth = 0.72f)

        assertTrue(abs(lowGeometry.tubeAngleDeg - highGeometry.tubeAngleDeg) > 1f)
        assertEquals(lowPanel.centerX, highPanel.centerX, 0.001f)
        assertEquals(lowPanel.topY, highPanel.topY, 0.001f)
        assertEquals(lowPanel.bottomY, highPanel.bottomY, 0.001f)
    }

    @Test
    fun `dome panel stays inside the dome at extreme azimuths`() {
        val size = Size(width = 1000f, height = 1000f)

        listOf(0f, 1f).forEach { azimuth ->
            val geometry = ObservatoryGeometry.from(
                size = size,
                state = observatoryState(azimuth = azimuth, altitude = 0.5f),
            )
            val panel = observatoryDomePanelGeometry(geometry, azimuth = azimuth)

            assertTrue(panel.centerX - panel.bottomWidth * 0.50f >= geometry.center.x - geometry.domeRadiusX)
            assertTrue(panel.centerX + panel.bottomWidth * 0.50f <= geometry.center.x + geometry.domeRadiusX)
            assertTrue(panel.topY >= geometry.domeBaseY - geometry.domeRadiusY)
            assertTrue(panel.bottomY <= geometry.domeBaseY)
        }
    }

    @Test
    fun `observation beam is hidden while closing dome`() {
        assertTrue(!shouldDrawObservationBeam(ObservatoryStep.CloseDome, domeProgress = 1f))
        assertTrue(shouldDrawObservationBeam(ObservatoryStep.Capture, domeProgress = 1f))
    }

    @Test
    fun `cloud band stays at fixed height and spans full scene width`() {
        val sceneSize = Size(width = 1000f, height = 800f)
        val band = observatoryCloudBand(
            sceneSize = sceneSize,
            unit = 656f,
        )

        assertEquals(sceneSize.height * ObservatoryCloudBandCenterYRatio, band.centerY, 0.001f)
        assertTrue(band.topLeft.x < 0f)
        assertTrue(band.topLeft.x + band.size.width > sceneSize.width)
    }

    @Test
    fun `visual readiness follows the animated reticle position`() {
        val readiness = observatoryVisualReadiness(
            azimuth = 0.42f,
            altitude = 0.56f,
            focus = 0.61f,
            targetAzimuth = 0.80f,
            targetAltitude = 0.82f,
            targetFocus = 0.61f,
            tolerance = 0.06f,
        )

        assertFalse(readiness.alignmentReady)
        assertFalse(readiness.focusReady)
    }

    @Test
    fun `visual readiness turns ready inside tolerance`() {
        val readiness = observatoryVisualReadiness(
            azimuth = 0.78f,
            altitude = 0.81f,
            focus = 0.59f,
            targetAzimuth = 0.80f,
            targetAltitude = 0.82f,
            targetFocus = 0.61f,
            tolerance = 0.06f,
        )

        assertTrue(readiness.alignmentReady)
        assertTrue(readiness.focusReady)
    }

    private fun observatoryState(
        azimuth: Float,
        altitude: Float,
    ): ObservatoryIllustrationState = ObservatoryIllustrationState(
        step = ObservatoryStep.Align,
        domeProgress = 1f,
        azimuth = azimuth,
        altitude = altitude,
        focus = 0.5f,
        domeReady = true,
        alignmentReady = false,
        focusReady = false,
        targetAzimuth = 0.5f,
        targetAltitude = 0.5f,
        targetFocus = 0.5f,
        cloudAlpha = 0f,
        captureProgress = 0f,
    )
}
