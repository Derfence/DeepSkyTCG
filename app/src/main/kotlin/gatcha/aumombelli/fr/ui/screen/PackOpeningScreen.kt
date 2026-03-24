package fr.aumombelli.gatcha.ui.screen

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
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
import fr.aumombelli.gatcha.ui.viewmodel.PackOpeningUiState
import kotlinx.coroutines.delay

@Composable
fun PackOpeningScreen(
    state: PackOpeningUiState,
    onDone: () -> Unit,
) {
    val packResult = state.packResult
    val displayCards = state.displayCards
    var cardsVisible by remember(packResult?.drawnAt) { mutableStateOf(false) }
    var fullscreenPage by remember(packResult?.drawnAt) { mutableStateOf<Int?>(null) }
    val scale = remember(packResult?.drawnAt) { Animatable(0.6f) }

    LaunchedEffect(packResult?.drawnAt) {
        if (packResult == null) return@LaunchedEffect
        cardsVisible = false
        fullscreenPage = null
        scale.snapTo(0.6f)
        scale.animateTo(1f, animationSpec = tween(durationMillis = 850, easing = FastOutSlowInEasing))
        delay(250)
        cardsVisible = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF060B14), Color(0xFF162840)))),
    ) {
        if (packResult == null) {
            EmptyPackState(onDone = onDone)
            return@Box
        }

        if (state.errorMessage != null && displayCards.isEmpty()) {
            PackOpeningErrorState(
                message = state.errorMessage,
                onDone = onDone,
            )
            return@Box
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
        ) {
            Text(
                text = "Pack Opening",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.testTag("pack-opening-title"),
            )
            Text(
                text = "Extension: ${displayCards.firstOrNull()?.extensionName ?: packResult.extensionId}",
                color = Color(0xFFD8E6F8),
            )

            if (!cardsVisible) {
                BoosterCover(scale = scale.value)
            } else if (displayCards.isNotEmpty()) {
                val pagerState = rememberPagerState(pageCount = { displayCards.size })
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.weight(1f),
                ) { page ->
                    RevealCard(
                        displayCard = displayCards[page],
                        page = page + 1,
                        total = displayCards.size,
                        onOpenFullscreen = { fullscreenPage = page },
                    )
                }
            }

            Button(
                onClick = onDone,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("pack-opening-done"),
            ) {
                Text("Back to menu")
            }
        }

        val fullscreenCard = fullscreenPage?.let(displayCards::getOrNull)
        if (fullscreenCard != null) {
            PackOpeningFullscreenDialog(
                displayCard = fullscreenCard,
                onDismiss = { fullscreenPage = null },
            )
        }
    }
}

@Composable
private fun EmptyPackState(
    onDone: () -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
    ) {
        Text("No opened pack available.", color = Color.White)
        Button(
            onClick = onDone,
            modifier = Modifier.testTag("pack-opening-done"),
        ) {
            Text("Back to menu")
        }
    }
}

@Composable
private fun PackOpeningErrorState(
    message: String,
    onDone: () -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
    ) {
        Text(message, color = Color(0xFFFFA3A3))
        Button(onClick = onDone) {
            Text("Back to menu")
        }
    }
}

@Composable
private fun BoosterCover(
    scale: Float,
) {
    Card(
        shape = RoundedCornerShape(28.dp),
        modifier = Modifier
            .padding(top = 28.dp)
            .size(width = 220.dp, height = 320.dp)
            .scale(scale),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(Color(0xFFF6B73C), Color(0xFF2EC4B6), Color(0xFF1B2A41)),
                    ),
                ),
        ) {
            Text(
                text = "Booster",
                color = Color.White,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Black,
                modifier = Modifier.align(Alignment.Center),
            )
        }
    }
}

@Composable
private fun RevealCard(
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
        Text(
            text = displayCard.definition.name,
            style = MaterialTheme.typography.headlineSmall,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.testTag("pack-opening-card-name"),
        )
        Text(
            text = "Touchez la carte pour voir toutes les donnees scientifiques.",
            color = Color(0xFFD7E7F7),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        Text(
            text = "Swipe to reveal the next card",
            color = Color(0xFFD7E7F7),
        )
    }
}

@Composable
private fun PackOpeningFullscreenDialog(
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
