package fr.aumombelli.dstcg.feature.trade

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fr.aumombelli.dstcg.data.TradeGateway
import fr.aumombelli.dstcg.model.DisplayCard
import fr.aumombelli.dstcg.model.toDisplayCard
import fr.aumombelli.dstcg.ui.component.AstroCardPreviewSurface
import fr.aumombelli.dstcg.ui.component.AstroCardSurfaceMode
import fr.aumombelli.dstcg.ui.component.calculateTradingCardFitWidth
import fr.aumombelli.dstcg.ui.screen.dstcgContentInsetsPadding
import kotlin.math.abs
import kotlinx.coroutines.delay

private val MinimumTradeCardPreviewWidth = 150.dp
private val TradeSuccessBottomSafetyPadding = 80.dp
private val TradeRevealStageTopPadding = 8.dp
private val TradeRevealStageBottomPadding = 8.dp
private val TradeRevealMinOutgoingLift = 28.dp
private val TradeRevealMaxOutgoingLift = 64.dp
private val TradeRevealMinIncomingDrop = 16.dp
private val TradeRevealMaxIncomingDrop = 36.dp
private val TradeRevealMaxCardWidth = 220.dp

private data class TradeRevealMotion(
    val outgoingLift: Dp,
    val incomingDrop: Dp,
)

private fun calculateTradeCardPreviewWidth(
    availableWidth: Dp,
    availableHeight: Dp,
    widthFraction: Float = 0.54f,
    heightFraction: Float = 0.42f,
    maxWidth: Dp = 240.dp,
): Dp {
    val widthLimit = minOf(availableWidth * widthFraction, maxWidth).coerceAtLeast(0.dp)
    val heightLimit = (availableHeight * heightFraction).coerceAtLeast(0.dp)
    val fittedWidth = calculateTradingCardFitWidth(
        maxWidth = widthLimit,
        maxHeight = heightLimit,
    )
    return fittedWidth.coerceAtLeast(minOf(MinimumTradeCardPreviewWidth, widthLimit))
}

private fun calculateTradeRevealMotion(cardWidth: Dp): TradeRevealMotion {
    if (cardWidth <= 0.dp) {
        return TradeRevealMotion(outgoingLift = 0.dp, incomingDrop = 0.dp)
    }
    return TradeRevealMotion(
        outgoingLift = minOf(
            TradeRevealMaxOutgoingLift,
            maxOf(TradeRevealMinOutgoingLift, cardWidth * 0.24f),
        ),
        incomingDrop = minOf(
            TradeRevealMaxIncomingDrop,
            maxOf(TradeRevealMinIncomingDrop, cardWidth * 0.14f),
        ),
    )
}

private fun calculateTradeRevealCardWidth(
    availableWidth: Dp,
    availableHeight: Dp,
): Dp {
    val widthLimit = minOf(availableWidth * 0.58f, TradeRevealMaxCardWidth).coerceAtLeast(0.dp)
    if (widthLimit <= 0.dp || availableHeight <= 0.dp) {
        return 0.dp
    }

    var cardWidth = widthLimit
    repeat(3) {
        val motion = calculateTradeRevealMotion(cardWidth)
        val cardHeightLimit = (
            availableHeight -
                motion.outgoingLift -
                motion.incomingDrop -
                TradeRevealStageTopPadding -
                TradeRevealStageBottomPadding
            ).coerceAtLeast(0.dp)
        cardWidth = calculateTradingCardFitWidth(
            maxWidth = widthLimit,
            maxHeight = cardHeightLimit,
        )
    }
    return cardWidth
}

