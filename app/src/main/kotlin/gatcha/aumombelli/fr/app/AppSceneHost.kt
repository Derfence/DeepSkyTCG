package fr.aumombelli.gatcha.app

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.lifecycle.viewmodel.compose.viewModel
import fr.aumombelli.gatcha.AppContainer
import fr.aumombelli.gatcha.feature.badges.BadgeBookScreen
import fr.aumombelli.gatcha.feature.badges.BadgeBookViewModel
import fr.aumombelli.gatcha.feature.badges.BadgeUnlockCelebrationOverlay
import fr.aumombelli.gatcha.feature.library.LibraryScreen
import fr.aumombelli.gatcha.feature.library.LibraryViewModel
import fr.aumombelli.gatcha.feature.packs.opening.PackOpeningScreen
import fr.aumombelli.gatcha.feature.packs.opening.PackOpeningViewModel
import fr.aumombelli.gatcha.feature.packs.selection.PackEvent
import fr.aumombelli.gatcha.feature.packs.selection.PackSelectionScreen
import fr.aumombelli.gatcha.feature.packs.selection.PackViewModel
import fr.aumombelli.gatcha.feature.start.StartEvent
import fr.aumombelli.gatcha.feature.start.StartScreen
import fr.aumombelli.gatcha.feature.start.StartViewModel
import fr.aumombelli.gatcha.ui.motion.AppScene
import fr.aumombelli.gatcha.ui.motion.AppSkyBackdrop
import fr.aumombelli.gatcha.ui.motion.BookPortalOverlay
import fr.aumombelli.gatcha.ui.motion.ChestPortalOverlay
import fr.aumombelli.gatcha.ui.motion.LaunchLogoMark
import fr.aumombelli.gatcha.ui.motion.randomSkyBackdropVariant
import fr.aumombelli.gatcha.ui.screen.MainMenuScreen
import fr.aumombelli.gatcha.ui.viewmodel.GatchaViewModelFactory
import kotlinx.coroutines.launch

