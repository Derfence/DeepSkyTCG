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
import androidx.compose.ui.graphics.TransformOrigin
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
import fr.aumombelli.dstcg.feature.home.HomeScreen
import fr.aumombelli.dstcg.feature.home.HomeViewModel
import fr.aumombelli.dstcg.feature.library.LibraryScreen
import fr.aumombelli.dstcg.feature.library.LibraryViewModel
import fr.aumombelli.dstcg.feature.packs.opening.PackOpeningScreen
import fr.aumombelli.dstcg.feature.packs.opening.PackOpeningViewModel
import fr.aumombelli.dstcg.feature.packs.selection.PackEvent
import fr.aumombelli.dstcg.feature.packs.selection.PackSelectionScreen
import fr.aumombelli.dstcg.feature.packs.selection.PackViewModel
import fr.aumombelli.dstcg.ui.motion.AppScene
import fr.aumombelli.dstcg.ui.motion.AppSkyBackdrop
import fr.aumombelli.dstcg.ui.motion.BrandLogoVariant
import fr.aumombelli.dstcg.ui.motion.BookPortalOverlay
import fr.aumombelli.dstcg.ui.motion.ChestPortalOverlay
import fr.aumombelli.dstcg.ui.motion.LaunchLogoMark
import fr.aumombelli.dstcg.ui.motion.brandLogoLayoutSpec
import fr.aumombelli.dstcg.ui.motion.homeLogoVariantFor
import fr.aumombelli.dstcg.ui.motion.randomSkyBackdropVariant
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
    val sceneStateHolder = remember(launchConfig) {
        mutableStateOf(initialAppSceneUiState(launchConfig))
    }
    val sceneState = sceneStateHolder.value

    val cameraTilt = remember(launchConfig) { Animatable(0f) }
    val mountainSkyBlend = remember(launchConfig) { Animatable(0f) }
    val horizonLights = remember(launchConfig, skyVariant) {
        Animatable(if (skyVariant.hasHorizonLights) 1f else 0f)
    }
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
    val homeLockupAlpha by animateFloatAsState(
        targetValue = if (sceneState.currentScene == AppScene.Home && sceneState.homeContentVisible) 1f else 0f,
        animationSpec = tween(durationMillis = 420, easing = FastOutSlowInEasing),
        label = "home-lockup-alpha",
    )
    val homeLockupRevealProgress by animateFloatAsState(
        targetValue = if (sceneState.currentScene == AppScene.Home && sceneState.homeContentVisible) 1f else 0f,
        animationSpec = tween(durationMillis = 520, easing = FastOutSlowInEasing),
        label = "home-lockup-reveal",
    )
    val statusBarInsetPx = WindowInsets.statusBars.getTop(density).toFloat()
    val launchBadgeBaseSize = 128.dp
    val launchBadgeLandingSize = launchBadgeBaseSize * 0.88f
    val launchLogoTargetTranslationY = if (sceneState.rootHeightPx > 0f && sceneState.homeHeroCardTopPx > 0f) {
        (sceneState.homeHeroCardTopPx / 2f) - (sceneState.rootHeightPx / 2f) + (statusBarInsetPx * 0.18f)
    } else {
        -(220f - statusBarInsetPx * 0.18f)
    }
    val homeLogoVariant = remember(skyVariant) { homeLogoVariantFor(skyVariant) }
    val homeLogoLayoutSpec = remember(homeLogoVariant) { brandLogoLayoutSpec(homeLogoVariant) }
    val homeLockupBadgeCenterOffsetPx = with(density) {
        (launchBadgeLandingSize * homeLogoLayoutSpec.badgeCenterYOffsetMultiplierFromBadgeSize).toPx()
    }

    LaunchedEffect(launchConfig) {
        if (launchConfig.resetProgressOnLaunch) {
            runCatching {
                appContainer.progressRepository.resetProgress()
                appContainer.packRepository.clearCurrentPackResult()
            }
            onboardingCoordinator.syncFromProgress()
            if (launchConfig.scene == AppLaunchScene.Home) {
                sceneStateHolder.value = sceneStateHolder.value.showHomeContent()
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
            AppScene.Home -> {
                val homeViewModel: HomeViewModel = viewModel(
                    key = "home",
                    factory = DstcgViewModelFactory {
                        HomeViewModel(
                            progressRepository = appContainer.progressRepository,
                        )
                    },
                )
                val uiState by homeViewModel.uiState.collectAsState()

                BackHandler(enabled = !sceneState.transitionLocked) {
                    activity?.finish()
                }

                HomeScreen(
                    state = uiState,
                    onOpenPack = {
                        if (!sceneState.transitionLocked) {
                            scope.launch {
                                onboardingCoordinator.onHomeOpenPackSelected()
                                transitions.animateHomeToPackSelection()
                            }
                        }
                    },
                    onOpenLibrary = {
                        if (!sceneState.transitionLocked) {
                            scope.launch {
                                val shouldResumeBadgeCelebration = onboardingCoordinator.onLibraryOpened()
                                if (shouldResumeBadgeCelebration) {
                                    sceneStateHolder.value = sceneStateHolder.value.resumePendingBadgeCelebration()
                                }
                                transitions.animateHomeToLibrary()
                            }
                        }
                    },
                    onOpenBadgeBook = {
                        if (!sceneState.transitionLocked) {
                            scope.launch {
                                onboardingCoordinator.onBadgeBookOpened()
                                transitions.animateHomeToBadgeBook()
                            }
                        }
                    },
                    onResetProgress = homeViewModel::resetProgress,
                    showBackground = false,
                    contentVisible = sceneState.homeContentVisible,
                    interactionsEnabled = !sceneState.transitionLocked,
                    onHeroCardTopChanged = { topPx ->
                        sceneStateHolder.value = sceneStateHolder.value.withHomeHeroCardTop(topPx)
                    },
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
                    scope.launch { transitions.animateLibraryToHome() }
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
                    scope.launch { transitions.animateBadgeBookToHome() }
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
                            scope.launch { transitions.animatePackSelectionToHome() }
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
                    val openingUiState by openingViewModel.uiState.collectAsState()

                    PackOpeningScreen(
                        state = openingUiState,
                        initialBoosterBounds = sceneState.selectedPackRevealBounds,
                        dismissSignal = sceneState.packOpeningExitSignal,
                        onDone = {
                            scope.launch { transitions.finishPackOpeningToHome() }
                        },
                    )
                }
            }
        }

        if (homeLockupAlpha > 0.01f) {
            LaunchLogoMark(
                variant = homeLogoVariant,
                emblemSize = launchBadgeLandingSize,
                modifier = Modifier
                    .align(Alignment.Center)
                    .graphicsLayer {
                        alpha = homeLockupAlpha
                        translationY = launchLogoTargetTranslationY + homeLockupBadgeCenterOffsetPx
                        transformOrigin = TransformOrigin(
                            pivotFractionX = homeLogoLayoutSpec.badgeCenterFractionX,
                            pivotFractionY = homeLogoLayoutSpec.badgeCenterFractionY,
                        )
                        scaleX = 0.99f + homeLockupRevealProgress * 0.01f
                    },
                testTag = "home-logo-lockup",
            )
        }

        if (launchLogoAlpha > 0.01f) {
            LaunchLogoMark(
                variant = BrandLogoVariant.Badge17,
                emblemSize = launchBadgeBaseSize,
                modifier = Modifier
                    .align(Alignment.Center)
                    .graphicsLayer {
                        alpha = launchLogoAlpha
                        translationY = launchLogoTargetTranslationY * launchLogoLiftProgress
                        scaleX = 1f - 0.12f * launchLogoLiftProgress
                        scaleY = 1f - 0.12f * launchLogoLiftProgress
                    },
                testTag = "app-launch-logo",
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

        val badgeCelebrationVisible = sceneState.currentScene == AppScene.Home &&
            sceneState.homeContentVisible &&
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
            targetBounds = sceneState.coachmarkTargetBounds[NewPlayerOnboardingTarget.HomeBadges],
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
