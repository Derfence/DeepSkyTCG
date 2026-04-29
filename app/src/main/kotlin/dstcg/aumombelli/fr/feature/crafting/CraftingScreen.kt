package fr.aumombelli.dstcg.feature.crafting

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import fr.aumombelli.dstcg.app.NewPlayerOnboardingTarget
import fr.aumombelli.dstcg.model.CraftingCardCandidate
import fr.aumombelli.dstcg.model.CraftingMode
import fr.aumombelli.dstcg.model.DisplayCard
import fr.aumombelli.dstcg.model.LibraryCardItem
import fr.aumombelli.dstcg.model.toDisplayCard
import fr.aumombelli.dstcg.ui.component.AstroCardDetailsSurface
import fr.aumombelli.dstcg.ui.component.AstroCardFullscreenCloseButton
import fr.aumombelli.dstcg.ui.component.AstroCardThumbnail
import fr.aumombelli.dstcg.ui.component.DisplayCardVariantSelector
import fr.aumombelli.dstcg.ui.screen.dstcgBottomInsetsPadding
import fr.aumombelli.dstcg.ui.screen.dstcgContentInsetsPadding
import fr.aumombelli.dstcg.ui.theme.SkyQualityPalette
import fr.aumombelli.dstcg.ui.theme.skyQualityPalette
import kotlinx.coroutines.delay

@Composable
fun CraftingScreen(
    state: CraftingUiState,
    onRefresh: () -> Unit,
    onSelectMode: (CraftingMode) -> Unit,
    onBackHome: () -> Unit,
    onBackToModes: () -> Unit,
    onApplyCrafting: (CraftingCardCandidate) -> Unit,
    contentVisible: Boolean = true,
    onCoachmarkTargetBoundsChanged: (NewPlayerOnboardingTarget, Rect?) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier,
) {
    val contentAlpha by animateFloatAsState(
        targetValue = if (contentVisible) 1f else 0f,
        animationSpec = tween(durationMillis = 520, easing = FastOutSlowInEasing),
        label = "crafting-content-alpha",
    )
    var selectedGroup by remember { mutableStateOf<CraftingCardGroup?>(null) }
    var selectedVariantKey by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(state.selectedMode) {
        selectedGroup = null
        selectedVariantKey = null
    }

    LaunchedEffect(contentVisible, state.selectedMode, state.sections, state.completion, state.isApplying, selectedGroup) {
        if (!contentVisible || state.selectedMode != null) {
            onCoachmarkTargetBoundsChanged(NewPlayerOnboardingTarget.CraftingDarkenSkyMode, null)
        }
        if (!contentVisible || state.selectedMode == null || state.sections.isEmpty() || selectedGroup != null) {
            onCoachmarkTargetBoundsChanged(NewPlayerOnboardingTarget.CraftingCandidate, null)
        }
        if (!contentVisible || selectedGroup == null || state.completion != null || state.isApplying) {
            onCoachmarkTargetBoundsChanged(NewPlayerOnboardingTarget.CraftingConfirm, null)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .alpha(contentAlpha)
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF07111B), Color(0xFF142638), Color(0xFF243344)),
                ),
            )
            .testTag("crafting-screen"),
    ) {
        if (state.selectedMode == null) {
            CraftingModeMenu(
                onSelectMode = onSelectMode,
                onBackHome = onBackHome,
                onCoachmarkTargetBoundsChanged = onCoachmarkTargetBoundsChanged,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
                CraftingCandidateLibrary(
                    state = state,
                    onRefresh = onRefresh,
                    onBackToModes = onBackToModes,
                    onOpenGroup = { group ->
                    selectedGroup = group
                        selectedVariantKey = group.candidates.firstOrNull()?.sourceVariant?.key
                    },
                    onCoachmarkTargetBoundsChanged = onCoachmarkTargetBoundsChanged,
                    modifier = Modifier
                    .fillMaxSize()
                    .dstcgContentInsetsPadding(includeBottom = true),
            )
        }
    }

    val group = selectedGroup
    if (group != null && state.selectedMode != null) {
        CraftingFullscreenDialog(
            group = group,
            mode = state.selectedMode,
            selectedVariantKey = selectedVariantKey,
            completion = state.completion,
            isApplying = state.isApplying,
            onVariantSelected = { selectedVariantKey = it },
            onApplyCrafting = onApplyCrafting,
            onCoachmarkTargetBoundsChanged = onCoachmarkTargetBoundsChanged,
            onDismiss = {
                selectedGroup = null
                selectedVariantKey = null
            },
        )
    }
}

