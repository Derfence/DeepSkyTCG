package fr.aumombelli.dstcg.ui.component

import android.annotation.SuppressLint
import android.graphics.Color as AndroidColor
import android.view.ViewGroup
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView

@Composable
internal fun AssetSvgImage(
    assetPath: String,
    modifier: Modifier = Modifier,
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
        InlineSvgImage(
            svgMarkup = svgMarkup,
            modifier = modifier,
        )
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun InlineSvgImage(
    svgMarkup: String,
    modifier: Modifier = Modifier,
) {
    val htmlDocument = remember(svgMarkup) {
        """
        <html>
          <head>
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
            <style>
              html, body {
                margin: 0;
                padding: 0;
                width: 100%;
                height: 100%;
                overflow: hidden;
                background: transparent;
              }
              svg {
                display: block;
                width: 100%;
                height: 100%;
              }
            </style>
          </head>
          <body>
            $svgMarkup
          </body>
        </html>
        """.trimIndent()
    }

    AndroidView(
        factory = { viewContext ->
            WebView(viewContext).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                )
                setBackgroundColor(AndroidColor.TRANSPARENT)
                overScrollMode = WebView.OVER_SCROLL_NEVER
                isVerticalScrollBarEnabled = false
                isHorizontalScrollBarEnabled = false
                isLongClickable = false
                settings.javaScriptEnabled = false
                settings.cacheMode = WebSettings.LOAD_NO_CACHE
                settings.loadsImagesAutomatically = true
                settings.allowFileAccess = false
                settings.allowContentAccess = false
                settings.blockNetworkLoads = true
                loadDataWithBaseURL(
                    "file:///android_asset/",
                    htmlDocument,
                    "text/html",
                    "utf-8",
                    null,
                )
            }
        },
        modifier = modifier,
    )
}
