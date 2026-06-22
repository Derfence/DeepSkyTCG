package fr.aumombelli.dstcg.feature.library

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import fr.aumombelli.dstcg.model.LibraryCardItem
import fr.aumombelli.dstcg.model.TradeCardCandidate
import fr.aumombelli.dstcg.model.canTradeAway
import fr.aumombelli.dstcg.model.toDisplayCard
import fr.aumombelli.dstcg.ui.component.AstroCardDetailsSurface
import fr.aumombelli.dstcg.ui.component.AstroCardFullscreenCloseButton
import fr.aumombelli.dstcg.ui.component.AstroCardPreviewSurface
import fr.aumombelli.dstcg.ui.component.AstroCardSurfaceMode
import fr.aumombelli.dstcg.ui.component.DisplayCardVariantSelector
import fr.aumombelli.dstcg.ui.component.SceneNavigationButton
import fr.aumombelli.dstcg.ui.component.SceneNavigationIcon
import fr.aumombelli.dstcg.ui.component.calculateTradingCardFitWidth
import fr.aumombelli.dstcg.ui.screen.dstcgContentInsetsPadding

@Composable
internal fun CardPreviewDialog(
    items: List<LibraryCardItem>,
    initialPage: Int,
    selectedVariantKey: (LibraryCardItem) -> String?,
    onPageChanged: (LibraryCardItem) -> Unit,
    onDismiss: () -> Unit,
    onExpand: (LibraryCardItem) -> Unit,
    onVariantSelected: (LibraryCardItem, String) -> Unit,
    onTrade: ((TradeCardCandidate) -> Unit)? = null,
) {
    if (items.isEmpty()) return
    val safeInitialPage = initialPage.coerceIn(0, items.lastIndex)
    val pagerState = rememberPagerState(
        initialPage = safeInitialPage,
        pageCount = { items.size },
    )

    BackHandler(onBack = onDismiss)

    LaunchedEffect(items.size) {
        if (pagerState.currentPage > items.lastIndex) {
            pagerState.scrollToPage(items.lastIndex)
        }
    }

    LaunchedEffect(items, pagerState.settledPage) {
        items.getOrNull(pagerState.settledPage)?.let(onPageChanged)
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.32f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss,
            )
            .dstcgContentInsetsPadding(includeBottom = true)
            .padding(16.dp)
            .testTag("library-card-preview"),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                SceneNavigationButton(
                    icon = SceneNavigationIcon.Close,
                    onClick = onDismiss,
                    contentDescription = "Fermer",
                    testTag = "library-card-preview-close",
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = true),
            ) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("library-card-preview-pager"),
                ) { page ->
                    val libraryItem = items[page]
                    val displayCard = libraryItem.toDisplayCard(selectedVariantKey(libraryItem)) ?: return@HorizontalPager
                    val tradeCandidate = libraryItem.toTradeCandidateOrNull(displayCard.activeVariant)

                    Box(modifier = Modifier.fillMaxSize()) {
                        if (page == pagerState.settledPage) {
                            Text(
                                text = libraryItem.definition.id,
                                modifier = Modifier
                                    .size(0.dp)
                                    .testTag("library-card-preview-current-id"),
                            )
                        }
                        Column(
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            BoxWithConstraints(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f, fill = true),
                            ) {
                                val cardWidth = calculateTradingCardFitWidth(
                                    maxWidth = maxWidth,
                                    maxHeight = maxHeight,
                                )
                                AstroCardPreviewSurface(
                                    displayCard = displayCard,
                                    mode = AstroCardSurfaceMode.Preview,
                                    modifier = Modifier
                                        .width(cardWidth)
                                        .testTag(
                                            if (page == pagerState.currentPage) {
                                                "library-card-preview-surface"
                                            } else {
                                                "library-card-preview-surface-${libraryItem.definition.id}"
                                            },
                                        ),
                                    onClick = { onExpand(libraryItem) },
                                )
                            }
                            DisplayCardVariantSelector(
                                variants = displayCard.availableVariants,
                                selectedVariantKey = displayCard.activeVariant.key,
                                onVariantSelected = { variant -> onVariantSelected(libraryItem, variant.key) },
                                modifier = Modifier.fillMaxWidth(),
                            )
                            if (tradeCandidate != null && onTrade != null) {
                                Button(
                                    onClick = { onTrade(tradeCandidate) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("library-card-trade"),
                                ) {
                                    Text("Échanger")
                                }
                            }
                        }
                    }
                }

                if (pagerState.currentPage > 0) {
                    LibraryNavigationHintArrow(
                        direction = LibraryNavigationHintDirection.Left,
                        testTag = "library-card-preview-arrow-left",
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .absoluteOffset(x = -LibraryNavigationHintOuterOffset),
                    )
                }
                if (pagerState.currentPage < items.lastIndex) {
                    LibraryNavigationHintArrow(
                        direction = LibraryNavigationHintDirection.Right,
                        testTag = "library-card-preview-arrow-right",
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .absoluteOffset(x = LibraryNavigationHintOuterOffset),
                    )
                }
            }
        }
    }
}