@Composable
private fun CraftingModeMenu(
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
private fun DarkenSkyKeogram(
    testTag: String,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(4.dp)
    Canvas(
        modifier = modifier
            .clip(shape)
            .testTag(testTag),
    ) {
        val panelCount = KeogramPanels.size
        val panelWidth = size.width / panelCount
        val cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
        drawRoundRect(
            color = Color.White.copy(alpha = 0.12f),
            cornerRadius = cornerRadius,
            style = Stroke(width = 1.dp.toPx()),
        )

        KeogramPanels.forEachIndexed { index, panel ->
            val left = panelWidth * index
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(panel.zenithColor, panel.midSkyColor, panel.horizonGlowColor),
                    startY = 0f,
                    endY = size.height,
                ),
                topLeft = Offset(left, 0f),
                size = androidx.compose.ui.geometry.Size(panelWidth + 1f, size.height),
            )
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        panel.lightPollutionColor,
                    ),
                    startY = 0f,
                    endY = size.height,
                ),
                topLeft = Offset(left, 0f),
                size = androidx.compose.ui.geometry.Size(panelWidth + 1f, size.height),
            )
            repeat(panel.starCount) { starIndex ->
                val star = KeogramStars[(starIndex * 7 + index * 11) % KeogramStars.size]
                val starCenter = Offset(
                    x = left + panelWidth * star.x,
                    y = size.height * star.y,
                )
                val radius = size.minDimension * star.radius
                drawCircle(
                    color = Color.White.copy(alpha = star.alpha),
                    radius = radius,
                    center = starCenter,
                )
            }
            if (index > 0) {
                drawLine(
                    color = Color.White.copy(alpha = 0.20f),
                    start = Offset(left, 0f),
                    end = Offset(left, size.height),
                    strokeWidth = 1.dp.toPx(),
                )
            }
        }
        drawRoundRect(
            color = Color.White.copy(alpha = 0.18f),
            cornerRadius = cornerRadius,
            style = Stroke(width = 1.dp.toPx()),
        )
    }
}

