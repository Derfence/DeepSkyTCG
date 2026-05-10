package fr.aumombelli.dstcg.feature.home

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Construction
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import fr.aumombelli.dstcg.app.NewPlayerOnboardingTarget
import fr.aumombelli.dstcg.ui.component.NewContentIndicator
import fr.aumombelli.dstcg.ui.component.TRADING_CARD_WIDTH_OVER_HEIGHT
import fr.aumombelli.dstcg.ui.component.drawEquipmentMountGlyph
import fr.aumombelli.dstcg.ui.motion.MotionCard
import fr.aumombelli.dstcg.ui.motion.BrandLogoVariant
import fr.aumombelli.dstcg.ui.screen.dstcgContentInsetsPadding
import kotlin.math.roundToInt

@Composable
fun HomeScreen(
    state: HomeUiState,
    onOpenPack: () -> Unit,
    onOpenLibrary: () -> Unit,
    onOpenCrafting: () -> Unit,
    onOpenEquipment: () -> Unit,
    onOpenBadgeBook: () -> Unit,
    onOpenMiniGamesMenu: () -> Unit = {},
    onResetProgress: () -> Unit,
    modifier: Modifier = Modifier,
    showBackground: Boolean = true,
    contentVisible: Boolean = true,
    interactionsEnabled: Boolean = true,
    allowAuxiliaryActions: Boolean = true,
    homeLogoVariant: BrandLogoVariant = BrandLogoVariant.Lockup19,
    onHomeLogoLayoutChanged: (Float, Float) -> Unit = { _, _ -> },
    onContentEntranceSettledChanged: (Boolean) -> Unit = {},
    onCoachmarkTargetBoundsChanged: (NewPlayerOnboardingTarget, Rect?) -> Unit = { _, _ -> },
) {
    var settingsExpanded by remember { mutableStateOf(false) }
    var resetConfirmationVisible by remember { mutableStateOf(false) }
    var aboutSheetVisible by remember { mutableStateOf(false) }
    val density = LocalDensity.current
    val contentAlpha by animateFloatAsState(
        targetValue = if (contentVisible) 1f else 0f,
        animationSpec = tween(durationMillis = 520, easing = FastOutSlowInEasing),
        finishedListener = { animatedValue ->
            if (contentVisible && animatedValue >= 0.99f) {
                onContentEntranceSettledChanged(true)
            }
        },
        label = "home-content-alpha",
    )
    val contentTranslationY by animateFloatAsState(
        targetValue = if (contentVisible) 0f else 34f,
        animationSpec = tween(durationMillis = 520, easing = FastOutSlowInEasing),
        label = "home-content-translation",
    )
    val backgroundBrush = if (showBackground) {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFF08101D),
                Color(0xFF12243F),
                Color(0xFF1A3052),
            ),
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(Color.Transparent, Color.Transparent),
        )
    }
    val navigationEnabled = interactionsEnabled &&
        !state.isLoading &&
        state.errorMessage == null &&
        !state.isResettingProgress
    val settingsEnabled = interactionsEnabled && contentVisible

    LaunchedEffect(contentVisible) {
        if (!contentVisible) {
            onContentEntranceSettledChanged(false)
            settingsExpanded = false
            resetConfirmationVisible = false
            aboutSheetVisible = false
            onCoachmarkTargetBoundsChanged(NewPlayerOnboardingTarget.HomeOpenPack, null)
            onCoachmarkTargetBoundsChanged(NewPlayerOnboardingTarget.HomeLibrary, null)
            onCoachmarkTargetBoundsChanged(NewPlayerOnboardingTarget.HomeEquipment, null)
            onCoachmarkTargetBoundsChanged(NewPlayerOnboardingTarget.HomeBadges, null)
            onCoachmarkTargetBoundsChanged(NewPlayerOnboardingTarget.HomeCrafting, null)
        }
    }

    LaunchedEffect(contentVisible, state.isEquipmentMenuVisible) {
        if (!contentVisible || !state.isEquipmentMenuVisible) {
            onCoachmarkTargetBoundsChanged(NewPlayerOnboardingTarget.HomeEquipment, null)
        }
    }

    LaunchedEffect(contentVisible, state.isLibraryMenuVisible) {
        if (!contentVisible || !state.isLibraryMenuVisible) {
            onCoachmarkTargetBoundsChanged(NewPlayerOnboardingTarget.HomeLibrary, null)
        }
    }

    LaunchedEffect(contentVisible, state.isBadgeBookMenuVisible) {
        if (!contentVisible || !state.isBadgeBookMenuVisible) {
            onCoachmarkTargetBoundsChanged(NewPlayerOnboardingTarget.HomeBadges, null)
        }
    }

    LaunchedEffect(contentVisible, state.isCraftingMenuAvailable) {
        if (!contentVisible || !state.isCraftingMenuAvailable) {
            onCoachmarkTargetBoundsChanged(NewPlayerOnboardingTarget.HomeCrafting, null)
        }
    }

    LaunchedEffect(allowAuxiliaryActions) {
        if (!allowAuxiliaryActions) {
            settingsExpanded = false
            resetConfirmationVisible = false
            aboutSheetVisible = false
        }
    }

    BackHandler(enabled = settingsExpanded || resetConfirmationVisible || aboutSheetVisible) {
        when {
            resetConfirmationVisible -> resetConfirmationVisible = false
            aboutSheetVisible -> aboutSheetVisible = false
            else -> settingsExpanded = false
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(brush = backgroundBrush),
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .dstcgContentInsetsPadding(includeBottom = true)
                .padding(horizontal = 22.dp, vertical = 18.dp),
        ) {
            val homeLayout = calculateHomeResponsiveLayout(
                availableWidth = maxWidth,
                availableHeight = maxHeight,
                logoVariant = homeLogoVariant,
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .onGloballyPositioned { coordinates ->
                        reportHomeLayoutMetrics(
                            density = density,
                            contentTopInRootPx = coordinates.boundsInRoot().top,
                            homeLayout = homeLayout,
                            onHomeLogoLayoutChanged = onHomeLogoLayoutChanged,
                        )
                    },
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .offset { IntOffset(x = 0, y = contentTranslationY.roundToInt()) }
                        .graphicsLayer {
                            alpha = contentAlpha
                        },
                ) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .testTag("home-settings-anchor"),
                    ) {
                        IconButton(
                            onClick = {
                                if (allowAuxiliaryActions) {
                                    settingsExpanded = true
                                }
                            },
                            enabled = settingsEnabled,
                            modifier = Modifier
                                .size(52.dp)
                                .background(
                                    color = Color(0x8A0B1524),
                                    shape = CircleShape,
                                )
                                .testTag("home-settings"),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Settings,
                                contentDescription = "Paramètres",
                                tint = Color.White,
                            )
                        }
                        DropdownMenu(
                            expanded = settingsExpanded && contentVisible,
                            onDismissRequest = { settingsExpanded = false },
                            modifier = Modifier.testTag("home-settings-menu"),
                        ) {
                            DropdownMenuItem(
                                text = { Text("Réinitialiser la bibliothèque") },
                                onClick = {
                                    settingsExpanded = false
                                    aboutSheetVisible = false
                                    resetConfirmationVisible = true
                                },
                                enabled = !state.isLoading && !state.isResettingProgress,
                                modifier = Modifier.testTag("home-settings-reset"),
                            )
                            DropdownMenuItem(
                                text = { Text("À propos") },
                                onClick = {
                                    settingsExpanded = false
                                    aboutSheetVisible = true
                                },
                                modifier = Modifier.testTag("home-settings-about"),
                            )
                        }
                    }

                    HomePackCard(
                        enabled = navigationEnabled,
                        isBusy = state.isLoading || state.isResettingProgress,
                        title = "Ouvrir un pack",
                        subtitle = when {
                            state.isResettingProgress -> "Réinitialisation en cours..."
                            state.isLoading -> "Préparation de ta collection locale..."
                            else -> "Traverse l'observatoire et découvre une nouvelle extension."
                        },
                        onClick = onOpenPack,
                        miniGamesEnabled = state.isMiniGamesMenuVisible,
                        showMiniGamesNewIndicator = state.showMiniGamesNewIndicator,
                        onOpenMiniGamesMenu = onOpenMiniGamesMenu,
                        interactionTestTag = "home-open-pack",
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .offset(y = homeLayout.heroCardTop)
                            .width(homeLayout.heroCardWidth)
                            .aspectRatio(TRADING_CARD_WIDTH_OVER_HEIGHT)
                            .onGloballyPositioned { coordinates ->
                                if (contentVisible) {
                                    onCoachmarkTargetBoundsChanged(
                                        NewPlayerOnboardingTarget.HomeOpenPack,
                                        coordinates.boundsInRoot(),
                                    )
                                }
                            }
                    )

                    state.errorMessage?.let { error ->
                        MotionCard(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth(0.78f)
                                .offset(y = -(homeLayout.menuButtonSize + 24.dp))
                                .testTag("home-error-card"),
                        ) {
                            Text(
                                text = error,
                                color = Color(0xFFFFC2C2),
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
                            )
                        }
                    }

                    if (state.isBadgeBookMenuVisible) {
                        HomeCornerActionButton(
                            imageVector = Icons.Filled.WorkspacePremium,
                            contentDescription = "Badges",
                            enabled = navigationEnabled,
                            onClick = onOpenBadgeBook,
                            buttonSize = 52.dp,
                            showNewIndicator = state.showBadgeBookNewIndicator,
                            newIndicatorTestTag = "home-badges-new-indicator",
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .onGloballyPositioned { coordinates ->
                                    if (contentVisible) {
                                        onCoachmarkTargetBoundsChanged(
                                            NewPlayerOnboardingTarget.HomeBadges,
                                            coordinates.boundsInRoot(),
                                        )
                                    }
                                }
                                .testTag("home-badges"),
                        )
                    }

                    if (state.isLibraryMenuVisible) {
                        HomeCornerActionButton(
                            imageVector = Icons.AutoMirrored.Filled.MenuBook,
                            contentDescription = "Bibliothèque",
                            enabled = navigationEnabled,
                            onClick = onOpenLibrary,
                            buttonSize = homeLayout.menuButtonSize,
                            showNewIndicator = state.showLibraryNewIndicator,
                            newIndicatorTestTag = "home-library-new-indicator",
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .onGloballyPositioned { coordinates ->
                                    if (contentVisible) {
                                        onCoachmarkTargetBoundsChanged(
                                            NewPlayerOnboardingTarget.HomeLibrary,
                                            coordinates.boundsInRoot(),
                                        )
                                    }
                                }
                                .testTag("home-library"),
                        )
                    }

                    if (state.isEquipmentMenuVisible) {
                        HomeCornerActionButton(
                            contentDescription = "Equipements",
                            enabled = navigationEnabled,
                            onClick = onOpenEquipment,
                            buttonSize = homeLayout.menuButtonSize,
                            showNewIndicator = state.showEquipmentNewIndicator,
                            newIndicatorTestTag = "home-equipment-new-indicator",
                            iconContent = { iconModifier ->
                                EquipmentHomeMountIcon(modifier = iconModifier)
                            },
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .onGloballyPositioned { coordinates ->
                                    if (contentVisible) {
                                        onCoachmarkTargetBoundsChanged(
                                            NewPlayerOnboardingTarget.HomeEquipment,
                                            coordinates.boundsInRoot(),
                                        )
                                    }
                                }
                                .testTag("home-equipment"),
                        )
                    }

                    if (state.isCraftingMenuAvailable) {
                        HomeCornerActionButton(
                            imageVector = Icons.Filled.Construction,
                            contentDescription = "Artisanat",
                            enabled = navigationEnabled,
                            onClick = onOpenCrafting,
                            buttonSize = homeLayout.menuButtonSize,
                            showNewIndicator = false,
                            newIndicatorTestTag = "home-crafting-new-indicator",
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .onGloballyPositioned { coordinates ->
                                    if (contentVisible) {
                                        onCoachmarkTargetBoundsChanged(
                                            NewPlayerOnboardingTarget.HomeCrafting,
                                            coordinates.boundsInRoot(),
                                        )
                                    }
                                }
                                .testTag("home-crafting"),
                        )
                    }
                }
            }
        }

        HomeAboutSheet(
            visible = aboutSheetVisible && contentVisible,
            onDismiss = { aboutSheetVisible = false },
        )

        if (resetConfirmationVisible && contentVisible) {
            HomeResetConfirmationDialog(
                onConfirm = {
                    resetConfirmationVisible = false
                    onResetProgress()
                },
                onCancel = { resetConfirmationVisible = false },
            )
        }
    }
}

