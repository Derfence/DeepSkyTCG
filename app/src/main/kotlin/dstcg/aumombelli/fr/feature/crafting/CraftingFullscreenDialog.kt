package fr.aumombelli.dstcg.feature.crafting

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import fr.aumombelli.dstcg.app.NewPlayerOnboardingTarget
import fr.aumombelli.dstcg.model.CraftingCardCandidate
import fr.aumombelli.dstcg.model.CraftingMode
import fr.aumombelli.dstcg.model.toDisplayCard
import fr.aumombelli.dstcg.ui.component.AstroCardDetailsSurface
import fr.aumombelli.dstcg.ui.component.AstroCardFullscreenCloseButton
import fr.aumombelli.dstcg.ui.component.DisplayCardVariantSelector
import fr.aumombelli.dstcg.ui.screen.dstcgContentInsetsPadding
import fr.aumombelli.dstcg.ui.theme.SkyQualityPalette
import fr.aumombelli.dstcg.ui.theme.skyQualityPalette
import kotlinx.coroutines.delay

@Composable
internal fun CraftingFullscreenDialog(
    group: CraftingCardGroup,
    mode: CraftingMode,
    selectedVariantKey: String?,
    completion: CraftingCompletion?,
    isApplying: Boolean,
    onVariantSelected: (String) -> Unit,
    onApplyCrafting: (CraftingCardCandidate) -> Unit,
    onCoachmarkTargetBoundsChanged: (NewPlayerOnboardingTarget, Rect?) -> Unit,
    onDismiss: () -> Unit,
) {
    val selectedCandidate = group.candidates.firstOrNull { it.sourceVariant.key == selectedVariantKey }
        ?: group.candidates.firstOrNull()
        ?: return
    var completedAnimationId by remember(group.cardId, selectedCandidate.sourceVariant.key) { mutableStateOf<Int?>(null) }
    val stampProgress = remember { Animatable(0f) }
    val borderProgress = remember { Animatable(0f) }
    val matchingCompletion = completion?.takeIf { completed ->
        completed.mode == mode &&
            completed.recipe.source == selectedCandidate.sourceRef &&
            completed.recipe.target == selectedCandidate.targetRef
    }
    val hasCompletedAnimation = matchingCompletion != null && completedAnimationId == matchingCompletion.id
    val isCompletionAnimating = matchingCompletion != null && !hasCompletedAnimation
    val isInteractionLocked = isApplying || isCompletionAnimating

    LaunchedEffect(matchingCompletion?.id) {
        completedAnimationId = null
        stampProgress.snapTo(0f)
        borderProgress.snapTo(0f)
        val currentCompletion = matchingCompletion ?: return@LaunchedEffect
        when (currentCompletion.mode) {
            CraftingMode.DarkenSky -> {
                borderProgress.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 900, easing = LinearEasing),
                )
                completedAnimationId = currentCompletion.id
            }

            CraftingMode.SpaceAgency -> {
                stampProgress.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 560, easing = FastOutSlowInEasing),
                )
                delay(120)
                completedAnimationId = currentCompletion.id
            }
        }
    }

    val activeVariant = if (hasCompletedAnimation) {
        selectedCandidate.targetVariant.copy(count = selectedCandidate.targetVariant.count + 1)
    } else {
        selectedCandidate.sourceVariant
    }
    val displayCard = selectedCandidate.card.toDisplayCard(
        extensionName = selectedCandidate.extensionName,
        activeVariant = activeVariant,
        availableVariants = group.availableVariants,
    )
    val sourcePalette = skyQualityPalette(selectedCandidate.sourceVariant.skyQuality)
    val targetPalette = skyQualityPalette(selectedCandidate.targetVariant.skyQuality)
    val cardPalette = if (mode == CraftingMode.DarkenSky) {
        lerpSkyQualityPalette(sourcePalette, targetPalette, borderProgress.value)
    } else {
        null
    }
    val borderColor = if (mode == CraftingMode.DarkenSky) {
        cardPalette?.glow ?: sourcePalette.glow
    } else {
        if (hasCompletedAnimation) Color(0xFFFFD36C) else Color.Transparent
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnClickOutside = !isInteractionLocked,
            usePlatformDefaultWidth = false,
        ),
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xE608101A))
                .dstcgContentInsetsPadding(includeBottom = true)
                .padding(14.dp)
                .testTag("crafting-fullscreen"),
        ) {
            val previewMaxHeight = craftingPreviewMaxHeight(maxHeight)

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 42.dp)
                    .border(
                        width = 4.dp,
                        color = borderColor,
                        shape = RoundedCornerShape(34.dp),
                    )
                    .testTag("crafting-card-border"),
            ) {
                AstroCardDetailsSurface(
                    displayCard = displayCard,
                    modifier = Modifier.fillMaxSize(),
                    paletteOverride = cardPalette,
                    previewMaxHeight = previewMaxHeight,
                    accessoryContent = {
                        if (!hasCompletedAnimation && !isInteractionLocked) {
                            DisplayCardVariantSelector(
                                variants = group.availableVariants,
                                selectedVariantKey = selectedCandidate.sourceVariant.key,
                                onVariantSelected = { variant -> onVariantSelected(variant.key) },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                        CraftingConfirmationPanel(
                            mode = mode,
                            candidate = selectedCandidate,
                            completed = hasCompletedAnimation,
                            isApplying = isInteractionLocked,
                            onApplyCrafting = { onApplyCrafting(selectedCandidate) },
                            onCoachmarkTargetBoundsChanged = onCoachmarkTargetBoundsChanged,
                        )
                    },
                )
                if (mode == CraftingMode.SpaceAgency && stampProgress.value > 0f && !hasCompletedAnimation) {
                    StampingOverlay(
                        progress = stampProgress.value,
                        modifier = Modifier.align(Alignment.Center),
                    )
                }
            }
            AstroCardFullscreenCloseButton(onClick = onDismiss)
        }
    }
}

