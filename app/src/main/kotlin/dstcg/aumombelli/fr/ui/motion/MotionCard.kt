package fr.aumombelli.dstcg.ui.motion

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

@Composable
fun MotionCard(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    Surface(
        color = Color(0xDD08101D),
        shape = RoundedCornerShape(28.dp),
        shadowElevation = 8.dp,
        modifier = modifier,
    ) {
        Box(
            modifier = Modifier.background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xAA1B304C),
                        Color(0xE008111C),
                    ),
                ),
            ),
            content = content,
        )
    }
}
