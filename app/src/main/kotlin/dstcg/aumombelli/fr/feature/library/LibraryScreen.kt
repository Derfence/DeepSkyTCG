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
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import fr.aumombelli.dstcg.app.NewPlayerBlockingModal
import fr.aumombelli.dstcg.app.NewPlayerBlockingModalPage
import fr.aumombelli.dstcg.audio.LocalAudioController
import fr.aumombelli.dstcg.audio.SoundCue
import fr.aumombelli.dstcg.model.LibraryCardItem
import fr.aumombelli.dstcg.model.TradeCardCandidate
import fr.aumombelli.dstcg.ui.component.AstroCardThumbnail
import fr.aumombelli.dstcg.ui.component.RarityStarBadge
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
    val audioController = LocalAudioController.current
    var filters by remember(state.sections) { mutableStateOf(LibraryFilters()) }
    val displaySections = remember(state.sections, filters) {
        filterLibrarySections(
            sections = state.sections,
            filters = filters,
        )
    }
    val navigableCards = remember(displaySections) {
        buildNavigableLibraryCards(displaySections)
    }
    var navigationState by remember(state.sections, filters) {
        mutableStateOf<LibraryCardNavigationState?>(null)
    }
    var selectedVariantKeysByCardId by remember(state.sections, filters) {
        mutableStateOf<Map<String, String>>(emptyMap())
    }
    val totalCardsByExtension = remember(state.sections) {
        state.sections.associate { section ->
            section.extension.id to section.cards.size
        }
    }

    val openedCardIndex = navigationState?.cardId?.let { openedCardId ->
        navigableCards.indexOfFirst { it.definition.id == openedCardId }
            .takeIf { it >= 0 }
    }
    fun selectedVariantKeyFor(card: LibraryCardItem): String? =
        selectedVariantKeysByCardId[card.definition.id] ?: card.defaultLibraryVariantKey(filters)

    fun selectVariant(cardId: String, variantKey: String) {
        selectedVariantKeysByCardId = selectedVariantKeysByCardId + (cardId to variantKey)
    }

    val walkthroughVisible = showOnboardingVariantWalkthrough && state.onboardingVariantWalkthroughPages.isNotEmpty()
    val closePreviewToLibrary = {
        audioController.play(SoundCue.UiNavigate)
        navigationState = null
        selectedVariantKeysByCardId = emptyMap()
    }

    LaunchedEffect(showOnboardingHint) {
        if (!showOnboardingHint) return@LaunchedEffect
        delay(2_800)
        onOnboardingHintConsumed()
    }

    LaunchedEffect(navigableCards, navigationState?.cardId) {
        val openedCardId = navigationState?.cardId ?: return@LaunchedEffect
        if (navigableCards.none { it.definition.id == openedCardId }) {
            navigationState = null
            selectedVariantKeysByCardId = emptyMap()
        }
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
                    }
                    Text(
                        text = "Les cartes obtenues peuvent être ouvertes en aperçu puis en plein écran. Les autres gardent un visuel masqué jusqu'à leur première obtention.",
                        color = Color(0xFFD0E0F2),
                    )
                    LibraryFilterPanel(
                        options = state.filterOptions,
                        filters = filters,
                        onFiltersChanged = { filters = it },
                        enabled = interactionsEnabled && !walkthroughVisible && !state.isLoading,
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

            if (!state.isLoading && state.errorMessage == null && displaySections.isEmpty() && filters != LibraryFilters()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        text = "Aucune carte ne correspond aux filtres.",
                        color = Color(0xFFD0E0F2),
                        modifier = Modifier
                            .padding(vertical = 20.dp)
                            .testTag("library-no-filtered-cards"),
                    )
                }
            }

            displaySections.forEach { section ->
                item(span = { GridItemSpan(maxLineSpan) }) {
                    LibrarySectionHeader(
                        extensionId = section.extension.id,
                        extensionName = section.extension.name,
                        ownedCount = section.cards.count { it.ownedCount > 0 },
                        totalCount = totalCardsByExtension[section.extension.id] ?: section.cards.size,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp, bottom = 4.dp),
                    )
                }
                section.cards.groupedByLibraryRarity().forEach { (rarityLabel, cards) ->
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        LibraryRaritySubsectionHeader(
                            extensionId = section.extension.id,
                            rarityLabel = rarityLabel,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp, bottom = 2.dp),
                        )
                    }
                    items(cards, key = { it.definition.id }) { card ->
                        val filteredVariantKey = if (filters.affectsVariantChoice()) {
                            card.bestVariantMatching(filters)?.key
                        } else {
                            null
                        }
                        AstroCardThumbnail(
                            item = card,
                            modifier = Modifier.fillMaxWidth(),
                            selectedVariantKey = filteredVariantKey,
                            onClick = if (interactionsEnabled && !walkthroughVisible) {
                                {
                                    audioController.play(SoundCue.UiNavigate)
                                    val initialVariantKey = filteredVariantKey ?: card.defaultLibraryVariantKey(filters)
                                    if (initialVariantKey != null) {
                                        selectVariant(card.definition.id, initialVariantKey)
                                    }
                                    navigationState = LibraryCardNavigationState(
                                        cardId = card.definition.id,
                                        mode = LibraryCardDialogMode.Preview,
                                    )
                                }
                            } else {
                                {}
                            },
                        )
                    }
                }
            }
        }

        if (navigationState?.mode == LibraryCardDialogMode.Preview && openedCardIndex != null) {
            CardPreviewDialog(
                items = navigableCards,
                initialPage = openedCardIndex,
                selectedVariantKey = { card -> selectedVariantKeyFor(card) },
                onPageChanged = { card ->
                    navigationState = LibraryCardNavigationState(
                        cardId = card.definition.id,
                        mode = LibraryCardDialogMode.Preview,
                    )
                },
                onDismiss = closePreviewToLibrary,
                onExpand = { card ->
                    audioController.play(SoundCue.UiNavigate)
                    navigationState = LibraryCardNavigationState(
                        cardId = card.definition.id,
                        mode = LibraryCardDialogMode.Fullscreen,
                    )
                },
                onVariantSelected = { card, variantKey ->
                    selectVariant(card.definition.id, variantKey)
                },
                onTrade = { candidate ->
                    closePreviewToLibrary()
                    onOpenTrade(candidate)
                },
            )
        }

        if (navigationState?.mode == LibraryCardDialogMode.Fullscreen && openedCardIndex != null) {
            FullscreenCardDialog(
                items = navigableCards,
                initialPage = openedCardIndex,
                selectedVariantKey = { card -> selectedVariantKeyFor(card) },
                onPageChanged = { card ->
                    navigationState = LibraryCardNavigationState(
                        cardId = card.definition.id,
                        mode = LibraryCardDialogMode.Fullscreen,
                    )
                },
                onDismiss = closePreviewToLibrary,
                onVariantSelected = { card, variantKey ->
                    selectVariant(card.definition.id, variantKey)
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

@Composable
private fun LibrarySectionHeader(
    extensionId: String,
    extensionName: String,
    ownedCount: Int,
    totalCount: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.testTag("library-section-$extensionId"),
    ) {
        Text(
            text = extensionName,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF9EE7FF),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = "$ownedCount/$totalCount",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFD0E0F2),
            maxLines = 1,
            modifier = Modifier.testTag("library-section-count-$extensionId"),
        )
    }
}

@Composable
private fun LibraryRaritySubsectionHeader(
    extensionId: String,
    rarityLabel: String,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.testTag("library-rarity-section-$extensionId-$rarityLabel"),
    ) {
        RarityStarBadge(
            rarityLabel = rarityLabel,
            modifier = Modifier
                .size(20.dp)
                .padding(start = 2.dp)
                .testTag("library-rarity-section-star-$extensionId-$rarityLabel"),
        )
        Text(
            text = rarityLabel,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFFD0E0F2),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