private fun craftingPreviewMaxHeight(viewportHeight: Dp): Dp {
    val maxAfterActionArea = (viewportHeight - CraftingActionAreaReservedHeight).coerceAtLeast(
        CraftingPreviewMinimumHeight,
    )
    val maxByViewport = viewportHeight * CraftingPreviewViewportFraction
    return minOf(maxAfterActionArea, maxByViewport)
}

@Composable
private fun CraftingConfirmationPanel(
    mode: CraftingMode,
    candidate: CraftingCardCandidate,
    completed: Boolean,
    isApplying: Boolean,
    onApplyCrafting: () -> Unit,
    onCoachmarkTargetBoundsChanged: (NewPlayerOnboardingTarget, Rect?) -> Unit,
) {
    Surface(
        color = Color.Black.copy(alpha = 0.32f),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("crafting-confirmation"),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(16.dp),
        ) {
            Text(
                text = mode.title(),
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "Consomme ${candidate.consumedCount} x ${candidate.sourceVariant.skyQualityLabel} · ${candidate.sourceVariant.finishLabel}",
                color = Color(0xFFD3E3F3),
                modifier = Modifier.testTag("crafting-consumed-text"),
            )
            Text(
                text = "Crée 1 x ${candidate.targetVariant.skyQualityLabel} · ${candidate.targetVariant.finishLabel}",
                color = Color(0xFFD3E3F3),
                modifier = Modifier.testTag("crafting-created-text"),
            )
            Spacer(modifier = Modifier.height(4.dp))
            Button(
                onClick = onApplyCrafting,
                enabled = !completed && !isApplying,
                modifier = Modifier
                    .fillMaxWidth()
                    .onGloballyPositioned { coordinates ->
                        if (!completed && !isApplying && mode == CraftingMode.DarkenSky) {
                            onCoachmarkTargetBoundsChanged(
                                NewPlayerOnboardingTarget.CraftingConfirm,
                                coordinates.boundsInRoot(),
                            )
                        }
                    }
                    .testTag("crafting-confirm"),
            ) {
                if (isApplying) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                    )
                }
                Text(
                    text = when {
                        isApplying -> "En cours..."
                        completed -> "Termine"
                        else -> mode.confirmLabel()
                    },
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }
    }
}

private val CraftingActionAreaReservedHeight = 280.dp
private val CraftingPreviewMinimumHeight = 180.dp
private const val CraftingPreviewViewportFraction = 0.68f

@Composable
private fun StampingOverlay(
    progress: Float,
    modifier: Modifier = Modifier,
) {
    val alpha = 0.35f + 0.65f * progress.coerceIn(0f, 1f)
    val scale = 1.2f - 0.2f * alpha
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(width = 210.dp * scale, height = 78.dp * scale)
            .testTag("crafting-stamp-animation"),
    ) {
        Surface(
            color = Color(0x22FF4848),
            contentColor = Color(0xFFFFD4D4),
            shape = RoundedCornerShape(10.dp),
            border = androidx.compose.foundation.BorderStroke(3.dp, Color(0xFFFF6969).copy(alpha = alpha)),
            modifier = Modifier
                .fillMaxSize()
                .rotate(-12f)
                .alpha(alpha),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = "TAMPONNEE",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                )
            }
        }
    }
}

private fun lerpSkyQualityPalette(
    source: SkyQualityPalette,
    target: SkyQualityPalette,
    fraction: Float,
): SkyQualityPalette {
    val progress = fraction.coerceIn(0f, 1f)
    return SkyQualityPalette(
        top = lerp(source.top, target.top, progress),
        bottom = lerp(source.bottom, target.bottom, progress),
        glow = lerp(source.glow, target.glow, progress),
        mist = lerp(source.mist, target.mist, progress),
    )
}
