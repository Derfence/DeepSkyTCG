package fr.aumombelli.dstcg.feature.equipment

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import fr.aumombelli.dstcg.ui.component.SceneNavigationButton
import fr.aumombelli.dstcg.ui.component.SceneNavigationIcon

@Composable
internal fun EquipmentHeader(
    errorMessage: String?,
    onRefresh: () -> Unit,
    onBack: (() -> Unit)? = null,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            onBack?.let { back ->
                SceneNavigationButton(
                    icon = SceneNavigationIcon.Back,
                    onClick = back,
                    contentDescription = "Retour",
                    testTag = "equipment-back",
                )
            }
            Text(
                text = "Equipements",
                color = Color.White,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
        }
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
internal fun EquipmentActiveSummaryCard(
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
