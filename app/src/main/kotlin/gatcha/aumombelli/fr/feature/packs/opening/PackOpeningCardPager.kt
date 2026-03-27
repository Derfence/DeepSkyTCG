package fr.aumombelli.gatcha.feature.packs.opening

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
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
    showPreviousArrow: Boolean,
    showNextArrow: Boolean,
    cardTranslationY: Float,
    nudgeActive: Boolean,
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
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) {
            if (showPreviousArrow) {
                NavigationHintArrow(
                    direction = NavigationHintDirection.Left,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .testTag("pack-opening-arrow-left"),
                )
            }
            if (showNextArrow) {
                NavigationHintArrow(
                    direction = NavigationHintDirection.Right,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .testTag("pack-opening-arrow-right"),
                )
            }
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth()
                    .padding(horizontal = 28.dp)
                    .graphicsLayer {
                        translationY = cardTranslationY
                    },
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
            if (nudgeActive) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .testTag("pack-opening-last-card-nudge"),
                )
            }
        }
    }
}

private enum class NavigationHintDirection {
    Left,
    Right,
}

@Composable
private fun NavigationHintArrow(
    direction: NavigationHintDirection,
    modifier: Modifier = Modifier,
) {
    Icon(
        imageVector = when (direction) {
            NavigationHintDirection.Left -> Icons.AutoMirrored.Filled.KeyboardArrowLeft
            NavigationHintDirection.Right -> Icons.AutoMirrored.Filled.KeyboardArrowRight
        },
        contentDescription = null,
        tint = Color.White.copy(alpha = 0.74f),
        modifier = modifier,
    )
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
            AstroCardDetailsSurface(
                displayCard = displayCard,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 42.dp),
            )
        }
    }
}
