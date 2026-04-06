package fr.aumombelli.dstcg.feature.home

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HomeAboutSheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!visible) return

    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = Color.Transparent,
        scrimColor = Color.Black.copy(alpha = 0.62f),
        sheetMaxWidth = Dp.Unspecified,
        modifier = modifier.testTag("home-about-sheet-container"),
        dragHandle = null,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(18.dp),
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF17314E),
                            Color(0xFF0C1828),
                        ),
                    ),
                )
                .testTag("home-about-sheet")
                .padding(horizontal = 24.dp, vertical = 20.dp),
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("home-about-sheet-header"),
            ) {
                Box(
                    modifier = Modifier
                        .width(44.dp)
                        .height(4.dp)
                        .background(
                            color = Color(0xFF7992AE),
                            shape = RoundedCornerShape(999.dp),
                        )
                        .testTag("home-about-sheet-handle"),
                )
                Text(
                    text = "Crédits",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
                Text(
                    text = HomeAboutAppVersion,
                    color = Color(0xFFF4D48A),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.testTag("home-about-sheet-version"),
                )
                Text(
                    text = "Fais glisser le panneau vers le bas ou touche le fond pour le fermer.",
                    color = Color(0xFF9FB4CD),
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                )
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
            ) {
                HomeAboutSections.forEach { section ->
                    Column(
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = section.title,
                            color = Color(0xFFB9D5F5),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        section.lines.forEach { line ->
                            Text(
                                text = line,
                                color = Color(0xFFE6EEF9),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun HomeResetConfirmationDialog(
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val confirmUnlockProgress = remember { androidx.compose.animation.core.Animatable(0f) }

    LaunchedEffect(Unit) {
        confirmUnlockProgress.snapTo(0f)
        confirmUnlockProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 2_000, easing = LinearEasing),
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("home-reset-confirmation"),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.68f))
                .testTag("home-reset-confirmation-scrim"),
        )

        Card(
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier
                .align(Alignment.Center)
                .padding(24.dp),
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
            ) {
                Text(
                    text = "Réinitialiser la bibliothèque ?",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "Cette action efface la progression locale et recrée une bibliothèque vide.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.testTag("home-reset-confirmation-message"),
                )
                Text(
                    text = "Le bouton de validation s'active après un court délai de sécurité.",
                    color = Color(0xFF6B778C),
                    style = MaterialTheme.typography.bodySmall,
                )
                Button(
                    onClick = onConfirm,
                    enabled = confirmUnlockProgress.value >= 0.999f,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("home-reset-confirmation-confirm"),
                ) {
                    Text("Valider")
                }
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("home-reset-confirmation-cancel"),
                ) {
                    Text("Annuler")
                }
            }
        }
    }
}
