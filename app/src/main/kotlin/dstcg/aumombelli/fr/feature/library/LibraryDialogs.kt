package fr.aumombelli.dstcg.feature.library

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import fr.aumombelli.dstcg.model.LibraryCardItem
import fr.aumombelli.dstcg.model.TradeCardCandidate
import fr.aumombelli.dstcg.model.canTradeAway
import fr.aumombelli.dstcg.model.toDisplayCard
import fr.aumombelli.dstcg.ui.component.AstroCardDetailsSurface
import fr.aumombelli.dstcg.ui.component.AstroCardFullscreenCloseButton
import fr.aumombelli.dstcg.ui.component.AstroCardPreviewSurface
import fr.aumombelli.dstcg.ui.component.AstroCardSurfaceMode
import fr.aumombelli.dstcg.ui.component.DisplayCardVariantSelector
import fr.aumombelli.dstcg.ui.component.SceneNavigationButton
import fr.aumombelli.dstcg.ui.component.SceneNavigationIcon
import fr.aumombelli.dstcg.ui.component.calculateTradingCardFitWidth
import fr.aumombelli.dstcg.ui.screen.dstcgContentInsetsPadding

@Composable
internal fun CardPreviewDialog(
    item: LibraryCardItem?,
    selectedVariantKey: String?,
    onDismiss: () -> Unit,
    onExpand: () -> Unit,
    onVariantSelected: (String) -> Unit,
    onTrade: ((TradeCardCandidate) -> Unit)? = null,
) {
    val libraryItem = item ?: return
    val displayCard = libraryItem.toDisplayCard(selectedVariantKey) ?: return
    val activeVariant = displayCard.activeVariant
    val tradeCandidate = if (activeVariant.canTradeAway()) {
        TradeCardCandidate(
            card = libraryItem.definition,
            extensionName = libraryItem.extensionName,
            variant = activeVariant,
        )
    } else {
        null
    }

    BackHandler(onBack = onDismiss)

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.32f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss,
            )
            .dstcgContentInsetsPadding(includeBottom = true)
            .padding(16.dp)
            .testTag("library-card-preview"),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                SceneNavigationButton(
                    icon = SceneNavigationIcon.Close,
                    onClick = onDismiss,
                    contentDescription = "Fermer",
                    testTag = "library-card-preview-close",
                )
            }
            BoxWithConstraints(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = true),
            ) {
                val cardWidth = calculateTradingCardFitWidth(
                    maxWidth = maxWidth,
                    maxHeight = maxHeight,
                )
                AstroCardPreviewSurface(
                    displayCard = displayCard,
                    mode = AstroCardSurfaceMode.Preview,
                    modifier = Modifier
                        .width(cardWidth)
                        .testTag("library-card-preview-surface"),
                    onClick = onExpand,
                )
            }
            DisplayCardVariantSelector(
                variants = displayCard.availableVariants,
                selectedVariantKey = displayCard.activeVariant.key,
                onVariantSelected = { variant -> onVariantSelected(variant.key) },
                modifier = Modifier.fillMaxWidth(),
            )
            if (tradeCandidate != null && onTrade != null) {
                Button(
                    onClick = { onTrade(tradeCandidate) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("library-card-trade"),
                ) {
                    Text("Échanger")
                }
            }
        }
    }
}

@Composable
internal fun FullscreenCardDialog(
    item: LibraryCardItem?,
    selectedVariantKey: String?,
    onDismiss: () -> Unit,
    onVariantSelected: (String) -> Unit,
) {
    val displayCard = item?.toDisplayCard(selectedVariantKey) ?: return

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
                .testTag("astro-card-fullscreen"),
        ) {
            AstroCardDetailsSurface(
                displayCard = displayCard,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 42.dp),
                accessoryContent = {
                    DisplayCardVariantSelector(
                        variants = displayCard.availableVariants,
                        selectedVariantKey = displayCard.activeVariant.key,
                        onVariantSelected = { variant -> onVariantSelected(variant.key) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                },
            )
            AstroCardFullscreenCloseButton(onClick = onDismiss)
        }
    }
}
