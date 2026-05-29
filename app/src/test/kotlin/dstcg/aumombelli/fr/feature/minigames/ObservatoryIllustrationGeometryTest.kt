package fr.aumombelli.dstcg.feature.minigames

import androidx.compose.ui.geometry.Size
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

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
}
