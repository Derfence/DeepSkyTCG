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
import androidx.compose.ui.unit.dp
import fr.aumombelli.gatcha.model.PackCard
import fr.aumombelli.gatcha.ui.theme.rarityColor
import fr.aumombelli.gatcha.ui.viewmodel.PackOpeningUiState
import kotlinx.coroutines.delay

@Composable
fun PackOpeningScreen(
    state: PackOpeningUiState,
    onDone: () -> Unit,
) {
    val packResult = state.packResult
    var cardsVisible by remember(packResult?.drawnAt) { mutableStateOf(false) }
    val scale = remember(packResult?.drawnAt) { Animatable(0.6f) }

    LaunchedEffect(packResult?.drawnAt) {
        if (packResult == null) return@LaunchedEffect
        cardsVisible = false
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
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .align(Alignment.Center)
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
                text = "Extension: ${packResult.extensionId}",
                color = Color(0xFFD8E6F8),
            )

            if (!cardsVisible) {
                Card(
                    shape = RoundedCornerShape(28.dp),
                    modifier = Modifier
                        .padding(top = 28.dp)
                        .size(width = 220.dp, height = 320.dp)
                        .scale(scale.value),
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
            } else {
                val pagerState = rememberPagerState(pageCount = { packResult.cards.size })
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.weight(1f),
                ) { page ->
                    RevealCard(
                        card = packResult.cards[page],
                        page = page + 1,
                        total = packResult.cards.size,
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
    }
}

@Composable
private fun RevealCard(
    card: PackCard,
    page: Int,
    total: Int,
) {
    Card(
        shape = RoundedCornerShape(30.dp),
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 18.dp, horizontal = 12.dp),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            rarityColor(card.rarityLabel),
                            Color(0xFF0C1424),
                        ),
                    ),
                )
                .padding(24.dp),
        ) {
            Text(
                text = "$page / $total",
                color = Color.White.copy(alpha = 0.9f),
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.testTag("pack-opening-progress"),
            )
            Text(
                text = card.cardId,
                color = Color(0xFFEAF4FF),
                modifier = Modifier.testTag("pack-opening-card-id"),
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(
                        Brush.radialGradient(
                            listOf(Color.White.copy(alpha = 0.25f), Color.Transparent),
                        ),
                        shape = RoundedCornerShape(24.dp),
                    ),
            )
            Text(
                text = card.name,
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.testTag("pack-opening-card-name"),
            )
            Text(
                text = card.rarityLabel,
                color = Color(0xFFFDE7A5),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = "Ciel: ${card.variant.skyQualityLabel}",
                color = Color(0xFFD7E7F7),
            )
            Text(
                text = "Finition: ${card.variant.finishLabel}",
                color = if (card.variant.isHolographic) Color(0xFFFFE59A) else Color(0xFFD7E7F7),
            )
            Text(
                text = "Swipe to reveal the next card",
                color = Color(0xFFD7E7F7),
            )
        }
    }
}
