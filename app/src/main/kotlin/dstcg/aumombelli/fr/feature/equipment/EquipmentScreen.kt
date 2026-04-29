package fr.aumombelli.dstcg.feature.equipment

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import fr.aumombelli.dstcg.model.EquipmentType
import fr.aumombelli.dstcg.model.bonusLabel
import fr.aumombelli.dstcg.ui.component.EquipmentArtBackground
import fr.aumombelli.dstcg.ui.component.EquipmentArtMode
import fr.aumombelli.dstcg.ui.component.TRADING_CARD_WIDTH_OVER_HEIGHT
import fr.aumombelli.dstcg.ui.component.equipmentCategoryColorTokens
import fr.aumombelli.dstcg.ui.component.drawEquipmentMountGlyph
import fr.aumombelli.dstcg.ui.component.drawEquipmentObservatoryGlyph
import fr.aumombelli.dstcg.ui.component.drawEquipmentTelescopeGlyph
import fr.aumombelli.dstcg.ui.screen.dstcgContentInsetsPadding

@Composable
fun EquipmentScreen(
    state: EquipmentUiState,
    onRefresh: () -> Unit,
    onActivateEquipment: (String) -> Unit,
    contentVisible: Boolean = true,
    onOnboardingActivationBoundsChanged: (Rect?) -> Unit = {},
    onOnboardingActivationScrollHintChanged: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val cardsById = remember(state.sections) {
        state.sections
            .flatMap { it.cards }
            .associateBy { it.definition.id }
    }
    var openedCardId by remember(state.sections) { mutableStateOf<String?>(null) }
    val openedCard = openedCardId?.let(cardsById::get)
    val closeOpenedCard = {
        openedCardId = null
    }

    val onboardingActivationTargetEnabled =
        contentVisible &&
            state.activatingCardId == null &&
            state.activeEffects.isEmpty()
    val listCoordinatesHolder = remember { LayoutCoordinatesHolder() }
    val listState = rememberLazyListState()
    val firstActivatableCardId = state.sections.asSequence()
        .flatMap { it.cards.asSequence() }
        .firstOrNull { it.activationEnabled && onboardingActivationTargetEnabled }
        ?.definition
        ?.id
    val firstActivatableSectionIndex = state.sections.indexOfFirst { section ->
        section.cards.any { it.definition.id == firstActivatableCardId }
    }.takeIf { it >= 0 }
    val sectionListStartIndex = 1 +
        (if (state.isLoading) 1 else 0) +
        (if (state.errorMessage != null) 1 else 0) +
        1
    val firstActivatableSectionItemIndex = firstActivatableSectionIndex?.let(sectionListStartIndex::plus)
    val maxVisibleListItemIndex = listState.layoutInfo.visibleItemsInfo.maxOfOrNull { it.index }
    val activationTargetEnabled = firstActivatableCardId != null && onboardingActivationTargetEnabled
    val targetSectionOffscreenBelow =
        firstActivatableSectionItemIndex != null &&
            maxVisibleListItemIndex != null &&
            firstActivatableSectionItemIndex > maxVisibleListItemIndex
    LaunchedEffect(
        firstActivatableCardId,
        onboardingActivationTargetEnabled,
        firstActivatableSectionItemIndex,
        maxVisibleListItemIndex,
    ) {
        when {
            !activationTargetEnabled -> {
                onOnboardingActivationBoundsChanged(null)
                onOnboardingActivationScrollHintChanged(false)
            }

            targetSectionOffscreenBelow -> {
                onOnboardingActivationBoundsChanged(null)
                onOnboardingActivationScrollHintChanged(true)
            }
        }
    }

    val contentAlpha by animateFloatAsState(
        targetValue = if (contentVisible) 1f else 0f,
        animationSpec = tween(durationMillis = 880, easing = FastOutSlowInEasing),
        label = "equipment-content-alpha",
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer { alpha = contentAlpha }
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF07121F),
                        Color(0xFF10233D),
                        Color(0xFF193555),
                    ),
                ),
            )
            .testTag("equipment-screen"),
    ) {
        LazyColumn(
            state = listState,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(16.dp),
            modifier = Modifier
                .fillMaxSize()
                .dstcgContentInsetsPadding(includeBottom = true)
                .onGloballyPositioned { coordinates ->
                    listCoordinatesHolder.value = coordinates
                }
                .testTag("equipment-list"),
        ) {
            item(key = "equipment-header") {
                EquipmentHeader(
                    errorMessage = state.errorMessage,
                    onRefresh = onRefresh,
                )
            }

            if (state.isLoading) {
                item(key = "equipment-loading") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 36.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }

            state.errorMessage?.let { error ->
                item(key = "equipment-error") {
                    Text(
                        text = error,
                        color = Color(0xFFFFA3A3),
                        modifier = Modifier.testTag("equipment-error"),
                    )
                }
            }

            item(key = "equipment-active-summary") {
                EquipmentActiveSummaryCard(activeEffects = state.activeEffects)
            }

            items(state.sections, key = { it.type.code }) { section ->
                EquipmentSectionCard(
                    section = section,
                    activatingCardId = state.activatingCardId,
                    onPreviewEquipment = { equipmentCardId ->
                        openedCardId = equipmentCardId
                    },
                    onActivateEquipment = onActivateEquipment,
                    firstActivatableCardId = firstActivatableCardId,
                    contentVisible = contentVisible,
                    listCoordinatesProvider = { listCoordinatesHolder.value },
                    onOnboardingActivationBoundsChanged = onOnboardingActivationBoundsChanged,
                    onOnboardingActivationScrollHintChanged = onOnboardingActivationScrollHintChanged,
                )
            }
        }

        if (openedCard != null) {
            EquipmentCardFullscreenDialog(
                card = openedCard,
                onDismiss = closeOpenedCard,
            )
        }
    }
}

