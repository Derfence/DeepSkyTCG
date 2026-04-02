package fr.aumombelli.dstcg.ui.motion

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag

@Composable
fun FadeScrim(
    alpha: Float,
    modifier: Modifier = Modifier,
) {
    if (alpha <= 0f) return
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = alpha.coerceIn(0f, 1f)))
            .testTag("app-fade-scrim"),
    )
}