@Composable
internal fun FullscreenCardDialog(
    items: List<LibraryCardItem>,
    initialPage: Int,
    selectedVariantKey: (LibraryCardItem) -> String?,
    onPageChanged: (LibraryCardItem) -> Unit,
    onDismiss: () -> Unit,
    onVariantSelected: (LibraryCardItem, String) -> Unit,
) {
    if (items.isEmpty()) return
    val safeInitialPage = initialPage.coerceIn(0, items.lastIndex)
    val pagerState = rememberPagerState(
        initialPage = safeInitialPage,
        pageCount = { items.size },
    )

    LaunchedEffect(items.size) {
        if (pagerState.currentPage > items.lastIndex) {
            pagerState.scrollToPage(items.lastIndex)
        }
    }

    LaunchedEffect(items, pagerState.settledPage) {
        items.getOrNull(pagerState.settledPage)?.let(onPageChanged)
    }

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
                .dstcgContentInsetsPadding(includeBottom = true)
                .padding(14.dp)
                .testTag("astro-card-fullscreen"),
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("astro-card-fullscreen-pager"),
            ) { page ->
                val libraryItem = items[page]
                val displayCard = libraryItem.toDisplayCard(selectedVariantKey(libraryItem)) ?: return@HorizontalPager

                Box(modifier = Modifier.fillMaxSize()) {
                    if (page == pagerState.settledPage) {
                        Text(
                            text = libraryItem.definition.id,
                            modifier = Modifier
                                .size(0.dp)
                                .testTag("astro-card-fullscreen-current-id"),
                        )
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
                                onVariantSelected = { variant -> onVariantSelected(libraryItem, variant.key) },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        },
                    )
                }
            }
            if (pagerState.currentPage > 0) {
                LibraryNavigationHintArrow(
                    direction = LibraryNavigationHintDirection.Left,
                    testTag = "astro-card-fullscreen-arrow-left",
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .absoluteOffset(x = -LibraryNavigationHintOuterOffset),
                )
            }
            if (pagerState.currentPage < items.lastIndex) {
                LibraryNavigationHintArrow(
                    direction = LibraryNavigationHintDirection.Right,
                    testTag = "astro-card-fullscreen-arrow-right",
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .absoluteOffset(x = LibraryNavigationHintOuterOffset),
                )
            }
            AstroCardFullscreenCloseButton(onClick = onDismiss)
        }
    }
}

private fun LibraryCardItem.toTradeCandidateOrNull(
    activeVariant: fr.aumombelli.dstcg.model.DisplayCardVariant,
): TradeCardCandidate? =
    if (activeVariant.canTradeAway()) {
        TradeCardCandidate(
            card = definition,
            extensionName = extensionName,
            variant = activeVariant,
        )
    } else {
        null
    }

private enum class LibraryNavigationHintDirection {
    Left,
    Right,
}

private val LibraryNavigationHintOuterOffset = 25.dp

@Composable
private fun LibraryNavigationHintArrow(
    direction: LibraryNavigationHintDirection,
    testTag: String,
    modifier: Modifier = Modifier,
) {
    Icon(
        imageVector = when (direction) {
            LibraryNavigationHintDirection.Left -> Icons.AutoMirrored.Filled.KeyboardArrowLeft
            LibraryNavigationHintDirection.Right -> Icons.AutoMirrored.Filled.KeyboardArrowRight
        },
        contentDescription = null,
        tint = Color.White.copy(alpha = 0.74f),
        modifier = modifier
            .size(46.dp)
            .testTag(testTag),
    )
}
