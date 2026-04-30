package fr.aumombelli.dstcg.feature.equipment

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import fr.aumombelli.dstcg.model.bonusLabel
import fr.aumombelli.dstcg.ui.component.EquipmentArtBackground
import fr.aumombelli.dstcg.ui.component.EquipmentArtMode
import fr.aumombelli.dstcg.ui.component.TRADING_CARD_WIDTH_OVER_HEIGHT

@Composable
internal fun EquipmentSectionCard(
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