@Composable
fun TradeScreen(
    viewModel: TradeViewModel,
    tradeGateway: TradeGateway,
    onDismiss: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val tradeScrollState = rememberScrollState()
    val density = LocalDensity.current
    val autoScrollThresholdPx = with(density) { 96.dp.toPx().toInt() }
    var prerequisiteRefresh by remember { mutableIntStateOf(0) }
    var previousDiscoveryPartnerCount by remember { mutableIntStateOf(0) }
    var previousDiscoveryScrollMax by remember { mutableIntStateOf(0) }
    val missingPermissions = remember(context, prerequisiteRefresh) {
        context.missingBluetoothTradePermissions()
    }
    val bluetoothEnabled = remember(context, prerequisiteRefresh, missingPermissions) {
        missingPermissions.isEmpty() && context.isBluetoothEnabledForTrade()
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) {
        prerequisiteRefresh += 1
    }
    val enableBluetoothLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) {
        prerequisiteRefresh += 1
    }
    val transportActive = !state.isLoading &&
        state.catalogFingerprint != null &&
        state.localName.isNotBlank() &&
        missingPermissions.isEmpty() &&
        bluetoothEnabled &&
        state.phase in ActiveBluetoothTradePhases
    val isSucceeded = state.phase == TradePhase.Succeeded
    val backgroundColors = if (isSucceeded) {
        listOf(Color(0xFF041116), Color(0xFF0A3440), Color(0xFF1B2C20))
    } else {
        listOf(Color(0xFF07111E), Color(0xFF102A3D))
    }

    BluetoothTradeSessionEffect(
        active = transportActive,
        state = state,
        tradeGateway = tradeGateway,
        onEvent = viewModel::onBluetoothEvent,
    )

    LaunchedEffect(state.phase, state.discoveredPartners.size) {
        val currentPartnerCount = if (state.phase == TradePhase.Discovering) {
            state.discoveredPartners.size
        } else {
            0
        }
        val partnerCountIncreased = currentPartnerCount > previousDiscoveryPartnerCount
        val wasNearBottom = tradeScrollState.value >=
            (previousDiscoveryScrollMax - autoScrollThresholdPx).coerceAtLeast(0)

        if (partnerCountIncreased && currentPartnerCount > 0 && wasNearBottom) {
            withFrameNanos { }
            tradeScrollState.animateScrollTo(tradeScrollState.maxValue)
        }

        previousDiscoveryPartnerCount = currentPartnerCount
        previousDiscoveryScrollMax = tradeScrollState.maxValue
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false,
        ),
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(colors = backgroundColors))
                .dstcgContentInsetsPadding(includeBottom = true)
                .padding(18.dp)
                .testTag("trade-screen"),
        ) {
            val proposedCardWidth = calculateTradeCardPreviewWidth(
                availableWidth = maxWidth,
                availableHeight = maxHeight,
            )

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
                    .let { baseModifier ->
                        if (isSucceeded) {
                            baseModifier
                        } else {
                            baseModifier.verticalScroll(tradeScrollState)
                        }
                    },
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Bluetooth,
                        contentDescription = null,
                        tint = Color(0xFF8DEBFF),
                    )
                    Text(
                        text = "Échange Bluetooth",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                }

                when {
                    state.isLoading -> LoadingTradeContent()
                    missingPermissions.isNotEmpty() -> BluetoothPermissionContent(
                        onRequestPermissions = {
                            permissionLauncher.launch(missingPermissions.toTypedArray())
                        },
                    )
                    !bluetoothEnabled -> BluetoothDisabledContent(
                        onEnableBluetooth = {
                            enableBluetoothLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
                        },
                    )
                    else -> TradeReadyContent(
                        state = state,
                        onLocalNameEdited = viewModel::onLocalNameEdited,
                        onSaveLocalName = viewModel::saveLocalName,
                        onSelectPartner = viewModel::selectPartner,
                        onConfirm = viewModel::confirmExchange,
                        onRetry = viewModel::retryExchange,
                        onDismiss = onDismiss,
                        proposedCardWidth = proposedCardWidth,
                        modifier = if (isSucceeded) {
                            Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        } else {
                            Modifier.fillMaxWidth()
                        },
                    )
                }

                if (!isSucceeded) {
                    Spacer(
                        modifier = Modifier
                            .height(96.dp)
                            .testTag("trade-scroll-bottom-spacer"),
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
private fun BluetoothPermissionContent(
    onRequestPermissions: () -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.Start,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("trade-bluetooth-permissions"),
    ) {
        Text(
            text = "Bluetooth doit être autorisé pour découvrir un partenaire proche.",
            color = Color(0xFFD7E8F6),
            style = MaterialTheme.typography.titleMedium,
        )
        Button(
            onClick = onRequestPermissions,
            modifier = Modifier.testTag("trade-request-bluetooth-permissions"),
        ) {
            Text("Autoriser Bluetooth")
        }
    }
}

@Composable
private fun BluetoothDisabledContent(
    onEnableBluetooth: () -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.Start,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("trade-bluetooth-disabled"),
    ) {
        Text(
            text = "Active le Bluetooth pour lancer la découverte.",
            color = Color(0xFFD7E8F6),
            style = MaterialTheme.typography.titleMedium,
        )
        Button(
            onClick = onEnableBluetooth,
            modifier = Modifier.testTag("trade-enable-bluetooth"),
        ) {
            Text("Activer Bluetooth")
        }
    }
}

