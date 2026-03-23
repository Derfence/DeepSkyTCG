package fr.aumombelli.gatcha.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import fr.aumombelli.gatcha.model.ExtensionDefinition
import fr.aumombelli.gatcha.ui.viewmodel.PackSelectionUiState
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun PackSelectionScreen(
    state: PackSelectionUiState,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onOpenPack: (String) -> Unit,
) {
    val nextDrawAtText = formatNextDrawAt(state.nextDrawAt)
    val drawLocked = state.nextDrawAt?.let { runCatching { Instant.parse(it).isAfter(Instant.now()) }.getOrDefault(false) } ?: false

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(listOf(Color(0xFF09111E), Color(0xFF14263D))),
            )
            .padding(16.dp),
    ) {
        item {
            TextButton(onClick = onBack) {
                Text("Back")
            }
        }
        item {
            Text(
                text = "Open Pack",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
        }
        item {
            Text(
                text = if (nextDrawAtText == null) {
                    "Aucun cooldown actif. Choisis une extension."
                } else {
                    "Prochain tirage disponible : $nextDrawAtText"
                },
                color = Color(0xFFD6E4F5),
            )
        }

        if (state.isLoading) {
            item {
                CircularProgressIndicator(modifier = Modifier.padding(top = 16.dp))
            }
        }

        state.errorMessage?.let { error ->
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = error, color = Color(0xFFFF9090))
                    Button(onClick = onRefresh) {
                        Text("Retry")
                    }
                }
            }
        }

        items(state.extensions, key = { it.id }) { extension ->
            ExtensionCard(
                extension = extension,
                drawLocked = drawLocked,
                onOpenPack = onOpenPack,
            )
        }
    }
}

@Composable
private fun ExtensionCard(
    extension: ExtensionDefinition,
    drawLocked: Boolean,
    onOpenPack: (String) -> Unit,
) {
    Card {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(Color(0xFF24486C), Color(0xFF121F31)),
                    ),
                )
                .padding(18.dp),
        ) {
            Text(
                text = extension.name,
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "Extension ID: ${extension.id}",
                color = Color(0xFFD4E7FF),
            )
            Button(
                onClick = { onOpenPack(extension.id) },
                enabled = !drawLocked,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (drawLocked) "Pack locked" else "Draw this pack")
            }
        }
    }
}

private fun formatNextDrawAt(nextDrawAt: String?): String? {
    val instant = nextDrawAt?.let { runCatching { Instant.parse(it) }.getOrNull() } ?: return null
    if (!instant.isAfter(Instant.now())) return null
    return DateTimeFormatter.ofPattern("dd/MM HH:mm")
        .withZone(ZoneId.systemDefault())
        .format(instant)
}
