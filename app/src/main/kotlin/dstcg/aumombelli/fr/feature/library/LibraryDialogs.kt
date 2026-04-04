package fr.aumombelli.dstcg.feature.library

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import fr.aumombelli.dstcg.model.LibraryCardItem
import fr.aumombelli.dstcg.model.toDisplayCard
import fr.aumombelli.dstcg.ui.component.AstroCardDetailsSurface
import fr.aumombelli.dstcg.ui.component.AstroCardFullscreenCloseButton
import fr.aumombelli.dstcg.ui.component.AstroCardPreviewSurface
import fr.aumombelli.dstcg.ui.component.AstroCardSurfaceMode
import fr.aumombelli.dstcg.ui.component.DisplayCardVariantSelector
import fr.aumombelli.dstcg.ui.screen.dstcgContentInsetsPadding

@Composable
internal fun CardPreviewDialog(
    item: LibraryCardItem?,
    selectedVariantKey: String?,
    onDismiss: () -> Unit,
    onExpand: () -> Unit,
    onVariantSelected: (String) -> Unit,
) {
    val displayCard = item?.toDisplayCard(selectedVariantKey) ?: return

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnClickOutside = true),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier
                .padding(16.dp)
                .testTag("library-card-preview"),
        ) {
            AstroCardPreviewSurface(
                displayCard = displayCard,
                mode = AstroCardSurfaceMode.Preview,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("library-card-preview-surface"),
                onClick = onExpand,
            )
            DisplayCardVariantSelector(
                variants = displayCard.availableVariants,
                selectedVariantKey = displayCard.activeVariant.key,
                onVariantSelected = { variant -> onVariantSelected(variant.key) },
                modifier = Modifier.fillMaxWidth(),
            )
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