@Composable
private fun SpaceAgencyLaunchTower(
    testTag: String,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(4.dp)
    Canvas(
        modifier = modifier
            .clip(shape)
            .testTag(testTag),
    ) {
        val cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF07111B),
                    Color(0xFF102033),
                    Color(0xFF050910),
                ),
                startY = 0f,
                endY = size.height,
            ),
        )
        drawCircle(
            color = Color(0x226CA7FF),
            radius = size.minDimension * 0.44f,
            center = Offset(size.width * 0.62f, size.height * 0.22f),
        )
        drawCircle(
            color = Color(0x18FFFFFF),
            radius = size.minDimension * 0.08f,
            center = Offset(size.width * 0.82f, size.height * 0.18f),
        )

        val groundY = size.height * 0.88f
        drawAgencyBuilding(
            left = size.width * 0.08f,
            bottom = groundY,
            width = size.width * 0.15f,
            height = size.height * 0.16f,
            bodyColor = Color(0xFF162C43),
            windowColumns = 2,
        )
        drawAgencyBuilding(
            left = size.width * 0.22f,
            bottom = groundY,
            width = size.width * 0.12f,
            height = size.height * 0.24f,
            bodyColor = Color(0xFF1C3854),
            windowColumns = 1,
        )
        drawAgencyBuilding(
            left = size.width * 0.66f,
            bottom = groundY,
            width = size.width * 0.13f,
            height = size.height * 0.22f,
            bodyColor = Color(0xFF18324D),
            windowColumns = 2,
        )
        drawAgencyBuilding(
            left = size.width * 0.79f,
            bottom = groundY,
            width = size.width * 0.14f,
            height = size.height * 0.13f,
            bodyColor = Color(0xFF13283D),
            windowColumns = 2,
        )
        drawRect(
            color = Color(0xFF09131E).copy(alpha = 0.82f),
            topLeft = Offset(0f, groundY),
            size = Size(size.width, size.height - groundY),
        )

        val towerTop = size.height * 0.12f
        val towerBottom = size.height * 0.87f
        val towerHeight = towerBottom - towerTop
        val towerWidth = size.width * 0.30f
        val towerLeft = (size.width - towerWidth) / 2f
        val towerRight = towerLeft + towerWidth
        val towerCenterX = (towerLeft + towerRight) / 2f
        val towerStroke = size.minDimension * 0.018f
        val towerColor = Color(0xFFE4EDF7).copy(alpha = 0.82f)
        val towerFill = Color(0xFF294865).copy(alpha = 0.82f)

        drawRoundRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF416B8E).copy(alpha = 0.72f),
                    towerFill,
                    Color(0xFF142A40).copy(alpha = 0.92f),
                ),
                startY = towerTop,
                endY = towerBottom,
            ),
            topLeft = Offset(towerLeft, towerTop),
            size = Size(towerWidth, towerHeight),
            cornerRadius = CornerRadius(towerStroke * 1.2f, towerStroke * 1.2f),
        )
        drawRoundRect(
            color = towerColor.copy(alpha = 0.58f),
            topLeft = Offset(towerLeft, towerTop),
            size = Size(towerWidth, towerHeight),
            cornerRadius = CornerRadius(towerStroke * 1.2f, towerStroke * 1.2f),
            style = Stroke(width = towerStroke * 0.82f),
        )
        listOf(0.32f, 0.55f, 0.76f).forEach { fraction ->
            val y = towerTop + towerHeight * fraction
            drawRoundRect(
                color = towerColor.copy(alpha = 0.64f),
                topLeft = Offset(towerLeft - towerWidth * 0.08f, y),
                size = Size(towerWidth * 1.16f, towerStroke * 0.72f),
                cornerRadius = CornerRadius(towerStroke, towerStroke),
            )
        }

        drawRoundRect(
            color = towerColor.copy(alpha = 0.78f),
            topLeft = Offset(towerCenterX - towerWidth * 0.12f, towerTop - towerHeight * 0.045f),
            size = Size(towerWidth * 0.24f, towerHeight * 0.06f),
            cornerRadius = CornerRadius(towerStroke, towerStroke),
        )
        drawLine(
            color = towerColor.copy(alpha = 0.72f),
            start = Offset(towerCenterX, towerTop - towerHeight * 0.045f),
            end = Offset(towerCenterX, towerTop - towerHeight * 0.12f),
            strokeWidth = towerStroke * 0.42f,
        )

        val logoRadius = towerWidth * 0.15f
        val logoCenter = Offset(towerCenterX, towerTop + towerHeight * 0.20f)
        drawCircle(
            color = Color(0xFF07111B).copy(alpha = 0.84f),
            radius = logoRadius,
            center = logoCenter,
        )
        drawCircle(
            color = Color(0xFF9EE7FF).copy(alpha = 0.30f),
            radius = logoRadius,
            center = logoCenter,
            style = Stroke(width = towerStroke * 0.86f),
        )
        drawCircle(
            color = Color(0x336CA7FF),
            radius = logoRadius * 0.72f,
            center = Offset(logoCenter.x, logoCenter.y + logoRadius * 0.10f),
        )

        drawRocketLogo(
            center = Offset(logoCenter.x, logoCenter.y - logoRadius * 0.06f),
            radius = logoRadius * 0.82f,
        )

        drawRoundRect(
            color = Color.White.copy(alpha = 0.18f),
            cornerRadius = cornerRadius,
            style = Stroke(width = 1.dp.toPx()),
        )
    }
}

