package fr.aumombelli.dstcg.feature.trade

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fr.aumombelli.dstcg.data.TradeGateway
import fr.aumombelli.dstcg.model.DisplayCard
import fr.aumombelli.dstcg.model.toDisplayCard
import fr.aumombelli.dstcg.ui.component.AstroCardPreviewSurface
import fr.aumombelli.dstcg.ui.component.AstroCardSurfaceMode
import fr.aumombelli.dstcg.ui.screen.dstcgContentInsetsPadding
import kotlin.math.abs

@Composable
fun TradeScreen(
    viewModel: TradeViewModel,
    tradeGateway: TradeGateway,
    onDismiss: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val isSucceeded = state.phase == TradePhase.Succeeded
    val backgroundColors = if (isSucceeded) {
        listOf(Color(0xFF041116), Color(0xFF0A3440), Color(0xFF1B2C20))
    } else {
        listOf(Color(0xFF07111E), Color(0xFF102A3D))
    }

    NfcTradeSessionEffect(
        state = state,
        tradeGateway = tradeGateway,
        onEvent = viewModel::onNfcEvent,
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false,
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = backgroundColors,
                    ),
                )
                .dstcgContentInsetsPadding(includeBottom = true)
                .padding(18.dp)
                .testTag("trade-screen"),
        ) {
            if (isSucceeded) {
                TradeSuccessBackdrop(modifier = Modifier.fillMaxSize())
            }

            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .testTag("trade-close"),
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Fermer",
                    tint = Color.White,
                )
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 42.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Filled.SwapHoriz,
                        contentDescription = null,
                        tint = Color(0xFF8DEBFF),
                    )
                    Text(
                        text = "Échange NFC",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                }

                when {
                    state.isLoading -> LoadingTradeContent()
                    else -> TradeReadyContent(
                        state = state,
                        onRetry = viewModel::retryExchange,
                        onDismiss = onDismiss,
                    )
                }
            }
        }
    }
}

@Composable
private fun LoadingTradeContent() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 48.dp),
    ) {
        CircularProgressIndicator()
        Text("Préparation des cartes échangeables...", color = Color(0xFFD7E8F6))
    }
}

@Composable
private fun TradeReadyContent(
    state: TradeUiState,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
) {
    val candidate = checkNotNull(state.selectedCandidate)
    val selectedDisplayCard = candidate.card.toDisplayCard(
        extensionName = candidate.extensionName,
        activeVariant = candidate.variant,
        availableVariants = listOf(candidate.variant),
    )

    if (state.phase == TradePhase.Succeeded) {
        TradeSucceededContent(
            selectedDisplayCard = selectedDisplayCard,
            receivedDisplayCard = state.receivedDisplayCard,
            isResolvingReceivedCard = state.isResolvingReceivedCard,
            message = state.message,
            onDismiss = onDismiss,
        )
        return
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("trade-ready"),
    ) {
        Text("Carte proposée", color = Color(0xFF8DEBFF))
        AstroCardPreviewSurface(
            displayCard = selectedDisplayCard,
            mode = AstroCardSurfaceMode.Preview,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .fillMaxWidth(0.76f)
                .widthIn(max = 300.dp)
                .testTag("trade-selected-card"),
        )

        state.message?.let { message ->
            Text(
                text = message,
                color = if (state.phase == TradePhase.Failed) Color(0xFFFFB4AB) else Color(0xFFD7E8F6),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.testTag("trade-message"),
            )
        }

        if (state.phase == TradePhase.Ready || state.phase == TradePhase.Exchanging) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
        }

        Spacer(modifier = Modifier.height(6.dp))

        if (state.phase == TradePhase.Succeeded) {
            Button(
                onClick = onDismiss,
                modifier = Modifier.testTag("trade-done"),
            ) {
                Text("Terminer")
            }
        } else if (state.phase == TradePhase.Failed) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onRetry,
                    modifier = Modifier.testTag("trade-retry"),
                ) {
                    Text("Réessayer")
                }
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("trade-cancel"),
                ) {
                    Text("Fermer")
                }
            }
        }
    }
}

