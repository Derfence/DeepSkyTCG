package fr.aumombelli.dstcg.feature.minigames

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import fr.aumombelli.dstcg.ui.component.AssetSvgImage
import fr.aumombelli.dstcg.ui.component.SceneNavigationButton
import fr.aumombelli.dstcg.ui.component.SceneNavigationIcon
import fr.aumombelli.dstcg.ui.screen.dstcgContentInsetsPadding

@Composable
fun MiniGamesMenuScreen(
    onBack: () -> Unit,
    contentVisible: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val contentAlpha by animateFloatAsState(
        targetValue = if (contentVisible) 1f else 0f,
        animationSpec = tween(durationMillis = 520, easing = FastOutSlowInEasing),
        label = "mini-games-menu-alpha",
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer { alpha = contentAlpha }
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF07111A),
                        Color(0xFF11283B),
                        Color(0xFF060A10),
                    ),
                ),
            )
            .testTag("mini-games-menu-screen"),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .testTag("mini-games-map"),
        ) {
            MiniGamesFallbackMap(modifier = Modifier.fillMaxSize())
            AssetSvgImage(
                assetPath = MiniGamesMapSvgAssetName,
                modifier = Modifier.fillMaxSize(),
            )
        }

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .dstcgContentInsetsPadding(includeBottom = true),
        ) {
            MiniGameMapButton(
                index = "1",
                anchorX = 0.24f,
                anchorY = 0.74f,
                testTag = "mini-games-quiz",
            )
            MiniGameMapButton(
                index = "2",
                anchorX = 0.40f,
                anchorY = 0.58f,
                testTag = "mini-games-memory",
            )
            MiniGameMapButton(
                index = "3",
                anchorX = 0.57f,
                anchorY = 0.42f,
                testTag = "mini-games-timeline",
            )
            MiniGameMapButton(
                index = "4",
                anchorX = 0.74f,
                anchorY = 0.26f,
                testTag = "mini-games-observatory",
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .dstcgContentInsetsPadding(includeBottom = false)
                .padding(horizontal = 18.dp, vertical = 18.dp),
        ) {
            SceneNavigationButton(
                icon = SceneNavigationIcon.Back,
                onClick = onBack,
                contentDescription = "Retour",
                testTag = "mini-games-menu-back",
                modifier = Modifier.align(Alignment.TopStart),
            )

            Text(
                text = "Mini-jeux",
                color = Color.White,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 4.dp),
            )
        }
    }
}

@Composable
private fun MiniGameMapButton(
    index: String,
    anchorX: Float,
    anchorY: Float,
    testTag: String,
) {
    val buttonSize = 52.dp
    BoxWithConstraints(
        modifier = Modifier.fillMaxSize(),
    ) {
        Surface(
            onClick = {},
            enabled = false,
            shape = CircleShape,
            color = Color(0xCC0A1524),
            contentColor = Color.White,
            shadowElevation = 8.dp,
            modifier = Modifier
                .offset(
                    x = (maxWidth * anchorX) - (buttonSize / 2f),
                    y = (maxHeight * anchorY) - (buttonSize / 2f),
                )
                .size(buttonSize)
                .testTag(testTag),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = index,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.72f),
                )
            }
        }
    }
}

@Composable
private fun MiniGamesFallbackMap(
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF18202D),
                    Color(0xFF0A1522),
                    Color(0xFF05080E),
                ),
            ),
        )
        val points = listOf(
            Offset(size.width * 0.24f, size.height * 0.74f),
            Offset(size.width * 0.40f, size.height * 0.58f),
            Offset(size.width * 0.57f, size.height * 0.42f),
            Offset(size.width * 0.74f, size.height * 0.26f),
        )
        points.zipWithNext().forEach { (start, end) ->
            drawLine(
                color = Color(0x99F6B73C),
                start = start,
                end = end,
                strokeWidth = size.minDimension * 0.012f,
            )
        }
        points.forEach { point ->
            drawCircle(
                color = Color(0xFFEAF3FF),
                radius = size.minDimension * 0.025f,
                center = point,
            )
            drawCircle(
                color = Color(0x7768E1D2),
                radius = size.minDimension * 0.06f,
                center = point,
                style = Stroke(width = size.minDimension * 0.006f),
            )
        }
    }
}

private const val MiniGamesMapSvgAssetName = "mini-games-map.svg"