@Composable
private fun TradeReadyContent(
    state: TradeUiState,
    onLocalNameEdited: (String) -> Unit,
    onSaveLocalName: () -> Unit,
    onSelectPartner: (BluetoothTradePartner) -> Unit,
    onConfirm: () -> Unit,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
    proposedCardWidth: Dp,
    modifier: Modifier = Modifier,
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
            modifier = modifier,
        )
        return
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("trade-ready"),
    ) {
        LocalNameEditor(
            state = state,
            onLocalNameEdited = onLocalNameEdited,
            onSaveLocalName = onSaveLocalName,
        )

        Text("Carte proposée", color = Color(0xFF8DEBFF))
        AstroCardPreviewSurface(
            displayCard = selectedDisplayCard,
            mode = AstroCardSurfaceMode.Preview,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .width(proposedCardWidth)
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

        when (state.phase) {
            TradePhase.Discovering -> PartnerDiscoveryContent(
                partners = state.discoveredPartners,
                onSelectPartner = onSelectPartner,
            )

            TradePhase.Connecting -> ProgressContent("Connexion Bluetooth...")

            TradePhase.Confirming -> ConfirmationContent(
                state = state,
                onConfirm = onConfirm,
                cardWidth = proposedCardWidth,
            )

            TradePhase.Exchanging -> ProgressContent("Échange en cours...")

            TradePhase.Failed -> FailedTradeActions(
                onRetry = onRetry,
                onDismiss = onDismiss,
            )

            TradePhase.Preparing,
            TradePhase.Succeeded -> Unit
        }
    }
}

@Composable
private fun LocalNameEditor(
    state: TradeUiState,
    onLocalNameEdited: (String) -> Unit,
    onSaveLocalName: () -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        OutlinedTextField(
            value = state.editableLocalName,
            onValueChange = onLocalNameEdited,
            label = { Text("Nom visible") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("trade-local-name"),
        )
        if (state.editableLocalName != state.localName) {
            OutlinedButton(
                onClick = onSaveLocalName,
                modifier = Modifier.testTag("trade-save-local-name"),
            ) {
                Text("Enregistrer")
            }
        }
    }
}

@Composable
private fun PartnerDiscoveryContent(
    partners: List<BluetoothTradePartner>,
    onSelectPartner: (BluetoothTradePartner) -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("trade-partner-discovery"),
    ) {
        if (partners.isEmpty()) {
            ProgressContent("Recherche de partenaires...")
            return
        }

        Text("Partenaires proches", color = Color(0xFF8DEBFF), fontWeight = FontWeight.SemiBold)
        partners.forEach { partner ->
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("trade-partner-${partner.id}"),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        ResizablePartnerName(
                            text = partner.displayName,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Text(
                            text = if (partner.isCompatible == false) {
                                "Catalogue différent"
                            } else {
                                "Disponible"
                            },
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    Button(
                        enabled = partner.isCompatible != false,
                        onClick = { onSelectPartner(partner) },
                    ) {
                        Text("Choisir")
                    }
                }
            }
        }
    }
}

