package fr.aumombelli.dstcg

import androidx.compose.ui.unit.dp
import fr.aumombelli.dstcg.feature.home.calculateHomeResponsiveLayout
import fr.aumombelli.dstcg.ui.component.TRADING_CARD_WIDTH_OVER_HEIGHT
import fr.aumombelli.dstcg.ui.motion.BrandLogoVariant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeResponsiveLayoutTest {
    @Test
    fun layout_scales_logo_and_menu_buttons_with_viewport_height() {
        val compactLayout = calculateHomeResponsiveLayout(
            availableWidth = 367.dp,
            availableHeight = 625.dp,
            logoVariant = BrandLogoVariant.Lockup19,
        )
        val tallLayout = calculateHomeResponsiveLayout(
            availableWidth = 349.dp,
            availableHeight = 702.dp,
            logoVariant = BrandLogoVariant.Lockup19,
        )

        assertTrue(tallLayout.logoBadgeLandingSize > compactLayout.logoBadgeLandingSize)
        assertTrue(tallLayout.menuButtonSize > compactLayout.menuButtonSize)
    }

    @Test
    fun layout_centers_hero_between_logo_and_menu_buttons() {
        val layout = calculateHomeResponsiveLayout(
            availableWidth = 367.dp,
            availableHeight = 625.dp,
            logoVariant = BrandLogoVariant.Lockup19,
        )

        val expectedHeroCenter = (layout.logoBottom + layout.menuButtonTop) / 2f
        val actualHeroCenter = layout.heroCardTop + (layout.heroCardHeight / 2f)

        assertEquals(expectedHeroCenter.value, actualHeroCenter.value, 0.01f)
    }

    @Test
    fun layout_preserves_trading_card_ratio() {
        val layout = calculateHomeResponsiveLayout(
            availableWidth = 349.dp,
            availableHeight = 702.dp,
            logoVariant = BrandLogoVariant.Lockup19,
        )

        assertEquals(
            TRADING_CARD_WIDTH_OVER_HEIGHT,
            layout.heroCardWidth.value / layout.heroCardHeight.value,
            0.01f,
        )
    }
}
