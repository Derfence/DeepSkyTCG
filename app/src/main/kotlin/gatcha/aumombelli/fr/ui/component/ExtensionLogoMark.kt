package fr.aumombelli.gatcha.ui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import fr.aumombelli.gatcha.ui.motion.ExtensionAnimationStyle
import fr.aumombelli.gatcha.ui.motion.ExtensionConstellationOverlay
import fr.aumombelli.gatcha.ui.motion.LaunchLogoMark
import fr.aumombelli.gatcha.ui.motion.extensionAnimationSpec

@Composable
fun ExtensionLogoMark(
    extensionId: String,
    compact: Boolean,
    emblemSize: androidx.compose.ui.unit.Dp? = null,
    modifier: Modifier = Modifier,
) {
    val spec = remember(extensionId) { extensionAnimationSpec(extensionId) }
    val size = emblemSize ?: if (compact) 32.dp else 40.dp

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(size),
    ) {
        if (spec.style == ExtensionAnimationStyle.BigDipper) {
            ExtensionConstellationOverlay(
                spec = spec,
                lineProgress = 1f,
                isReversing = false,
                modifier = Modifier.fillMaxSize(),
                tag = null,
            )
        } else {
            LaunchLogoMark(
                showWordmark = false,
                emblemSize = size,
            )
        }
    }
}
