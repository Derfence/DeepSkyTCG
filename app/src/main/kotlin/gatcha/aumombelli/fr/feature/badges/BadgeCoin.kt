package fr.aumombelli.gatcha.feature.badges

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.unit.width
import fr.aumombelli.gatcha.ui.component.ExtensionLogoMark
import fr.aumombelli.gatcha.ui.component.TwinklingStarsOverlay
import fr.aumombelli.gatcha.ui.theme.SkyQualityPalette
import fr.aumombelli.gatcha.ui.theme.skyQualityPalette

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
            logoSize = (coinSize * 0.60f).coerceAtLeast(40.dp),
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
    onClick: (() -> Unit)? = null,
) {
    val palette = badgePalette(badge)
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

        if (badge.requirementType != BadgeRequirementType.SkyQuality) {
            TwinklingStarsOverlay(
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

        ExtensionLogoMark(
            extensionId = badge.extensionId,
            compact = false,
            emblemSize = logoSize,
            modifier = Modifier.size(logoSize),
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
    else -> Brush.verticalGradient(
        colors = listOf(
            palette.top,
            palette.bottom,
        ),
    )
}
