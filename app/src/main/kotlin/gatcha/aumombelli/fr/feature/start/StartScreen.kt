package fr.aumombelli.gatcha.feature.start

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

@Composable
fun StartScreen(
    state: StartUiState,
    onBegin: () -> Unit,
    modifier: Modifier = Modifier,
    showBackground: Boolean = true,
    contentVisible: Boolean = true,
    onCardTopChanged: (Float) -> Unit = {},
) {
    val cardAlpha by animateFloatAsState(
        targetValue = if (contentVisible) 1f else 0f,
        animationSpec = tween(durationMillis = 520, easing = FastOutSlowInEasing),
        label = "start-card-alpha",
    )
    val backgroundBrush = if (showBackground) {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFF08101D),
                Color(0xFF12243F),
                Color(0xFF1A3052),
            ),
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                Color.Transparent,
                Color.Transparent,
            ),
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(brush = backgroundBrush),
    ) {
        StartCard(
            state = state,
            cardAlpha = cardAlpha,
            onBegin = onBegin,
            onCardTopChanged = onCardTopChanged,
        )
    }
}
