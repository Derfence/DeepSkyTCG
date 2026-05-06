package fr.aumombelli.dstcg.ui.component

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color as AndroidColor
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

@SuppressLint("SetJavaScriptEnabled")
@Composable
internal fun SvgHtmlImage(
    bodyMarkup: String,
    modifier: Modifier = Modifier,
    allowTouchInteraction: Boolean = false,
    style: String = DefaultSvgHtmlStyle,
) {
    val htmlDocument = remember(bodyMarkup, style) {
        """
        <html>
          <head>
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
            <style>
              $style
            </style>
          </head>
          <body>
            $bodyMarkup
          </body>
        </html>
        """.trimIndent()
    }

    AndroidView(
        factory = { viewContext ->
            SvgWebView(
                context = viewContext,
                allowTouchInteraction = allowTouchInteraction,
            ).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                )
                setBackgroundColor(AndroidColor.TRANSPARENT)
                overScrollMode = WebView.OVER_SCROLL_NEVER
                isVerticalScrollBarEnabled = false
                isHorizontalScrollBarEnabled = false
                isLongClickable = false
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
                settings.javaScriptEnabled = false
                settings.cacheMode = WebSettings.LOAD_NO_CACHE
                settings.loadsImagesAutomatically = true
                settings.allowFileAccess = false
                settings.allowContentAccess = false
                settings.blockNetworkLoads = true
            }
        },
        update = { webView ->
            webView.loadSvgDocumentIfChanged(htmlDocument)
        },
        modifier = modifier,
    )
}

private fun SvgWebView.loadSvgDocumentIfChanged(htmlDocument: String) {
    if (loadedHtmlDocument == htmlDocument) return
    loadedHtmlDocument = htmlDocument
    loadDataWithBaseURL(
        "file:///android_asset/",
        htmlDocument,
        "text/html",
        "utf-8",
        null,
    )
}

private class SvgWebView(
    context: Context,
    private val allowTouchInteraction: Boolean,
) : WebView(context) {
    var loadedHtmlDocument: String? = null

    override fun onTouchEvent(event: MotionEvent?): Boolean =
        if (allowTouchInteraction) {
            super.onTouchEvent(event)
        } else {
            false
        }
}

private val DefaultSvgHtmlStyle = """
html, body {
  margin: 0;
  padding: 0;
  width: 100%;
  height: 100%;
  overflow: hidden;
  background: transparent;
  pointer-events: none;
}
svg {
  display: block;
  width: 100%;
  height: 100%;
  pointer-events: none;
}
""".trimIndent()