@Composable
private fun TradeSucceededContent(
    selectedDisplayCard: DisplayCard,
    receivedDisplayCard: DisplayCard?,
    isResolvingReceivedCard: Boolean,
    message: String?,
    onDismiss: () -> Unit,
) {
    var revealed by remember(
        receivedDisplayCard?.definition?.id,
        receivedDisplayCard?.activeVariant?.key,
    ) {
        mutableStateOf(false)
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("trade-success"),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = Color(0xFF8DF6E7),
                modifier = Modifier.size(30.dp),
            )
            Text(
                text = message ?: "Échange réussi !",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.testTag("trade-success-title"),
            )
        }

        Text(
            text = if (receivedDisplayCard == null && isResolvingReceivedCard) {
                "Ta carte reçue est ajoutée à ta bibliothèque."
            } else if (receivedDisplayCard == null) {
                "Ta carte reçue est dans ta bibliothèque. Elle sera visible en revenant à la collection."
            } else if (revealed) {
                "Ta nouvelle carte est maintenant dans ta bibliothèque."
            } else {
                "La connexion est validée. Révèle la carte reçue quand tu es prêt."
            },
            color = Color(0xFFD7F7F3),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.testTag("trade-success-copy"),
        )

        ExchangeCardRevealStage(
            selectedDisplayCard = selectedDisplayCard,
            receivedDisplayCard = receivedDisplayCard,
            revealed = revealed,
            onReveal = { revealed = true },
            modifier = Modifier.fillMaxWidth(),
        )

        if (receivedDisplayCard == null && isResolvingReceivedCard) {
            CircularProgressIndicator(
                color = Color(0xFF8DF6E7),
                modifier = Modifier.testTag("trade-received-loading"),
            )
        } else if (receivedDisplayCard == null) {
            Button(
                onClick = onDismiss,
                modifier = Modifier.testTag("trade-done"),
            ) {
                Text("Terminer")
            }
        } else if (!revealed) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Filled.KeyboardArrowUp,
                    contentDescription = null,
                    tint = Color(0xFFF7D985),
                )
                Text(
                    text = "Appuie ou glisse vers le haut.",
                    color = Color(0xFFF7D985),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.testTag("trade-reveal-hint"),
                )
            }
            Button(
                onClick = { revealed = true },
                modifier = Modifier.testTag("trade-reveal-received"),
            ) {
                Text("Révéler la carte reçue")
            }
        } else {
            Text(
                text = "Carte reçue",
                color = Color(0xFF8DF6E7),
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.testTag("trade-received-label"),
            )
            Button(
                onClick = onDismiss,
                modifier = Modifier.testTag("trade-done"),
            ) {
                Text("Terminer")
            }
        }
    }
}

