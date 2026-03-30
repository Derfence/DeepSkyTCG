package fr.aumombelli.gatcha.ui.screen

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun MainMenuScreen(
    onOpenPack: () -> Unit,
    onOpenLibrary: () -> Unit,
    onOpenBadgeBook: () -> Unit,
    modifier: Modifier = Modifier,
    showBackground: Boolean = true,
    contentVisible: Boolean = true,
    interactionsEnabled: Boolean = true,
    onBadgeButtonBoundsChanged: (Rect?) -> Unit = {},
) {
    val panelAlpha by animateFloatAsState(
        targetValue = if (contentVisible) 1f else 0f,
        animationSpec = tween(durationMillis = 520, easing = FastOutSlowInEasing),
        label = "main-menu-panel-alpha",
    )
    val panelTranslationY by animateFloatAsState(
        targetValue = if (contentVisible) 0f else 54f,
        animationSpec = tween(durationMillis = 520, easing = FastOutSlowInEasing),
        label = "main-menu-panel-translation",
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                if (showBackground) {
                    Brush.radialGradient(
                        colors = listOf(Color(0xFF21446F), Color(0xFF08101D)),
                    )
                } else {
                    Brush.radialGradient(
                        colors = listOf(Color.Transparent, Color.Transparent),
                    )
                },
            ),
    ) {
        Surface(
            shape = RoundedCornerShape(32.dp),
            tonalElevation = 6.dp,
            modifier = Modifier
                .align(Alignment.Center)
                .graphicsLayer {
                    alpha = panelAlpha
                    translationY = panelTranslationY
                }
                .gatchaContentInsetsPadding(includeBottom = true)
                .padding(24.dp),
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
            ) {
                Text(
                    text = "Main Menu",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.testTag("menu-panel"),
                )
                Text(
                    text = "Choisis ton prochain mouvement : enrichir ta collection ou l'explorer.",
                    style = MaterialTheme.typography.bodyLarge,
                )
                Button(
                    onClick = onOpenPack,
                    enabled = interactionsEnabled,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("menu-open-pack"),
                ) {
                    Text("Open Pack")
                }
                Button(
                    onClick = onOpenLibrary,
                    enabled = interactionsEnabled,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("menu-library"),
                ) {
                    Text("Library")
                }
                Button(
                    onClick = onOpenBadgeBook,
                    enabled = interactionsEnabled,
                    modifier = Modifier
                        .fillMaxWidth()
                        .onGloballyPositioned { coordinates ->
                            if (contentVisible) {
                                onBadgeButtonBoundsChanged(coordinates.boundsInRoot())
                            }
                        }
                        .testTag("menu-badges"),
                ) {
                    Text("Badges")
                }
            }
        }
    }
}
