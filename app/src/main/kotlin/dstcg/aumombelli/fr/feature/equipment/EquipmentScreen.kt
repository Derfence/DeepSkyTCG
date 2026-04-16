package fr.aumombelli.dstcg.feature.equipment

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
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

    DisposableEffect(reportOnboardingTargetBounds) {
        onDispose {
            if (reportOnboardingTargetBounds) {
                onOnboardingActivationBoundsChanged(null)
                onOnboardingActivationScrollHintChanged(false)
            }
        }
    }

    Surface(
        shape = RoundedCornerShape(26.dp),
        color = Color.Transparent,
        modifier = Modifier
            .width(236.dp)
            .testTag("equipment-card-${card.definition.id}"),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(26.dp))
                .background(palette.cardBrush)
                .clickable { onPreviewEquipment(card.definition.id) }
                .padding(16.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    EquipmentLevelPill(
                        text = "Niveau ${card.definition.level}",
                        palette = palette,
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    if (card.isActive && card.packsRemaining != null) {
                        EquipmentLevelPill(
                            text = "Actif",
                            palette = palette,
                        )
                    }
                }

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
    }
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
                    .padding(top = 42.dp),
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(30.dp))
                        .background(palette.panelBrush)
                        .verticalScroll(rememberScrollState())
                        .padding(22.dp),
                ) {
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
private fun EquipmentCategoryBadge(
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
                EquipmentCategoryIconUi.Observatory -> {
                    drawRoundRect(
                        color = strokeColor,
                        topLeft = Offset(size.width * 0.2f, size.height * 0.62f),
                        size = Size(size.width * 0.6f, size.height * 0.14f),
                        cornerRadius = CornerRadius(size.width * 0.08f, size.width * 0.08f),
                    )
                    drawArc(
                        color = strokeColor,
                        startAngle = 180f,
                        sweepAngle = 180f,
                        useCenter = false,
                        topLeft = Offset(size.width * 0.2f, size.height * 0.26f),
                        size = Size(size.width * 0.6f, size.height * 0.52f),
                        style = Stroke(width = strokeWidth),
                    )
                    drawLine(
                        color = strokeColor,
                        start = Offset(size.width * 0.75f, size.height * 0.2f),
                        end = Offset(size.width * 0.7f, size.height * 0.25f),
                        strokeWidth = strokeWidth * 1.5f,
                        cap = StrokeCap.Square,
                    )
                    drawCircle(
                        color = strokeColor,
                        radius = strokeWidth * 0.55f,
                        center = Offset(size.width * 0.74f, size.height * 0.22f),
                    )
                }

                EquipmentCategoryIconUi.Telescope -> {
                    val bodyCenter = Offset(size.width * 0.48f, size.height * 0.46f)
                    rotate(
                        degrees = -45f,
                        pivot = bodyCenter,
                    ) {
                        val bodyWidth = size.width
                        val bodyHeight = size.height * 0.42f
                        val bodyTopLeft = Offset(
                            bodyCenter.x - bodyWidth / 2f,
                            bodyCenter.y - bodyHeight / 2f,
                        )
                        val bodyCorner = CornerRadius(bodyHeight * 0.48f, bodyHeight * 0.48f)
                        drawRoundRect(
                            color = strokeColor,
                            topLeft = bodyTopLeft,
                            size = Size(bodyWidth, bodyHeight),
                            cornerRadius = bodyCorner,
                        )
                        val apertureWidth = bodyWidth * 0.22f
                        val apertureHeight = bodyHeight * 0.72f
                        val apertureTopLeft = Offset(
                            bodyTopLeft.x + bodyWidth - apertureWidth - bodyWidth * 0.05f,
                            bodyTopLeft.y + (bodyHeight - apertureHeight) / 2f,
                        )
                        drawOval(
                            color = Color.Black.copy(alpha = 0.34f),
                            topLeft = apertureTopLeft,
                            size = Size(apertureWidth, apertureHeight),
                        )
                        val spiderCenter = Offset(
                            apertureTopLeft.x + apertureWidth/2,
                            bodyCenter.y,
                        )
                        val spiderStroke = strokeWidth * 0.55f
                        val spiderHorizontalStart = Offset(spiderCenter.x - apertureWidth/2, spiderCenter.y)
                        val spiderHorizontalEnd = Offset(spiderCenter.x + apertureWidth/2, spiderCenter.y)
                        val spiderVerticalStart = Offset(spiderCenter.x, spiderCenter.y - apertureHeight/2)
                        val spiderVerticalEnd = Offset(spiderCenter.x, spiderCenter.y + apertureHeight/2)
                        drawLine(
                            color = strokeColor,
                            start = spiderHorizontalStart,
                            end = spiderHorizontalEnd,
                            strokeWidth = spiderStroke,
                            cap = StrokeCap.Round,
                        )
                        drawLine(
                            color = strokeColor,
                            start = spiderVerticalStart,
                            end = spiderVerticalEnd,
                            strokeWidth = spiderStroke,
                            cap = StrokeCap.Round,
                        )
                        val secondaryWidth = bodyHeight * 0.16f
                        val secondaryHeight = bodyHeight * 0.11f
                        val secondaryTopLeft = Offset(
                            spiderCenter.x - secondaryWidth / 2f,
                            spiderCenter.y - secondaryHeight / 2f,
                        )
                        drawOval(
                            color = strokeColor,
                            topLeft = secondaryTopLeft,
                            size = Size(secondaryWidth, secondaryHeight),
                        )
                        val focuserWidth = bodyWidth * 0.10f
                        val focuserHeight = bodyHeight * 0.22f
                        val focuserTopLeft = Offset(
                            spiderCenter.x - bodyWidth * 0.14f,
                            bodyTopLeft.y - focuserHeight * 0.55f,
                        )
                        val focuserCorner = CornerRadius(focuserWidth * 0.25f, focuserWidth * 0.25f)
                        drawRoundRect(
                            color = strokeColor,
                            topLeft = focuserTopLeft,
                            size = Size(focuserWidth, focuserHeight),
                            cornerRadius = focuserCorner,
                        )
                    }
                }

                EquipmentCategoryIconUi.Mount -> {
                    val centerBase = Offset(size.width * 0.58f, size.height * 0.65f)
                    val headSize = strokeWidth * 2
                    val headTopLeft = Offset(centerBase.x - headSize / 2, centerBase.y - headSize)
                    drawRect(
                        color = strokeColor,
                        topLeft = headTopLeft,
                        size = Size(headSize, headSize),
                    )
                    val circleRadius = headSize/2
                    val circleCenter = Offset(
                        headTopLeft.x + circleRadius,
                        headTopLeft.y,
                    )
                    drawCircle(
                        color = strokeColor,
                        radius = circleRadius,
                        center = circleCenter,
                    )
                    val diamondCenter = Offset(
                        circleCenter.x - circleRadius,
                        circleCenter.y - circleRadius,
                    )
                    val telescopeLength = headSize * 5f
                    val telescopeWidth = strokeWidth * 1.5f
                    rotate(
                        degrees = 45f,
                        pivot = diamondCenter,
                    ) {
                        drawRect(
                            color = strokeColor,
                            topLeft = Offset(
                                diamondCenter.x - headSize / 2f,
                                diamondCenter.y - headSize / 2f,
                            ),
                            size = Size(headSize, headSize),
                        )
                        drawRect(
                            color = strokeColor,
                            topLeft = Offset(
                                diamondCenter.x - headSize / 4f,
                                diamondCenter.y - headSize,
                            ),
                            size = Size(headSize / 2f, headSize / 2f),
                        )
                        drawLine(
                            color = strokeColor,
                            start = Offset(
                                diamondCenter.x - telescopeLength / 2f,
                                diamondCenter.y - 3f * headSize / 2f + telescopeWidth / 2f),
                            end = Offset(
                                diamondCenter.x + telescopeLength / 2f,
                                diamondCenter.y - 3f * headSize / 2f + telescopeWidth / 2f),
                            strokeWidth = telescopeWidth,
                            cap = StrokeCap.Round,
                        )
                        drawLine(
                            color = strokeColor,
                            start = diamondCenter,
                            end = Offset(diamondCenter.x, diamondCenter.y + size.width * 0.3f),
                            strokeWidth = strokeWidth,
                        )
                        drawCircle(
                            color = strokeColor,
                            center = Offset(diamondCenter.x, diamondCenter.y + size.width * 0.2f),
                            radius = strokeWidth,
                        )
                    }
                    drawLine(
                        color = strokeColor,
                        start = Offset(centerBase.x - strokeWidth/2, centerBase.y),
                        end = Offset(centerBase.x - size.width * 0.14f, centerBase.y + size.width * 0.26f),
                        strokeWidth = strokeWidth,
                        cap = StrokeCap.Round,
                    )
                    drawLine(
                        color = strokeColor,
                        start = centerBase,
                        end = Offset(centerBase.x, centerBase.y + size.width * 0.28f),
                        strokeWidth = strokeWidth,
                        cap = StrokeCap.Round,
                    )
                    drawLine(
                        color = strokeColor,
                        start = Offset(centerBase.x + strokeWidth/2, centerBase.y),
                        end = Offset(centerBase.x + size.width * 0.14f, centerBase.y + size.width * 0.26f),
                        strokeWidth = strokeWidth,
                        cap = StrokeCap.Round,
                    )
                }
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

private fun equipmentPalette(type: EquipmentType): EquipmentCategoryPalette = when (type) {
    EquipmentType.Observatory -> EquipmentCategoryPalette(
        panelBrush = Brush.linearGradient(
            colors = listOf(
                Color(0x66204154),
                Color(0x44253E62),
            ),
        ),
        cardBrush = Brush.verticalGradient(
            colors = listOf(
                Color(0xAA173043),
                Color(0x88111F32),
            ),
        ),
        iconBrush = Brush.radialGradient(
            colors = listOf(
                Color(0xAA5BE7E0),
                Color(0x4444AFC6),
            ),
        ),
        accent = Color(0xFF63E0D7),
        accentText = Color(0xFFABF7F0),
        iconStroke = Color(0xFFE6FFFE),
        chipColor = Color(0x263AD0C6),
    )

    EquipmentType.Telescope -> EquipmentCategoryPalette(
        panelBrush = Brush.linearGradient(
            colors = listOf(
                Color(0x66533E22),
                Color(0x44483826),
            ),
        ),
        cardBrush = Brush.verticalGradient(
            colors = listOf(
                Color(0xAA3A2B16),
                Color(0x88261E12),
            ),
        ),
        iconBrush = Brush.radialGradient(
            colors = listOf(
                Color(0xAAF4D277),
                Color(0x44D29F42),
            ),
        ),
        accent = Color(0xFFF0CC6A),
        accentText = Color(0xFFFFE7A6),
        iconStroke = Color(0xFFFFF7E0),
        chipColor = Color(0x26F0CC6A),
    )

    EquipmentType.Mount -> EquipmentCategoryPalette(
        panelBrush = Brush.linearGradient(
            colors = listOf(
                Color(0x66553A34),
                Color(0x44473439),
            ),
        ),
        cardBrush = Brush.verticalGradient(
            colors = listOf(
                Color(0xAA38211F),
                Color(0x8825151A),
            ),
        ),
        iconBrush = Brush.radialGradient(
            colors = listOf(
                Color(0xAAFF9478),
                Color(0x44D86474),
            ),
        ),
        accent = Color(0xFFFF9B7A),
        accentText = Color(0xFFFFD2C2),
        iconStroke = Color(0xFFFFF1EB),
        chipColor = Color(0x26FF9B7A),
    )
}