@Composable
private fun ResizablePartnerName(
    text: String,
    modifier: Modifier = Modifier,
    maxFontSize: TextUnit = 16.sp,
) {
    val minFontSize = 11.sp
    var fontSize by remember(text, maxFontSize) { mutableStateOf(maxFontSize) }

    Text(
        text = text,
        fontWeight = FontWeight.SemiBold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        style = MaterialTheme.typography.bodyLarge.copy(
            fontSize = fontSize,
            lineHeight = (fontSize.value * 1.18f).sp,
        ),
        onTextLayout = { result ->
            if (result.hasVisualOverflow && fontSize.value > minFontSize.value) {
                fontSize = (fontSize.value - 1f).coerceAtLeast(minFontSize.value).sp
            }
        },
        modifier = modifier,
    )
}

@Composable
private fun ConfirmationContent(
    state: TradeUiState,
    onConfirm: () -> Unit,
    cardWidth: Dp,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("trade-confirmation"),
    ) {
        Text(
            text = "Code ${state.verificationCode ?: "----"}",
            color = Color(0xFFF7D985),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.testTag("trade-verification-code"),
        )
        if (state.isResolvingRemoteCard) {
            CircularProgressIndicator(modifier = Modifier.testTag("trade-remote-card-loading"))
        } else if (state.remoteDisplayCard != null) {
            Text(
                text = "Carte proposée par ${state.remotePartnerName ?: "le partenaire"}",
                color = Color(0xFF8DEBFF),
                fontWeight = FontWeight.SemiBold,
            )
            AstroCardPreviewSurface(
                displayCard = state.remoteDisplayCard,
                mode = AstroCardSurfaceMode.Preview,
                modifier = Modifier
                    .width(cardWidth)
                    .testTag("trade-remote-card"),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                enabled = !state.localConfirmed,
                onClick = onConfirm,
                modifier = Modifier.testTag("trade-confirm"),
            ) {
                Text(if (state.localConfirmed) "Confirmé" else "Confirmer")
            }
        }
        if (state.remoteConfirmed) {
            Text(
                text = "Partenaire confirmé",
                color = Color(0xFF8DF6E7),
                modifier = Modifier.testTag("trade-remote-confirmed"),
            )
        }
    }
}

@Composable
private fun ProgressContent(
    text: String,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        CircularProgressIndicator()
        Text(text, color = Color(0xFFD7E8F6))
    }
}

