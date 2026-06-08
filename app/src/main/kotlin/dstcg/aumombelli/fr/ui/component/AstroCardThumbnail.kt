package fr.aumombelli.dstcg.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import fr.aumombelli.dstcg.model.LibraryCardItem
import fr.aumombelli.dstcg.model.toDisplayCard

@Composable
fun AstroCardThumbnail(
    item: LibraryCardItem,
    modifier: Modifier = Modifier,
    selectedVariantKey: String? = null,
    onClick: () -> Unit,
) {
    val owned = item.ownedCount > 0
    val displayCard = remember(item, selectedVariantKey) {
        item.toDisplayCard(selectedVariantKey) ?: fallbackDisplayCard(item)
    }
    val artVisibility = if (owned) CardArtVisibility.Visible else CardArtVisibility.Hidden

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.testTag("library-card-${item.definition.id}"),
    ) {
        Box(
            modifier = Modifier.alpha(if (owned) 1f else 0.42f),
        ) {
            AstroCardPreviewSurface(
                displayCard = displayCard,
                mode = AstroCardSurfaceMode.Thumbnail,
                artVisibility = artVisibility,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("library-card-surface-${item.definition.id}"),
                onClick = if (owned) onClick else null,
            )
            if (item.showNewIndicator) {
                NewContentIndicator(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .absoluteOffset(x = (-14).dp, y = (-14).dp)
                        .testTag("library-card-new-indicator-${item.definition.id}"),
                    iconSize = 20.dp,
                )
            }
            QuantityPill(
                text = if (owned) "×${item.ownedCount}" else "0",
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(10.dp),
            )
        }
        androidx.compose.material3.Text(
            text = if (owned) "En collection : ${item.ownedCount}" else "Pas encore obtenue",
            color = Color(0xFFD3E3F4),
            modifier = Modifier.testTag("library-owned-${item.definition.id}"),
        )
    }
}
