package fr.aumombelli.dstcg.feature.library

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import fr.aumombelli.dstcg.ui.component.ExtensionLogoMark
import fr.aumombelli.dstcg.ui.component.RarityStarBadge
import fr.aumombelli.dstcg.ui.theme.skyQualityPalette

@Composable
internal fun LibraryFilterPanel(
    options: LibraryFilterOptions,
    filters: LibraryFilters,
    onFiltersChanged: (LibraryFilters) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("library-filter-panel"),
    ) {
        LibraryFilterGroup(
            title = "Extension",
            options = options.extensions,
            selectedOptionId = filters.extensionId,
            enabled = enabled,
            testTagPrefix = "library-filter-extension",
            leadingIcon = { option ->
                ExtensionLogoMark(
                    extensionId = option.id,
                    compact = true,
                    emblemSize = 18.dp,
                    modifier = Modifier.testTag("library-filter-extension-logo-${option.id}"),
                )
            },
            onOptionSelected = { extensionId ->
                onFiltersChanged(
                    filters.copy(
                        extensionId = extensionId.toggleFrom(filters.extensionId),
                    ),
                )
            },
        )
        LibraryRarityFilterGroup(
            title = "Rareté",
            options = options.rarities,
            selectedOptionId = filters.rarityLabel,
            enabled = enabled,
            onOptionSelected = { rarityLabel ->
                onFiltersChanged(
                    filters.copy(
                        rarityLabel = rarityLabel.toggleFrom(filters.rarityLabel),
                    ),
                )
            },
        )
        LibrarySkyQualityFilterGroup(
            title = "Qualité du ciel",
            options = options.skyQualities,
            selectedOptionId = filters.skyQuality,
            enabled = enabled,
            onOptionSelected = { skyQuality ->
                onFiltersChanged(
                    filters.copy(
                        skyQuality = skyQuality.toggleFrom(filters.skyQuality),
                    ),
                )
            },
        )
        LibraryTradeableFilterChip(
            selected = filters.tradeableOnly,
            enabled = enabled,
            onClick = {
                onFiltersChanged(
                    filters.copy(tradeableOnly = !filters.tradeableOnly),
                )
            },
        )
    }
}

@Composable
private fun LibraryFilterGroup(
    title: String,
    options: List<LibraryFilterOption>,
    selectedOptionId: String?,
    enabled: Boolean,
    testTagPrefix: String,
    leadingIcon: @Composable ((LibraryFilterOption) -> Unit)? = null,
    onOptionSelected: (String) -> Unit,
) {
    if (options.isEmpty()) return

    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.testTag("$testTagPrefix-group"),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFFD0E0F2),
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.horizontalScroll(rememberScrollState()),
        ) {
            options.forEach { option ->
                val selected = option.id == selectedOptionId
                FilterChip(
                    selected = selected,
                    onClick = { onOptionSelected(option.id) },
                    enabled = enabled,
                    border = null,
                    colors = libraryFilterChipColors(),
                    label = {
                        Text(
                            text = option.label,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    leadingIcon = leadingIcon?.let { icon ->
                        { icon(option) }
                    },
                    modifier = Modifier
                        .filterChipBorder(selected = selected)
                        .testTag("$testTagPrefix-${option.id}"),
                )
            }
        }
    }
}

@Composable
private fun LibraryRarityFilterGroup(
    title: String,
    options: List<LibraryFilterOption>,
    selectedOptionId: String?,
    enabled: Boolean,
    onOptionSelected: (String) -> Unit,
) {
    if (options.isEmpty()) return

    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.testTag("library-filter-rarity-group"),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFFD0E0F2),
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            options.forEach { option ->
                val selected = option.id == selectedOptionId
                FilterChip(
                    selected = selected,
                    onClick = { onOptionSelected(option.id) },
                    enabled = enabled,
                    border = null,
                    colors = libraryFilterChipColors(),
                    label = {
                        Text(
                            text = option.label,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Clip,
                        )
                    },
                    leadingIcon = {
                        RarityStarBadge(
                            rarityLabel = option.id,
                            modifier = Modifier
                                .size(16.dp)
                                .testTag("library-filter-rarity-star-${option.id}"),
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .filterChipBorder(selected = selected)
                        .testTag("library-filter-rarity-${option.id}"),
                )
            }
        }
    }
}

