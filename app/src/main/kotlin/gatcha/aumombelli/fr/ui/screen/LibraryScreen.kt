package gatcha.aumombelli.fr.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import gatcha.aumombelli.fr.model.LibraryCardItem
import gatcha.aumombelli.fr.ui.viewmodel.LibraryUiState

@Composable
fun LibraryScreen(
    state: LibraryUiState,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 150.dp),
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF08101D), Color(0xFF0E1D33)),
                ),
            ),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                TextButton(onClick = onBack) {
                    Text("Back")
                }
                Text(
                    text = "Library",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
                Text(
                    text = "Toutes les cartes sont groupées par extension et triées par identifiant.",
                    color = Color(0xFFD0E0F2),
                )
                if (state.errorMessage != null) {
                    Button(onClick = onRefresh) {
                        Text("Retry")
                    }
                }
            }
        }

        if (state.isLoading) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                ) {
                    CircularProgressIndicator(modifier = Modifier.padding(8.dp))
                }
            }
        }

        state.errorMessage?.let { error ->
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(text = error, color = Color(0xFFFF8E8E))
            }
        }

        state.sections.forEach { section ->
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    text = section.extension.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF9EE7FF),
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
                )
            }
            items(section.cards, key = { it.definition.id }) { card ->
                LibraryCard(
                    item = card,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun LibraryCard(
    item: LibraryCardItem,
    modifier: Modifier = Modifier,
) {
    val owned = item.ownedCount > 0
    Card(
        modifier = modifier.alpha(if (owned) 1f else 0.45f),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            rarityColor(item.definition.rarityLabel),
                            Color(0xFF101828),
                        ),
                    ),
                )
                .padding(14.dp),
        ) {
            Text(
                text = item.definition.id,
                color = Color.White.copy(alpha = 0.9f),
                style = MaterialTheme.typography.labelMedium,
            )
            Text(
                text = item.definition.name,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = item.definition.rarityLabel,
                color = Color(0xFFFCE8B2),
            )
            Text(
                text = if (owned) "Owned: ${item.ownedCount}" else "Not owned yet",
                color = Color(0xFFD3E3F4),
            )
        }
    }
}

private fun rarityColor(rarityLabel: String): Color = when (rarityLabel) {
    "Epic" -> Color(0xFF8D5CFF)
    "Rare" -> Color(0xFF2EC4B6)
    else -> Color(0xFF587291)
}
