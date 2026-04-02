package fr.aumombelli.dstcg.ui.motion

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import fr.aumombelli.dstcg.ui.theme.EmberGold

@Composable
fun LaunchLogoMark(
    showWordmark: Boolean = true,
    emblemSize: androidx.compose.ui.unit.Dp = 104.dp,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.testTag("app-launch-logo"),
    ) {
        Canvas(modifier = Modifier.size(emblemSize)) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val outerRadius = size.minDimension * 0.42f
            val innerRadius = size.minDimension * 0.18f
            drawCircle(
                color = EmberGold.copy(alpha = 0.2f),
                radius = size.minDimension * 0.48f,
                center = center,
            )
            drawPath(
                path = starPath(
                    center = center,
                    points = 6,
                    outerRadius = outerRadius,
                    innerRadius = innerRadius,
                ),
                color = EmberGold,
            )
            drawPath(
                path = starPath(
                    center = center,
                    points = 4,
                    outerRadius = size.minDimension * 0.18f,
                    innerRadius = size.minDimension * 0.08f,
                ),
                color = Color.White,
            )
        }
        if (showWordmark) {
            Text(
                text = "Deep Sky TCG",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Black,
                color = Color.White,
            )
        }
    }
}
