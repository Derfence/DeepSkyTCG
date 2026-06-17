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
    fun layout_preserves_safe_space_around_hero_card() {
        val layout = calculateHomeResponsiveLayout(
            availableWidth = 367.dp,
            availableHeight = 625.dp,
            logoVariant = BrandLogoVariant.Lockup19,
        )

        assertTrue(layout.heroCardWidth <= 367.dp - 24.dp)
        assertTrue(layout.heroCardTop >= layout.logoBottom + 12.dp)
        assertTrue(layout.heroCardTop + layout.heroCardHeight <= layout.menuButtonTop - 12.dp)
    }

    @Test
    fun layout_uses_more_than_legacy_fixed_card_width_when_space_allows() {
        val layout = calculateHomeResponsiveLayout(
            availableWidth = 430.dp,
            availableHeight = 1_200.dp,
            logoVariant = BrandLogoVariant.Lockup19,
        )

        assertTrue(layout.heroCardWidth > 320.dp)
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
