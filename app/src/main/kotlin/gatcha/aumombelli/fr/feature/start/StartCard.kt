package fr.aumombelli.gatcha.feature.start

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import fr.aumombelli.gatcha.ui.screen.gatchaContentInsetsPadding
import fr.aumombelli.gatcha.ui.theme.AuroraTeal

@Composable
internal fun BoxScope.StartCard(
    state: StartUiState,
    cardAlpha: Float,
    onBegin: () -> Unit,
    onResetProgress: () -> Unit,
    onCardTopChanged: (Float) -> Unit,
) {
    Card(
        shape = RoundedCornerShape(28.dp),
        modifier = Modifier
            .align(Alignment.Center)
            .onGloballyPositioned { coordinates ->
                onCardTopChanged(coordinates.positionInRoot().y)
            }
            .graphicsLayer {
                alpha = cardAlpha
            }
            .gatchaContentInsetsPadding(includeBottom = true)
            .padding(24.dp),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .testTag("app-launch-start-card"),
        ) {
            Text(
                text = "Gatcha",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.testTag("start-title"),
            )
            Text(
                text = "Commence une partie locale et fais grandir ta collection hors ligne.",
                style = MaterialTheme.typography.bodyMedium,
            )

            state.errorMessage?.let { error ->
                Text(
                    text = error,
                    color = Color(0xFFFF7A7A),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.testTag("start-error"),
                )
            }

            state.warningMessage?.let { warning ->
                Text(
                    text = warning,
                    color = Color(0xFFFFD28A),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.testTag("start-warning"),
                )
            }

            Button(
                onClick = onBegin,
                enabled = !state.isLoading &&
                    !state.isTransitioningToMenu &&
                    state.errorMessage == null &&
                    !state.isResettingProgress,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("start-begin"),
            ) {
                if (state.isLoading || state.isTransitioningToMenu) {
                    CircularProgressIndicator(
                        color = AuroraTeal,
                        strokeWidth = 2.dp,
                        modifier = Modifier.padding(vertical = 4.dp),
                    )
                } else {
                    Text("Commencer")
                }
            }

            if (state.canResetProgress) {
                Button(
                    onClick = onResetProgress,
                    enabled = !state.isLoading && !state.isResettingProgress,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("start-reset-progress"),
                ) {
                    Text("Réinitialiser la progression")
                }
            }
        }
    }
}
