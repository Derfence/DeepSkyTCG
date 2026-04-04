package fr.aumombelli.dstcg

import fr.aumombelli.dstcg.ui.motion.BurstParticleMotion
import fr.aumombelli.dstcg.ui.motion.BrandLogoVariant
import fr.aumombelli.dstcg.ui.motion.ExtensionAnimationStyle
import fr.aumombelli.dstcg.ui.motion.SkyBackdropVariant
import fr.aumombelli.dstcg.ui.motion.buildBurstParticleSpecs
import fr.aumombelli.dstcg.ui.motion.burstRarityLabelsUpTo
import fr.aumombelli.dstcg.ui.motion.calculateBookPose
import fr.aumombelli.dstcg.ui.motion.extensionAnimationSpec
import fr.aumombelli.dstcg.ui.motion.extensionPointReveal
import fr.aumombelli.dstcg.ui.motion.homeLogoVariantFor
import fr.aumombelli.dstcg.ui.motion.pickSkyBackdropVariant
import fr.aumombelli.dstcg.ui.motion.projectExtensionPattern
import fr.aumombelli.dstcg.ui.motion.summarizePackOpening
import fr.aumombelli.dstcg.model.toDisplayCard
import fr.aumombelli.dstcg.model.toDisplayVariant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppMotionTest {
    @Test
    fun `book pose starts closed with front cover facing the viewer`() {
        val pose = calculateBookPose(0f)

        assertEquals(0f, pose.openAngle, 0.001f)
        assertTrue(pose.pitchX > 0f)
        assertTrue(pose.yawY < 0f)
        assertTrue(pose.frontCoverDominance > 0.95f)
        assertTrue(pose.spreadWidth < 0.9f)
    }

    @Test
    fun `book pose mid transition has opened pages and wider spread`() {
        val closedPose = calculateBookPose(0f)
        val midPose = calculateBookPose(0.5f)

        assertTrue(midPose.openAngle > 45f)
        assertTrue(midPose.pageFan > 0f)
        assertTrue(midPose.spreadWidth > closedPose.spreadWidth)
        assertTrue(midPose.shadowAlpha > closedPose.shadowAlpha)
    }

    @Test
    fun `book pose ends fully opened without overshooting`() {
        val pose = calculateBookPose(1f)

        assertEquals(142f, pose.openAngle, 0.001f)
        assertEquals(10f, pose.pitchX, 0.001f)
        assertEquals(-6f, pose.yawY, 0.001f)
        assertEquals(12f, pose.pageFan, 0.001f)
        assertTrue(pose.spreadWidth <= 1.27f)
    }

    @Test
    fun `sky backdrop selection wraps deterministically`() {
        assertEquals(SkyBackdropVariant.City, pickSkyBackdropVariant(0))
        assertEquals(SkyBackdropVariant.City, pickSkyBackdropVariant(4))
        assertEquals(SkyBackdropVariant.Mountain, pickSkyBackdropVariant(3))
    }

    @Test
    fun `home lockup variant always resolves to lockup 19`() {
        assertEquals(BrandLogoVariant.Lockup19, homeLogoVariantFor(SkyBackdropVariant.City))
        assertEquals(BrandLogoVariant.Lockup19, homeLogoVariantFor(SkyBackdropVariant.Suburban))
        assertEquals(BrandLogoVariant.Lockup19, homeLogoVariantFor(SkyBackdropVariant.Rural))
        assertEquals(BrandLogoVariant.Lockup19, homeLogoVariantFor(SkyBackdropVariant.Mountain))
    }

    @Test
    fun `extension animation resolves big dipper only for astronomes en herbe`() {
        val astronomesSpec = extensionAnimationSpec("astronomes-en-herbe")
        val fallbackSpec = extensionAnimationSpec("unknown-extension")

        assertEquals(ExtensionAnimationStyle.BigDipper, astronomesSpec.style)
        assertEquals(7, astronomesSpec.starPattern.size)
        assertEquals(7, astronomesSpec.lineConnections.size)
        assertEquals(ExtensionAnimationStyle.NeutralSky, fallbackSpec.style)
        assertTrue(fallbackSpec.starPattern.isEmpty())
    }

    @Test
    fun `extension pattern projection preserves geometry on non square canvas`() {
        val spec = extensionAnimationSpec("astronomes-en-herbe")
        val projection = projectExtensionPattern(
            spec = spec,
            canvasWidth = 240f,
            canvasHeight = 120f,
        )
        val projectedStart = projection.project(spec.starPattern.first())
        val projectedEnd = projection.project(spec.starPattern[4])
        val rawStart = spec.starPattern.first()
        val rawEnd = spec.starPattern[4]

        assertEquals(
            projection.scale,
            (projectedEnd.x - projectedStart.x) / (rawEnd.x - rawStart.x),
            0.001f,
        )
        assertEquals(
            -projection.scale,
            (projectedEnd.y - projectedStart.y) / (rawEnd.y - rawStart.y),
            0.001f,
        )
    }

    @Test
    fun `extension pattern projection preserves original upward orientation`() {
        val spec = extensionAnimationSpec("astronomes-en-herbe")
        val projection = projectExtensionPattern(
            spec = spec,
            canvasWidth = 240f,
            canvasHeight = 120f,
        )
        val dubhe = projection.project(spec.starPattern[4])
        val merak = projection.project(spec.starPattern[5])
        val megrez = projection.project(spec.starPattern[3])
        val phecda = projection.project(spec.starPattern[6])

        assertTrue(dubhe.y < merak.y)
        assertTrue(megrez.y < phecda.y)
    }

    @Test
    fun `extension points appear when their first connected line starts`() {
        val spec = extensionAnimationSpec("astronomes-en-herbe")

        assertEquals(
            0f,
            extensionPointReveal(
                spec = spec,
                pointIndex = 0,
                lineProgress = 0f,
                isReversing = false,
                revealWindow = 0.25f,
            ),
            0.001f,
        )
        assertTrue(
            extensionPointReveal(
                spec = spec,
                pointIndex = 0,
                lineProgress = 0.01f,
                isReversing = false,
                revealWindow = 0.25f,
            ) > 0f,
        )
    }

    @Test
    fun `extension points disappear when their last full line starts receding`() {
        val spec = extensionAnimationSpec("astronomes-en-herbe")

        assertEquals(
            1f,
            extensionPointReveal(
                spec = spec,
                pointIndex = 4,
                lineProgress = 1f,
                isReversing = true,
                revealWindow = 0.25f,
            ),
            0.001f,
        )
        assertEquals(
            0f,
            extensionPointReveal(
                spec = spec,
                pointIndex = 4,
                lineProgress = 0.56f,
                isReversing = true,
                revealWindow = 0.25f,
            ),
            0.001f,
        )
    }

    @Test
    fun `pack opening summary exposes max rarity and holographic presence`() {
        val commonCard = testCardDefinition("ALP-001", rarityLabel = "Common").toDisplayCard(
            extensionName = "Astronomes en herbe",
            activeVariant = testPackCard(
                cardId = "ALP-001",
                name = "Nebuleuse d'Orion",
                rarityLabel = "Common",
                imageRef = "m42",
            ).variant.toDisplayVariant(),
        )
        val epicHoloCard = testCardDefinition("ALP-777", rarityLabel = "Epic").toDisplayCard(
            extensionName = "Astronomes en herbe",
            activeVariant = testPackCard(
                cardId = "ALP-777",
                name = "Comete",
                rarityLabel = "Epic",
                imageRef = "comet",
                finish = "holographic",
                finishLabel = "Holographique",
                isHolographic = true,
            ).variant.toDisplayVariant(),
        )

        val summary = summarizePackOpening(listOf(commonCard, epicHoloCard))

        assertEquals("Epic", summary?.highestRarityLabel)
        assertTrue(summary?.hasHolographicCard == true)
    }

    @Test
    fun `pack opening summary detects non holographic bursts`() {
        val rareCard = testCardDefinition("ALP-002", rarityLabel = "Rare").toDisplayCard(
            extensionName = "Astronomes en herbe",
            activeVariant = testPackCard(
                cardId = "ALP-002",
                name = "Galaxie d'Andromede",
                rarityLabel = "Rare",
                imageRef = "m31",
                skyQuality = "rural",
                skyQualityLabel = "Campagne",
            ).variant.toDisplayVariant(),
        )

        val summary = summarizePackOpening(listOf(rareCard))

        assertEquals("Rare", summary?.highestRarityLabel)
        assertFalse(summary?.hasHolographicCard == true)
    }

    @Test
    fun `burst rarity labels include every rarity up to the best card`() {
        assertEquals(listOf("Common"), burstRarityLabelsUpTo("Common"))
        assertEquals(listOf("Common", "Uncommon"), burstRarityLabelsUpTo("Uncommon"))
        assertEquals(listOf("Common", "Uncommon", "Rare"), burstRarityLabelsUpTo("Rare"))
        assertEquals(listOf("Common", "Uncommon", "Rare", "Epic"), burstRarityLabelsUpTo("Epic"))
    }

    @Test
    fun `burst particles are staggered and travel beyond the viewport`() {
        val specs = buildBurstParticleSpecs(
            highestRarityLabel = "Rare",
            hasHolographicBurst = false,
        )

        assertTrue(specs.any { it.delayFraction > 0f })
        assertTrue(
            specs.filter { it.motion == BurstParticleMotion.Radial }
                .all { it.travelFactor > 1f },
        )
    }

    @Test
    fun `holographic burst adds falling star rain only when needed`() {
        val nonHoloSpecs = buildBurstParticleSpecs(
            highestRarityLabel = "Epic",
            hasHolographicBurst = false,
        )
        val holoSpecs = buildBurstParticleSpecs(
            highestRarityLabel = "Epic",
            hasHolographicBurst = true,
        )

        assertFalse(nonHoloSpecs.any { it.motion == BurstParticleMotion.Falling })
        assertTrue(holoSpecs.any { it.motion == BurstParticleMotion.Falling })
    }
}
