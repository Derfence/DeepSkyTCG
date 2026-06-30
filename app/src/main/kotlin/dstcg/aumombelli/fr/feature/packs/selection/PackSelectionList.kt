package fr.aumombelli.dstcg.feature.packs.selection

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.aumombelli.dstcg.model.ExtensionDefinition
import fr.aumombelli.dstcg.ui.motion.MotionCard
import kotlinx.coroutines.delay

internal val EXTENSION_CARD_HEIGHT: Dp = 164.dp
internal val EXTENSION_CARD_SPACING: Dp = 12.dp
internal val EXTENSION_LIST_TOP_PADDING: Dp = 44.dp
internal const val EXTENSION_CARD_ENTRANCE_DURATION_MS = 520
internal const val EXTENSION_CARD_ENTRANCE_STAGGER_MS = 300
internal const val EXTENSION_CARD_ENTRANCE_OFFSET_PX = 168f

@Composable
internal fun ExtensionList(
    extensions: List<ExtensionDefinition>,
    extensionCardProgress: Map<String, ExtensionCardProgress>,
    drawLocked: Boolean,
    onSelectExtension: (String) -> Unit,
    interactionsEnabled: Boolean,
    highlightedExtensionId: String?,
    highlightProgress: Float,
    badgeAnimationsEnabled: Boolean,
    onFirstEnabledExtensionBoundsChanged: (Rect?) -> Unit = {},
    entranceSignal: Int = 0,
    modifier: Modifier = Modifier,
) {
    val firstEnabledExtensionId = if (!drawLocked) {
        extensions.firstOrNull()?.id
    } else {
        null
    }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier
            .testTag("pack-extension-list"),
    ) {
        itemsIndexed(
            items = extensions,
            key = { _, extension -> extension.id },
        ) { index, extension ->
            val entranceProgress = remember(extension.id, entranceSignal) {
                Animatable(if (entranceSignal > 0) 0f else 1f)
            }
            LaunchedEffect(extension.id, entranceSignal) {
                if (entranceSignal <= 0) {
                    entranceProgress.snapTo(1f)
                    return@LaunchedEffect
                }

                entranceProgress.snapTo(0f)
                delay(extensionEntranceDelayMillis(index).toLong())
                entranceProgress.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(
                        durationMillis = EXTENSION_CARD_ENTRANCE_DURATION_MS,
                        easing = FastOutSlowInEasing,
                    ),
                )
            }
            val alpha = when {
                highlightedExtensionId == null -> 1f
                extension.id == highlightedExtensionId -> (1f - highlightProgress * 4f).coerceIn(0f, 1f)
                else -> (1f - highlightProgress).coerceIn(0f, 1f)
            }
            val progress = extensionCardProgress[extension.id]
            val cardVisibleForInteraction = alpha * extensionEntranceAlpha(entranceProgress.value) >= 0.99f
            MotionCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(EXTENSION_CARD_HEIGHT)
                    .graphicsLayer {
                        this.alpha = alpha * extensionEntranceAlpha(entranceProgress.value)
                        translationY = extensionEntranceTranslationYPx(entranceProgress.value)
                    },
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.Start,
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxSize()
                        .padding(18.dp)
                        .testTag("pack-extension-${extension.id}"),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        ResizableExtensionName(
                            text = extension.name,
                            modifier = Modifier.weight(1f),
                        )
                        progress?.let {
                            Text(
                                text = it.displayLabel,
                                style = MaterialTheme.typography.labelLarge,
                                color = Color(0xFFCFE6FF),
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier
                                    .padding(start = 10.dp)
                                    .testTag("pack-extension-progress-${extension.id}"),
                            )
                        }
                        ExtensionAnimatedBadge(
                            extensionId = extension.id,
                            animationsEnabled = badgeAnimationsEnabled,
                            startDelayMillis = extensionBadgeStartDelayMillis(index),
                            modifier = Modifier
                                .padding(start = 12.dp)
                                .width(78.dp)
                                .height(54.dp),
                        )
                    }
                    Button(
                        onClick = { onSelectExtension(extension.id) },
                        enabled = !drawLocked && interactionsEnabled && cardVisibleForInteraction,
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(
                                if (extension.id == firstEnabledExtensionId && highlightedExtensionId == null) {
                                    Modifier.onGloballyPositioned { coordinates ->
                                        onFirstEnabledExtensionBoundsChanged(coordinates.boundsInRoot())
                                    }
                                } else {
                                    Modifier
                                },
                            )
                            .testTag("pack-extension-enter-${extension.id}"),
                    ) {
                        Text(if (drawLocked) "Pas de pack disponible" else "Observer")
                    }
                }
            }
        }
    }
}

internal fun extensionEntranceDelayMillis(index: Int): Int =
    index.coerceAtLeast(0) * EXTENSION_CARD_ENTRANCE_STAGGER_MS

internal fun extensionBadgeStartDelayMillis(index: Int): Int =
    extensionEntranceDelayMillis(index) + EXTENSION_CARD_ENTRANCE_DURATION_MS

internal fun extensionEntranceAlpha(progress: Float): Float =
    progress.coerceIn(0f, 1f)

internal fun extensionEntranceTranslationYPx(progress: Float): Float =
    EXTENSION_CARD_ENTRANCE_OFFSET_PX * (1f - progress.coerceIn(0f, 1f))

@Composable
private fun ResizableExtensionName(
    text: String,
    modifier: Modifier = Modifier,
    maxFontSize: TextUnit = MaterialTheme.typography.titleLarge.fontSize,
) {
    val minFontSize = 12.sp

    BoxWithConstraints(modifier = modifier) {
        var fontSize by remember(text, maxFontSize, maxWidth) { mutableStateOf(maxFontSize) }

        Text(
            text = text,
            style = MaterialTheme.typography.titleLarge.copy(
                fontSize = fontSize,
                lineHeight = (fontSize.value * 1.18f).sp,
            ),
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            onTextLayout = { result ->
                if (result.hasVisualOverflow && fontSize.value > minFontSize.value) {
                    fontSize = (fontSize.value - 1f).coerceAtLeast(minFontSize.value).sp
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
