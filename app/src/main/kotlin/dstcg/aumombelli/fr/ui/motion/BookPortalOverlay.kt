package fr.aumombelli.dstcg.ui.motion

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

@Composable
fun BookPortalOverlay(
    progress: Float,
    overlayAlpha: Float = 1f,
    modifier: Modifier = Modifier,
) {
    val visibleProgress = progress.coerceIn(0f, 1f)
    val travelProgress = calculateLibraryBookTravelProgress(visibleProgress)
    if (visibleProgress <= 0f) return
    val visibleAlpha = overlayAlpha.coerceIn(0f, 1f)
    if (visibleAlpha <= 0f) return

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("app-transition-book"),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = (24 + 280 * travelProgress).dp)
                .graphicsLayer {
                    alpha = visibleProgress * visibleAlpha
                }
                .size(220.dp),
        ) {
            BookPortalFigure(
                progress = visibleProgress,
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(176.dp),
            )
        }
    }
}

@Composable
private fun BookPortalFigure(
    progress: Float,
    modifier: Modifier = Modifier,
) {
    val pose = remember(progress) { calculateLibraryBookPose(progress) }

    Canvas(modifier = modifier) {
        drawTransitionBook(pose)
    }
}
