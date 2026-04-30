package fr.aumombelli.dstcg.feature.crafting

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import fr.aumombelli.dstcg.app.NewPlayerOnboardingTarget
import fr.aumombelli.dstcg.model.LibraryCardItem
import fr.aumombelli.dstcg.ui.component.AstroCardThumbnail

@Composable
internal fun CraftingCandidateLibrary(
    state: CraftingUiState,
    onRefresh: () -> Unit,
    onBackToModes: () -> Unit,
    onOpenGroup: (CraftingCardGroup) -> Unit,
    onCoachmarkTargetBoundsChanged: (NewPlayerOnboardingTarget, Rect?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val firstCoachmarkCardId = state.sections.firstOrNull()?.cards?.firstOrNull()?.cardId
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 150.dp),
        modifier = modifier.testTag("crafting-candidate-grid"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    IconButton(
                        onClick = onBackToModes,
                        modifier = Modifier.testTag("crafting-back-to-modes"),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Retour",
                            tint = Color.White,
                        )
                    }
                    Column {
                        Text(
                            text = state.selectedMode.title(),
                            color = Color.White,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = state.selectedMode.subtitle(),
                            color = Color(0xFFD3E3F3),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
                state.successMessage?.let { message ->
                    Text(
                        text = message,
                        color = Color(0xFF9EE7FF),
                        modifier = Modifier.testTag("crafting-success-message"),
                    )
                }
                state.errorMessage?.let { message ->
                    Text(
                        text = message,
                        color = Color(0xFFFFB5B5),
                        modifier = Modifier.testTag("crafting-error-message"),
                    )
                    Button(
                        onClick = onRefresh,
                        modifier = Modifier.testTag("crafting-refresh"),
                    ) {
                        Text("Reessayer")
                    }
                }
            }
        }

        if (state.isLoading) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .padding(24.dp)
                        .testTag("crafting-loading"),
                )
            }
        }

        if (!state.isLoading && state.errorMessage == null && state.sections.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    text = "Aucune carte eligible pour cet atelier.",
                    color = Color(0xFFD3E3F3),
                    modifier = Modifier
                        .padding(vertical = 24.dp)
                        .testTag("crafting-empty"),
                )
            }
        }

        state.sections.forEach { section ->
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    text = section.extensionName,
                    color = Color(0xFF9EE7FF),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .padding(top = 8.dp, bottom = 4.dp)
                        .testTag("crafting-section-${section.extensionId}"),
                )
            }
            items(section.cards, key = { it.cardId }) { group ->
                AstroCardThumbnail(
                    item = group.toLibraryItem(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .onGloballyPositioned { coordinates ->
                            if (group.cardId == firstCoachmarkCardId) {
                                onCoachmarkTargetBoundsChanged(
                                    NewPlayerOnboardingTarget.CraftingCandidate,
                                    coordinates.boundsInRoot(),
                                )
                            }
                        },
                    onClick = { onOpenGroup(group) },
                )
            }
        }
    }
}

private fun CraftingCardGroup.toLibraryItem(): LibraryCardItem =
    LibraryCardItem(
        definition = firstCandidate.card,
        extensionName = firstCandidate.extensionName,
        ownedCount = availableVariants.sumOf { it.count },
        availableVariants = availableVariants,
    )
