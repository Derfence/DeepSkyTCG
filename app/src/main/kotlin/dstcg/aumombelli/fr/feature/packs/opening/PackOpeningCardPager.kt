package fr.aumombelli.dstcg.feature.packs.opening

import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import fr.aumombelli.dstcg.feature.equipment.EquipmentCategoryBadge
import fr.aumombelli.dstcg.feature.equipment.toCategoryVisualUi
import fr.aumombelli.dstcg.model.DisplayCard
import fr.aumombelli.dstcg.model.bonusLabel
import fr.aumombelli.dstcg.ui.component.AstroCardDetailsSurface
import fr.aumombelli.dstcg.ui.component.AstroCardFullscreenCloseButton
import fr.aumombelli.dstcg.ui.component.AstroCardPreviewSurface
import fr.aumombelli.dstcg.ui.component.AstroCardSurfaceMode
import fr.aumombelli.dstcg.ui.component.EquipmentArtBackground
import fr.aumombelli.dstcg.ui.component.EquipmentArtMode
import fr.aumombelli.dstcg.ui.component.NewContentIndicator
import fr.aumombelli.dstcg.ui.component.TRADING_CARD_WIDTH_OVER_HEIGHT
import fr.aumombelli.dstcg.ui.motion.HolographicCardMotion
import fr.aumombelli.dstcg.ui.motion.PackRevealBounds
import fr.aumombelli.dstcg.ui.motion.easeInOutBurst
import fr.aumombelli.dstcg.ui.screen.dstcgContentInsetsPadding
import kotlin.math.PI
import kotlin.math.sin

@Composable
internal fun RevealCard(
    item: PackRevealUiItem,
    isCurrentPage: Boolean,
    showPreviousArrow: Boolean,
    showNextArrow: Boolean,
    cardTranslationY: Float,
    nudgeActive: Boolean,
    holographicArrivalProgress: Float = 0f,
    holographicMotion: HolographicCardMotion? = null,
    onCardBoundsChanged: ((PackRevealBounds?) -> Unit)? = null,
    onOpenFullscreen: () -> Unit,
) {
    BoxWithConstraints(
        modifier = Modifier.fillMaxSize(),
    ) {
        val revealSlot = calculatePackOpeningRevealSlotLayout(
            availableWidth = maxWidth,
            availableHeight = maxHeight,
        )
        val arrivalProgress = holographicArrivalProgress.coerceIn(0f, 1f)
        val arrivalScale = holographicArrivalScale(arrivalProgress)
        val arrivalLiftPx = holographicArrivalLiftPx(arrivalProgress)
        val arrivalRotationZ = holographicArrivalRotationZ(arrivalProgress)

        PackOpeningRevealCardFrame(
            modifier = Modifier.fillMaxSize(),
            cardTranslationY = cardTranslationY + arrivalLiftPx,
            cardScale = arrivalScale,
            cardRotationZ = arrivalRotationZ,
            onCardBoundsChanged = onCardBoundsChanged,
        ) {
            when (item) {
                is AstroPackRevealUiItem -> {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        AstroCardPreviewSurface(
                            displayCard = item.displayCard,
                            mode = AstroCardSurfaceMode.PackReveal,
                            holographicMotion = holographicMotion,
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
                        if (item.showFirstEncounterIndicator) {
                            NewContentIndicator(
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(10.dp)
                                    .testTag(
                                        "pack-opening-first-encounter-indicator-${item.displayCard.definition.id}",
                                    ),
                                iconSize = 15.dp,
                            )
                        }
                    }
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
internal fun PackOpeningRevealCardFrame(
    modifier: Modifier = Modifier,
    cardTranslationY: Float = 0f,
    cardScale: Float = 1f,
    cardRotationZ: Float = 0f,
    onCardBoundsChanged: ((PackRevealBounds?) -> Unit)? = null,
    onCardCoordinatesChanged: ((LayoutCoordinates?) -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    BoxWithConstraints(
        modifier = modifier,
    ) {
        val revealSlot = calculatePackOpeningRevealSlotLayout(
            availableWidth = maxWidth,
            availableHeight = maxHeight,
        )

        Box(
            modifier = Modifier.fillMaxSize(),
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .absoluteOffset(
                        x = revealSlot.cardStart,
                        y = revealSlot.cardTop,
                    )
                    .width(revealSlot.cardWidth)
                    .graphicsLayer {
                        translationY = cardTranslationY
                        scaleX = cardScale
                        scaleY = cardScale
                        rotationZ = cardRotationZ
                    }
                    .then(
                        if (onCardBoundsChanged != null || onCardCoordinatesChanged != null) {
                            Modifier.onGloballyPositioned { coordinates ->
                                if (onCardBoundsChanged != null) {
                                    val bounds = coordinates.boundsInRoot()
                                    onCardBoundsChanged(
                                        PackRevealBounds(
                                            leftPx = bounds.left,
                                            topPx = bounds.top,
                                            widthPx = bounds.width,
                                            heightPx = bounds.height,
                                        ),
                                    )
                                }
                                onCardCoordinatesChanged?.invoke(coordinates)
                            }
                        } else {
                            Modifier
                        },
                    ),
            ) {
                content()
            }
        }
    }
}

internal fun holographicArrivalScale(progress: Float): Float {
    val clamped = progress.coerceIn(0f, 1f)
    val punch = easeInOutBurst((clamped / 0.16f).coerceIn(0f, 1f))
    val settleEnvelope = holographicArrivalSettleEnvelope(
        progress = clamped,
        settleStart = 0.18f,
    )
    return 1f + 0.18f * punch * settleEnvelope
}

internal fun holographicArrivalLiftPx(progress: Float): Float {
    val clamped = progress.coerceIn(0f, 1f)
    val rise = easeInOutBurst((clamped / 0.13f).coerceIn(0f, 1f))
    val settleEnvelope = holographicArrivalSettleEnvelope(
        progress = clamped,
        settleStart = 0.14f,
    )
    return -68f * rise * settleEnvelope
}

private fun holographicArrivalRotationZ(progress: Float): Float {
    val clamped = progress.coerceIn(0f, 1f)
    return sin(clamped * PI * 5.5f).toFloat() * 3.4f * (1f - clamped)
}

private fun holographicArrivalSettleEnvelope(
    progress: Float,
    settleStart: Float,
): Float = 1f - easeInOutBurst(
    ((progress - settleStart) / (1f - settleStart)).coerceIn(0f, 1f),
)

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
        Box(
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
        ) {
            EquipmentArtBackground(
                definition = item.definition,
                mode = EquipmentArtMode.PackReveal,
                modifier = Modifier.fillMaxSize(),
                artTestTag = "pack-opening-equipment-art-${item.definition.id}",
                fallbackTestTag = "pack-opening-equipment-art-fallback-${item.definition.id}",
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    EquipmentCategoryBadge(
                        type = item.definition.type,
                        icon = item.definition.type.toCategoryVisualUi().icon,
                        badgeSize = 46.dp,
                        modifier = Modifier.testTag("pack-opening-equipment-icon-${item.definition.id}"),
                    )
                    Column(
                        verticalArrangement = Arrangement.spacedBy(2.dp),
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
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
