package fr.aumombelli.gatcha.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.unit.dp
import fr.aumombelli.gatcha.model.LibraryCardItem
import fr.aumombelli.gatcha.model.toDisplayCard
import fr.aumombelli.gatcha.ui.component.AstroCardDetailsSurface
import fr.aumombelli.gatcha.ui.component.AstroCardPreviewSurface
import fr.aumombelli.gatcha.ui.component.AstroCardThumbnail
import fr.aumombelli.gatcha.ui.component.DisplayCardVariantSelector
import fr.aumombelli.gatcha.ui.viewmodel.LibraryUiState

@Composable
fun LibraryScreen(
    state: LibraryUiState,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
) {
    val cardsById = remember(state.sections) {
        state.sections
            .flatMap { it.cards }
            .associateBy { it.definition.id }
    }
    var previewCardId by remember(state.sections) { mutableStateOf<String?>(null) }
    var fullscreenCardId by remember(state.sections) { mutableStateOf<String?>(null) }
    var selectedVariantKey by remember(state.sections) { mutableStateOf<String?>(null) }

    val previewItem = previewCardId?.let(cardsById::get)
    val fullscreenItem = fullscreenCardId?.let(cardsById::get)
    val previewCard = previewItem?.toDisplayCard(selectedVariantKey)
    val fullscreenCard = fullscreenItem?.toDisplayCard(selectedVariantKey)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF08101D), Color(0xFF0E1D33)),
                ),
            ),
    ) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 150.dp),
            modifier = Modifier
                .fillMaxSize()
                .testTag("library-grid"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    TextButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("library-back"),
                    ) {
                        Text("Back")
                    }
                    Text(
                        text = "Library",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                    Text(
                        text = "Les cartes obtenues peuvent etre ouvertes en apercu puis en plein ecran.",
                        color = Color(0xFFD0E0F2),
                    )
                    if (state.errorMessage != null) {
                        Button(
                            onClick = onRefresh,
                            modifier = Modifier.testTag("library-refresh"),
                        ) {
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
                        modifier = Modifier
                            .padding(top = 8.dp, bottom = 4.dp)
                            .testTag("library-section-${section.extension.id}"),
                    )
                }
                items(section.cards, key = { it.definition.id }) { card ->
                    AstroCardThumbnail(
                        item = card,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            previewCardId = card.definition.id
                            selectedVariantKey = card.availableVariants.firstOrNull()?.key
                            fullscreenCardId = null
                        },
                    )
                }
            }
        }

        if (previewCard != null && fullscreenCardId == null) {
            CardPreviewDialog(
                item = previewItem,
                selectedVariantKey = selectedVariantKey,
                onDismiss = {
                    previewCardId = null
                    selectedVariantKey = null
                },
                onExpand = {
                    fullscreenCardId = previewCardId
                },
                onVariantSelected = { variantKey ->
                    selectedVariantKey = variantKey
                },
            )
        }

        if (fullscreenCard != null) {
            FullscreenCardDialog(
                item = fullscreenItem,
                selectedVariantKey = selectedVariantKey,
                onDismiss = { fullscreenCardId = null },
                onVariantSelected = { variantKey ->
                    selectedVariantKey = variantKey
                },
            )
        }
    }
}

@Composable
private fun CardPreviewDialog(
    item: LibraryCardItem?,
    selectedVariantKey: String?,
    onDismiss: () -> Unit,
    onExpand: () -> Unit,
    onVariantSelected: (String) -> Unit,
) {
    val displayCard = item?.toDisplayCard(selectedVariantKey) ?: return

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnClickOutside = true),
    ) {
        Surface(
            color = Color(0xFF07101A),
            shape = RoundedCornerShape(30.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
                .testTag("library-card-preview"),
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.padding(16.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = "Apercu",
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("library-card-preview-close"),
                    ) {
                        Text("Fermer")
                    }
                }
                AstroCardPreviewSurface(
                    displayCard = displayCard,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("library-card-preview-surface"),
                    onClick = onExpand,
                    accessoryContent = {
                        DisplayCardVariantSelector(
                            variants = displayCard.availableVariants,
                            selectedVariantKey = displayCard.activeVariant.key,
                            onVariantSelected = { variant -> onVariantSelected(variant.key) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    },
                )
                Text(
                    text = "Touchez la carte pour l'agrandir.",
                    color = Color(0xFFD0E0F2),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
private fun FullscreenCardDialog(
    item: LibraryCardItem?,
    selectedVariantKey: String?,
    onDismiss: () -> Unit,
    onVariantSelected: (String) -> Unit,
) {
    val displayCard = item?.toDisplayCard(selectedVariantKey) ?: return

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false,
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xE608101A))
                .padding(14.dp)
                .testTag("astro-card-fullscreen"),
        ) {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .testTag("astro-card-fullscreen-close"),
            ) {
                Text("Fermer")
            }
            AstroCardDetailsSurface(
                displayCard = displayCard,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 42.dp),
                accessoryContent = {
                    DisplayCardVariantSelector(
                        variants = displayCard.availableVariants,
                        selectedVariantKey = displayCard.activeVariant.key,
                        onVariantSelected = { variant -> onVariantSelected(variant.key) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                },
            )
        }
    }
}
