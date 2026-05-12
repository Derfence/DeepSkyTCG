package fr.aumombelli.dstcg.feature.minigames

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import fr.aumombelli.dstcg.ui.motion.SkyBackdropVariant
import fr.aumombelli.dstcg.ui.screen.dstcgContentInsetsPadding

@Composable
internal fun MiniGamesMenuScreen(
    state: MiniGamesUiState,
    onBack: () -> Unit,
    onOpenMemory: () -> Unit,
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
            .testTag("mini-games-menu-screen"),
    ) {
        MiniGameSceneBackdrop(
            variant = SkyBackdropVariant.Suburban,
            sparkleBoost = 0.12f,
            modifier = Modifier.fillMaxSize(),
        )
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
                enabled = false,
                onClick = {},
                testTag = "mini-games-quiz",
            )
            MiniGameMapButton(
                index = "2",
                anchorX = 0.40f,
                anchorY = 0.58f,
                enabled = !state.isLoading,
                onClick = onOpenMemory,
                testTag = "mini-games-memory",
            )
            MiniGameMapButton(
                index = "3",
                anchorX = 0.57f,
                anchorY = 0.42f,
                enabled = false,
                onClick = {},
                testTag = "mini-games-timeline",
            )
            MiniGameMapButton(
                index = "4",
                anchorX = 0.74f,
                anchorY = 0.26f,
                enabled = false,
                onClick = {},
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

            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(
                        color = Color(0xAA07111A),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                    )
                    .padding(horizontal = 16.dp, vertical = 14.dp)
                    .testTag("mini-games-memory-status"),
            ) {
                Text(
                    text = "Memory amateur",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = state.memoryStatusLabel,
                    color = Color(0xFFD6E7F7),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 4.dp),
                )
                state.errorMessage?.let { message ->
                    Text(
                        text = message,
                        color = Color(0xFFFFC4BD),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun MiniGameMapButton(
    index: String,
    anchorX: Float,
    anchorY: Float,
    enabled: Boolean,
    onClick: () -> Unit,
    testTag: String,
) {
    val buttonSize = 52.dp
    BoxWithConstraints(
        modifier = Modifier.fillMaxSize(),
    ) {
        val ringSize = buttonSize + 24.dp
        MiniGamePulsingRing(
            enabled = enabled,
            tone = if (enabled) MiniGameFeedbackTone.Success else MiniGameFeedbackTone.Error,
            modifier = Modifier
                .offset(
                    x = (maxWidth * anchorX) - (ringSize / 2f),
                    y = (maxHeight * anchorY) - (ringSize / 2f),
                )
                .size(ringSize),
        )
        Surface(
            onClick = onClick,
            enabled = enabled,
            shape = CircleShape,
            color = if (enabled) Color(0xDD0F4050) else Color(0xCC0A1524),
            contentColor = Color.White,
            shadowElevation = if (enabled) 12.dp else 8.dp,
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
                    color = Color.White.copy(alpha = if (enabled) 1f else 0.72f),
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
