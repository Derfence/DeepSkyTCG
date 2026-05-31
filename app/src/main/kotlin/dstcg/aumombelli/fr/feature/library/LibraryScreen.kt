package fr.aumombelli.dstcg.feature.library

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import fr.aumombelli.dstcg.app.NewPlayerBlockingModal
import fr.aumombelli.dstcg.app.NewPlayerBlockingModalPage
import fr.aumombelli.dstcg.model.TradeCardCandidate
import fr.aumombelli.dstcg.model.firstTradeableVariant
import fr.aumombelli.dstcg.model.hasTradeableVariant
import fr.aumombelli.dstcg.model.toDisplayCard
import fr.aumombelli.dstcg.ui.component.AstroCardThumbnail
import fr.aumombelli.dstcg.ui.component.SceneNavigationButton
import fr.aumombelli.dstcg.ui.component.SceneNavigationIcon
import fr.aumombelli.dstcg.ui.screen.dstcgContentInsetsPadding
import kotlinx.coroutines.delay

@Composable
fun LibraryScreen(
    state: LibraryUiState,
    onRefresh: () -> Unit,
    onOpenTrade: (TradeCardCandidate) -> Unit = {},
    contentVisible: Boolean = true,
    interactionsEnabled: Boolean = true,
    showOnboardingHint: Boolean = false,
    onOnboardingHintConsumed: () -> Unit = {},
    showOnboardingVariantWalkthrough: Boolean = false,
    onOnboardingVariantWalkthroughCompleted: () -> Unit = {},
    onBack: (() -> Unit)? = null,
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
    var showExchangeableOnly by remember(state.sections) { mutableStateOf(false) }
    val displaySections = remember(state.sections, showExchangeableOnly) {
        if (!showExchangeableOnly) {
            state.sections
        } else {
            state.sections.mapNotNull { section ->
                val cards = section.cards.filter { card ->
                    card.hasTradeableVariant()
                }
                section.copy(cards = cards).takeIf { cards.isNotEmpty() }
            }
        }
    }

    val previewItem = previewCardId?.let(cardsById::get)
    val fullscreenItem = fullscreenCardId?.let(cardsById::get)
    val previewCard = previewItem?.toDisplayCard(selectedVariantKey)
    val fullscreenCard = fullscreenItem?.toDisplayCard(selectedVariantKey)
    val walkthroughVisible = showOnboardingVariantWalkthrough && state.onboardingVariantWalkthroughPages.isNotEmpty()
    val closePreviewToLibrary = {
        previewCardId = null
        fullscreenCardId = null
        selectedVariantKey = null
    }

    LaunchedEffect(showOnboardingHint) {
        if (!showOnboardingHint) return@LaunchedEffect
        delay(2_800)
        onOnboardingHintConsumed()
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
                .dstcgContentInsetsPadding(includeBottom = true)
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
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f),
                        ) {
                            onBack?.let { back ->
                                SceneNavigationButton(
                                    icon = SceneNavigationIcon.Back,
                                    onClick = back,
                                    contentDescription = "Retour",
                                    testTag = "library-back",
                                )
                            }
                            Text(
                                text = "Bibliothèque",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                            )
                        }
                        FilterChip(
                            selected = showExchangeableOnly,
                            onClick = { showExchangeableOnly = !showExchangeableOnly },
                            enabled = interactionsEnabled && !walkthroughVisible,
                            label = { Text("Échangeable") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Filled.SwapHoriz,
                                    contentDescription = null,
                                )
                            },
                            modifier = Modifier.testTag("library-filter-tradeable"),
                        )
                    }
                    Text(
                        text = "Les cartes obtenues peuvent etre ouvertes en apercu puis en plein ecran. Les autres gardent un visuel masque jusqu'a leur premiere obtention.",
                        color = Color(0xFFD0E0F2),
                    )
                    if (showOnboardingHint) {
                        Text(
                            text = "Touche une carte obtenue pour l'ouvrir.",
                            color = Color(0xFFF8D98D),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.testTag("library-onboarding-hint"),
                        )
                    }
                    if (state.errorMessage != null) {
                        Button(
                            onClick = onRefresh,
                            modifier = Modifier.testTag("library-refresh"),
                        ) {
                            Text("Réessayer")
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

            if (!state.isLoading && state.errorMessage == null && displaySections.isEmpty() && showExchangeableOnly) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        text = "Aucune carte echangeable pour le moment.",
                        color = Color(0xFFD0E0F2),
                        modifier = Modifier
                            .padding(vertical = 20.dp)
                            .testTag("library-no-tradeable-cards"),
                    )
                }
            }

            displaySections.forEach { section ->
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
                        onClick = if (interactionsEnabled && !walkthroughVisible) {
                            {
                                previewCardId = card.definition.id
                                selectedVariantKey = if (showExchangeableOnly) {
                                    card.firstTradeableVariant()?.key
                                } else {
                                    card.availableVariants.firstOrNull()?.key
                                }
                                fullscreenCardId = null
                            }
                        } else {
                            {}
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
                onTrade = { candidate ->
                    closePreviewToLibrary()
                    onOpenTrade(candidate)
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

        if (walkthroughVisible) {
            NewPlayerBlockingModal(
                testTag = "new-player-modal-library-variants",
                pages = state.onboardingVariantWalkthroughPages.map { page ->
                    NewPlayerBlockingModalPage(
                        title = page.title,
                        message = page.message,
                    )
                },
                finishButtonLabel = "Terminer",
                onFinished = onOnboardingVariantWalkthroughCompleted,
                heightAwarePageContent = { pageIndex, availableHeight ->
                    LibraryOnboardingVariantWalkthroughVisual(
                        page = state.onboardingVariantWalkthroughPages[pageIndex],
                        availableHeight = (availableHeight - 4.dp).coerceAtLeast(0.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                    )
                },
            )
        }
    }
}
