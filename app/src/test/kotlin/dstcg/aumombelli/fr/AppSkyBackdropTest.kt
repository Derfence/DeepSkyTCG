package fr.aumombelli.dstcg

import fr.aumombelli.dstcg.ui.motion.SkyBackdropVariant
import fr.aumombelli.dstcg.ui.motion.buildBuildingWindowLights
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppSkyBackdropTest {
    @Test
    fun `building window lights stay deterministic and scoped to city and suburban backdrops`() {
        val cityLights = buildBuildingWindowLights(SkyBackdropVariant.City)
        val cityLightsAgain = buildBuildingWindowLights(SkyBackdropVariant.City)
        val suburbanLights = buildBuildingWindowLights(SkyBackdropVariant.Suburban)

        assertEquals(cityLights, cityLightsAgain)
        assertTrue(cityLights.isNotEmpty())
        assertTrue(suburbanLights.isNotEmpty())
        assertTrue(cityLights.size > suburbanLights.size)
        assertTrue(buildBuildingWindowLights(SkyBackdropVariant.Rural).isEmpty())
        assertTrue(buildBuildingWindowLights(SkyBackdropVariant.Mountain).isEmpty())
    }

    @Test
    fun `city window lights cover the full skyline band`() {
        val lights = buildBuildingWindowLights(SkyBackdropVariant.City)

        assertTrue(lights.isNotEmpty())
        assertTrue(lights.any { it.x > 0.5f })
        lights.forEach { light ->
            assertTrue(light.x in 0f..1f)
            assertTrue(light.yOffset in -0.30f..-0.005f)
            assertTrue(light.width > 0f)
            assertTrue(light.height > 0f)
            assertTrue(light.alpha in 0.46f..0.80f)
            assertTrue(light.x + light.width / 2f <= 1f)
        }
    }

    @Test
    fun `suburban window lights remain inside the left skyline band`() {
        val lights = buildBuildingWindowLights(SkyBackdropVariant.Suburban)

        assertTrue(lights.isNotEmpty())
        lights.forEach { light ->
            assertTrue(light.x in 0f..0.5f)
            assertTrue(light.yOffset in -0.30f..-0.005f)
            assertTrue(light.width > 0f)
            assertTrue(light.height > 0f)
            assertTrue(light.alpha in 0.46f..0.80f)
            assertTrue(light.x + light.width / 2f <= 0.5f)
        }
    }
}
