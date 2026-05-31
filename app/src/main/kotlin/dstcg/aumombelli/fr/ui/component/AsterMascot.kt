package fr.aumombelli.dstcg.ui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex

internal enum class AsterFace(val assetIndex: Int) {
    Smile(1),
    HappyEyes(2),
    BigSmile(3),
    HappyEyesBigSmile(4),
    SmileLookingRight(5),
}

internal enum class AsterHand(val assetIndex: Int) {
    Point(1),
    Open(2),
    Wrench(3),
    Cards(4),
    Telescope(5),
}

internal enum class AsterHandSide {
    Left,
    Right,
}

internal enum class AsterAnchor {
    BottomStart,
    BottomCenter,
    BottomEnd,
}

internal enum class AsterMascotScale {
    Standard,
    Compact,
}

internal data class AsterMascotSpec(
    val face: AsterFace,
    val hand: AsterHand,
    val handSide: AsterHandSide,
    val anchor: AsterAnchor,
    val scale: AsterMascotScale = AsterMascotScale.Standard,
    val showBothHands: Boolean = false,
    val mirroredHand: AsterHand? = null,
    val sizeMultiplier: Float = 1f,
)

internal const val ASTER_MASCOT_TEST_TAG = "aster-mascot"
internal const val AsterMascotOverlayZIndex = 100f
internal const val AsterMascotSizeMultiplier = 1.5f

internal fun asterMascotWidthForContainer(
    containerWidth: Float,
    scale: AsterMascotScale,
    sizeMultiplier: Float = 1f,
): Float {
    val baseWidthFraction = when (scale) {
        AsterMascotScale.Standard -> 0.31f
        AsterMascotScale.Compact -> 0.24f
    }
    val baseMinWidth = when (scale) {
        AsterMascotScale.Standard -> 108f
        AsterMascotScale.Compact -> 82f
    }
    val baseMaxWidth = when (scale) {
        AsterMascotScale.Standard -> 172f
        AsterMascotScale.Compact -> 116f
    }
    val combinedMultiplier = AsterMascotSizeMultiplier * sizeMultiplier
    return (containerWidth * baseWidthFraction * combinedMultiplier)
        .coerceIn(
            minimumValue = baseMinWidth * combinedMultiplier,
            maximumValue = baseMaxWidth * combinedMultiplier,
        )
}

internal fun asterMascotHeightForContainer(
    containerWidth: Float,
    scale: AsterMascotScale,
    sizeMultiplier: Float = 1f,
): Float = asterMascotWidthForContainer(
    containerWidth = containerWidth,
    scale = scale,
    sizeMultiplier = sizeMultiplier,
) / AsterMascotAspectRatio

internal fun AsterMascotSpec.assetLayers(): List<LayeredSvgAsset> = buildList {
    add(
        LayeredSvgAsset(
            assetPath = "aster/hair.svg",
            mirrorHorizontally = anchor == AsterAnchor.BottomStart,
        ),
    )
    add(LayeredSvgAsset("aster/suit.svg"))
    add(LayeredSvgAsset("aster/face${face.assetIndex}.svg"))
    add(LayeredSvgAsset("aster/bowtie.svg"))
    val secondHand = mirroredHand ?: hand.takeIf { showBothHands }
    if (secondHand != null) {
        add(LayeredSvgAsset(assetPath = "aster/hand${hand.assetIndex}.svg"))
        add(
            LayeredSvgAsset(
                assetPath = "aster/hand${secondHand.assetIndex}.svg",
                mirrorHorizontally = true,
            ),
        )
    } else {
        add(
            LayeredSvgAsset(
                assetPath = "aster/hand${hand.assetIndex}.svg",
                mirrorHorizontally = handSide == AsterHandSide.Right,
            ),
        )
    }
}

@Composable
internal fun AsterMascot(
    spec: AsterMascotSpec,
    modifier: Modifier = Modifier,
) {
    val layers = remember(spec) { spec.assetLayers() }
    Box(
        modifier = modifier.testTag(ASTER_MASCOT_TEST_TAG),
    ) {
        LayeredAssetSvgImage(layers = layers)
    }
}

@Composable
internal fun AsterMascotOverlay(
    spec: AsterMascotSpec,
    modifier: Modifier = Modifier,
    bottomPadding: Dp = 10.dp,
    topPadding: Dp? = null,
    widthOverride: Dp? = null,
) {
    BoxWithConstraints(modifier = modifier.zIndex(AsterMascotOverlayZIndex)) {
        val mascotWidth = widthOverride ?: asterMascotWidthForContainer(
            containerWidth = maxWidth.value,
            scale = spec.scale,
            sizeMultiplier = spec.sizeMultiplier,
        ).dp
        val horizontalPadding = when (spec.scale) {
            AsterMascotScale.Standard -> 10.dp
            AsterMascotScale.Compact -> 8.dp
        }
        val alignment = if (topPadding != null) {
            Alignment.TopCenter
        } else {
            when (spec.anchor) {
                AsterAnchor.BottomStart -> Alignment.BottomStart
                AsterAnchor.BottomCenter -> Alignment.BottomCenter
                AsterAnchor.BottomEnd -> Alignment.BottomEnd
            }
        }
        val mascotModifier = Modifier
            .align(alignment)
            .offset(y = topPadding ?: 0.dp)
            .padding(
                start = if (spec.anchor == AsterAnchor.BottomStart) horizontalPadding else 0.dp,
                end = if (spec.anchor == AsterAnchor.BottomEnd) horizontalPadding else 0.dp,
                bottom = if (topPadding == null) bottomPadding else 0.dp,
            )
            .width(mascotWidth)
            .aspectRatio(AsterMascotAspectRatio)

        AsterMascot(
            spec = spec,
            modifier = mascotModifier,
        )
    }
}

internal const val AsterMascotAspectRatio = 264f / 202f
