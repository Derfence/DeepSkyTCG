package fr.aumombelli.dstcg.feature.trade

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fr.aumombelli.dstcg.data.TradeGateway
import fr.aumombelli.dstcg.model.toDisplayCard
import fr.aumombelli.dstcg.ui.component.AstroCardPreviewSurface
import fr.aumombelli.dstcg.ui.component.AstroCardSurfaceMode
import fr.aumombelli.dstcg.ui.screen.dstcgContentInsetsPadding

@Composable
fun TradeScreen(
    viewModel: TradeViewModel,
    tradeGateway: TradeGateway,
    onDismiss: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

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
                        colors = listOf(Color(0xFF07111E), Color(0xFF102A3D)),
                    ),
                )
                .dstcgContentInsetsPadding(includeBottom = true)
                .padding(18.dp)
                .testTag("trade-screen"),
        ) {
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
                        text = "Echange NFC",
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
        Text("Preparation des cartes echangeables...", color = Color(0xFFD7E8F6))
    }
}

@Composable
private fun TradeReadyContent(
    state: TradeUiState,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
) {
    val candidate = checkNotNull(state.selectedCandidate)
    Column(
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("trade-ready"),
    ) {
        Text("Carte proposee", color = Color(0xFF8DEBFF))
        AstroCardPreviewSurface(
            displayCard = candidate.card.toDisplayCard(
                extensionName = candidate.extensionName,
                activeVariant = candidate.variant,
                availableVariants = listOf(candidate.variant),
            ),
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
                    Text("Reessayer")
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
            onEvent(NfcTradeControllerEvent.Failed("Ecran Android indisponible pour le NFC."))
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
