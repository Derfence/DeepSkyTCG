package fr.aumombelli.dstcg.ui.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import fr.aumombelli.dstcg.ui.theme.SkyQualityPalette
import fr.aumombelli.dstcg.ui.theme.rarityBadgeStyle
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
internal fun QuantityPill(
    text: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = Color.Black.copy(alpha = 0.35f),
        shape = androidx.compose.foundation.shape.CircleShape,
        modifier = modifier,
    ) {
        Text(
            text = text,
            color = Color.White,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
        )
    }
}

@Composable
internal fun HeroAtmosphere(
    palette: SkyQualityPalette,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        palette.glow,
                        Color.Transparent,
                    ),
                    center = Offset(280f, 120f),
                    radius = 520f,
                ),
            ),
    )
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        palette.mist,
                        Color.Transparent,
                    ),
                    center = Offset(120f, 340f),
                    radius = 420f,
                ),
            ),
    )
}

@Composable
internal fun RarityStarBadge(
    rarityLabel: String,
    modifier: Modifier = Modifier,
) {
    val style = rarityBadgeStyle(rarityLabel)

    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2f, size.height / 2f)
        drawCircle(
            color = style.glowColor,
            radius = size.minDimension * 0.44f,
            center = center,
        )
        drawPath(
            path = starPath(
                center = center,
                points = style.branchCount,
                outerRadius = size.minDimension * 0.34f,
                innerRadius = size.minDimension * 0.16f,
            ),
            color = style.color,
            style = Fill,
        )
        drawPath(
            path = starPath(
                center = center,
                points = style.branchCount,
                outerRadius = size.minDimension * 0.34f,
                innerRadius = size.minDimension * 0.16f,
            ),
            color = Color.White.copy(alpha = 0.75f),
            style = Stroke(width = size.minDimension * 0.03f),
        )
    }
}

@Composable
internal fun TwinklingStarsOverlay(
    animated: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val progress = if (animated) {
        val transition = rememberInfiniteTransition(label = "twinkle")
        val animatedProgress by transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 3600, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
            label = "twinkle-progress",
        )
        animatedProgress
    } else {
        0.27f
    }

    Canvas(modifier = modifier) {
        TwinkleStars.forEach { star ->
            val alpha = 0.18f + 0.82f * ((sin((progress + star.phase) * PI * 2).toFloat() + 1f) / 2f)
            val center = Offset(size.width * star.x, size.height * star.y)
            drawPath(
                path = starPath(
                    center = center,
                    points = 4,
                    outerRadius = size.minDimension * star.radius,
                    innerRadius = size.minDimension * star.radius * 0.42f,
                ),
                color = Color.White.copy(alpha = alpha),
            )
            drawCircle(
                color = Color(0xFFFFF8D6).copy(alpha = alpha * 0.7f),
                radius = size.minDimension * star.radius * 0.12f,
                center = center,
            )
        }
    }
}

internal fun starPath(
    center: Offset,
    points: Int,
    outerRadius: Float,
    innerRadius: Float,
): Path {
    val path = Path()
    val angleStep = PI / points

    repeat(points * 2) { index ->
        val radius = if (index % 2 == 0) outerRadius else innerRadius
        val angle = -PI / 2 + angleStep * index
        val x = center.x + radius * cos(angle).toFloat()
        val y = center.y + radius * sin(angle).toFloat()
        if (index == 0) {
            path.moveTo(x, y)
        } else {
            path.lineTo(x, y)
        }
    }
    path.close()
    return path
}

private data class TwinkleStar(
    val x: Float,
    val y: Float,
    val radius: Float,
    val phase: Float,
)

private val TwinkleStars = listOf(
    TwinkleStar(0.16f, 0.14f, 0.024f, 0.0f),
    TwinkleStar(0.78f, 0.18f, 0.019f, 0.17f),
    TwinkleStar(0.64f, 0.34f, 0.016f, 0.39f),
    TwinkleStar(0.24f, 0.44f, 0.021f, 0.56f),
    TwinkleStar(0.83f, 0.58f, 0.018f, 0.73f),
    TwinkleStar(0.14f, 0.72f, 0.02f, 0.88f),
    TwinkleStar(0.52f, 0.78f, 0.014f, 0.28f),
    TwinkleStar(0.72f, 0.86f, 0.022f, 0.49f),
)
