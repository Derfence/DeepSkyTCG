package fr.aumombelli.dstcg.ui.component

import android.util.Base64
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext

internal data class LayeredSvgAsset(
    val assetPath: String,
    val mirrorHorizontally: Boolean = false,
)

@Composable
internal fun LayeredAssetSvgImage(
    layers: List<LayeredSvgAsset>,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current.applicationContext
    val bodyMarkup = remember(context, layers) {
        layers.joinToString(separator = "\n") { layer ->
            val encodedSvg = runCatching {
                context.assets.open(layer.assetPath).use { input ->
                    Base64.encodeToString(input.readBytes(), Base64.NO_WRAP)
                }
            }.getOrDefault("")
            val mirrorClass = if (layer.mirrorHorizontally) " mirrored" else ""
            """
            <div class="svg-layer$mirrorClass">
              <img src="data:image/svg+xml;base64,$encodedSvg" />
            </div>
            """.trimIndent()
        }
    }

    SvgHtmlImage(
        bodyMarkup = """<div class="layered-svg">$bodyMarkup</div>""",
        modifier = modifier,
        style = LayeredSvgHtmlStyle,
    )
}

private val LayeredSvgHtmlStyle = """
html, body {
  margin: 0;
  padding: 0;
  width: 100%;
  height: 100%;
  overflow: hidden;
  background: transparent;
  pointer-events: none;
}
.layered-svg {
  position: relative;
  width: 100%;
  height: 100%;
  overflow: hidden;
}
.svg-layer {
  position: absolute;
  inset: 0;
  transform-origin: 50% 50%;
  pointer-events: none;
}
.svg-layer.mirrored {
  transform: scaleX(-1);
}
.svg-layer img {
  display: block;
  width: 100%;
  height: 100%;
  pointer-events: none;
}
""".trimIndent()
