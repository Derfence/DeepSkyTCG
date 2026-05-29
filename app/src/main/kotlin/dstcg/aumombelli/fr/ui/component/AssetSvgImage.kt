package fr.aumombelli.dstcg.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext

@Composable
internal fun AssetSvgImage(
    assetPath: String,
    modifier: Modifier = Modifier,
    allowTouchInteraction: Boolean = false,
) {
    val context = LocalContext.current.applicationContext
    val svgMarkup = remember(context, assetPath) {
        runCatching {
            context.assets.open(assetPath).bufferedReader().use { reader ->
                reader.readText()
            }
        }.getOrNull()
    }

    if (svgMarkup != null) {
        SvgHtmlImage(
            bodyMarkup = svgMarkup,
            modifier = modifier,
            allowTouchInteraction = allowTouchInteraction,
        )
    }
}
