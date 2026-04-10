package fr.aumombelli.dstcg.feature.packs.opening

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import fr.aumombelli.dstcg.model.DisplayCard
import fr.aumombelli.dstcg.model.bonusLabel
import fr.aumombelli.dstcg.ui.component.AstroCardDetailsSurface
import fr.aumombelli.dstcg.ui.component.AstroCardFullscreenCloseButton
import fr.aumombelli.dstcg.ui.component.AstroCardPreviewSurface
import fr.aumombelli.dstcg.ui.component.AstroCardSurfaceMode
import fr.aumombelli.dstcg.ui.component.TRADING_CARD_WIDTH_OVER_HEIGHT
import fr.aumombelli.dstcg.ui.screen.dstcgContentInsetsPadding

@Composable
internal fun RevealCard(
    item: PackRevealUiItem,
    isCurrentPage: Boolean,
    showPreviousArrow: Boolean,
    showNextArrow: Boolean,
    cardTranslationY: Float,
    nudgeActive: Boolean,
    onOpenFullscreen: () -> Unit,
) {
    BoxWithConstraints(
        modifier = Modifier.fillMaxSize(),
    ) {
        val revealLayout = calculatePackOpeningRevealLayout(
            availableWidth = maxWidth,
            availableHeight = maxHeight,
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    horizontal = revealLayout.pageHorizontalPadding,
                    vertical = revealLayout.pageVerticalPadding,
                )
                .padding(
                    top = revealLayout.topOverlayReserve,
                    bottom = revealLayout.bottomSafeInset,
                ),
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .width(revealLayout.cardWidth)
                    .graphicsLayer {
                        translationY = cardTranslationY
                    },
            ) {
                when (item) {
                    is AstroPackRevealUiItem -> {
                        AstroCardPreviewSurface(
                            displayCard = item.displayCard,
                            mode = AstroCardSurfaceMode.PackReveal,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag(
                                    if (isCurrentPage) {
                                        "pack-opening-current-card-surface"
                                    } else {
                                        "pack-opening-card-surface"
                                    },
                                ),
                            onClick = onOpenFullscreen,
                        )
                    }

                    is EquipmentPackRevealUiItem -> {
                        EquipmentRevealSurface(
                            item = item,
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(TRADING_CARD_WIDTH_OVER_HEIGHT)
                                .testTag(
                                    if (isCurrentPage) {
                                        "pack-opening-current-card-surface"
                                    } else {
                                        "pack-opening-card-surface"
                                    },
                                ),
                        )
                    }
                }
            }
        }

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

        if (nudgeActive) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .testTag("pack-opening-last-card-nudge"),
            )
        }
    }
}

@Composable
private fun EquipmentRevealSurface(
    item: EquipmentPackRevealUiItem,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = {},
        shape = androidx.compose.foundation.shape.RoundedCornerShape(28.dp),
        color = Color(0xFF132032),
        modifier = modifier.clip(androidx.compose.foundation.shape.RoundedCornerShape(28.dp)),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF17314D),
                            Color(0xFF0C1422),
                        ),
                    ),
                )
                .padding(20.dp),
        ) {
            Text(
                text = item.definition.type.displayName,
                color = Color(0xFFF0D995),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = item.definition.displayName,
                color = Color.White,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "Niveau ${item.definition.level}",
                color = Color(0xFFD7E8FF),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = item.definition.bonusLabel(),
                color = Color(0xFF9EE7FF),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = "Actif pendant ${item.definition.packsAffected} packs",
                color = Color(0xFFE6EEF9),
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = item.definition.description,
                color = Color(0xFFC7D6E8),
                style = MaterialTheme.typography.bodyMedium,
            )
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
                .dstcgContentInsetsPadding(includeBottom = true)
                .padding(14.dp)
                .testTag("astro-card-fullscreen"),
        ) {
            AstroCardDetailsSurface(
                displayCard = displayCard,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 42.dp),
            )
            AstroCardFullscreenCloseButton(onClick = onDismiss)
        }
    }
}