private fun DrawScope.drawAgencyBuilding(
    left: Float,
    bottom: Float,
    width: Float,
    height: Float,
    bodyColor: Color,
    windowColumns: Int,
) {
    val top = bottom - height
    val cornerRadius = CornerRadius(width * 0.08f, width * 0.08f)
    drawRoundRect(
        color = bodyColor.copy(alpha = 0.88f),
        topLeft = Offset(left, top),
        size = Size(width, height),
        cornerRadius = cornerRadius,
    )
    drawRoundRect(
        color = Color.White.copy(alpha = 0.12f),
        topLeft = Offset(left, top),
        size = Size(width, height),
        cornerRadius = cornerRadius,
        style = Stroke(width = width * 0.035f),
    )
    val windowSize = width * 0.13f
    val rowCount = 3
    repeat(windowColumns) { column ->
        repeat(rowCount) { row ->
            drawRoundRect(
                color = Color(0xFF9EE7FF).copy(alpha = 0.28f),
                topLeft = Offset(
                    x = left + width * (0.28f + column * 0.34f),
                    y = top + height * (0.28f + row * 0.18f),
                ),
                size = Size(windowSize, windowSize * 1.16f),
                cornerRadius = CornerRadius(windowSize * 0.22f, windowSize * 0.22f),
            )
        }
    }
}

