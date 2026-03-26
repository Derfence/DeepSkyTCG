package fr.aumombelli.gatcha.feature.library

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import fr.aumombelli.gatcha.model.toDisplayCard
import fr.aumombelli.gatcha.ui.component.AstroCardThumbnail
import fr.aumombelli.gatcha.ui.screen.gatchaContentInsetsPadding

@Composable
fun LibraryScreen(
    state: LibraryUiState,
    onRefresh: () -> Unit,
    contentVisible: Boolean = true,
) {
    val contentAlpha by animateFloatAsState(
        targetValue = if (contentVisible) 1f else 0f,
        animationSpec = tween(durationMillis = 960, easing = FastOutSlowInEasing),
        label = "library-content-alpha",
    )
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
    val closePreviewToLibrary = {
        previewCardId = null
        fullscreenCardId = null
        selectedVariantKey = null
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                alpha = contentAlpha
            }
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
                .gatchaContentInsetsPadding(includeBottom = true)
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
                onDismiss = closePreviewToLibrary,
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
                onDismiss = closePreviewToLibrary,
                onVariantSelected = { variantKey ->
                    selectedVariantKey = variantKey
                },
            )
        }
    }
}
