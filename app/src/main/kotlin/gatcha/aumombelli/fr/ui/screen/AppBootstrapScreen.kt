package fr.aumombelli.gatcha.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import fr.aumombelli.gatcha.ui.theme.AuroraTeal
import fr.aumombelli.gatcha.ui.theme.EmberGold
import fr.aumombelli.gatcha.ui.viewmodel.AppBootstrapUiState

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
        Card(
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier
                .align(Alignment.Center)
                .gatchaContentInsetsPadding(includeBottom = true)
                .padding(24.dp),
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
            ) {
                Text(
                    text = "Gatcha",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = if (state.isLoading) {
                        "Checking catalog compatibility..."
                    } else {
                        "A compatible client/server pair is required before the app can start."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                )

                if (state.isLoading) {
                    CircularProgressIndicator(
                        color = AuroraTeal,
                        strokeWidth = 2.dp,
                        modifier = Modifier.testTag("app-bootstrap-loading"),
                    )
                } else {
                    Text(
                        text = state.message ?: "Compatibility could not be verified.",
                        color = EmberGold,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.testTag("app-bootstrap-message"),
                    )
                    if (state.canRetry) {
                        Button(
                            onClick = onRetry,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("app-bootstrap-retry"),
                        ) {
                            Text("Retry")
                        }
                    }
                }
            }
        }
    }
}
