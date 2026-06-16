package fr.aumombelli.dstcg.feature.home

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import fr.aumombelli.dstcg.ui.component.TRADING_CARD_WIDTH_OVER_HEIGHT
import fr.aumombelli.dstcg.ui.motion.BrandLogoVariant
import fr.aumombelli.dstcg.ui.motion.brandLogoLayoutSpec

internal const val HOME_LOGO_LANDING_SCALE = 0.88f

private val HomeHeroCardHorizontalSafetyPadding = 12.dp
private val HomeHeroCardVerticalSafetyGap = 12.dp

internal data class HomeResponsiveLayout(
    val heroCardWidth: Dp,
    val heroCardHeight: Dp,
    val heroCardTop: Dp,
    val menuButtonSize: Dp,
    val menuButtonTop: Dp,
    val logoBadgeBaseSize: Dp,
    val logoBadgeLandingSize: Dp,
    val logoTopPadding: Dp,
    val logoLockupHeight: Dp,
    val logoBottom: Dp,
    val logoBadgeCenterY: Dp,
)

internal fun calculateHomeResponsiveLayout(
    availableWidth: Dp,
    availableHeight: Dp,
    logoVariant: BrandLogoVariant,
): HomeResponsiveLayout {
    val logoLayoutSpec = brandLogoLayoutSpec(logoVariant)
    val menuButtonSize = (availableHeight * 0.105f)
        .coerceIn(64.dp, 78.dp)
    val logoBadgeLandingSize = (availableHeight * 0.16f)
        .coerceIn(96.dp, 124.dp)
    val logoBadgeBaseSize = logoBadgeLandingSize / HOME_LOGO_LANDING_SCALE
    val logoTopPadding = (availableHeight * 0.02f)
        .coerceIn(8.dp, 18.dp)
    val logoLockupHeight = logoBadgeLandingSize * logoLayoutSpec.heightMultiplierFromBadgeSize
    val logoBottom = logoTopPadding + logoLockupHeight
    val menuButtonTop = availableHeight - menuButtonSize
    val heroCardWidth = calculateHomeHeroCardWidth(
        availableWidth = availableWidth,
        availableMiddleGap = (menuButtonTop - logoBottom).coerceAtLeast(0.dp),
    )
    val heroCardHeight = heroCardWidth / TRADING_CARD_WIDTH_OVER_HEIGHT
    val heroCardTop = logoBottom + ((menuButtonTop - logoBottom - heroCardHeight) / 2f)
        .coerceAtLeast(0.dp)
    val logoBadgeCenterY = logoTopPadding + (logoLockupHeight * logoLayoutSpec.badgeCenterFractionY)

    return HomeResponsiveLayout(
        heroCardWidth = heroCardWidth,
        heroCardHeight = heroCardHeight,
        heroCardTop = heroCardTop,
        menuButtonSize = menuButtonSize,
        menuButtonTop = menuButtonTop,
        logoBadgeBaseSize = logoBadgeBaseSize,
        logoBadgeLandingSize = logoBadgeLandingSize,
        logoTopPadding = logoTopPadding,
        logoLockupHeight = logoLockupHeight,
        logoBottom = logoBottom,
        logoBadgeCenterY = logoBadgeCenterY,
    )
}

private fun calculateHomeHeroCardWidth(
    availableWidth: Dp,
    availableMiddleGap: Dp,
): Dp {
    val widthLimitedSize = (availableWidth - (HomeHeroCardHorizontalSafetyPadding * 2f))
        .coerceAtLeast(0.dp)
    val middleGapLimitedSize = (availableMiddleGap - (HomeHeroCardVerticalSafetyGap * 2f))
        .coerceAtLeast(0.dp) * TRADING_CARD_WIDTH_OVER_HEIGHT
    return minOf(widthLimitedSize, middleGapLimitedSize)
}
