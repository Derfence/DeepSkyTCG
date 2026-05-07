package fr.aumombelli.dstcg.ui.motion

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import fr.aumombelli.dstcg.ui.theme.EmberGold

@Composable
fun ExtensionConstellationOverlay(
    spec: ExtensionAnimationSpec,
    lineProgress: Float,
    isReversing: Boolean,
    modifier: Modifier = Modifier,
    tag: String? = "pack-extension-constellation",
) {
    if (spec.style == ExtensionAnimationStyle.NeutralSky) return
    if (lineProgress <= 0f) return

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .then(
                if (tag != null) {
                    Modifier.testTag(tag)
                } else {
                    Modifier
                },
            ),
    ) {
        drawExtensionPattern(
            spec = spec,
            lineProgress = lineProgress,
            isReversing = isReversing,
            revealWindow = 0.25f,
            style = ExtensionPatternDrawStyle(
                pointCoreBaseRadiusFraction = 0.006f,
                pointCoreRevealRadiusFraction = 0.003f,
                pointHaloRadiusFraction = 0.018f,
                pointHaloAlphaMultiplier = 0.35f,
                lineStrokeWidthFraction = 0.004f,
                lineAlphaMultiplier = 0.9f,
                orbitStrokeWidthFraction = 0.004f,
                orbitAlphaMultiplier = 0.72f,
                pointColor = Color.White,
                pointHaloColor = EmberGold,
                lineColor = Color(0xFFE2F0FF),
                orbitColor = Color(0xFFE2F0FF),
            ),
        )
    }
}
