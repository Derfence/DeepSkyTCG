package fr.aumombelli.gatcha.feature.start

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
internal fun StartFooter(
    contentAlpha: Float,
    onOpenAbout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    var dragDistance by remember { mutableFloatStateOf(0f) }
    var dragOpened by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(
                WindowInsets.safeDrawing.only(
                    WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom,
                ),
            )
            .graphicsLayer {
                alpha = contentAlpha
                translationY = (1f - contentAlpha) * 32f
            }
            .padding(horizontal = 24.dp, vertical = 18.dp)
            .testTag("start-footer"),
    ) {
        Text(
            text = StartFooterAppVersion,
            color = Color(0xFFD5E4F7),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .testTag("start-footer-version"),
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .width(220.dp)
                .height(96.dp)
                .draggable(
                    orientation = Orientation.Vertical,
                    state = rememberDraggableState { delta ->
                        if (dragOpened) {
                            return@rememberDraggableState
                        }
                        dragDistance += delta
                        if (dragDistance <= -24f) {
                            dragOpened = true
                            onOpenAbout()
                        }
                    },
                    onDragStarted = {
                        dragDistance = 0f
                        dragOpened = false
                    },
                    onDragStopped = {
                        dragDistance = 0f
                        dragOpened = false
                    },
                )
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onOpenAbout,
                )
                .padding(horizontal = 18.dp, vertical = 12.dp)
                .testTag("start-about-trigger"),
        ) {
            Text(
                text = "à propos",
                color = Color.White,
                style = MaterialTheme.typography.labelLarge,
                textAlign = TextAlign.Center,
            )
            Icon(
                imageVector = Icons.Rounded.KeyboardArrowDown,
                contentDescription = null,
                tint = Color(0xFFD5E4F7),
                modifier = Modifier.testTag("start-about-arrow"),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun StartAboutSheet(
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
        modifier = modifier.testTag("start-about-sheet-container"),
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
                .testTag("start-about-sheet")
                .padding(horizontal = 24.dp, vertical = 20.dp),
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("start-about-sheet-header"),
            ) {
                Box(
                    modifier = Modifier
                        .width(44.dp)
                        .height(4.dp)
                        .background(
                            color = Color(0xFF7992AE),
                            shape = RoundedCornerShape(999.dp),
                        )
                        .testTag("start-about-sheet-handle"),
                )
                Text(
                    text = "Crédits",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
                Text(
                    text = StartFooterAppVersion,
                    color = Color(0xFFF4D48A),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.testTag("start-about-sheet-version"),
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
                StartAboutSections.forEach { section ->
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
internal fun StartResetConfirmationDialog(
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
            .testTag("start-reset-confirmation"),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.68f))
                .testTag("start-reset-confirmation-scrim"),
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
                    modifier = Modifier.testTag("start-reset-confirmation-message"),
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
                        .testTag("start-reset-confirmation-confirm"),
                ) {
                    Text("Valider")
                }
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("start-reset-confirmation-cancel"),
                ) {
                    Text("Annuler")
                }
            }
        }
    }
}
