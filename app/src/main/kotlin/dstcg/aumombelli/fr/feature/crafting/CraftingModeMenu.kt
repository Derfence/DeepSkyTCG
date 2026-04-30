package fr.aumombelli.dstcg.feature.crafting

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import fr.aumombelli.dstcg.app.NewPlayerOnboardingTarget
import fr.aumombelli.dstcg.model.CraftingMode
import fr.aumombelli.dstcg.ui.screen.dstcgBottomInsetsPadding
import fr.aumombelli.dstcg.ui.screen.dstcgContentInsetsPadding

@Composable
internal fun CraftingModeMenu(
    onSelectMode: (CraftingMode) -> Unit,
    onBackHome: () -> Unit,
    onCoachmarkTargetBoundsChanged: (NewPlayerOnboardingTarget, Rect?) -> Unit,
    modifier: Modifier = Modifier,
) {
    var darkenSkyBounds by remember { mutableStateOf<Rect?>(null) }
    var darkenSkyDescriptionBounds by remember { mutableStateOf<Rect?>(null) }

    LaunchedEffect(darkenSkyBounds, darkenSkyDescriptionBounds) {
        val modeBounds = darkenSkyBounds ?: return@LaunchedEffect
        val interactionTop = darkenSkyDescriptionBounds
            ?.bottom
            ?.coerceIn(modeBounds.top, modeBounds.bottom)
            ?: modeBounds.top
        onCoachmarkTargetBoundsChanged(
            NewPlayerOnboardingTarget.CraftingDarkenSkyMode,
            Rect(
                left = modeBounds.left,
                top = interactionTop,
                right = modeBounds.right,
                bottom = modeBounds.bottom,
            ),
        )
    }

    Box(modifier = modifier) {
        Column(modifier = Modifier.fillMaxSize()) {
            CraftingModeHalfButton(
                mode = CraftingMode.DarkenSky,
                copyPlacement = CraftingModeCopyPlacement.Top,
                svgSlotTestTag = "crafting-mode-darken-sky-svg-slot",
                background = Brush.verticalGradient(
                    listOf(Color(0xFF091523), Color(0xFF132A42)),
                ),
                onClick = { onSelectMode(CraftingMode.DarkenSky) },
                onDescriptionBoundsChanged = { bounds ->
                    darkenSkyDescriptionBounds = bounds
                },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .onGloballyPositioned { coordinates ->
                        darkenSkyBounds = coordinates.boundsInRoot()
                    }
                    .testTag("crafting-mode-darken-sky"),
            )
            CraftingModeHalfButton(
                mode = CraftingMode.SpaceAgency,
                copyPlacement = CraftingModeCopyPlacement.Bottom,
                svgSlotTestTag = "crafting-mode-space-agency-svg-slot",
                background = Brush.verticalGradient(
                    listOf(Color(0xFF172331), Color(0xFF07111B)),
                ),
                onClick = { onSelectMode(CraftingMode.SpaceAgency) },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .testTag("crafting-mode-space-agency"),
            )
        }
        IconButton(
            onClick = onBackHome,
            modifier = Modifier
                .align(Alignment.TopStart)
                .dstcgContentInsetsPadding()
                .padding(start = 8.dp, top = 8.dp)
                .testTag("crafting-back-home"),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Retour",
                tint = Color.White,
            )
        }
    }
}

@Composable
private fun CraftingModeHalfButton(
    mode: CraftingMode,
    copyPlacement: CraftingModeCopyPlacement,
    svgSlotTestTag: String,
    background: Brush,
    onClick: () -> Unit,
    onDescriptionBoundsChanged: (Rect?) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .background(background)
            .clickable(onClick = onClick),
    ) {
        val graphicModifier = Modifier
            .fillMaxSize()
        when (mode) {
            CraftingMode.DarkenSky -> DarkenSkyKeogram(
                testTag = svgSlotTestTag,
                modifier = graphicModifier,
            )

            CraftingMode.SpaceAgency -> SpaceAgencyLaunchTower(
                testTag = svgSlotTestTag,
                modifier = graphicModifier,
            )
        }
        CraftingModeCopy(
            mode = mode,
            placement = copyPlacement,
            onDescriptionBoundsChanged = onDescriptionBoundsChanged,
        )
        CraftingModeActionHint(
            mode = mode,
            placement = copyPlacement,
        )
    }
}

@Composable
private fun BoxScope.CraftingModeActionHint(
    mode: CraftingMode,
    placement: CraftingModeCopyPlacement,
) {
    val modifier = when (placement) {
        CraftingModeCopyPlacement.Top -> Modifier
            .align(Alignment.BottomEnd)
            .padding(end = 22.dp, bottom = 18.dp)

        CraftingModeCopyPlacement.Bottom -> Modifier
            .align(Alignment.BottomEnd)
            .dstcgBottomInsetsPadding()
            .padding(end = 22.dp, bottom = 24.dp)
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(46.dp)
            .clip(RoundedCornerShape(50))
            .background(Color(0xB007111B))
            .testTag("crafting-mode-action-hint-${mode.name}"),
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(24.dp),
        )
    }
}

@Composable
private fun BoxScope.CraftingModeCopy(
    mode: CraftingMode,
    placement: CraftingModeCopyPlacement,
    onDescriptionBoundsChanged: (Rect?) -> Unit = {},
) {
    val alignment = when (placement) {
        CraftingModeCopyPlacement.Top -> Alignment.TopEnd
        CraftingModeCopyPlacement.Bottom -> Alignment.BottomStart
    }
    val modifier = when (placement) {
        CraftingModeCopyPlacement.Top -> Modifier
            .align(alignment)
            .fillMaxWidth()
            .dstcgContentInsetsPadding()
            .padding(start = 72.dp, top = 18.dp, end = 22.dp)

        CraftingModeCopyPlacement.Bottom -> Modifier
            .align(alignment)
            .fillMaxWidth()
            .dstcgBottomInsetsPadding()
            .padding(start = 22.dp, end = 22.dp, bottom = 24.dp)
    }
    val horizontalAlignment = when (placement) {
        CraftingModeCopyPlacement.Top -> Alignment.End
        CraftingModeCopyPlacement.Bottom -> Alignment.Start
    }
    val textAlign = when (placement) {
        CraftingModeCopyPlacement.Top -> TextAlign.End
        CraftingModeCopyPlacement.Bottom -> TextAlign.Start
    }
    val copyAlignment = when (placement) {
        CraftingModeCopyPlacement.Top -> Alignment.CenterEnd
        CraftingModeCopyPlacement.Bottom -> Alignment.CenterStart
    }
    val copyBackgroundShape = RoundedCornerShape(8.dp)

    Box(
        modifier = modifier.testTag("crafting-mode-copy-${mode.name}"),
    ) {
        Column(
            horizontalAlignment = horizontalAlignment,
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .align(copyAlignment)
                .fillMaxWidth(0.88f)
                .clip(copyBackgroundShape)
                .background(Color(0x9907111B))
                .padding(horizontal = 14.dp, vertical = 10.dp),
        ) {
            Text(
                text = mode.title(),
                color = Color.White,
                textAlign = textAlign,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                text = mode.subtitle(),
                color = Color(0xFFD3E3F3),
                textAlign = textAlign,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .fillMaxWidth()
                    .onGloballyPositioned { coordinates ->
                        onDescriptionBoundsChanged(coordinates.boundsInRoot())
                    },
            )
        }
    }
}

private enum class CraftingModeCopyPlacement {
    Top,
    Bottom,
}
