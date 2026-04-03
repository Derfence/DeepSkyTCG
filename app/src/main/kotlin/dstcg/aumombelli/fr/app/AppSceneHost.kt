package fr.aumombelli.dstcg.app

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
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import fr.aumombelli.dstcg.AppContainer
import fr.aumombelli.dstcg.feature.badges.BadgeBookScreen
import fr.aumombelli.dstcg.feature.badges.BadgeBookViewModel
import fr.aumombelli.dstcg.feature.badges.BadgeUnlockCelebrationOverlay
import fr.aumombelli.dstcg.feature.library.LibraryScreen
import fr.aumombelli.dstcg.feature.library.LibraryViewModel
import fr.aumombelli.dstcg.feature.packs.opening.PackOpeningScreen
import fr.aumombelli.dstcg.feature.packs.opening.PackOpeningViewModel
import fr.aumombelli.dstcg.feature.packs.selection.PackEvent
import fr.aumombelli.dstcg.feature.packs.selection.PackSelectionScreen
import fr.aumombelli.dstcg.feature.packs.selection.PackViewModel
import fr.aumombelli.dstcg.feature.start.StartEvent
import fr.aumombelli.dstcg.feature.start.StartScreen
import fr.aumombelli.dstcg.feature.start.StartViewModel
import fr.aumombelli.dstcg.ui.motion.AppScene
import fr.aumombelli.dstcg.ui.motion.AppSkyBackdrop
import fr.aumombelli.dstcg.ui.motion.BookPortalOverlay
import fr.aumombelli.dstcg.ui.motion.ChestPortalOverlay
import fr.aumombelli.dstcg.ui.motion.LaunchLogoMark
import fr.aumombelli.dstcg.ui.motion.randomSkyBackdropVariant
import fr.aumombelli.dstcg.ui.screen.MainMenuScreen
import fr.aumombelli.dstcg.ui.viewmodel.DstcgViewModelFactory
import kotlinx.coroutines.launch