private fun reportHomeLayoutMetrics(
    density: Density,
    contentTopInRootPx: Float,
    homeLayout: HomeResponsiveLayout,
    onHomeLogoLayoutChanged: (Float, Float) -> Unit,
) {
    with(density) {
        onHomeLogoLayoutChanged(
            contentTopInRootPx + homeLayout.logoBadgeCenterY.toPx(),
            homeLayout.logoBadgeLandingSize.toPx(),
        )
    }
}

@Composable
private fun HomeCornerActionButton(
    contentDescription: String,
    enabled: Boolean,
    onClick: () -> Unit,
    buttonSize: Dp,
    showNewIndicator: Boolean,
    newIndicatorTestTag: String,
    modifier: Modifier = Modifier,
    imageVector: ImageVector? = null,
    iconContent: (@Composable (Modifier) -> Unit)? = null,
) {
    val mainIconSize = buttonSize * 0.36f
    val indicatorSize = buttonSize * 0.36f
    val indicatorPadding = buttonSize * 0.10f
    val resolvedIconContent: @Composable (Modifier) -> Unit = iconContent ?: { iconModifier ->
        Icon(
            imageVector = requireNotNull(imageVector),
            contentDescription = contentDescription,
            tint = Color.White,
            modifier = iconModifier,
        )
    }

    Surface(
        onClick = onClick,
        enabled = enabled,
        color = Color(0xB30A1524),
        contentColor = Color.White,
        shape = CircleShape,
        shadowElevation = 8.dp,
        modifier = modifier.size(buttonSize),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize(),
        ) {
            resolvedIconContent(Modifier.size(mainIconSize))
            if (showNewIndicator) {
                HomeMenuNewIndicator(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(indicatorPadding)
                        .size(indicatorSize)
                        .testTag(newIndicatorTestTag),
                )
            }
        }
    }
}

@Composable
private fun EquipmentHomeMountIcon(
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        drawEquipmentMountGlyph(
            strokeColor = Color.White,
            strokeWidth = size.minDimension * 0.07f,
        )
    }
}

@Composable
private fun HomeMenuNewIndicator(
    modifier: Modifier = Modifier,
) {
    NewContentIndicator(
        modifier = modifier,
        iconSize = 16.dp,
    )
}