private fun DrawScope.drawRocketLogo(
    center: Offset,
    radius: Float,
) {
    val rocketPath = Path().apply {
        moveTo(center.x, center.y - radius * 0.62f)
        cubicTo(
            center.x + radius * 0.36f,
            center.y - radius * 0.36f,
            center.x + radius * 0.32f,
            center.y + radius * 0.14f,
            center.x + radius * 0.10f,
            center.y + radius * 0.40f,
        )
        lineTo(center.x + radius * 0.04f, center.y + radius * 0.54f)
        lineTo(center.x - radius * 0.04f, center.y + radius * 0.54f)
        lineTo(center.x - radius * 0.10f, center.y + radius * 0.40f)
        cubicTo(
            center.x - radius * 0.32f,
            center.y + radius * 0.14f,
            center.x - radius * 0.36f,
            center.y - radius * 0.36f,
            center.x,
            center.y - radius * 0.62f,
        )
        close()
    }
    val leftFin = Path().apply {
        moveTo(center.x - radius * 0.10f, center.y + radius * 0.25f)
        lineTo(center.x - radius * 0.34f, center.y + radius * 0.56f)
        lineTo(center.x - radius * 0.08f, center.y + radius * 0.44f)
        close()
    }
    val rightFin = Path().apply {
        moveTo(center.x + radius * 0.10f, center.y + radius * 0.25f)
        lineTo(center.x + radius * 0.34f, center.y + radius * 0.56f)
        lineTo(center.x + radius * 0.08f, center.y + radius * 0.44f)
        close()
    }
    val flame = Path().apply {
        moveTo(center.x, center.y + radius * 0.52f)
        cubicTo(
            center.x + radius * 0.13f,
            center.y + radius * 0.76f,
            center.x + radius * 0.04f,
            center.y + radius * 0.96f,
            center.x,
            center.y + radius * 1.06f,
        )
        cubicTo(
            center.x - radius * 0.04f,
            center.y + radius * 0.96f,
            center.x - radius * 0.13f,
            center.y + radius * 0.76f,
            center.x,
            center.y + radius * 0.52f,
        )
        close()
    }

    listOf(-0.28f, 0f, 0.28f).forEach { offset ->
        drawLine(
            color = Color.White.copy(alpha = 0.20f),
            start = Offset(center.x + radius * offset, center.y + radius * 0.72f),
            end = Offset(center.x + radius * offset, center.y + radius * 1.34f),
            strokeWidth = radius * 0.018f,
        )
    }
    drawPath(
        path = leftFin,
        color = Color(0xFF74D9FF).copy(alpha = 0.88f),
    )
    drawPath(
        path = rightFin,
        color = Color(0xFF74D9FF).copy(alpha = 0.88f),
    )
    drawPath(
        path = flame,
        color = Color(0xFFFFB35C).copy(alpha = 0.86f),
    )
    drawPath(
        path = rocketPath,
        color = Color.White.copy(alpha = 0.94f),
    )
    drawPath(
        path = rocketPath,
        color = Color(0xFF9EE7FF).copy(alpha = 0.34f),
        style = Stroke(width = radius * 0.045f),
    )
    drawCircle(
        color = Color(0xFF07111B).copy(alpha = 0.72f),
        radius = radius * 0.11f,
        center = Offset(center.x, center.y - radius * 0.18f),
    )
    drawCircle(
        color = Color(0xFF9EE7FF).copy(alpha = 0.88f),
        radius = radius * 0.065f,
        center = Offset(center.x, center.y - radius * 0.18f),
    )
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

private data class KeogramPanel(
    val zenithColor: Color,
    val midSkyColor: Color,
    val horizonGlowColor: Color,
    val lightPollutionColor: Color,
    val starCount: Int,
)

private data class KeogramStar(
    val x: Float,
    val y: Float,
    val radius: Float,
    val alpha: Float,
)

private val KeogramPanels = listOf(
    KeogramPanel(
        zenithColor = Color(0xFF312C49),
        midSkyColor = Color(0xFF5F5148),
        horizonGlowColor = Color(0xFFD0A35B),
        lightPollutionColor = Color(0x55F7CE78),
        starCount = 5,
    ),
    KeogramPanel(
        zenithColor = Color(0xFF151B36),
        midSkyColor = Color(0xFF363650),
        horizonGlowColor = Color(0xFF9B7158),
        lightPollutionColor = Color(0x3FD49A6A),
        starCount = 10,
    ),
    KeogramPanel(
        zenithColor = Color(0xFF061222),
        midSkyColor = Color(0xFF0D3156),
        horizonGlowColor = Color(0xFF255F79),
        lightPollutionColor = Color(0x2A6FBAC9),
        starCount = 17,
    ),
    KeogramPanel(
        zenithColor = Color(0xFF01030A),
        midSkyColor = Color(0xFF06142B),
        horizonGlowColor = Color(0xFF102F4A),
        lightPollutionColor = Color(0x1F6AA9FF),
        starCount = 28,
    ),
)

private val KeogramStars = listOf(
    KeogramStar(0.17f, 0.11f, 0.005f, 0.74f),
    KeogramStar(0.83f, 0.64f, 0.004f, 0.58f),
    KeogramStar(0.46f, 0.27f, 0.006f, 0.80f),
    KeogramStar(0.29f, 0.79f, 0.004f, 0.60f),
    KeogramStar(0.72f, 0.39f, 0.005f, 0.70f),
    KeogramStar(0.09f, 0.52f, 0.003f, 0.48f),
    KeogramStar(0.61f, 0.93f, 0.003f, 0.46f),
    KeogramStar(0.37f, 0.72f, 0.004f, 0.66f),
    KeogramStar(0.91f, 0.19f, 0.005f, 0.68f),
    KeogramStar(0.53f, 0.58f, 0.004f, 0.62f),
    KeogramStar(0.22f, 0.35f, 0.004f, 0.64f),
    KeogramStar(0.78f, 0.86f, 0.003f, 0.50f),
    KeogramStar(0.34f, 0.05f, 0.005f, 0.76f),
    KeogramStar(0.66f, 0.70f, 0.004f, 0.58f),
    KeogramStar(0.12f, 0.42f, 0.004f, 0.56f),
    KeogramStar(0.88f, 0.96f, 0.003f, 0.42f),
    KeogramStar(0.48f, 0.83f, 0.004f, 0.56f),
    KeogramStar(0.74f, 0.08f, 0.006f, 0.82f),
    KeogramStar(0.19f, 0.90f, 0.003f, 0.44f),
    KeogramStar(0.58f, 0.18f, 0.004f, 0.68f),
    KeogramStar(0.95f, 0.48f, 0.003f, 0.52f),
    KeogramStar(0.41f, 0.90f, 0.003f, 0.48f),
    KeogramStar(0.06f, 0.24f, 0.004f, 0.60f),
    KeogramStar(0.69f, 0.54f, 0.004f, 0.64f),
    KeogramStar(0.31f, 0.60f, 0.003f, 0.52f),
    KeogramStar(0.86f, 0.31f, 0.004f, 0.62f),
    KeogramStar(0.15f, 0.68f, 0.003f, 0.50f),
    KeogramStar(0.55f, 0.43f, 0.005f, 0.72f),
    KeogramStar(0.76f, 0.58f, 0.004f, 0.58f),
    KeogramStar(0.25f, 0.15f, 0.004f, 0.66f),
    KeogramStar(0.63f, 0.76f, 0.003f, 0.50f),
)

@Composable
private fun CraftingCandidateLibrary(
    state: CraftingUiState,
    onRefresh: () -> Unit,
    onBackToModes: () -> Unit,
    onOpenGroup: (CraftingCardGroup) -> Unit,
    onCoachmarkTargetBoundsChanged: (NewPlayerOnboardingTarget, Rect?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val firstCoachmarkCardId = state.sections.firstOrNull()?.cards?.firstOrNull()?.cardId
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 150.dp),
        modifier = modifier.testTag("crafting-candidate-grid"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    IconButton(
                        onClick = onBackToModes,
                        modifier = Modifier.testTag("crafting-back-to-modes"),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Retour",
                            tint = Color.White,
                        )
                    }
                    Column {
                        Text(
                            text = state.selectedMode.title(),
                            color = Color.White,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = state.selectedMode.subtitle(),
                            color = Color(0xFFD3E3F3),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
                state.successMessage?.let { message ->
                    Text(
                        text = message,
                        color = Color(0xFF9EE7FF),
                        modifier = Modifier.testTag("crafting-success-message"),
                    )
                }
                state.errorMessage?.let { message ->
                    Text(
                        text = message,
                        color = Color(0xFFFFB5B5),
                        modifier = Modifier.testTag("crafting-error-message"),
                    )
                    Button(
                        onClick = onRefresh,
                        modifier = Modifier.testTag("crafting-refresh"),
                    ) {
                        Text("Reessayer")
                    }
                }
            }
        }

        if (state.isLoading) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .padding(24.dp)
                        .testTag("crafting-loading"),
                )
            }
        }

        if (!state.isLoading && state.errorMessage == null && state.sections.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    text = "Aucune carte eligible pour cet atelier.",
                    color = Color(0xFFD3E3F3),
                    modifier = Modifier
                        .padding(vertical = 24.dp)
                        .testTag("crafting-empty"),
                )
            }
        }

        state.sections.forEach { section ->
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    text = section.extensionName,
                    color = Color(0xFF9EE7FF),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .padding(top = 8.dp, bottom = 4.dp)
                        .testTag("crafting-section-${section.extensionId}"),
                )
            }
            items(section.cards, key = { it.cardId }) { group ->
                AstroCardThumbnail(
                    item = group.toLibraryItem(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .onGloballyPositioned { coordinates ->
                            if (group.cardId == firstCoachmarkCardId) {
                                onCoachmarkTargetBoundsChanged(
                                    NewPlayerOnboardingTarget.CraftingCandidate,
                                    coordinates.boundsInRoot(),
                                )
                            }
                        },
                    onClick = { onOpenGroup(group) },
                )
            }
        }
    }
}

