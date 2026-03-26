package fr.aumombelli.gatcha.feature.library

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import fr.aumombelli.gatcha.model.LibraryCardItem
import fr.aumombelli.gatcha.model.toDisplayCard
import fr.aumombelli.gatcha.ui.component.AstroCardDetailsSurface
import fr.aumombelli.gatcha.ui.component.AstroCardPreviewSurface
import fr.aumombelli.gatcha.ui.component.AstroCardSurfaceMode
import fr.aumombelli.gatcha.ui.component.DisplayCardVariantSelector
import fr.aumombelli.gatcha.ui.screen.gatchaContentInsetsPadding

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
        Surface(
            color = Color(0xFF07101A),
            shape = RoundedCornerShape(30.dp),
            modifier = Modifier
                .fillMaxWidth()
                .gatchaContentInsetsPadding(includeBottom = true)
                .padding(12.dp)
                .testTag("library-card-preview"),
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.padding(16.dp),
            ) {
                androidx.compose.foundation.layout.Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = "Apercu",
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("library-card-preview-close"),
                    ) {
                        Text("Fermer")
                    }
                }
                AstroCardPreviewSurface(
                    displayCard = displayCard,
                    mode = AstroCardSurfaceMode.Preview,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("library-card-preview-surface"),
                    onClick = onExpand,
                    accessoryContent = {
                        DisplayCardVariantSelector(
                            variants = displayCard.availableVariants,
                            selectedVariantKey = displayCard.activeVariant.key,
                            onVariantSelected = { variant -> onVariantSelected(variant.key) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    },
                )
                Text(
                    text = "Touchez la carte pour l'agrandir.",
                    color = Color(0xFFD0E0F2),
                    style = MaterialTheme.typography.bodyMedium,
                )
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
                .gatchaContentInsetsPadding(includeBottom = true)
                .padding(14.dp)
                .testTag("astro-card-fullscreen"),
        ) {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .testTag("astro-card-fullscreen-close"),
            ) {
                Text("Fermer")
            }
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
        }
    }
}