@Composable
private fun EquipmentHeader(
    errorMessage: String?,
    onRefresh: () -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = "Equipements",
            color = Color.White,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "Chaque categorie agit comme un module specialise. Active une carte pour influencer les prochains packs, un type a la fois.",
            color = Color(0xFFD0E0F2),
        )
        if (errorMessage != null) {
            Button(
                onClick = onRefresh,
                modifier = Modifier.testTag("equipment-refresh"),
            ) {
                Text("Reessayer")
            }
        }
    }
}

@Composable
private fun EquipmentActiveSummaryCard(
    activeEffects: List<EquipmentActiveSummaryItemUi>,
) {
    Surface(
        shape = RoundedCornerShape(30.dp),
        color = Color.Transparent,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("equipment-active-summary"),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0x6630415A),
                            Color(0x44213149),
                        ),
                    ),
                )
                .padding(18.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Effets actifs",
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                if (activeEffects.isEmpty()) {
                    Text(
                        text = "Aucun equipement actif pour le moment.",
                        color = Color(0xFFD0E0F2),
                    )
                } else {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(end = 4.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        items(activeEffects, key = { it.type.code }) { item ->
                            EquipmentActiveEffectChip(item = item)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EquipmentActiveEffectChip(
    item: EquipmentActiveSummaryItemUi,
) {
    val palette = equipmentPalette(item.type)

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = Color.Transparent,
        modifier = Modifier
            .width(264.dp)
            .testTag("equipment-active-${item.type.code}"),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(palette.cardBrush)
                .padding(14.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    EquipmentCategoryBadge(
                        type = item.type,
                        icon = item.visual.icon,
                        badgeSize = 44.dp,
                    )
                    Column(
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(
                            text = item.type.displayName,
                            color = Color.White,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = item.displayName,
                            color = Color(0xFFE9F1FF),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    EquipmentPill(
                        text = "${item.packsRemaining} packs",
                        palette = palette,
                    )
                }
                Text(
                    text = item.bonusLabel,
                    color = palette.accentText,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}

@Composable
private fun EquipmentSectionCard(
    section: EquipmentSectionUi,
    activatingCardId: String?,
    onPreviewEquipment: (String) -> Unit,
    onActivateEquipment: (String) -> Unit,
    firstActivatableCardId: String?,
    contentVisible: Boolean,
    listCoordinatesProvider: () -> LayoutCoordinates?,
    onOnboardingActivationBoundsChanged: (Rect?) -> Unit,
    onOnboardingActivationScrollHintChanged: (Boolean) -> Unit,
) {
    val palette = equipmentPalette(section.type)

    Surface(
        shape = RoundedCornerShape(30.dp),
        color = Color.Transparent,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("equipment-section-${section.type.code}"),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(30.dp))
                .background(palette.panelBrush),
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    EquipmentCategoryBadge(
                        type = section.type,
                        icon = section.visual.icon,
                        badgeSize = 62.dp,
                        modifier = Modifier.testTag("equipment-icon-${section.type.code}"),
                    )
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(
                            text = section.title,
                            color = Color.White,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = section.visual.benefitLabel,
                            color = palette.accentText,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.testTag("equipment-benefit-${section.type.code}"),
                        )
                    }
                }

                section.lastActivatedLabel?.let { label ->
                    Text(
                        text = "Dernier utilise : $label",
                        color = Color(0xFFF4DC99),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.testTag("equipment-last-used-${section.type.code}"),
                    )
                }

                if (section.cards.isEmpty()) {
                    Text(
                        text = "Aucune carte disponible pour cette categorie.",
                        color = Color(0xFFD0E0F2),
                    )
                } else {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(end = 4.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("equipment-cards-${section.type.code}"),
                    ) {
                        items(section.cards, key = { it.definition.id }) { card ->
                            EquipmentInventoryCard(
                                card = card,
                                palette = palette,
                                isActivating = activatingCardId == card.definition.id,
                                onPreviewEquipment = onPreviewEquipment,
                                onActivateEquipment = onActivateEquipment,
                                highlightOnboardingActivationTarget =
                                    card.definition.id == firstActivatableCardId,
                                contentVisible = contentVisible,
                                listCoordinatesProvider = listCoordinatesProvider,
                                onOnboardingActivationBoundsChanged = onOnboardingActivationBoundsChanged,
                                onOnboardingActivationScrollHintChanged = onOnboardingActivationScrollHintChanged,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EquipmentInventoryCard(
    card: EquipmentInventoryCardUi,
    palette: EquipmentCategoryPalette,
    isActivating: Boolean,
    onPreviewEquipment: (String) -> Unit,
    onActivateEquipment: (String) -> Unit,
    highlightOnboardingActivationTarget: Boolean,
    contentVisible: Boolean,
    listCoordinatesProvider: () -> LayoutCoordinates?,
    onOnboardingActivationBoundsChanged: (Rect?) -> Unit,
    onOnboardingActivationScrollHintChanged: (Boolean) -> Unit,
) {
    val reportOnboardingTargetBounds =
        highlightOnboardingActivationTarget &&
            contentVisible &&
            card.activationEnabled &&
            !isActivating
    val visual = card.definition.type.toCategoryVisualUi()
    val activeIndicatorVisible = card.isActive && card.packsRemaining != null

    DisposableEffect(reportOnboardingTargetBounds) {
        onDispose {
            if (reportOnboardingTargetBounds) {
                onOnboardingActivationBoundsChanged(null)
                onOnboardingActivationScrollHintChanged(false)
            }
        }
    }

    val cardShape = RoundedCornerShape(26.dp)

    Surface(
        shape = cardShape,
        color = Color.Transparent,
        modifier = Modifier
            .width(236.dp)
            .aspectRatio(TRADING_CARD_WIDTH_OVER_HEIGHT)
            .testTag("equipment-card-${card.definition.id}"),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(cardShape)
                .background(palette.cardBrush)
                .clickable { onPreviewEquipment(card.definition.id) },
        ) {
            EquipmentArtBackground(
                definition = card.definition,
                mode = EquipmentArtMode.Inventory,
                modifier = Modifier.fillMaxSize(),
                artTestTag = "equipment-card-art-${card.definition.id}",
                fallbackTestTag = "equipment-card-art-fallback-${card.definition.id}",
            )
            EquipmentCategoryBadge(
                type = card.definition.type,
                icon = visual.icon,
                badgeSize = 42.dp,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(14.dp)
                    .testTag("equipment-card-icon-${card.definition.id}"),
            )
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
            ) {
                EquipmentLevelPill(
                    text = "Niveau ${card.definition.level}",
                    palette = palette,
                )

                Text(
                    text = card.definition.displayName,
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = card.definition.bonusLabel(),
                    color = palette.accentText,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = "Actif pendant ${card.definition.packsAffected} packs",
                    color = Color(0xFFD5E5FB),
                    style = MaterialTheme.typography.bodyMedium,
                )

                Spacer(modifier = Modifier.weight(1f))

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        EquipmentMetricTile(
                            label = "Stock",
                            value = "x${card.stockCount}",
                        )
                        EquipmentMetricTile(
                            label = card.definition.type.usageCountLabel(),
                            value = "${card.activationCount}",
                        )
                    }

                    Button(
                        onClick = { onActivateEquipment(card.definition.id) },
                        enabled = card.activationEnabled && !isActivating,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = palette.accent,
                            contentColor = Color(0xFF06101D),
                            disabledContainerColor = Color.White.copy(alpha = 0.10f),
                            disabledContentColor = Color(0xFF8FA4BC),
                        ),
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .onGloballyPositioned { coordinates ->
                                if (reportOnboardingTargetBounds) {
                                    val listCoordinates = listCoordinatesProvider()
                                    val boundsInRoot = coordinates.boundsInRoot()
                                    val boundsInList = if (listCoordinates != null && listCoordinates.isAttached) {
                                        listCoordinates.localBoundingBoxOf(
                                            sourceCoordinates = coordinates,
                                            clipBounds = false,
                                        )
                                    } else {
                                        null
                                    }
                                    val coachmarkVisibility = resolveEquipmentActivationCoachmarkVisibility(
                                        targetEnabled = true,
                                        buttonBoundsInRoot = boundsInRoot,
                                        buttonBoundsInViewport = boundsInList,
                                        viewportHeightPx = listCoordinates?.size?.height?.toFloat() ?: 0f,
                                        targetSectionOffscreenBelow = false,
                                    )
                                    onOnboardingActivationBoundsChanged(coachmarkVisibility.visibleBounds)
                                    onOnboardingActivationScrollHintChanged(coachmarkVisibility.showScrollDownHint)
                                }
                            }
                            .testTag("equipment-activate-${card.definition.id}"),
                    ) {
                        Text(
                            when {
                                isActivating -> "Activation..."
                                card.isActive -> "Actif"
                                else -> "Activer"
                            },
                        )
                    }
                }
            }
            if (activeIndicatorVisible) {
                EquipmentActiveCardHalo(
                    palette = palette,
                    shape = cardShape,
                    modifier = Modifier
                        .matchParentSize()
                        .testTag("equipment-card-active-indicator-${card.definition.id}"),
                )
            }
        }
    }
}

@Composable
private fun EquipmentActiveCardHalo(
    palette: EquipmentCategoryPalette,
    shape: RoundedCornerShape,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .background(
                color = palette.accent.copy(alpha = 0.10f),
                shape = shape,
            )
            .border(
                width = 3.dp,
                color = palette.accent,
                shape = shape,
            )
            .padding(5.dp)
            .border(
                width = 1.dp,
                color = palette.accent.copy(alpha = 0.55f),
                shape = RoundedCornerShape(21.dp),
            ),
    )
}

@Composable
private fun EquipmentCardFullscreenDialog(
    card: EquipmentInventoryCardUi,
    onDismiss: () -> Unit,
) {
    val palette = equipmentPalette(card.definition.type)
    val visual = card.definition.type.toCategoryVisualUi()

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
                .testTag("equipment-card-fullscreen"),
        ) {
            Surface(
                shape = RoundedCornerShape(30.dp),
                color = Color.Transparent,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 56.dp, bottom = 8.dp),
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(30.dp))
                        .background(palette.panelBrush)
                        .verticalScroll(rememberScrollState())
                        .padding(start = 22.dp, top = 22.dp, end = 22.dp, bottom = 34.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(TRADING_CARD_WIDTH_OVER_HEIGHT)
                            .clip(RoundedCornerShape(24.dp))
                            .background(palette.cardBrush)
                            .testTag("equipment-card-fullscreen-hero"),
                    ) {
                        EquipmentArtBackground(
                            definition = card.definition,
                            mode = EquipmentArtMode.Detail,
                            modifier = Modifier.fillMaxSize(),
                            artTestTag = "equipment-card-fullscreen-art-${card.definition.id}",
                            fallbackTestTag = "equipment-card-fullscreen-art-fallback-${card.definition.id}",
                        )
                        EquipmentCategoryBadge(
                            type = card.definition.type,
                            icon = visual.icon,
                            badgeSize = 62.dp,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(14.dp)
                                .testTag("equipment-card-fullscreen-icon-${card.definition.id}"),
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        EquipmentCategoryBadge(
                            type = card.definition.type,
                            icon = visual.icon,
                            badgeSize = 72.dp,
                        )
                        Column(
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(
                                text = card.definition.type.displayName,
                                color = palette.accentText,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = card.definition.displayName,
                                color = Color.White,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = previewStatusLabel(card),
                                color = Color(0xFFD3E4F8),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        EquipmentLevelPill(
                            text = "Niveau ${card.definition.level}",
                            palette = palette,
                        )
                        EquipmentPill(
                            text = card.definition.type.toCategoryVisualUi().benefitLabel,
                            palette = palette,
                        )
                    }

                    Text(
                        text = card.definition.bonusLabel(),
                        color = palette.accentText,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        EquipmentMetricTile(
                            label = "Stock",
                            value = "x${card.stockCount}",
                        )
                        EquipmentMetricTile(
                            label = card.definition.type.usageCountLabel(),
                            value = "${card.activationCount}",
                        )
                        EquipmentMetricTile(
                            label = "Duree",
                            value = "${card.definition.packsAffected} packs",
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = Color.White.copy(alpha = 0.06f),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(16.dp),
                        ) {
                            Text(
                                text = "Description",
                                color = Color.White,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = card.definition.description,
                                color = Color(0xFFE8F2FF),
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        }
                    }
                }
            }

            EquipmentFullscreenCloseButton(onClick = onDismiss)
        }
    }
}

private fun previewStatusLabel(card: EquipmentInventoryCardUi): String = when {
    card.isActive && card.packsRemaining != null -> "${card.packsRemaining} packs restants"
    card.stockCount > 0 -> "Disponible"
    else -> "Pas encore obtenue"
}

@Composable
private fun EquipmentFullscreenCloseButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    IconButton(
        onClick = onClick,
        modifier = modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.34f))
            .testTag("equipment-card-fullscreen-close"),
    ) {
        Icon(
            imageVector = Icons.Filled.Close,
            contentDescription = "Fermer",
            tint = Color.White,
        )
    }
}

