package fr.aumombelli.dstcg.feature.equipment

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import fr.aumombelli.dstcg.model.bonusLabel
import fr.aumombelli.dstcg.ui.screen.dstcgContentInsetsPadding

@Composable
fun EquipmentScreen(
    state: EquipmentUiState,
    onRefresh: () -> Unit,
    onActivateEquipment: (String) -> Unit,
    contentVisible: Boolean = true,
) {
    val contentAlpha by animateFloatAsState(
        targetValue = if (contentVisible) 1f else 0f,
        animationSpec = tween(durationMillis = 880, easing = FastOutSlowInEasing),
        label = "equipment-content-alpha",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = contentAlpha }
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0A1422),
                        Color(0xFF12243F),
                        Color(0xFF1B3051),
                    ),
                ),
            )
            .testTag("equipment-screen"),
    ) {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(16.dp),
            modifier = Modifier
                .fillMaxSize()
                .dstcgContentInsetsPadding(includeBottom = true),
        ) {
            item(key = "equipment-header") {
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
                        text = "Active une carte pour influencer les prochains packs. Un seul equipement par type peut rester actif a la fois.",
                        color = Color(0xFFD0E0F2),
                    )
                    if (state.errorMessage != null) {
                        Button(
                            onClick = onRefresh,
                            modifier = Modifier.testTag("equipment-refresh"),
                        ) {
                            Text("Reessayer")
                        }
                    }
                }
            }

            if (state.isLoading) {
                item(key = "equipment-loading") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
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
                Surface(
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(28.dp),
                    color = Color.Black.copy(alpha = 0.22f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("equipment-active-summary"),
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                    ) {
                        Text(
                            text = "Effets actifs",
                            color = Color.White,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                        )
                        if (state.activeEffects.isEmpty()) {
                            Text(
                                text = "Aucun equipement actif pour le moment.",
                                color = Color(0xFFD0E0F2),
                            )
                        } else {
                            state.activeEffects.forEach { item ->
                                Text(
                                    text = "${item.type.displayName} : ${item.displayName} · ${item.bonusLabel} · ${item.packsRemaining} packs restants",
                                    color = Color(0xFFF0D995),
                                    modifier = Modifier.testTag("equipment-active-${item.type.code}"),
                                )
                            }
                        }
                    }
                }
            }

            items(state.sections, key = { it.type.code }) { section ->
                EquipmentSectionCard(
                    section = section,
                    activatingCardId = state.activatingCardId,
                    onActivateEquipment = onActivateEquipment,
                )
            }
        }
    }
}

@Composable
private fun EquipmentSectionCard(
    section: EquipmentSectionUi,
    activatingCardId: String?,
    onActivateEquipment: (String) -> Unit,
) {
    Surface(
        shape = androidx.compose.foundation.shape.RoundedCornerShape(28.dp),
        color = Color.Black.copy(alpha = 0.22f),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("equipment-section-${section.type.code}"),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
        ) {
            Text(
                text = section.title,
                color = Color.White,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            section.lastActivatedLabel?.let { label ->
                Text(
                    text = "Dernier utilise : $label",
                    color = Color(0xFFF0D995),
                    modifier = Modifier.testTag("equipment-last-used-${section.type.code}"),
                )
            }
            section.cards.forEach { card ->
                EquipmentInventoryCardRow(
                    card = card,
                    isActivating = activatingCardId == card.definition.id,
                    onActivateEquipment = onActivateEquipment,
                )
            }
        }
    }
}

@Composable
private fun EquipmentInventoryCardRow(
    card: EquipmentInventoryCardUi,
    isActivating: Boolean,
    onActivateEquipment: (String) -> Unit,
) {
    Surface(
        shape = androidx.compose.foundation.shape.RoundedCornerShape(22.dp),
        color = Color(0x55111E30),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("equipment-card-${card.definition.id}"),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Text(
                text = card.definition.displayName,
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "Niveau ${card.definition.level} · ${card.definition.bonusLabel()}",
                color = Color(0xFF9EE7FF),
            )
            Text(
                text = "Stock x${card.stockCount}",
                color = Color(0xFFD7E8FF),
            )
            Text(
                text = "Activations totales : ${card.activationCount}",
                color = Color(0xFFD7E8FF),
            )
            if (card.isActive && card.packsRemaining != null) {
                Text(
                    text = "Actif · ${card.packsRemaining} packs restants",
                    color = Color(0xFFF0D995),
                )
            } else {
                Text(
                    text = card.definition.description,
                    color = Color(0xFFC7D6E8),
                )
            }
            Row(
                horizontalArrangement = Arrangement.End,
                modifier = Modifier.fillMaxWidth(),
            ) {
                OutlinedButton(
                    onClick = { onActivateEquipment(card.definition.id) },
                    enabled = card.activationEnabled && !isActivating,
                    modifier = Modifier.testTag("equipment-activate-${card.definition.id}"),
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