@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun AppSceneHost(
    appContainer: AppContainer,
    launchConfig: AppLaunchConfig = AppLaunchConfig(),
) {
    val scope = rememberCoroutineScope()
    val activity = LocalContext.current.findActivity()
    val skyVariant = remember { randomSkyBackdropVariant() }
    val density = LocalDensity.current
    val startsAtMainMenu = launchConfig.scene == AppLaunchScene.MainMenu
    val sceneStateHolder = remember(launchConfig) {
        mutableStateOf(initialAppSceneUiState(launchConfig))
    }
    val sceneState = sceneStateHolder.value

    val cameraTilt = remember(launchConfig) { Animatable(if (startsAtMainMenu) 1f else 0f) }
    val mountainSkyBlend = remember(launchConfig) { Animatable(if (startsAtMainMenu) 1f else 0f) }
    val horizonLights = remember(launchConfig) { Animatable(if (startsAtMainMenu) 0f else 1f) }
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
    val onboardingCoordinator = remember(appContainer) {
        NewPlayerOnboardingCoordinator(appContainer.progressRepository)
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

    LaunchedEffect(launchConfig) {
        if (launchConfig.resetProgressOnLaunch) {
            runCatching {
                appContainer.progressRepository.resetProgress()
                appContainer.packRepository.clearCurrentPackResult()
            }
            onboardingCoordinator.syncFromProgress()
            if (launchConfig.scene == AppLaunchScene.MainMenu) {
                sceneStateHolder.value = sceneStateHolder.value.showMenuContent()
            }
        } else {
            onboardingCoordinator.syncFromProgress()
        }

        if (launchConfig.scene == AppLaunchScene.Start) {
            transitions.runLaunchSequence()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .semantics {
                testTagsAsResourceId = true
            }
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
                    factory = DstcgViewModelFactory {
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
                                    onboardingCoordinator.syncFromProgress()
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
                    onResetProgress = startViewModel::resetProgress,
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
                            scope.launch {
                                onboardingCoordinator.onMenuOpenPackSelected()
                                transitions.animateMenuToPackSelection()
                            }
                        }
                    },
                    onOpenLibrary = {
                        scope.launch {
                            val shouldResumeBadgeCelebration = onboardingCoordinator.onLibraryOpened()
                            if (shouldResumeBadgeCelebration) {
                                sceneStateHolder.value = sceneStateHolder.value.resumePendingBadgeCelebration()
                            }
                            transitions.animateMenuToLibrary()
                        }
                    },
                    onOpenBadgeBook = {
                        scope.launch {
                            onboardingCoordinator.onBadgeBookOpened()
                            transitions.animateMenuToBadgeBook()
                        }
                    },
                    showBackground = false,
                    contentVisible = sceneState.menuContentVisible,
                    interactionsEnabled = !sceneState.transitionLocked,
                    onCoachmarkTargetBoundsChanged = { target, bounds ->
                        sceneStateHolder.value = sceneStateHolder.value.withCoachmarkTargetBounds(target, bounds)
                    },
                )
            }

            AppScene.Library -> {
                val libraryViewModel: LibraryViewModel = viewModel(
                    key = "library",
                    factory = DstcgViewModelFactory {
                        LibraryViewModel(
                            catalogRepository = appContainer.catalogRepository,
                            collectionRepository = appContainer.collectionRepository,
                        )
                    },
                )
                val uiState by libraryViewModel.uiState.collectAsState()

                LaunchedEffect(sceneState.libraryRefreshSignal) {
                    if (!uiState.isLoading || uiState.sections.isNotEmpty() || uiState.errorMessage != null) {
                        libraryViewModel.refresh()
                    }
                }

                BackHandler(enabled = !sceneState.transitionLocked) {
                    scope.launch { transitions.animateLibraryToMenu() }
                }

                LibraryScreen(
                    state = uiState,
                    onRefresh = libraryViewModel::refresh,
                    contentVisible = sceneState.libraryContentVisible,
                    showOnboardingHint = onboardingCoordinator.uiState.libraryCardHintVisible &&
                        sceneState.onboardingHintsVisible,
                    onOnboardingHintConsumed = onboardingCoordinator::onLibraryCardHintConsumed,
                )
            }

            AppScene.BadgeBook -> {
                val badgeBookViewModel: BadgeBookViewModel = viewModel(
                    key = "badges",
                    factory = DstcgViewModelFactory {
                        BadgeBookViewModel(
                            catalogRepository = appContainer.catalogRepository,
                            progressRepository = appContainer.progressRepository,
                        )
                    },
                )
                val uiState by badgeBookViewModel.uiState.collectAsState()

                LaunchedEffect(sceneState.badgeBookRefreshSignal) {
                    if (!uiState.isLoading || uiState.sections.isNotEmpty() || uiState.errorMessage != null) {
                        badgeBookViewModel.refresh()
                    }
                }

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
                    key = "pack",
                    factory = DstcgViewModelFactory {
                        PackViewModel(
                            catalogRepository = appContainer.catalogRepository,
                            progressRepository = appContainer.progressRepository,
                            packRepository = appContainer.packRepository,
                            gameSettings = appContainer.gameSettings,
                        )
                    },
                )
                val uiState by packViewModel.uiState.collectAsState()

                LaunchedEffect(sceneState.packRefreshSignal) {
                    if (
                        !uiState.isLoading ||
                        uiState.extensions.isNotEmpty() ||
                        uiState.errorMessage != null ||
                        uiState.selectedExtensionId != null
                    ) {
                        packViewModel.refresh()
                    }
                }

                LaunchedEffect(packViewModel) {
                    packViewModel.events.collect { event ->
                        when (event) {
                            is PackEvent.PackReadyForReveal -> {
                                val shouldDeferBadgeCelebration = onboardingCoordinator.onFirstPackOpened()
                                sceneStateHolder.value = sceneStateHolder.value.registerPackReady(
                                    event.newlyUnlockedBadges,
                                    deferBadgeCelebration = shouldDeferBadgeCelebration,
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
                    onSelectExtension = { extensionId ->
                        packViewModel.selectExtension(extensionId)
                        scope.launch { onboardingCoordinator.onExtensionSelected() }
                    },
                    onSelectBooster = packViewModel::selectBooster,
                    onOpenPack = packViewModel::openPack,
                    onPackRevealReady = {
                        sceneStateHolder.value = sceneStateHolder.value.enterPackOpening()
                    },
                    onSelectedBoosterBoundsChanged = { bounds ->
                        sceneStateHolder.value = sceneStateHolder.value.withPackRevealBounds(bounds)
                    },
                    onCoachmarkTargetBoundsChanged = { target, bounds ->
                        sceneStateHolder.value = sceneStateHolder.value.withCoachmarkTargetBounds(target, bounds)
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
                        key = "pack-opening",
                        factory = DstcgViewModelFactory {
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
                showWordmark = false,
                emblemSize = 128.dp,
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

        val badgeCelebrationVisible = sceneState.currentScene == AppScene.MainMenu &&
            sceneState.menuContentVisible &&
            sceneState.pendingBadgeCelebration.isNotEmpty() &&
            !sceneState.badgeCelebrationDeferred

        onboardingCoordinator.activeCoachmark(
            currentScene = sceneState.currentScene,
            sceneState = sceneState,
            badgeCelebrationVisible = badgeCelebrationVisible,
        )?.let { spec ->
            sceneState.coachmarkTargetBounds[spec.target]?.let { targetBounds ->
                NewPlayerCoachmarkOverlay(
                    spec = spec,
                    targetBounds = targetBounds,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }

        BadgeUnlockCelebrationOverlay(
            badges = sceneState.pendingBadgeCelebration,
            targetBounds = sceneState.coachmarkTargetBounds[NewPlayerOnboardingTarget.MenuBadges],
            visible = badgeCelebrationVisible,
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