@Composable
internal fun AppSceneHost(appContainer: AppContainer) {
    val scope = rememberCoroutineScope()
    val activity = LocalContext.current.findActivity()
    val skyVariant = remember { randomSkyBackdropVariant() }
    val density = LocalDensity.current
    val sceneStateHolder = remember { mutableStateOf(AppSceneUiState()) }
    val sceneState = sceneStateHolder.value

    val cameraTilt = remember { Animatable(0f) }
    val mountainSkyBlend = remember { Animatable(0f) }
    val horizonLights = remember { Animatable(1f) }
    val bookProgress = remember { Animatable(0f) }
    val bookOverlayAlpha = remember { Animatable(1f) }
    val chestProgress = remember { Animatable(0f) }
    val chestOverlayAlpha = remember { Animatable(1f) }

    val transitions = remember(appContainer, skyVariant) {
        AppSceneTransitionController(
            appContainer = appContainer,
            skyVariant = skyVariant,
            cameraTilt = cameraTilt,
            mountainSkyBlend = mountainSkyBlend,
            horizonLights = horizonLights,
            bookProgress = bookProgress,
            bookOverlayAlpha = bookOverlayAlpha,
            chestProgress = chestProgress,
            chestOverlayAlpha = chestOverlayAlpha,
            readState = { sceneStateHolder.value },
            writeState = { sceneStateHolder.value = it },
            awaitNextFrame = { withFrameNanos { } },
        )
    }

    val launchLogoAlpha by animateFloatAsState(
        targetValue = if (sceneState.launchLogoVisible) 1f else 0f,
        animationSpec = tween(durationMillis = 520, easing = FastOutSlowInEasing),
        label = "app-launch-logo-alpha",
    )
    val launchLogoLiftProgress by animateFloatAsState(
        targetValue = if (sceneState.launchLogoRaised) 1f else 0f,
        animationSpec = tween(durationMillis = 980, easing = FastOutSlowInEasing),
        label = "app-launch-logo-lift",
    )
    val statusBarInsetPx = WindowInsets.statusBars.getTop(density).toFloat()
    val launchLogoTargetTranslationY = if (sceneState.rootHeightPx > 0f && sceneState.startCardTopPx > 0f) {
        (sceneState.startCardTopPx / 2f) - (sceneState.rootHeightPx / 2f) + (statusBarInsetPx * 0.18f)
    } else {
        -(220f - statusBarInsetPx * 0.18f)
    }

    LaunchedEffect(Unit) {
        transitions.runLaunchSequence()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { size ->
                sceneStateHolder.value = sceneStateHolder.value.withRootHeight(size.height.toFloat())
            },
    ) {
        AppSkyBackdrop(
            variant = skyVariant,
            cameraTiltProgress = cameraTilt.value,
            horizonLightAlpha = horizonLights.value,
            mountainBlendProgress = mountainSkyBlend.value,
        )

        when (sceneState.currentScene) {
            AppScene.Start -> {
                val startViewModel: StartViewModel = viewModel(
                    key = "start",
                    factory = GatchaViewModelFactory {
                        StartViewModel(
                            progressRepository = appContainer.progressRepository,
                        )
                    },
                )
                val uiState by startViewModel.uiState.collectAsState()

                LaunchedEffect(startViewModel) {
                    startViewModel.events.collect { event ->
                        when (event) {
                            StartEvent.ReadyToEnterMenu -> {
                                scope.launch {
                                    sceneStateHolder.value = sceneStateHolder.value.hideStartCard()
                                    kotlinx.coroutines.delay(560)
                                    transitions.animateStartToMenu()
                                }
                            }
                        }
                    }
                }

                StartScreen(
                    state = uiState,
                    onBegin = startViewModel::begin,
                    onCardTopChanged = { topPx ->
                        sceneStateHolder.value = sceneStateHolder.value.withStartCardTop(topPx)
                    },
                    showBackground = false,
                    contentVisible = sceneState.startCardVisible,
                )
            }

            AppScene.MainMenu -> {
                BackHandler(enabled = !sceneState.transitionLocked) {
                    activity?.finish()
                }

                MainMenuScreen(
                    onOpenPack = {
                        if (!sceneState.transitionLocked) {
                            scope.launch { transitions.animateMenuToPackSelection() }
                        }
                    },
                    onOpenLibrary = {
                        scope.launch { transitions.animateMenuToLibrary() }
                    },
                    onOpenBadgeBook = {
                        scope.launch { transitions.animateMenuToBadgeBook() }
                    },
                    showBackground = false,
                    contentVisible = sceneState.menuContentVisible,
                    interactionsEnabled = !sceneState.transitionLocked,
                    onBadgeButtonBoundsChanged = { bounds ->
                        sceneStateHolder.value = sceneStateHolder.value.withMenuBadgeButtonBounds(bounds)
                    },
                )
            }

            AppScene.Library -> {
                val libraryViewModel: LibraryViewModel = viewModel(
                    key = "library-${sceneState.libraryViewModelKey}",
                    factory = GatchaViewModelFactory {
                        LibraryViewModel(
                            catalogRepository = appContainer.catalogRepository,
                            collectionRepository = appContainer.collectionRepository,
                        )
                    },
                )
                val uiState by libraryViewModel.uiState.collectAsState()

                BackHandler(enabled = !sceneState.transitionLocked) {
                    scope.launch { transitions.animateLibraryToMenu() }
                }

                LibraryScreen(
                    state = uiState,
                    onRefresh = libraryViewModel::refresh,
                    contentVisible = sceneState.libraryContentVisible,
                )
            }

            AppScene.BadgeBook -> {
                val badgeBookViewModel: BadgeBookViewModel = viewModel(
                    key = "badges-${sceneState.badgeBookViewModelKey}",
                    factory = GatchaViewModelFactory {
                        BadgeBookViewModel(
                            catalogRepository = appContainer.catalogRepository,
                            collectionRepository = appContainer.collectionRepository,
                        )
                    },
                )
                val uiState by badgeBookViewModel.uiState.collectAsState()

                BackHandler(enabled = !sceneState.transitionLocked) {
                    scope.launch { transitions.animateBadgeBookToMenu() }
                }

                BadgeBookScreen(
                    state = uiState,
                    onRefresh = badgeBookViewModel::refresh,
                    contentVisible = sceneState.badgeBookContentVisible,
                )
            }

            AppScene.PackSelection,
            AppScene.PackOpening,
            -> {
                val packViewModel: PackViewModel = viewModel(
                    key = "pack-${sceneState.packFlowKey}",
                    factory = GatchaViewModelFactory {
                        PackViewModel(
                            catalogRepository = appContainer.catalogRepository,
                            progressRepository = appContainer.progressRepository,
                            packRepository = appContainer.packRepository,
                            gameSettings = appContainer.gameSettings,
                        )
                    },
                )
                val uiState by packViewModel.uiState.collectAsState()

                LaunchedEffect(packViewModel) {
                    packViewModel.events.collect { event ->
                        when (event) {
                            is PackEvent.PackReadyForReveal -> {
                                sceneStateHolder.value = sceneStateHolder.value.registerPackReady(
                                    event.newlyUnlockedBadges,
                                )
                            }
                        }
                    }
                }

                if (sceneState.currentScene == AppScene.PackSelection) {
                    BackHandler(
                        enabled = !sceneState.transitionLocked && !uiState.isAwaitingPackResult,
                    ) {
                        if (uiState.selectedExtensionId != null) {
                            packViewModel.clearExtensionSelection()
                        } else {
                            scope.launch { transitions.animatePackSelectionToMenu() }
                        }
                    }
                } else {
                    BackHandler(enabled = sceneState.packOpeningExitSignal == 0) {
                        sceneStateHolder.value = sceneStateHolder.value.requestPackOpeningExit()
                    }
                }

                PackSelectionScreen(
                    state = uiState,
                    onRefresh = packViewModel::refresh,
                    onSelectExtension = packViewModel::selectExtension,
                    onSelectBooster = packViewModel::selectBooster,
                    onOpenPack = packViewModel::openPack,
                    onPackRevealReady = {
                        sceneStateHolder.value = sceneStateHolder.value.enterPackOpening()
                    },
                    onSelectedBoosterBoundsChanged = { bounds ->
                        sceneStateHolder.value = sceneStateHolder.value.withPackRevealBounds(bounds)
                    },
                    packReadySignal = sceneState.packReadySignal,
                    showBackground = false,
                    sceneVisible = sceneState.currentScene == AppScene.PackSelection && sceneState.packSceneVisible,
                    extensionListVisible = sceneState.currentScene == AppScene.PackSelection &&
                        sceneState.packExtensionListVisible,
                    interactionsEnabled = !sceneState.transitionLocked && sceneState.currentScene == AppScene.PackSelection,
                )

                if (sceneState.currentScene == AppScene.PackOpening) {
                    val openingViewModel: PackOpeningViewModel = viewModel(
                        key = "pack-opening-${sceneState.packFlowKey}",
                        factory = GatchaViewModelFactory {
                            PackOpeningViewModel(
                                catalogRepository = appContainer.catalogRepository,
                                packRepository = appContainer.packRepository,
                            )
                        },
                    )
                    val uiState by openingViewModel.uiState.collectAsState()

                    PackOpeningScreen(
                        state = uiState,
                        initialBoosterBounds = sceneState.selectedPackRevealBounds,
                        dismissSignal = sceneState.packOpeningExitSignal,
                        onDone = {
                            scope.launch { transitions.finishPackOpeningToMenu() }
                        },
                    )
                }
            }
        }

        if (launchLogoAlpha > 0.01f && sceneState.currentScene == AppScene.Start) {
            LaunchLogoMark(
                modifier = Modifier
                    .align(Alignment.Center)
                    .graphicsLayer {
                        alpha = launchLogoAlpha
                        translationY = launchLogoTargetTranslationY * launchLogoLiftProgress
                        scaleX = 1f - 0.12f * launchLogoLiftProgress
                        scaleY = 1f - 0.12f * launchLogoLiftProgress
                    },
            )
        }

        BookPortalOverlay(
            progress = bookProgress.value,
            overlayAlpha = bookOverlayAlpha.value,
        )

        ChestPortalOverlay(
            progress = chestProgress.value,
            overlayAlpha = chestOverlayAlpha.value,
        )

        BadgeUnlockCelebrationOverlay(
            badges = sceneState.pendingBadgeCelebration,
            targetBounds = sceneState.menuBadgeButtonBounds,
            visible = sceneState.currentScene == AppScene.MainMenu && sceneState.menuContentVisible,
            onFinished = transitions::completeBadgeCelebration,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
