package fr.aumombelli.dstcg.feature.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import fr.aumombelli.dstcg.ui.motion.MotionCard

@Composable
internal fun HomePackCard(
    enabled: Boolean,
    isBusy: Boolean,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    MotionCard(
        modifier = modifier
            .graphicsLayer {
                alpha = if (enabled) 1f else 0.78f
            }
            .clickable(
                enabled = enabled,
                onClick = onClick,
            ),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF112B49),
                            Color(0xFF091423),
                            Color(0xFF050910),
                        ),
                    ),
                )
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.10f),
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.18f),
                        ),
                    ),
                    cornerRadius = CornerRadius(size.minDimension * 0.10f, size.minDimension * 0.10f),
                )
                drawRoundRect(
                    color = Color(0x66F3D59F),
                    cornerRadius = CornerRadius(size.minDimension * 0.10f, size.minDimension * 0.10f),
                    style = Stroke(width = size.minDimension * 0.014f),
                )

                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0x55E1F1FF),
                            Color.Transparent,
                        ),
                    ),
                    radius = size.minDimension * 0.38f,
                    center = Offset(size.width * 0.62f, size.height * 0.28f),
                    blendMode = BlendMode.Screen,
                )
                drawCircle(
                    color = Color(0x33FFC76A),
                    radius = size.minDimension * 0.12f,
                    center = Offset(size.width * 0.62f, size.height * 0.28f),
                    blendMode = BlendMode.Screen,
                )

                val stars = listOf(
                    Triple(0.18f, 0.16f, 0.010f),
                    Triple(0.30f, 0.22f, 0.008f),
                    Triple(0.42f, 0.14f, 0.012f),
                    Triple(0.74f, 0.18f, 0.009f),
                    Triple(0.82f, 0.30f, 0.007f),
                    Triple(0.66f, 0.10f, 0.006f),
                    Triple(0.24f, 0.34f, 0.007f),
                )
                stars.forEach { (x, y, radius) ->
                    drawCircle(
                        color = Color.White.copy(alpha = 0.88f),
                        radius = size.minDimension * radius,
                        center = Offset(size.width * x, size.height * y),
                    )
                    drawCircle(
                        color = Color(0x33FFD580),
                        radius = size.minDimension * radius * 3.2f,
                        center = Offset(size.width * x, size.height * y),
                    )
                }

                drawLine(
                    color = Color(0x55CFE2FF),
                    start = Offset(size.width * 0.20f, size.height * 0.25f),
                    end = Offset(size.width * 0.42f, size.height * 0.14f),
                    strokeWidth = size.minDimension * 0.004f,
                    cap = StrokeCap.Round,
                )
                drawLine(
                    color = Color(0x55CFE2FF),
                    start = Offset(size.width * 0.42f, size.height * 0.14f),
                    end = Offset(size.width * 0.62f, size.height * 0.28f),
                    strokeWidth = size.minDimension * 0.004f,
                    cap = StrokeCap.Round,
                )

                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color(0x33040911),
                            Color(0xCC040911),
                        ),
                        startY = size.height * 0.50f,
                        endY = size.height,
                    ),
                    topLeft = Offset.Zero,
                    size = size,
                )

                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0x00000000),
                            Color(0x66112338),
                            Color(0xFF05080F),
                        ),
                    ),
                    topLeft = Offset(0f, size.height * 0.58f),
                    size = Size(size.width, size.height * 0.42f),
                )

                drawPath(
                    path = observatoryPath(size),
                    color = Color(0xFF09111D),
                )
                drawPath(
                    path = horizonPath(size),
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF1F3652),
                            Color(0xFF0A121E),
                        ),
                    ),
                )
            }

            if (isBusy) {
                CircularProgressIndicator(
                    strokeWidth = 2.5.dp,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(18.dp),
                )
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(horizontal = 22.dp, vertical = 20.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFD8E7F9),
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }
}

private fun observatoryPath(size: Size): Path = Path().apply {
    moveTo(size.width * 0.10f, size.height * 0.84f)
    lineTo(size.width * 0.24f, size.height * 0.70f)
    lineTo(size.width * 0.34f, size.height * 0.70f)
    lineTo(size.width * 0.34f, size.height * 0.57f)
    quadraticTo(
        size.width * 0.42f,
        size.height * 0.45f,
        size.width * 0.52f,
        size.height * 0.57f,
    )
    lineTo(size.width * 0.52f, size.height * 0.70f)
    lineTo(size.width * 0.66f, size.height * 0.70f)
    lineTo(size.width * 0.78f, size.height * 0.78f)
    lineTo(size.width * 0.90f, size.height * 0.78f)
    lineTo(size.width * 0.90f, size.height)
    lineTo(size.width * 0.10f, size.height)
    close()
}

private fun horizonPath(size: Size): Path = Path().apply {
    moveTo(0f, size.height * 0.76f)
    quadraticTo(
        size.width * 0.18f,
        size.height * 0.72f,
        size.width * 0.34f,
        size.height * 0.75f,
    )
    quadraticTo(
        size.width * 0.56f,
        size.height * 0.78f,
        size.width * 0.74f,
        size.height * 0.73f,
    )
    quadraticTo(
        size.width * 0.88f,
        size.height * 0.70f,
        size.width,
        size.height * 0.74f,
    )
    lineTo(size.width, size.height)
    lineTo(0f, size.height)
    close()
}