@Composable
private fun FailedTradeActions(
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
) {
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

@Composable
private fun TradeSucceededContent(
    selectedDisplayCard: DisplayCard,
    receivedDisplayCard: DisplayCard?,
    isResolvingReceivedCard: Boolean,
    message: String?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var revealed by remember(
        receivedDisplayCard?.definition?.id,
        receivedDisplayCard?.activeVariant?.key,
    ) {
        mutableStateOf(false)
    }

    BoxWithConstraints(
        modifier = modifier.testTag("trade-success"),
    ) {
        val compact = maxHeight < 560.dp
        val contentSpacing = if (compact) 6.dp else 8.dp
        val titleStyle = if (compact) {
            MaterialTheme.typography.titleLarge
        } else {
            MaterialTheme.typography.headlineSmall
        }
        val copyStyle = if (compact) {
            MaterialTheme.typography.bodyMedium
        } else {
            MaterialTheme.typography.bodyLarge
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(contentSpacing),
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = TradeSuccessBottomSafetyPadding),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF8DF6E7),
                    modifier = Modifier.size(if (compact) 26.dp else 30.dp),
                )
                Text(
                    text = message ?: "Échange réussi !",
                    style = titleStyle,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .testTag("trade-success-title"),
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
                style = copyStyle,
                textAlign = TextAlign.Center,
                maxLines = if (compact) 2 else 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.testTag("trade-success-copy"),
            )

            ExchangeCardRevealStage(
                selectedDisplayCard = selectedDisplayCard,
                receivedDisplayCard = receivedDisplayCard,
                revealed = revealed,
                onReveal = { revealed = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
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
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
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
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
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
                    maxLines = 1,
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
}

@Composable
private fun ExchangeCardRevealStage(
    selectedDisplayCard: DisplayCard,
    receivedDisplayCard: DisplayCard?,
    revealed: Boolean,
    onReveal: () -> Unit,
    modifier: Modifier = Modifier,
) {
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

    BoxWithConstraints(
        modifier = modifier
            .then(gestureModifier)
            .testTag("trade-success-card-stage"),
    ) {
        val density = LocalDensity.current
        val cardWidth = calculateTradeRevealCardWidth(
            availableWidth = maxWidth,
            availableHeight = maxHeight,
        )
        val motion = calculateTradeRevealMotion(cardWidth)
        val outgoingLiftPx = with(density) { motion.outgoingLift.toPx() }
        val incomingDropPx = with(density) { motion.incomingDrop.toPx() }
        val cardBottomInset = motion.incomingDrop + TradeRevealStageBottomPadding

        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = cardBottomInset),
            ) {
                AstroCardPreviewSurface(
                    displayCard = selectedDisplayCard,
                    mode = AstroCardSurfaceMode.Preview,
                    modifier = Modifier
                        .width(cardWidth)
                        .graphicsLayer {
                            alpha = 1f - incomingProgress
                            translationY = -outgoingLiftPx * revealProgress
                            scaleX = 1f - 0.08f * revealProgress
                            scaleY = 1f - 0.08f * revealProgress
                            rotationZ = -5f * revealProgress
                        }
                        .testTag("trade-success-outgoing-card"),
                )
            }

            if (beamAlpha > 0f) {
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .width(cardWidth * 1.18f)
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
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = cardBottomInset),
                ) {
                    AstroCardPreviewSurface(
                        displayCard = receivedDisplayCard,
                        mode = AstroCardSurfaceMode.Preview,
                        modifier = Modifier
                            .width(cardWidth)
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
private fun BluetoothTradeSessionEffect(
    active: Boolean,
    state: TradeUiState,
    tradeGateway: TradeGateway,
    onEvent: (BluetoothTradeControllerEvent) -> Unit,
) {
    val candidate = state.selectedCandidate
    val catalogFingerprint = state.catalogFingerprint
    val localName = state.localName
    val context = LocalContext.current
    val controllerRef = remember { mutableStateOf<BluetoothTradeController?>(null) }
    var lastStartedLocalName by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(active, candidate, catalogFingerprint, localName, tradeGateway) {
        val previousController = controllerRef.value
        val previousName = lastStartedLocalName
        val restartingAfterRename = active &&
            previousController != null &&
            previousName != null &&
            previousName != localName

        if (previousController != null) {
            previousController.stop()
            controllerRef.value = null
        }

        if (!active || candidate == null || catalogFingerprint == null || localName.isBlank()) {
            return@LaunchedEffect
        }

        if (restartingAfterRename) {
            delay(BluetoothTradeLeavingAdvertisementDurationMillis)
        }

        val controller = BluetoothTradeController(
            context = context,
            tradeGateway = tradeGateway,
            localCard = candidate.toTradeCardRef(),
            catalogFingerprint = catalogFingerprint,
            localName = localName,
            onEvent = onEvent,
        )
        controllerRef.value = controller
        lastStartedLocalName = localName
        controller.start()
    }

    DisposableEffect(Unit) {
        onDispose {
            controllerRef.value?.stop()
            controllerRef.value = null
        }
    }

    LaunchedEffect(state.connectionCommand?.id) {
        val command = state.connectionCommand
        if (command != null) {
            controllerRef.value?.connectTo(command.partnerId)
        }
    }

    LaunchedEffect(state.confirmationCommandId) {
        if (state.confirmationCommandId > 0L) {
            controllerRef.value?.confirmExchange()
        }
    }
}

private fun Context.missingBluetoothTradePermissions(): List<String> =
    requiredBluetoothTradePermissions().filter { permission ->
        ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED
    }

private fun requiredBluetoothTradePermissions(): List<String> =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        listOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_ADVERTISE,
            Manifest.permission.BLUETOOTH_CONNECT,
        )
    } else {
        listOf(Manifest.permission.ACCESS_FINE_LOCATION)
    }

@SuppressLint("MissingPermission")
private fun Context.isBluetoothEnabledForTrade(): Boolean {
    val manager = getSystemService(BluetoothManager::class.java) ?: return false
    return manager.adapter?.isEnabled == true
}

private val ActiveBluetoothTradePhases = setOf(
    TradePhase.Discovering,
    TradePhase.Connecting,
    TradePhase.Confirming,
    TradePhase.Exchanging,
)