private fun Rect.isVerticallyVisibleWithin(viewportHeightPx: Float): Boolean =
    bottom > 0f && top < viewportHeightPx

internal data class EquipmentActivationCoachmarkVisibility(
    val visibleBounds: Rect?,
    val showScrollDownHint: Boolean,
)

internal fun resolveEquipmentActivationCoachmarkVisibility(
    targetEnabled: Boolean,
    buttonBoundsInRoot: Rect?,
    buttonBoundsInViewport: Rect?,
    viewportHeightPx: Float,
    targetSectionOffscreenBelow: Boolean,
): EquipmentActivationCoachmarkVisibility {
    if (!targetEnabled) {
        return EquipmentActivationCoachmarkVisibility(
            visibleBounds = null,
            showScrollDownHint = false,
        )
    }

    if (buttonBoundsInViewport != null && viewportHeightPx > 0f) {
        val buttonVisible = buttonBoundsInViewport.isVerticallyVisibleWithin(viewportHeightPx)
        return EquipmentActivationCoachmarkVisibility(
            visibleBounds = if (buttonVisible) buttonBoundsInRoot else null,
            showScrollDownHint = !buttonVisible && buttonBoundsInViewport.top >= viewportHeightPx,
        )
    }

    return EquipmentActivationCoachmarkVisibility(
        visibleBounds = null,
        showScrollDownHint = targetSectionOffscreenBelow,
    )
}

