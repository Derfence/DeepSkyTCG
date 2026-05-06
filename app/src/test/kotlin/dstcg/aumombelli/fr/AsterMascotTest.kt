package fr.aumombelli.dstcg

import fr.aumombelli.dstcg.ui.component.AsterAnchor
import fr.aumombelli.dstcg.ui.component.AsterFace
import fr.aumombelli.dstcg.ui.component.AsterHand
import fr.aumombelli.dstcg.ui.component.AsterHandSide
import fr.aumombelli.dstcg.ui.component.AsterMascotOverlayZIndex
import fr.aumombelli.dstcg.ui.component.AsterMascotScale
import fr.aumombelli.dstcg.ui.component.AsterMascotSizeMultiplier
import fr.aumombelli.dstcg.ui.component.AsterMascotSpec
import fr.aumombelli.dstcg.ui.component.LayeredSvgAsset
import fr.aumombelli.dstcg.ui.component.asterMascotWidthForContainer
import fr.aumombelli.dstcg.ui.component.assetLayers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AsterMascotTest {
    @Test
    fun `asset layers are ordered from back to front`() {
        val layers = AsterMascotSpec(
            face = AsterFace.BigSmile,
            hand = AsterHand.Point,
            handSide = AsterHandSide.Left,
            anchor = AsterAnchor.BottomEnd,
        ).assetLayers()

        assertEquals(
            listOf(
                LayeredSvgAsset("aster/hair.svg"),
                LayeredSvgAsset("aster/suit.svg"),
                LayeredSvgAsset("aster/face3.svg"),
                LayeredSvgAsset("aster/bowtie.svg"),
                LayeredSvgAsset("aster/hand1.svg"),
            ),
            layers,
        )
    }

    @Test
    fun `right hand mirrors the original hand asset`() {
        val layers = AsterMascotSpec(
            face = AsterFace.SmileLookingRight,
            hand = AsterHand.Wrench,
            handSide = AsterHandSide.Right,
            anchor = AsterAnchor.BottomStart,
        ).assetLayers()

        assertEquals(
            LayeredSvgAsset(
                assetPath = "aster/hand3.svg",
                mirrorHorizontally = true,
            ),
            layers.last(),
        )
    }

    @Test
    fun `both hands render left hand then mirrored right hand`() {
        val layers = AsterMascotSpec(
            face = AsterFace.Smile,
            hand = AsterHand.Open,
            handSide = AsterHandSide.Left,
            anchor = AsterAnchor.BottomCenter,
            showBothHands = true,
        ).assetLayers()

        assertEquals(
            listOf(
                LayeredSvgAsset("aster/hand2.svg"),
                LayeredSvgAsset(
                    assetPath = "aster/hand2.svg",
                    mirrorHorizontally = true,
                ),
            ),
            layers.takeLast(2),
        )
    }

    @Test
    fun `both hands can render different hand assets`() {
        val layers = AsterMascotSpec(
            face = AsterFace.BigSmile,
            hand = AsterHand.Cards,
            handSide = AsterHandSide.Left,
            anchor = AsterAnchor.BottomCenter,
            showBothHands = true,
            mirroredHand = AsterHand.Telescope,
        ).assetLayers()

        assertEquals(
            listOf(
                LayeredSvgAsset("aster/hand4.svg"),
                LayeredSvgAsset(
                    assetPath = "aster/hand5.svg",
                    mirrorHorizontally = true,
                ),
            ),
            layers.takeLast(2),
        )
    }

    @Test
    fun `bottom start anchor mirrors hair asset`() {
        val layers = AsterMascotSpec(
            face = AsterFace.SmileLookingRight,
            hand = AsterHand.Telescope,
            handSide = AsterHandSide.Right,
            anchor = AsterAnchor.BottomStart,
        ).assetLayers()

        assertEquals(
            LayeredSvgAsset(
                assetPath = "aster/hair.svg",
                mirrorHorizontally = true,
            ),
            layers.first(),
        )
    }

    @Test
    fun `telescope hand uses hand five asset`() {
        val layers = AsterMascotSpec(
            face = AsterFace.SmileLookingRight,
            hand = AsterHand.Telescope,
            handSide = AsterHandSide.Left,
            anchor = AsterAnchor.BottomEnd,
        ).assetLayers()

        assertEquals(
            LayeredSvgAsset("aster/hand5.svg"),
            layers.last(),
        )
    }

    @Test
    fun `overlay z index keeps Aster above onboarding overlays`() {
        assertTrue(AsterMascotOverlayZIndex > 0f)
    }

    @Test
    fun `mascot width is scaled one and a half times`() {
        assertEquals(1.5f, AsterMascotSizeMultiplier, 0.001f)
        assertEquals(167.4f, asterMascotWidthForContainer(360f, AsterMascotScale.Standard), 0.001f)
        assertEquals(129.6f, asterMascotWidthForContainer(360f, AsterMascotScale.Compact), 0.001f)
        assertEquals(
            259.2f,
            asterMascotWidthForContainer(
                containerWidth = 360f,
                scale = AsterMascotScale.Compact,
                sizeMultiplier = 2f,
            ),
            0.001f,
        )
    }
}