@Composable
private fun CraftingFullscreenDialog(
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xE608101A))
                .dstcgContentInsetsPadding(includeBottom = true)
                .padding(14.dp)
                .testTag("crafting-fullscreen"),
        ) {
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
                text = "Cree 1 x ${candidate.targetVariant.skyQualityLabel} · ${candidate.targetVariant.finishLabel}",
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

private fun CraftingCardGroup.toLibraryItem(): LibraryCardItem =
    LibraryCardItem(
        definition = firstCandidate.card,
        extensionName = firstCandidate.extensionName,
        ownedCount = availableVariants.sumOf { it.count },
        availableVariants = availableVariants,
    )

private fun CraftingMode?.title(): String = when (this) {
    CraftingMode.DarkenSky -> "Assombrir le ciel"
    CraftingMode.SpaceAgency -> "Agence spatiale"
    null -> "Artisanat"
}

private fun CraftingMode?.subtitle(): String = when (this) {
    CraftingMode.DarkenSky -> "Assombrir le ciel d'une carte grâce à ses doublons."
    CraftingMode.SpaceAgency -> "Faîtes tamponner une carte une fois que vous l'aurez suffisamment observée."
    null -> ""
}

private fun CraftingMode.confirmLabel(): String = when (this) {
    CraftingMode.DarkenSky -> "Assombrir"
    CraftingMode.SpaceAgency -> "Tamponner"
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
