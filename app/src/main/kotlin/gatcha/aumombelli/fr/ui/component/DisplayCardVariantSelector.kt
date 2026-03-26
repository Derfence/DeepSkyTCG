package fr.aumombelli.gatcha.ui.component

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import fr.aumombelli.gatcha.model.DisplayCardVariant

@Composable
fun DisplayCardVariantSelector(
    variants: List<DisplayCardVariant>,
    selectedVariantKey: String,
    onVariantSelected: (DisplayCardVariant) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (variants.size <= 1) return

    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.horizontalScroll(rememberScrollState()),
    ) {
        variants.forEach { variant ->
            FilterChip(
                selected = variant.key == selectedVariantKey,
                onClick = { onVariantSelected(variant) },
                label = { Text(variant.selectorLabel) },
                modifier = Modifier.testTag("astro-card-variant-${variant.skyQuality}-${variant.finish}"),
            )
        }
    }
}
