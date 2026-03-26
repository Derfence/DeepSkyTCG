package fr.aumombelli.gatcha.feature.bootstrap

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

@Composable
fun AppBootstrapScreen(
    state: AppBootstrapUiState,
    onRetry: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF08101D),
                        Color(0xFF12243F),
                        Color(0xFF1A3052),
                    ),
                ),
            ),
    ) {
        AppBootstrapCard(
            state = state,
            onRetry = onRetry,
        )
    }
}