@Composable
private fun LibrarySkyQualityFilterGroup(
    title: String,
    options: List<LibraryFilterOption>,
    selectedOptionId: String?,
    enabled: Boolean,
    onOptionSelected: (String) -> Unit,
) {
    if (options.isEmpty()) return

    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.testTag("library-filter-sky-group"),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFFD0E0F2),
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            options.forEach { option ->
                val selected = option.id == selectedOptionId
                val backgroundBrush = skyQualityChipBrush(option.id, selected)
                FilterChip(
                    selected = selected,
                    onClick = { onOptionSelected(option.id) },
                    enabled = enabled,
                    border = null,
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = Color.Transparent,
                        labelColor = Color.White,
                        selectedContainerColor = Color.Transparent,
                        selectedLabelColor = Color.White,
                        disabledContainerColor = Color.Transparent,
                        disabledLabelColor = Color.White.copy(alpha = 0.45f),
                    ),
                    label = {
                        Text(
                            text = option.label,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Clip,
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .filterChipBorder(
                            selected = selected,
                            inactiveColor = Color.Transparent,
                        )
                        .clip(CircleShape)
                        .background(backgroundBrush)
                        .testTag("library-filter-sky-${option.id}"),
                )
            }
        }
    }
}

@Composable
private fun LibraryTradeableFilterChip(
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.testTag("library-filter-availability-group"),
    ) {
        Text(
            text = "Disponibilité",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFFD0E0F2),
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip(
                selected = selected,
                onClick = onClick,
                enabled = enabled,
                border = null,
                colors = libraryFilterChipColors(),
                label = { Text("Échangeable") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.SwapHoriz,
                        contentDescription = null,
                    )
                },
                modifier = Modifier
                    .filterChipBorder(selected = selected)
                    .testTag("library-filter-tradeable"),
            )
        }
    }
}

private fun skyQualityChipBrush(
    skyQuality: String,
    selected: Boolean,
): Brush {
    if (skyQuality == "holographic") {
        return Brush.horizontalGradient(
            listOf(
                Color(0xFF4FEAFF).copy(alpha = if (selected) 1f else 0.88f),
                Color(0xFFFF5FD2).copy(alpha = if (selected) 1f else 0.88f),
                Color(0xFFFFD76A).copy(alpha = if (selected) 0.98f else 0.84f),
            ),
        )
    }
    val palette = skyQualityPalette(skyQuality)
    val alpha = if (selected) 1f else 0.9f
    return Brush.verticalGradient(
        listOf(
            palette.top.copy(alpha = alpha),
            palette.bottom.copy(alpha = alpha),
        ),
    )
}

@Composable
private fun libraryFilterChipColors() =
    FilterChipDefaults.filterChipColors(
        containerColor = Color.Transparent,
        labelColor = Color(0xFFD0E0F2),
        iconColor = Color(0xFFD0E0F2),
        selectedContainerColor = Color.Transparent,
        selectedLabelColor = Color.White,
        selectedLeadingIconColor = Color.White,
        disabledContainerColor = Color.Transparent,
        disabledLabelColor = Color.White.copy(alpha = 0.45f),
        disabledLeadingIconColor = Color.White.copy(alpha = 0.45f),
    )

private fun Modifier.filterChipBorder(
    selected: Boolean,
    inactiveColor: Color = Color.White.copy(alpha = 0.72f),
): Modifier =
    border(
        width = if (selected) 2.dp else 1.dp,
        color = if (selected) ActiveFilterBorderColor else inactiveColor,
        shape = CircleShape,
    )

private fun String.toggleFrom(currentValue: String?): String? =
    takeUnless { it == currentValue }

private val ActiveFilterBorderColor = Color(0xFF68F27D)
