package fr.aumombelli.gatcha.feature.packs.opening

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.unit.dp
import fr.aumombelli.gatcha.model.DisplayCard
import fr.aumombelli.gatcha.ui.component.AstroCardDetailsSurface
import fr.aumombelli.gatcha.ui.component.AstroCardPreviewSurface
import fr.aumombelli.gatcha.ui.component.AstroCardSurfaceMode
import fr.aumombelli.gatcha.ui.screen.gatchaContentInsetsPadding

@Composable
internal fun RevealCard(
    displayCard: DisplayCard,
    page: Int,
    total: Int,
    onOpenFullscreen: () -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 18.dp, horizontal = 12.dp),
    ) {
        Text(
            text = "$page / $total",
            color = Color.White.copy(alpha = 0.9f),
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.testTag("pack-opening-progress"),
        )
        Text(
            text = displayCard.definition.id,
            color = Color(0xFFEAF4FF),
            modifier = Modifier.testTag("pack-opening-card-id"),
        )
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.weight(1f),
        ) {
            AstroCardPreviewSurface(
                displayCard = displayCard,
                mode = AstroCardSurfaceMode.PackReveal,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("pack-opening-card-surface"),
                onClick = onOpenFullscreen,
            )
        }
    }
}

@Composable
internal fun PackOpeningFullscreenDialog(
    displayCard: DisplayCard,
    onDismiss: () -> Unit,
) {
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
            )
        }
    }
}
