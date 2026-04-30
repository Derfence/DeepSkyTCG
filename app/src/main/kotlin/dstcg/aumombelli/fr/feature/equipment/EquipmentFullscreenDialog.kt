package fr.aumombelli.dstcg.feature.equipment

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import fr.aumombelli.dstcg.model.bonusLabel
import fr.aumombelli.dstcg.ui.component.EquipmentArtBackground
import fr.aumombelli.dstcg.ui.component.EquipmentArtMode
import fr.aumombelli.dstcg.ui.component.TRADING_CARD_WIDTH_OVER_HEIGHT
import fr.aumombelli.dstcg.ui.screen.dstcgContentInsetsPadding

@Composable
internal fun EquipmentCardFullscreenDialog(
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
