package fr.aumombelli.dstcg.ui.motion

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import fr.aumombelli.dstcg.R

enum class BrandLogoVariant {
    Badge17,
    Lockup18,
    Lockup19,
}

fun homeLogoVariantFor(skyBackdropVariant: SkyBackdropVariant): BrandLogoVariant = when (skyBackdropVariant) {
    SkyBackdropVariant.City,
    SkyBackdropVariant.Suburban,
    SkyBackdropVariant.Rural,
    SkyBackdropVariant.Mountain,
    -> BrandLogoVariant.Lockup19
}

data class BrandLogoLayoutSpec(
    val widthMultiplierFromBadgeSize: Float,
    val heightMultiplierFromBadgeSize: Float,
    val badgeCenterFractionX: Float,
    val badgeCenterFractionY: Float,
    val badgeCenterYOffsetMultiplierFromBadgeSize: Float,
)

fun brandLogoLayoutSpec(variant: BrandLogoVariant): BrandLogoLayoutSpec = when (variant) {
    BrandLogoVariant.Badge17 -> BrandLogoLayoutSpec(
        widthMultiplierFromBadgeSize = 1f,
        heightMultiplierFromBadgeSize = 1f,
        badgeCenterFractionX = 0.5f,
        badgeCenterFractionY = 0.5f,
        badgeCenterYOffsetMultiplierFromBadgeSize = 0f,
    )

    BrandLogoVariant.Lockup18,
    BrandLogoVariant.Lockup19,
    -> BrandLogoLayoutSpec(
        // Derived from the exported PNG metrics so the lockup badge matches the standalone badge.
        widthMultiplierFromBadgeSize = 0.99993175f,
        heightMultiplierFromBadgeSize = 1.338874f,
        badgeCenterFractionX = 0.49916667f,
        badgeCenterFractionY = 0.37429994f,
        badgeCenterYOffsetMultiplierFromBadgeSize = 0.16787997f,
    )
}

@Composable
fun LaunchLogoMark(
    variant: BrandLogoVariant = BrandLogoVariant.Lockup19,
    emblemSize: Dp = 104.dp,
    modifier: Modifier = Modifier,
    testTag: String? = null,
) {
    val layoutSpec = brandLogoLayoutSpec(variant)
    val painter = painterResource(
        when (variant) {
            BrandLogoVariant.Badge17 -> R.drawable.logo_badge_17
            BrandLogoVariant.Lockup18 -> R.drawable.logo_lockup_18
            BrandLogoVariant.Lockup19 -> R.drawable.logo_lockup_19
        },
    )
    val contentModifier = Modifier.size(
        width = emblemSize * layoutSpec.widthMultiplierFromBadgeSize,
        height = emblemSize * layoutSpec.heightMultiplierFromBadgeSize,
    )
    val taggedModifier = testTag?.let { modifier.testTag(it) } ?: modifier
    Image(
        painter = painter,
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = taggedModifier
            .then(contentModifier),
    )
}
