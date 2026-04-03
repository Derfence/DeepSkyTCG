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

@Composable
fun LaunchLogoMark(
    showWordmark: Boolean = true,
    emblemSize: Dp = 104.dp,
    modifier: Modifier = Modifier,
) {
    val painter = painterResource(if (showWordmark) R.drawable.logo_lockup_19 else R.drawable.logo_badge_17)
    val contentModifier = if (showWordmark) {
        Modifier
            .size(width = emblemSize * (10420f / 13950f) * 1.78f, height = emblemSize * 1.78f)
    } else {
        Modifier.size(emblemSize)
    }
    Image(
        painter = painter,
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = modifier
            .testTag("app-launch-logo")
            .then(contentModifier),
    )
}
