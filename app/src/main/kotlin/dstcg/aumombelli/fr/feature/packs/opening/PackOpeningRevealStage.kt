package fr.aumombelli.dstcg.feature.packs.opening

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import fr.aumombelli.dstcg.ui.component.TRADING_CARD_WIDTH_OVER_HEIGHT
import fr.aumombelli.dstcg.ui.motion.PackRevealBounds
import fr.aumombelli.dstcg.ui.screen.dstcgContentInsetsPadding

@Composable
private fun PackOpeningProgressIndicator(
    currentPage: Int,
    total: Int,
    alpha: Float = 1f,
) {
    androidx.compose.material3.Text(
        text = "${currentPage + 1} / $total",
        color = Color.White.copy(alpha = 0.9f),
        style = androidx.compose.material3.MaterialTheme.typography.labelLarge,
        modifier = Modifier
            .graphicsLayer {
                this.alpha = alpha
            }
            .testTag("pack-opening-progress"),
    )
}

@Composable
internal fun PackOpeningRevealSlotProbe(
    extensionLabel: String,
    totalItems: Int,
    onBoundsChanged: (PackRevealBounds?) -> Unit,
    onCoordinatesChanged: (LayoutCoordinates?) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(
        modifier = modifier
            .dstcgContentInsetsPadding(includeBottom = true)
            .clearAndSetSemantics {},
    ) {
        val availableWidth = maxWidth
        val availableHeight = maxHeight

        androidx.compose.foundation.layout.Box(
            modifier = Modifier.fillMaxSize(),
        ) {
            val sceneLayout = calculatePackOpeningSceneLayout(
                availableWidth = availableWidth,
                availableHeight = availableHeight,
            )
            val progressBelowPager = shouldPlacePackOpeningProgressBelowPager(
                availableWidth = availableWidth,
                availableHeight = availableHeight,
                sceneLayout = sceneLayout,
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(sceneLayout.sectionSpacing),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        horizontal = sceneLayout.horizontalContentPadding,
                        vertical = sceneLayout.verticalContentPadding,
                    ),
            ) {
                androidx.compose.material3.Text(
                    text = "Ouverture du pack",
                    style = if (sceneLayout.compactHeader) {
                        androidx.compose.material3.MaterialTheme.typography.titleLarge
                    } else {
                        androidx.compose.material3.MaterialTheme.typography.headlineMedium
                    },
                    fontWeight = FontWeight.Bold,
                    color = Color.Transparent,
                )
                androidx.compose.material3.Text(
                    text = "Extension : $extensionLabel",
                    color = Color.Transparent,
                    style = if (sceneLayout.compactHeader) {
                        androidx.compose.material3.MaterialTheme.typography.bodySmall
                    } else {
                        androidx.compose.material3.MaterialTheme.typography.bodyMedium
                    },
                )

                if (!progressBelowPager) {
                    ProbePackOpeningProgressIndicator(total = totalItems.coerceAtLeast(1))
                }

                Box(
                    modifier = Modifier.weight(1f),
                ) {
                    PackOpeningRevealCardFrame(
                        modifier = Modifier.fillMaxSize(),
                        onCardBoundsChanged = onBoundsChanged,
                        onCardCoordinatesChanged = onCoordinatesChanged,
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(TRADING_CARD_WIDTH_OVER_HEIGHT),
                        )
                    }
                }

                if (progressBelowPager) {
                    ProbePackOpeningProgressIndicator(total = totalItems.coerceAtLeast(1))
                }
            }
        }
    }
}

@Composable
private fun ProbePackOpeningProgressIndicator(total: Int) {
    androidx.compose.material3.Text(
        text = "1 / $total",
        color = Color.Transparent,
        style = androidx.compose.material3.MaterialTheme.typography.labelLarge,
    )
}

@Composable
internal fun PackOpeningRevealStageScaffold(
    extensionLabel: String,
    currentPage: Int,
    totalItems: Int,
    sceneLayout: PackOpeningSceneLayout,
    progressBelowPager: Boolean,
    headerAlpha: Float = 1f,
    modifier: Modifier = Modifier,
    stageContent: @Composable BoxScope.() -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(sceneLayout.sectionSpacing),
        modifier = modifier
            .fillMaxSize()
            .padding(
                horizontal = sceneLayout.horizontalContentPadding,
                vertical = sceneLayout.verticalContentPadding,
            ),
    ) {
        androidx.compose.material3.Text(
            text = "Ouverture du pack",
            style = if (sceneLayout.compactHeader) {
                androidx.compose.material3.MaterialTheme.typography.titleLarge
            } else {
                androidx.compose.material3.MaterialTheme.typography.headlineMedium
            },
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier
                .graphicsLayer {
                    alpha = headerAlpha
                }
                .testTag("pack-opening-title"),
        )
        androidx.compose.material3.Text(
            text = "Extension : $extensionLabel",
            color = Color(0xFFD8E6F8),
            style = if (sceneLayout.compactHeader) {
                androidx.compose.material3.MaterialTheme.typography.bodySmall
            } else {
                androidx.compose.material3.MaterialTheme.typography.bodyMedium
            },
            modifier = Modifier.graphicsLayer {
                alpha = headerAlpha
            },
        )

        if (!progressBelowPager) {
            PackOpeningProgressIndicator(
                currentPage = currentPage,
                total = totalItems,
                alpha = headerAlpha,
            )
        }

        Box(
            modifier = Modifier.weight(1f),
            content = stageContent,
        )

        if (progressBelowPager) {
            PackOpeningProgressIndicator(
                currentPage = currentPage,
                total = totalItems,
                alpha = headerAlpha,
            )
        }
    }
}

internal fun shouldPlacePackOpeningProgressBelowPager(
    availableWidth: Dp,
    availableHeight: Dp,
    sceneLayout: PackOpeningSceneLayout,
): Boolean {
    val estimatedCardWidth = (
        availableWidth -
            (sceneLayout.horizontalContentPadding * 2f) -
            PackOpeningRevealHorizontalChrome
        ).coerceAtLeast(0.dp)
    val estimatedCardHeight = estimatedCardWidth / TRADING_CARD_WIDTH_OVER_HEIGHT
    val estimatedPagerBudget = (
        availableHeight -
            (sceneLayout.verticalContentPadding * 2f) -
            PackOpeningRevealHeaderEstimate -
            (sceneLayout.sectionSpacing * 2f)
        ).coerceAtLeast(0.dp)
    return estimatedCardHeight > estimatedPagerBudget - PackOpeningProgressReserve
}

private val PackOpeningRevealHorizontalChrome = 72.dp
private val PackOpeningRevealHeaderEstimate = 96.dp
private val PackOpeningProgressReserve = 32.dp

internal data class PagerScrollSnapshot(
    val isScrollInProgress: Boolean,
    val currentPage: Int,
    val offsetFraction: Float,
)

internal fun shouldTriggerPackOpeningHolographicCue(
    cardsVisible: Boolean,
    currentPage: Int,
    lastObservedPage: Int,
    isHolographic: Boolean,
    alreadyPlayed: Boolean,
): Boolean = cardsVisible &&
    currentPage != lastObservedPage &&
    isHolographic &&
    !alreadyPlayed