@Composable
private fun ExchangeCardRevealStage(
    selectedDisplayCard: DisplayCard,
    receivedDisplayCard: DisplayCard?,
    revealed: Boolean,
    onReveal: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val outgoingLiftPx = with(density) { 92.dp.toPx() }
    val incomingDropPx = with(density) { 54.dp.toPx() }
    val progress = remember(
        selectedDisplayCard.definition.id,
        selectedDisplayCard.activeVariant.key,
        receivedDisplayCard?.definition?.id,
        receivedDisplayCard?.activeVariant?.key,
    ) {
        Animatable(0f)
    }
    LaunchedEffect(revealed, receivedDisplayCard) {
        if (revealed && receivedDisplayCard != null) {
            progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 820, easing = FastOutSlowInEasing),
            )
        } else {
            progress.snapTo(0f)
        }
    }

    val revealProgress = progress.value
    val incomingProgress = ((revealProgress - 0.34f) / 0.66f).coerceIn(0f, 1f)
    val beamAlpha = (1f - abs(revealProgress - 0.5f) * 2f).coerceIn(0f, 1f)
    val gestureCard = receivedDisplayCard
    val gestureModifier = if (gestureCard != null && !revealed) {
        Modifier.pointerInput(gestureCard.definition.id, gestureCard.activeVariant.key) {
            var totalDrag = 0f
            detectVerticalDragGestures(
                onDragStart = { totalDrag = 0f },
                onVerticalDrag = { _, dragAmount ->
                    totalDrag += dragAmount
                    if (totalDrag < -120f) {
                        onReveal()
                    }
                },
            )
        }
    } else {
        Modifier
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .height(420.dp)
            .then(gestureModifier)
            .testTag("trade-success-card-stage"),
    ) {
        AstroCardPreviewSurface(
            displayCard = selectedDisplayCard,
            mode = AstroCardSurfaceMode.Preview,
            modifier = Modifier
                .fillMaxWidth(0.76f)
                .widthIn(max = 300.dp)
                .graphicsLayer {
                    alpha = 1f - incomingProgress
                    translationY = -outgoingLiftPx * revealProgress
                    scaleX = 1f - 0.08f * revealProgress
                    scaleY = 1f - 0.08f * revealProgress
                    rotationZ = -5f * revealProgress
                }
                .testTag("trade-success-outgoing-card"),
        )

        if (beamAlpha > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.88f)
                    .height(10.dp)
                    .graphicsLayer {
                        alpha = beamAlpha * 0.72f
                        scaleX = 0.55f + beamAlpha * 0.65f
                        rotationZ = -7f
                    }
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                Color.Transparent,
                                Color(0xFF8DF6E7),
                                Color(0xFFF7D985),
                                Color.Transparent,
                            ),
                        ),
                    )
                    .testTag("trade-exchange-beam"),
            )
        }

        if (receivedDisplayCard != null) {
            AstroCardPreviewSurface(
                displayCard = receivedDisplayCard,
                mode = AstroCardSurfaceMode.Preview,
                modifier = Modifier
                    .fillMaxWidth(0.76f)
                    .widthIn(max = 300.dp)
                    .graphicsLayer {
                        alpha = incomingProgress
                        translationY = incomingDropPx * (1f - incomingProgress)
                        scaleX = 0.94f + 0.06f * incomingProgress
                        scaleY = 0.94f + 0.06f * incomingProgress
                        rotationZ = 4f * (1f - incomingProgress)
                    }
                    .testTag("trade-received-card"),
            )
        }
    }
}

@Composable
private fun TradeSuccessBackdrop(
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(260.dp)
                .graphicsLayer { alpha = 0.42f }
                .background(
                    Brush.radialGradient(
                        listOf(
                            Color(0xFF68E1D2),
                            Color.Transparent,
                        ),
                    ),
                ),
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(220.dp)
                .graphicsLayer { alpha = 0.22f }
                .background(
                    Brush.radialGradient(
                        listOf(
                            Color(0xFFF6B73C),
                            Color.Transparent,
                        ),
                    ),
                ),
        )
    }
}

@Composable
private fun NfcTradeSessionEffect(
    state: TradeUiState,
    tradeGateway: TradeGateway,
    onEvent: (NfcTradeControllerEvent) -> Unit,
) {
    val candidate = state.selectedCandidate
    val catalogFingerprint = state.catalogFingerprint
    val active = candidate != null &&
        catalogFingerprint != null &&
        (state.phase == TradePhase.Ready || state.phase == TradePhase.Exchanging)
    val context = LocalContext.current

    DisposableEffect(active, candidate, catalogFingerprint, tradeGateway) {
        if (!active) {
            return@DisposableEffect onDispose {}
        }
        val selectedCandidate = checkNotNull(candidate)
        val selectedCatalogFingerprint = checkNotNull(catalogFingerprint)
        val activity = context.findActivity()
        if (activity == null) {
            onEvent(NfcTradeControllerEvent.Failed("Écran Android indisponible pour le NFC."))
            return@DisposableEffect onDispose {}
        }
        val controller = NfcTradeController(
            activity = activity,
            tradeGateway = tradeGateway,
            localCard = selectedCandidate.toTradeCardRef(),
            catalogFingerprint = selectedCatalogFingerprint,
            onEvent = onEvent,
        )
        controller.start()
        onDispose {
            controller.stop()
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