private class LayoutCoordinatesHolder {
    var value: LayoutCoordinates? = null
}

private fun EquipmentType.usageCountLabel(): String = when (this) {
    EquipmentType.Observatory,
    EquipmentType.Telescope,
    -> "Utilisés"

    EquipmentType.Mount -> "Utilisées"
}

@Composable
private fun EquipmentMetricTile(
    label: String,
    value: String,
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color.White.copy(alpha = 0.08f),
        modifier = Modifier.widthIn(min = 88.dp),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
        ) {
            Text(
                text = label,
                color = Color(0xFF9AB4D3),
                style = MaterialTheme.typography.labelSmall,
            )
            Text(
                text = value,
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun EquipmentPill(
    text: String,
    palette: EquipmentCategoryPalette,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = palette.chipColor,
        modifier = modifier,
    ) {
        Text(
            text = text,
            color = palette.accentText,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        )
    }
}

@Composable
private fun EquipmentLevelPill(
    text: String,
    palette: EquipmentCategoryPalette,
) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = Color.Black.copy(alpha = 0.20f),
    ) {
        Text(
            text = text,
            color = palette.accentText,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
        )
    }
}

@Composable
internal fun EquipmentCategoryBadge(
    type: EquipmentType,
    icon: EquipmentCategoryIconUi,
    badgeSize: Dp,
    modifier: Modifier = Modifier,
) {
    val palette = equipmentPalette(type)

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(badgeSize)
            .clip(CircleShape)
            .background(palette.iconBrush),
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(badgeSize * 0.2f),
        ) {
            val strokeWidth = minOf(size.width, size.height) * 0.075f
            val strokeColor = palette.iconStroke
            when (icon) {
                EquipmentCategoryIconUi.Observatory -> drawEquipmentObservatoryGlyph(
                    strokeColor = strokeColor,
                    strokeWidth = strokeWidth,
                )

                EquipmentCategoryIconUi.Telescope -> drawEquipmentTelescopeGlyph(
                    strokeColor = strokeColor,
                    strokeWidth = strokeWidth,
                )

                EquipmentCategoryIconUi.Mount -> drawEquipmentMountGlyph(
                    strokeColor = strokeColor,
                    strokeWidth = strokeWidth,
                )
            }
        }
    }
}

private data class EquipmentCategoryPalette(
    val panelBrush: Brush,
    val cardBrush: Brush,
    val iconBrush: Brush,
    val accent: Color,
    val accentText: Color,
    val iconStroke: Color,
    val chipColor: Color,
)

private fun equipmentPalette(type: EquipmentType): EquipmentCategoryPalette {
    val tokens = equipmentCategoryColorTokens(type)
    return EquipmentCategoryPalette(
        panelBrush = Brush.linearGradient(
            colors = listOf(
                tokens.panelStart,
                tokens.panelEnd,
            ),
        ),
        cardBrush = Brush.verticalGradient(
            colors = listOf(
                tokens.cardStart,
                tokens.cardEnd,
            ),
        ),
        iconBrush = Brush.radialGradient(
            colors = listOf(
                tokens.iconStart,
                tokens.iconEnd,
            ),
        ),
        accent = tokens.accent,
        accentText = tokens.accentText,
        iconStroke = tokens.iconStroke,
        chipColor = tokens.chipColor,
    )
}
