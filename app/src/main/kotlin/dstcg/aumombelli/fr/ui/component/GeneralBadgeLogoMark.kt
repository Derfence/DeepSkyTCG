package fr.aumombelli.dstcg.ui.component

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

internal const val GeneralBadgeLogoAssetPath: String = "branding/21-badge-logo-blanc.svg"

@Composable
fun GeneralBadgeLogoMark(
    emblemSize: Dp = 56.dp,
    modifier: Modifier = Modifier,
    testTag: String? = null,
) {
    val taggedModifier = testTag?.let { modifier.testTag(it) } ?: modifier
    AssetSvgImage(
        assetPath = GeneralBadgeLogoAssetPath,
        modifier = taggedModifier.size(emblemSize),
    )
}
