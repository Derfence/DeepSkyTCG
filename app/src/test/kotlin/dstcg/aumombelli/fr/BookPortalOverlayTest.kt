package fr.aumombelli.dstcg

import androidx.compose.ui.geometry.Size
import fr.aumombelli.dstcg.ui.motion.calculateLibraryBookPose
import fr.aumombelli.dstcg.ui.motion.calculateLibraryBookOpeningProgress
import fr.aumombelli.dstcg.ui.motion.calculateLibraryBookTravelProgress
import fr.aumombelli.dstcg.ui.motion.calculateFlatBookPortalLayout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BookPortalOverlayTest {
    @Test
    fun `opened library book pages stay slightly smaller than the covers`() {
        val canvasSize = Size(176f, 176f)
        val closedLayout = calculateFlatBookPortalLayout(
            canvasSize = canvasSize,
            pose = calculateLibraryBookPose(0f),
        )
        val openLayout = calculateFlatBookPortalLayout(
            canvasSize = canvasSize,
            pose = calculateLibraryBookPose(1f),
        )

        assertTrue(openLayout.leftPageRect.width < closedLayout.closedCoverRect.width)
        assertTrue(openLayout.leftPageRect.height < closedLayout.closedCoverRect.height)
        assertEquals(openLayout.leftPageRect.width, openLayout.rightPageRect.width, 0.001f)
        assertEquals(openLayout.leftPageRect.height, openLayout.rightPageRect.height, 0.001f)
        assertEquals(closedLayout.closedCoverRect.width, openLayout.backCoverRect.width, 0.001f)
        assertTrue(
            kotlin.math.abs(closedLayout.closedCoverRect.height - openLayout.backCoverRect.height) <= 1f,
        )
    }

    @Test
    fun `opened library book keeps the back cover in place and unfolds the left page from the spine`() {
        val closedLayout = calculateFlatBookPortalLayout(
            canvasSize = Size(176f, 176f),
            pose = calculateLibraryBookPose(0f),
        )
        val layout = calculateFlatBookPortalLayout(
            canvasSize = Size(176f, 176f),
            pose = calculateLibraryBookPose(1f),
        )

        assertEquals(layout.leftPageRect.top, layout.rightPageRect.top, 0.001f)
        assertEquals(layout.leftPageRect.bottom, layout.rightPageRect.bottom, 0.001f)
        assertTrue(layout.rightPageRect.bottom < layout.backCoverRect.bottom)
        assertTrue(layout.rightPageRect.right < layout.backCoverRect.right)
        assertEquals(closedLayout.closedCoverRect.left, layout.backCoverRect.left, 0.001f)
        assertEquals(closedLayout.closedCoverRect.right, layout.backCoverRect.right, 0.001f)
        assertEquals(layout.spineRect.right, layout.rightPageRect.left, 0.001f)
        assertEquals(layout.spineRect.left, layout.leftPageRect.right, 0.001f)
    }

    @Test
    fun `right pages are present before the left pages begin to unfold`() {
        val layout = calculateFlatBookPortalLayout(
            canvasSize = Size(176f, 176f),
            pose = calculateLibraryBookPose(1f / 3f),
        )

        assertEquals(1f, layout.rightPagesAlpha, 0.001f)
        assertEquals(0f, layout.leftPagesAlpha, 0.001f)
        assertEquals(layout.spineRect.left, layout.leftPageRect.left, 0.001f)
        assertEquals(layout.spineRect.left, layout.leftPageRect.right, 0.001f)
    }

    @Test
    fun `left pages start to appear with the inner front cover`() {
        val layout = calculateFlatBookPortalLayout(
            canvasSize = Size(176f, 176f),
            pose = calculateLibraryBookPose(0.75f),
        )

        assertEquals(1f, layout.rightPagesAlpha, 0.001f)
        assertTrue(layout.leftPagesAlpha > 0f)
        assertTrue(layout.frontInnerCoverAlpha > 0f)
        assertTrue(layout.leftPageRect.width > 0f)
    }

    @Test
    fun `library book has completed half the travel before opening starts`() {
        val pose = calculateLibraryBookPose(1f / 3f)

        assertEquals(0f, pose.openAngle, 0.001f)
        assertEquals(0.5f, calculateLibraryBookTravelProgress(1f / 3f), 0.001f)
        assertEquals(0f, calculateLibraryBookOpeningProgress(1f / 3f), 0.001f)
    }

    @Test
    fun `library book is half open when travel completes`() {
        val pose = calculateLibraryBookPose(2f / 3f)

        assertEquals(142f * 0.5f, pose.openAngle, 0.001f)
        assertEquals(1f, calculateLibraryBookTravelProgress(2f / 3f), 0.001f)
        assertEquals(0.5f, calculateLibraryBookOpeningProgress(2f / 3f), 0.001f)
    }

    @Test
    fun `library book finishes opening once it is immobile`() {
        val pose = calculateLibraryBookPose(1f)

        assertEquals(142f, pose.openAngle, 0.001f)
        assertEquals(1f, calculateLibraryBookTravelProgress(1f), 0.001f)
        assertEquals(1f, calculateLibraryBookOpeningProgress(1f), 0.001f)
        assertTrue(pose.pageFan > 0f)
    }
}
