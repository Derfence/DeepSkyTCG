package fr.aumombelli.gatcha.feature.start

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@Composable
internal fun StartFooter(
    contentAlpha: Float,
    onOpenAbout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    var dragDistance by remember { mutableFloatStateOf(0f) }

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
                .width(136.dp)
                .height(72.dp)
                .pointerInput(onOpenAbout) {
                    detectVerticalDragGestures(
                        onDragStart = {
                            dragDistance = 0f
                        },
                        onVerticalDrag = { change, dragAmount ->
                            change.consume()
                            dragDistance += dragAmount
                        },
                        onDragCancel = {
                            dragDistance = 0f
                        },
                        onDragEnd = {
                            if (dragDistance < -40f) {
                                onOpenAbout()
                            }
                            dragDistance = 0f
                        },
                    )
                }
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onOpenAbout,
                )
                .padding(horizontal = 18.dp, vertical = 10.dp)
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

@Composable
internal fun StartAboutSheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing)) +
            slideInVertically(
                animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing),
                initialOffsetY = { it },
            ),
        exit = fadeOut(animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing)) +
            slideOutVertically(
                animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing),
                targetOffsetY = { it },
            ),
        modifier = modifier.fillMaxSize(),
    ) {
        val interactionSource = remember { MutableInteractionSource() }
        var dragDistance by remember { mutableFloatStateOf(0f) }
        var dismissEnabled by remember { mutableStateOf(false) }

        LaunchedEffect(Unit) {
            dismissEnabled = false
            delay(180)
            dismissEnabled = true
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .testTag("start-about-sheet-container"),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.62f))
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        enabled = dismissEnabled,
                        onClick = onDismiss,
                    )
                    .testTag("start-about-sheet-scrim"),
            )

            Surface(
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                color = Color(0xFF10233C),
                tonalElevation = 8.dp,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .pointerInput(onDismiss) {
                        detectVerticalDragGestures(
                            onDragStart = {
                                dragDistance = 0f
                            },
                            onVerticalDrag = { change, dragAmount ->
                                change.consume()
                                dragDistance += dragAmount
                            },
                            onDragCancel = {
                                dragDistance = 0f
                            },
                            onDragEnd = {
                                if (dragDistance > 100f) {
                                    onDismiss()
                                }
                                dragDistance = 0f
                            },
                        )
                    }
                    .testTag("start-about-sheet"),
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFF17314E),
                                    Color(0xFF0C1828),
                                ),
                            ),
                        )
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp, vertical = 20.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .width(44.dp)
                            .height(4.dp)
                            .background(
                                color = Color(0xFF7992AE),
                                shape = RoundedCornerShape(999.dp),
                            ),
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
