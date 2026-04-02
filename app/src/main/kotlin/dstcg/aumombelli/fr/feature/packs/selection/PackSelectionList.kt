package fr.aumombelli.dstcg.feature.packs.selection

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import fr.aumombelli.dstcg.model.ExtensionDefinition
import fr.aumombelli.dstcg.ui.motion.MotionCard

internal val EXTENSION_CARD_HEIGHT: Dp = 164.dp
internal val EXTENSION_CARD_SPACING: Dp = 12.dp
internal val EXTENSION_LIST_TOP_PADDING: Dp = 44.dp

@Composable
internal fun ExtensionList(
    extensions: List<ExtensionDefinition>,
    drawLocked: Boolean,
    onSelectExtension: (String) -> Unit,
    interactionsEnabled: Boolean,
    highlightedExtensionId: String?,
    highlightProgress: Float,
    badgeAnimationsEnabled: Boolean,
    onFirstEnabledExtensionBoundsChanged: (Rect?) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val firstEnabledExtensionId = if (!drawLocked) {
        extensions.firstOrNull()?.id
    } else {
        null
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier,
    ) {
        extensions.forEachIndexed { index, extension ->
            val alpha = when {
                highlightedExtensionId == null -> 1f
                extension.id == highlightedExtensionId -> (1f - highlightProgress * 4f).coerceIn(0f, 1f)
                else -> (1f - highlightProgress).coerceIn(0f, 1f)
            }
            MotionCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(EXTENSION_CARD_HEIGHT)
                    .graphicsLayer {
                        this.alpha = alpha
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
                        Text(
                            text = extension.name,
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f),
                        )
                        ExtensionAnimatedBadge(
                            extensionId = extension.id,
                            animationsEnabled = badgeAnimationsEnabled,
                            startDelayMillis = 760 + index * 90,
                            modifier = Modifier
                                .padding(start = 12.dp)
                                .width(78.dp)
                                .height(54.dp),
                        )
                    }
                    Button(
                        onClick = { onSelectExtension(extension.id) },
                        enabled = !drawLocked && interactionsEnabled,
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
                        Text(if (drawLocked) "Verrouillé" else "Observer")
                    }
                }
            }
        }
    }
}
