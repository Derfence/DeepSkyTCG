package fr.aumombelli.dstcg

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.IntSize
import fr.aumombelli.dstcg.feature.badges.badgeCelebrationEndCenter
import fr.aumombelli.dstcg.feature.badges.badgeCelebrationInitialCoinCenters
import fr.aumombelli.dstcg.feature.badges.badgeCelebrationStartCenter
import fr.aumombelli.dstcg.feature.badges.badgeCelebrationStaticTitleTopLeft
import fr.aumombelli.dstcg.feature.badges.badgeCelebrationTargetBoundsInViewport
import fr.aumombelli.dstcg.feature.badges.badgeCelebrationTitleTopLeft
import org.junit.Assert.assertEquals
import org.junit.Test

class BadgeCelebrationLayoutTest {
    @Test
    fun celebration_starts_from_screen_center() {
        val center = badgeCelebrationStartCenter(IntSize(width = 1080, height = 2400))

        assertEquals(540f, center.x, 0.01f)
        assertEquals(1200f, center.y, 0.01f)
    }

    @Test
    fun celebration_lands_above_target_top_with_configured_gap() {
        val target = Rect(left = 900f, top = 2100f, right = 1044f, bottom = 2244f)

        val endCenter = badgeCelebrationEndCenter(
            targetBounds = target,
            displayedCoinSizePx = 46f,
            gapPx = 10f,
        )

        assertEquals(972f, endCenter.x, 0.01f)
        assertEquals(2113f, endCenter.y, 0.01f)
    }

    @Test
    fun celebration_target_inside_viewport_is_unchanged() {
        val target = Rect(left = 900f, top = 1500f, right = 1044f, bottom = 1644f)

        val resolvedTarget = badgeCelebrationTargetBoundsInViewport(
            targetBounds = target,
            rootSize = IntSize(width = 1080, height = 1920),
        )

        assertEquals(target, resolvedTarget)
    }

    @Test
    fun celebration_target_outside_viewport_keeps_size_and_moves_inside_screen() {
        val target = Rect(left = 900f, top = 2100f, right = 1044f, bottom = 2244f)

        val resolvedTarget = badgeCelebrationTargetBoundsInViewport(
            targetBounds = target,
            rootSize = IntSize(width = 1080, height = 1920),
        )

        assertEquals(900f, resolvedTarget.left, 0.01f)
        assertEquals(1776f, resolvedTarget.top, 0.01f)
        assertEquals(1044f, resolvedTarget.right, 0.01f)
        assertEquals(1920f, resolvedTarget.bottom, 0.01f)
    }

    @Test
    fun celebration_title_stays_just_above_initial_badge_group() {
        val titleTopLeft = badgeCelebrationTitleTopLeft(
            titleSize = IntSize(width = 260, height = 48),
            coinCenters = listOf(Offset(x = 540f, y = 1200f)),
            groupCenterX = 540f,
            displayedCoinSizePx = 92f,
            gapPx = 12f,
        )

        assertEquals(410f, titleTopLeft.x, 0.01f)
        assertEquals(1094f, titleTopLeft.y, 0.01f)
    }

    @Test
    fun celebration_static_title_uses_initial_center_layout() {
        val titleTopLeft = badgeCelebrationStaticTitleTopLeft(
            titleSize = IntSize(width = 260, height = 48),
            startCenter = Offset(x = 540f, y = 1200f),
            badgeCount = 1,
            radius = 132f,
            displayedCoinSizePx = 92f * 1.04f,
            gapPx = 12f,
        )

        assertEquals(410f, titleTopLeft.x, 0.01f)
        assertEquals(1092.16f, titleTopLeft.y, 0.01f)
    }

    @Test
    fun celebration_initial_coin_centers_fan_out_from_start_center() {
        val centers = badgeCelebrationInitialCoinCenters(
            badgeCount = 3,
            startCenter = Offset(x = 540f, y = 1200f),
            radius = 132f,
        )

        assertEquals(540f, centers[0].x, 0.01f)
        assertEquals(1200f, centers[0].y, 0.01f)
        assertEquals(512.56f, centers[1].x, 0.02f)
        assertEquals(1178.88f, centers[1].y, 0.01f)
        assertEquals(567.44f, centers[2].x, 0.02f)
        assertEquals(1178.88f, centers[2].y, 0.01f)
    }
}
