package fr.aumombelli.dstcg.feature.badges

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import fr.aumombelli.dstcg.performance.LocalAppPerformanceProfile
import fr.aumombelli.dstcg.ui.component.ExtensionLogoMark
import fr.aumombelli.dstcg.ui.motion.BrandLogoVariant
import fr.aumombelli.dstcg.ui.motion.LaunchLogoMark
import fr.aumombelli.dstcg.ui.component.TwinklingStarsOverlay
import fr.aumombelli.dstcg.ui.theme.SkyQualityPalette
import fr.aumombelli.dstcg.ui.theme.skyQualityPalette

@Composable
internal fun BadgeCoinCard(
    badge: BadgeItem,
    modifier: Modifier = Modifier,
    coinSize: Dp = 92.dp,
    isCoinHidden: Boolean = false,
    onCoinPositioned: (Rect) -> Unit = {},
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier,
    ) {
        BadgeCoinFace(
            badge = badge,
            modifier = Modifier
                .size(coinSize)
                .alpha(if (isCoinHidden) 0f else 1f)
                .onGloballyPositioned { coordinates ->
                    onCoinPositioned(coordinates.boundsInRoot())
                }
                .testTag("badge-coin-${badge.id}"),
            logoSize = badgeCoinLogoSize(badge = badge, coinSize = coinSize),
            centerMarkTestTag = "badge-center-mark-${badge.id}",
            onClick = onClick,
        )
        Text(
            text = badge.title,
            color = Color.White,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            maxLines = 2,
            modifier = Modifier.width(coinSize + 20.dp),
        )
    }
}

@Composable
internal fun BadgeCoinFace(
    badge: BadgeItem,
    modifier: Modifier = Modifier,
    logoSize: Dp = 56.dp,
    centerMarkTestTag: String? = null,
    onClick: (() -> Unit)? = null,
) {
    val palette = badgePalette(badge)
    val performanceProfile = LocalAppPerformanceProfile.current
    val backgroundBrush = badgeBackgroundBrush(badge, palette)
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        palette.glow.copy(alpha = 0.78f),
                        Color.Transparent,
                    ),
                    center = Offset(0.32f, 0.24f),
                    radius = 1.2f,
                ),
            )
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onClick,
                    )
                } else {
                    Modifier
                },
            ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(
                    backgroundBrush,
                ),
        )

        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val outerRadius = size.minDimension * 0.48f
            val innerRadius = size.minDimension * 0.40f

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.18f),
                        Color.Transparent,
                    ),
                    center = Offset(size.width * 0.32f, size.height * 0.22f),
                    radius = size.minDimension * 0.64f,
                ),
                radius = outerRadius,
                center = center,
            )
            drawCircle(
                color = Color(0xFFF7E7C0).copy(alpha = 0.92f),
                radius = outerRadius,
                center = center,
                style = Stroke(width = size.minDimension * 0.05f),
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.22f),
                radius = innerRadius,
                center = center,
                style = Stroke(width = size.minDimension * 0.02f),
            )
        }

        if (badgeUsesTwinklingStars(badge)) {
            TwinklingStarsOverlay(
                animated = performanceProfile.enableAnimatedBadgeCoins,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape),
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.14f),
                            Color.Transparent,
                        ),
                        center = Offset(220f, 260f),
                        radius = 280f,
                    ),
                ),
        )

        BadgeCenterMark(
            badge = badge,
            logoSize = logoSize,
            modifier = Modifier.size(logoSize),
            testTag = centerMarkTestTag,
        )

        if (!badge.isUnlocked) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(Color(0xBB777777)),
            )
        }
    }
}

private fun badgePalette(badge: BadgeItem): SkyQualityPalette = when (badge.requirementType) {
    BadgeRequirementType.FirstPackOpened -> SkyQualityPalette(
        top = Color(0xFFF8C95E),
        bottom = Color(0xFFB66C10),
        glow = Color(0xAAFFE29B),
        mist = Color(0x55F8BE57),
    )
    BadgeRequirementType.SkyQuality -> skyQualityPalette(badge.skyQualityCode.orEmpty())
    BadgeRequirementType.Holographic -> SkyQualityPalette(
        top = Color(0xFF9EA4B1),
        bottom = Color(0xFF555A67),
        glow = Color(0x88D9E1F2),
        mist = Color(0x44C6D2E6),
    )
    BadgeRequirementType.MountainHolographic -> skyQualityPalette("mountain")
    BadgeRequirementType.PerfectCollection -> SkyQualityPalette(
        top = Color(0xFFB5BCCE),
        bottom = Color(0xFF596274),
        glow = Color(0x99DCE4FF),
        mist = Color(0x55C9D7F0),
    )
}

private fun badgeBackgroundBrush(
    badge: BadgeItem,
    palette: SkyQualityPalette,
): Brush = when (badge.requirementType) {
    BadgeRequirementType.PerfectCollection -> Brush.verticalGradient(
        colorStops = arrayOf(
            0.00f to skyQualityPalette("mountain").top,
            0.24f to skyQualityPalette("mountain").top,
            0.25f to skyQualityPalette("rural").top,
            0.49f to skyQualityPalette("rural").top,
            0.50f to skyQualityPalette("suburban").top,
            0.74f to skyQualityPalette("suburban").top,
            0.75f to skyQualityPalette("city").top,
            1.00f to skyQualityPalette("city").bottom,
        ),
    )
    BadgeRequirementType.FirstPackOpened -> Brush.verticalGradient(
        colors = listOf(
            Color(0xFFFFD77D),
            Color(0xFFE09B24),
            Color(0xFF8D4E0B),
        ),
    )
    else -> Brush.verticalGradient(
        colors = listOf(
            palette.top,
            palette.bottom,
        ),
    )
}

@Composable
private fun BadgeCenterMark(
    badge: BadgeItem,
    logoSize: Dp,
    modifier: Modifier = Modifier,
    testTag: String? = null,
) {
    val taggedModifier = testTag?.let { Modifier.testTag(it) } ?: Modifier
    Box(modifier = taggedModifier) {
        if (badge.requirementType == BadgeRequirementType.FirstPackOpened) {
            LaunchLogoMark(
                variant = BrandLogoVariant.Badge17,
                emblemSize = logoSize,
                modifier = modifier,
            )
        } else {
            ExtensionLogoMark(
                extensionId = badge.extensionId,
                compact = false,
                emblemSize = logoSize,
                modifier = modifier,
            )
        }
    }
}

internal fun badgeCoinLogoScale(badge: BadgeItem): Float = when (badge.requirementType) {
    BadgeRequirementType.FirstPackOpened -> 0.72f
    else -> 0.60f
}

internal fun badgeCoinLogoSize(
    badge: BadgeItem,
    coinSize: Dp,
): Dp = (coinSize * badgeCoinLogoScale(badge)).coerceAtLeast(40.dp)

private fun badgeUsesTwinklingStars(badge: BadgeItem): Boolean = when (badge.requirementType) {
    BadgeRequirementType.FirstPackOpened,
    BadgeRequirementType.SkyQuality,
    -> false
    BadgeRequirementType.Holographic,
    BadgeRequirementType.MountainHolographic,
    BadgeRequirementType.PerfectCollection,
    -> true
}
