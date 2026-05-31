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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import fr.aumombelli.dstcg.AppContainer
import fr.aumombelli.dstcg.feature.badges.BadgeUnlockCelebrationOverlay
import fr.aumombelli.dstcg.feature.home.HOME_LOGO_LANDING_SCALE
import fr.aumombelli.dstcg.model.NewPlayerOnboardingContent
import fr.aumombelli.dstcg.model.NewPlayerOnboardingStep
import fr.aumombelli.dstcg.model.TradeCardCandidate
import fr.aumombelli.dstcg.ui.component.AsterMascotAspectRatio
import fr.aumombelli.dstcg.ui.component.AsterMascotOverlay
import fr.aumombelli.dstcg.ui.component.AsterMascotSpec
import fr.aumombelli.dstcg.ui.component.asterMascotHeightForContainer
import fr.aumombelli.dstcg.ui.motion.AppScene
import fr.aumombelli.dstcg.ui.motion.AppSkyBackdrop
import fr.aumombelli.dstcg.ui.motion.BrandLogoVariant
import fr.aumombelli.dstcg.ui.motion.BookPortalOverlay
import fr.aumombelli.dstcg.ui.motion.ChestPortalOverlay
import fr.aumombelli.dstcg.ui.motion.EquipmentPortalOverlay
import fr.aumombelli.dstcg.ui.motion.LaunchLogoMark
import fr.aumombelli.dstcg.ui.motion.brandLogoLayoutSpec
import fr.aumombelli.dstcg.ui.motion.homeLogoVariantFor
import fr.aumombelli.dstcg.ui.motion.randomSkyBackdropVariant
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
    val hasEnteredHomeOnce = remember(launchConfig) { mutableStateOf(false) }
    val homeContentEntranceSettled = remember(launchConfig) { mutableStateOf(false) }
    val homeLockupEntranceSettled = remember(launchConfig) { mutableStateOf(false) }
    val selectedTradeCandidate = remember(launchConfig) { mutableStateOf<TradeCardCandidate?>(null) }
    val launchWelcomeAwaitingHomeEntrance = remember(launchConfig) {
        mutableStateOf(launchConfig.scene == AppLaunchScene.Start)
    }

    val cameraTilt = remember(launchConfig) { Animatable(0f) }
    val mountainSkyBlend = remember(launchConfig) { Animatable(0f) }
    val horizonLights = remember(launchConfig, skyVariant) {
        Animatable(if (skyVariant.hasHorizonLights) 1f else 0f)
    }
    val bookProgress = remember { Animatable(0f) }
    val bookOverlayAlpha = remember { Animatable(1f) }
    val chestProgress = remember { Animatable(0f) }
    val chestOverlayAlpha = remember { Animatable(1f) }
    val equipmentProgress = remember { Animatable(0f) }
    val equipmentOverlayAlpha = remember { Animatable(1f) }

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
            equipmentProgress = equipmentProgress,
            equipmentOverlayAlpha = equipmentOverlayAlpha,
            readState = { sceneStateHolder.value },
            writeState = { sceneStateHolder.value = it },
            awaitNextFrame = { withFrameNanos { } },
        )
    }
    val onboardingCoordinator = remember(appContainer) {
        NewPlayerOnboardingCoordinator(
            progressRepository = appContainer.progressRepository,
            craftingRepository = appContainer.craftingRepository,
        )
    }
    val onboardingStep = onboardingCoordinator.uiState.currentStep
    val blockingModalSpec = onboardingCoordinator.activeBlockingModal(
        currentScene = sceneState.currentScene,
        sceneState = sceneState,
    )

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
        finishedListener = { animatedValue ->
            if (
                sceneState.currentScene == AppScene.Home &&
                sceneState.homeContentVisible &&
                animatedValue >= 0.99f
            ) {
                homeLockupEntranceSettled.value = true
            }
        },
        label = "home-lockup-reveal",
    )
    val launchBadgeLandingSize = if (sceneState.homeLogoBadgeLandingSizePx > 0f) {
        with(density) { sceneState.homeLogoBadgeLandingSizePx.toDp() }
    } else {
        112.dp
    }
    val launchBadgeBaseSize = launchBadgeLandingSize / HOME_LOGO_LANDING_SCALE
    val launchLogoTargetTranslationY = if (
        sceneState.rootHeightPx > 0f &&
        sceneState.homeLogoBadgeCenterYPx > 0f
    ) {
        sceneState.homeLogoBadgeCenterYPx - (sceneState.rootHeightPx / 2f)
    } else {
        -220f
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

    LaunchedEffect(sceneState.currentScene, sceneState.homeContentVisible) {
        if (sceneState.currentScene != AppScene.Home || !sceneState.homeContentVisible) {
            homeContentEntranceSettled.value = false
            homeLockupEntranceSettled.value = false
        }
        if (sceneState.currentScene != AppScene.Library) {
            selectedTradeCandidate.value = null
        }
    }

    LaunchedEffect(
        launchWelcomeAwaitingHomeEntrance.value,
        sceneState.currentScene,
        sceneState.homeContentVisible,
        sceneState.onboardingHintsVisible,
        homeContentEntranceSettled.value,
        homeLockupEntranceSettled.value,
    ) {
        if (
            launchWelcomeAwaitingHomeEntrance.value &&
            sceneState.currentScene == AppScene.Home &&
            sceneState.homeContentVisible &&
            !sceneState.onboardingHintsVisible &&
            homeContentEntranceSettled.value &&
            homeLockupEntranceSettled.value
        ) {
            sceneStateHolder.value = sceneStateHolder.value.showOnboardingHints()
            launchWelcomeAwaitingHomeEntrance.value = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .semantics {
                testTagsAsResourceId = true
            }
            .onSizeChanged { size ->
                sceneStateHolder.value = sceneStateHolder.value.withRootSize(
                    widthPx = size.width.toFloat(),
                    heightPx = size.height.toFloat(),
                )
            },
    ) {
        BackHandler(enabled = sceneState.transitionLocked) {
        }

        AppSkyBackdrop(
            variant = skyVariant,
            cameraTiltProgress = cameraTilt.value,
            horizonLightAlpha = horizonLights.value,
            mountainBlendProgress = mountainSkyBlend.value,
        )

        AppSceneContent(
            appContainer = appContainer,
            activity = activity,
            sceneState = sceneState,
            onboardingCoordinator = onboardingCoordinator,
            onboardingStep = onboardingStep,
            blockingModalSpec = blockingModalSpec,
            selectedTradeCandidate = selectedTradeCandidate,
            hasEnteredHomeOnce = hasEnteredHomeOnce,
            homeContentEntranceSettled = homeContentEntranceSettled,
            homeLogoVariant = homeLogoVariant,
            transitions = transitions,
            scope = scope,
            updateSceneState = { transform ->
                sceneStateHolder.value = transform(sceneStateHolder.value)
            },
        )

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

        EquipmentPortalOverlay(
            progress = equipmentProgress.value,
            overlayAlpha = equipmentOverlayAlpha.value,
        )

        val badgeCelebrationTargetBounds =
            sceneState.coachmarkTargetBounds[NewPlayerOnboardingTarget.HomeBadges]
        val badgeCelebrationVisible = sceneState.currentScene == AppScene.Home &&
            sceneState.homeContentVisible &&
            sceneState.pendingBadgeCelebration.isNotEmpty() &&
            !sceneState.badgeCelebrationDeferred &&
            badgeCelebrationTargetBounds != null
        val activeCoachmarkSpec = if (blockingModalSpec == null) {
            onboardingCoordinator.activeCoachmark(
                currentScene = sceneState.currentScene,
                sceneState = sceneState,
                badgeCelebrationVisible = badgeCelebrationVisible,
            )
        } else {
            null
        }

        if (blockingModalSpec?.kind == NewPlayerBlockingModalKind.WelcomeIntro) {
            val welcomeMascotSpec = resolveNewPlayerBlockingModalMascotSpec(
                NewPlayerBlockingModalKind.WelcomeIntro,
            )
            NewPlayerBlockingModal(
                testTag = "new-player-modal-welcome",
                pages = listOf(
                    NewPlayerBlockingModalPage(
                        title = NewPlayerOnboardingContent.welcomeIntro.title,
                        message = NewPlayerOnboardingContent.welcomeIntro.message,
                    ),
                ),
                finishButtonLabel = "Commencer",
                onFinished = {
                    scope.launch { onboardingCoordinator.onWelcomeIntroAcknowledged() }
                },
                decorativeHeight = {
                    welcomeMascotSpec?.centeredModalHeight(
                        containerWidth = maxWidth,
                        containerHeight = maxHeight,
                    ) ?: 0.dp
                },
                decorativeOverlay = { topPadding, mascotHeight ->
                    welcomeMascotSpec?.let { spec ->
                        AsterMascotOverlay(
                            spec = spec,
                            topPadding = topPadding,
                            widthOverride = mascotHeight * AsterMascotAspectRatio,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                },
            )
        }

        if (blockingModalSpec?.kind == NewPlayerBlockingModalKind.Conclusion) {
            val conclusionMascotSpec = resolveNewPlayerBlockingModalMascotSpec(
                NewPlayerBlockingModalKind.Conclusion,
            )
            NewPlayerBlockingModal(
                testTag = "new-player-modal-conclusion",
                pages = listOf(
                    NewPlayerBlockingModalPage(
                        title = NewPlayerOnboardingContent.conclusion.title,
                        message = NewPlayerOnboardingContent.conclusion.message,
                    ),
                ),
                finishButtonLabel = "Terminer",
                onFinished = {
                    scope.launch { onboardingCoordinator.onConclusionAcknowledged() }
                },
                decorativeHeight = {
                    conclusionMascotSpec?.centeredModalHeight(
                        containerWidth = maxWidth,
                        containerHeight = maxHeight,
                    ) ?: 0.dp
                },
                decorativeOverlay = { topPadding, mascotHeight ->
                    conclusionMascotSpec?.let { spec ->
                        AsterMascotOverlay(
                            spec = spec,
                            topPadding = topPadding,
                            widthOverride = mascotHeight * AsterMascotAspectRatio,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                },
            )
        }

        if (blockingModalSpec == null) {
            resolveNewPlayerSceneMascotSpec(
                currentStep = onboardingStep,
                currentScene = sceneState.currentScene,
                sceneState = sceneState,
                activeCoachmarkSpec = activeCoachmarkSpec,
                badgeCelebrationVisible = badgeCelebrationVisible,
            )?.let { spec ->
                AsterMascotOverlay(
                    spec = spec,
                    modifier = Modifier.fillMaxSize(),
                )
            }

            activeCoachmarkSpec?.let { spec ->
                val targetBounds = sceneState.coachmarkTargetBounds[spec.target]
                val forceScrollDownHint =
                    spec.target == NewPlayerOnboardingTarget.EquipmentActivation &&
                        sceneState.equipmentActivationScrollHintVisible
                if (targetBounds != null || forceScrollDownHint) {
                    NewPlayerCoachmarkOverlay(
                        spec = spec,
                        targetBounds = targetBounds,
                        forceScrollDownHint = forceScrollDownHint,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }

            BadgeUnlockCelebrationOverlay(
                badges = sceneState.pendingBadgeCelebration,
                targetBounds = badgeCelebrationTargetBounds,
                visible = badgeCelebrationVisible,
                onFinished = transitions::completeBadgeCelebration,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

internal fun AsterMascotSpec.centeredModalHeight(
    containerWidth: Dp,
    containerHeight: Dp,
): Dp = minOf(
    asterMascotHeightForContainer(
        containerWidth = containerWidth.value,
        scale = scale,
        sizeMultiplier = sizeMultiplier,
    ).dp,
    containerHeight * CenteredModalAsterMaxHeightFraction,
)

private const val CenteredModalAsterMaxHeightFraction = 0.38f
