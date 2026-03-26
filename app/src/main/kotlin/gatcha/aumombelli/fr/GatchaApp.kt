package fr.aumombelli.gatcha

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.lifecycle.viewmodel.compose.viewModel
import fr.aumombelli.gatcha.ui.motion.AppScene
import fr.aumombelli.gatcha.ui.motion.AppSkyBackdrop
import fr.aumombelli.gatcha.ui.motion.BookPortalOverlay
import fr.aumombelli.gatcha.ui.motion.LaunchLogoMark
import fr.aumombelli.gatcha.ui.motion.PackRevealBounds
import fr.aumombelli.gatcha.ui.motion.randomSkyBackdropVariant
import fr.aumombelli.gatcha.ui.screen.AppBootstrapScreen
import fr.aumombelli.gatcha.ui.screen.LibraryScreen
import fr.aumombelli.gatcha.ui.screen.LoginScreen
import fr.aumombelli.gatcha.ui.screen.MainMenuScreen
import fr.aumombelli.gatcha.ui.screen.PackOpeningScreen
import fr.aumombelli.gatcha.ui.screen.PackSelectionScreen
import fr.aumombelli.gatcha.ui.viewmodel.AppBootstrapViewModel
import fr.aumombelli.gatcha.ui.viewmodel.GatchaViewModelFactory
import fr.aumombelli.gatcha.ui.viewmodel.LibraryViewModel
import fr.aumombelli.gatcha.ui.viewmodel.LoginEvent
import fr.aumombelli.gatcha.ui.viewmodel.LoginViewModel
import fr.aumombelli.gatcha.ui.viewmodel.PackEvent
import fr.aumombelli.gatcha.ui.viewmodel.PackOpeningViewModel
import fr.aumombelli.gatcha.ui.viewmodel.PackViewModel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun GatchaApp(appContainer: AppContainer) {
    val bootstrapViewModel: AppBootstrapViewModel = viewModel(
        factory = GatchaViewModelFactory {
            AppBootstrapViewModel(appContainer.appStatusRepository)
        },
    )
    val bootstrapState by bootstrapViewModel.uiState.collectAsState()

    if (!bootstrapState.isCompatible) {
        AppBootstrapScreen(
            state = bootstrapState,
            onRetry = bootstrapViewModel::retry,
        )
        return
    }

    AppSceneHost(appContainer)
}

@Composable
private fun AppSceneHost(appContainer: AppContainer) {
    val scope = rememberCoroutineScope()
    val activity = LocalContext.current.findActivity()
    val skyVariant = remember { randomSkyBackdropVariant() }
    val density = LocalDensity.current
    var currentScene by remember { mutableStateOf(AppScene.Login) }
    var launchLogoVisible by remember { mutableStateOf(false) }
    var launchLogoRaised by remember { mutableStateOf(false) }
    var loginFormVisible by remember { mutableStateOf(false) }
    var menuContentVisible by remember { mutableStateOf(false) }
    var libraryContentVisible by remember { mutableStateOf(false) }
    var packSceneVisible by remember { mutableStateOf(false) }
    var packExtensionListVisible by remember { mutableStateOf(false) }
    var transitionLocked by remember { mutableStateOf(false) }
    var rootHeightPx by remember { mutableFloatStateOf(0f) }
    var loginFormTopPx by remember { mutableFloatStateOf(0f) }
    var loginViewModelKey by remember { mutableIntStateOf(0) }
    var libraryViewModelKey by remember { mutableIntStateOf(0) }
    var packFlowKey by remember { mutableIntStateOf(0) }
    var packReadySignal by remember { mutableIntStateOf(0) }
    var selectedPackRevealBounds by remember { mutableStateOf<PackRevealBounds?>(null) }

    val cameraTilt = remember { Animatable(0f) }
    val mountainSkyBlend = remember { Animatable(0f) }
    val horizonLights = remember { Animatable(1f) }
    val bookProgress = remember { Animatable(0f) }
    val bookOverlayAlpha = remember { Animatable(1f) }

    val launchLogoAlpha by animateFloatAsState(
        targetValue = if (launchLogoVisible) 1f else 0f,
        animationSpec = tween(durationMillis = 520, easing = FastOutSlowInEasing),
        label = "app-launch-logo-alpha",
    )
    val launchLogoLiftProgress by animateFloatAsState(
        targetValue = if (launchLogoRaised) 1f else 0f,
        animationSpec = tween(durationMillis = 980, easing = FastOutSlowInEasing),
        label = "app-launch-logo-lift",
    )
    val statusBarInsetPx = WindowInsets.statusBars.getTop(density).toFloat()
    val launchLogoTargetTranslationY = if (rootHeightPx > 0f && loginFormTopPx > 0f) {
        (loginFormTopPx / 2f) - (rootHeightPx / 2f) + (statusBarInsetPx * 0.18f)
    } else {
        -(220f - statusBarInsetPx * 0.18f)
    }

    fun finishPackOpeningToMenu() {
        appContainer.packRepository.clearCurrentPackResult()
        selectedPackRevealBounds = null
        packSceneVisible = false
        packExtensionListVisible = false
        currentScene = AppScene.MainMenu
        menuContentVisible = true
    }

    LaunchedEffect(Unit) {
        loginFormVisible = false
        launchLogoVisible = false
        launchLogoRaised = false
        delay(120)
        launchLogoVisible = true
        delay(900)
        launchLogoRaised = true
        delay(720)
        loginFormVisible = true
    }

    suspend fun animateLoginToMenu() {
        if (transitionLocked) return
        transitionLocked = true
        menuContentVisible = false
        launchLogoVisible = false
        coroutineScope {
            launch {
                cameraTilt.animateTo(
                    1f,
                    animationSpec = tween(durationMillis = 760, easing = FastOutSlowInEasing),
                )
            }
            launch {
                mountainSkyBlend.animateTo(
                    1f,
                    animationSpec = tween(durationMillis = 760, easing = FastOutSlowInEasing),
                )
            }
            if (skyVariant.hasHorizonLights) {
                launch {
                    horizonLights.animateTo(
                        0f,
                        animationSpec = tween(durationMillis = 760, easing = FastOutSlowInEasing),
                    )
                }
            }
        }
        currentScene = AppScene.MainMenu
        menuContentVisible = true
        transitionLocked = false
    }

    suspend fun animateMenuToLogin() {
        if (transitionLocked) return
        transitionLocked = true
        menuContentVisible = false
        delay(560)
        appContainer.sessionRepository.clearActiveSession()
        appContainer.packRepository.clearCurrentPackResult()
        loginViewModelKey += 1
        loginFormVisible = false
        launchLogoRaised = true
        currentScene = AppScene.Login
        launchLogoVisible = true
        coroutineScope {
            launch {
                cameraTilt.animateTo(
                    0f,
                    animationSpec = tween(durationMillis = 760, easing = FastOutSlowInEasing),
                )
            }
            launch {
                mountainSkyBlend.animateTo(
                    0f,
                    animationSpec = tween(durationMillis = 760, easing = FastOutSlowInEasing),
                )
            }
            if (skyVariant.hasHorizonLights) {
                launch {
                    horizonLights.animateTo(
                        1f,
                        animationSpec = tween(durationMillis = 760, easing = FastOutSlowInEasing),
                    )
                }
            }
        }
        loginFormVisible = true
        transitionLocked = false
    }

    suspend fun animateMenuToLibrary() {
        if (transitionLocked) return
        transitionLocked = true
        menuContentVisible = false
        libraryContentVisible = false
        delay(520)
        libraryViewModelKey += 1
        bookProgress.snapTo(0f)
        bookOverlayAlpha.snapTo(1f)
        bookProgress.animateTo(1f, animationSpec = tween(durationMillis = 980, easing = FastOutSlowInEasing))
        currentScene = AppScene.Library
        withFrameNanos { }
        libraryContentVisible = true
        bookOverlayAlpha.animateTo(0f, animationSpec = tween(durationMillis = 960, easing = FastOutSlowInEasing))
        bookProgress.snapTo(0f)
        bookOverlayAlpha.snapTo(1f)
        transitionLocked = false
    }

    suspend fun animateMenuToPackSelection() {
        if (transitionLocked) return
        transitionLocked = true
        menuContentVisible = false
        packSceneVisible = false
        packExtensionListVisible = false
        delay(520)
        appContainer.packRepository.clearCurrentPackResult()
        packFlowKey += 1
        packReadySignal = 0
        selectedPackRevealBounds = null
        currentScene = AppScene.PackSelection
        withFrameNanos { }
        packSceneVisible = true
        delay(460)
        packExtensionListVisible = true
        delay(760)
        transitionLocked = false
    }

    suspend fun animatePackSelectionToMenu() {
        if (transitionLocked) return
        transitionLocked = true
        menuContentVisible = false
        packExtensionListVisible = false
        delay(760)
        packSceneVisible = false
        delay(420)
        appContainer.packRepository.clearCurrentPackResult()
        selectedPackRevealBounds = null
        currentScene = AppScene.MainMenu
        withFrameNanos { }
        menuContentVisible = true
        packSceneVisible = false
        packExtensionListVisible = false
        delay(520)
        transitionLocked = false
    }

    suspend fun animateLibraryToMenu() {
        if (transitionLocked) return
        transitionLocked = true
        menuContentVisible = false
        bookProgress.snapTo(1f)
        bookOverlayAlpha.snapTo(0f)
        libraryContentVisible = false
        bookOverlayAlpha.animateTo(1f, animationSpec = tween(durationMillis = 960, easing = FastOutSlowInEasing))
        currentScene = AppScene.MainMenu
        bookProgress.animateTo(0f, animationSpec = tween(durationMillis = 980, easing = FastOutSlowInEasing))
        menuContentVisible = true
        transitionLocked = false
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { rootHeightPx = it.height.toFloat() },
    ) {
        AppSkyBackdrop(
            variant = skyVariant,
            cameraTiltProgress = cameraTilt.value,
            horizonLightAlpha = horizonLights.value,
            mountainBlendProgress = mountainSkyBlend.value,
        )

        when (currentScene) {
            AppScene.Login -> {
                val loginViewModel: LoginViewModel = viewModel(
                    key = "login-$loginViewModelKey",
                    factory = GatchaViewModelFactory {
                        LoginViewModel(
                            apiService = appContainer.apiService,
                            sessionRepository = appContainer.sessionRepository,
                            collectionRepository = appContainer.collectionRepository,
                        )
                    },
                )
                val uiState by loginViewModel.uiState.collectAsState()

                LaunchedEffect(loginViewModel) {
                    loginViewModel.events.collect { event ->
                        when (event) {
                            LoginEvent.AuthenticationSucceeded -> {
                                scope.launch {
                                    loginFormVisible = false
                                    delay(560)
                                    animateLoginToMenu()
                                }
                            }
                        }
                    }
                }

                LoginScreen(
                    state = uiState,
                    onUsernameChange = loginViewModel::updateUsername,
                    onEmailChange = loginViewModel::updateEmail,
                    onPasswordChange = loginViewModel::updatePassword,
                    onModeToggle = loginViewModel::toggleMode,
                    onSubmit = loginViewModel::submit,
                    onFormTopChanged = { loginFormTopPx = it },
                    showBackground = false,
                    contentVisible = loginFormVisible,
                )
            }

            AppScene.MainMenu -> {
                BackHandler(enabled = !transitionLocked) {
                    activity?.finish()
                }

                MainMenuScreen(
                    onOpenPack = {
                        if (!transitionLocked) {
                            scope.launch { animateMenuToPackSelection() }
                        }
                    },
                    onOpenLibrary = {
                        scope.launch { animateMenuToLibrary() }
                    },
                    onLogout = {
                        scope.launch { animateMenuToLogin() }
                    },
                    showBackground = false,
                    contentVisible = menuContentVisible,
                    interactionsEnabled = !transitionLocked,
                )
            }

            AppScene.Library -> {
                val viewModel: LibraryViewModel = viewModel(
                    key = "library-$libraryViewModelKey",
                    factory = GatchaViewModelFactory {
                        LibraryViewModel(
                            catalogRepository = appContainer.catalogRepository,
                            collectionRepository = appContainer.collectionRepository,
                        )
                    },
                )
                val uiState by viewModel.uiState.collectAsState()

                BackHandler(enabled = !transitionLocked) {
                    scope.launch { animateLibraryToMenu() }
                }

                LibraryScreen(
                    state = uiState,
                    onRefresh = viewModel::refresh,
                    contentVisible = libraryContentVisible,
                )
            }

            AppScene.PackSelection,
            AppScene.PackOpening,
            -> {
                val viewModel: PackViewModel = viewModel(
                    key = "pack-$packFlowKey",
                    factory = GatchaViewModelFactory {
                        PackViewModel(
                            catalogRepository = appContainer.catalogRepository,
                            collectionRepository = appContainer.collectionRepository,
                            packRepository = appContainer.packRepository,
                            sessionRepository = appContainer.sessionRepository,
                        )
                    },
                )
                val uiState by viewModel.uiState.collectAsState()

                LaunchedEffect(viewModel) {
                    viewModel.events.collect { event ->
                        when (event) {
                        PackEvent.PackReadyForReveal -> packReadySignal += 1
                    }
                    }
                }

                if (currentScene == AppScene.PackSelection) {
                    BackHandler(
                        enabled = !transitionLocked && !uiState.isAwaitingPackResult,
                    ) {
                        if (uiState.selectedExtensionId != null) {
                            viewModel.clearExtensionSelection()
                        } else {
                            scope.launch { animatePackSelectionToMenu() }
                        }
                    }
                } else {
                    BackHandler(enabled = true) {
                        finishPackOpeningToMenu()
                    }
                }

                PackSelectionScreen(
                    state = uiState,
                    onRefresh = viewModel::refresh,
                    onSelectExtension = viewModel::selectExtension,
                    onSelectBooster = viewModel::selectBooster,
                    onOpenPack = viewModel::openPack,
                    onPackRevealReady = {
                        packSceneVisible = false
                        packExtensionListVisible = false
                        currentScene = AppScene.PackOpening
                    },
                    onSelectedBoosterBoundsChanged = { selectedPackRevealBounds = it },
                    packReadySignal = packReadySignal,
                    showBackground = false,
                    sceneVisible = currentScene == AppScene.PackSelection && packSceneVisible,
                    extensionListVisible = currentScene == AppScene.PackSelection && packExtensionListVisible,
                    interactionsEnabled = !transitionLocked && currentScene == AppScene.PackSelection,
                )
                if (currentScene == AppScene.PackOpening) {
                    val openingViewModel: PackOpeningViewModel = viewModel(
                        key = "pack-opening-$packFlowKey",
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
                        initialBoosterBounds = selectedPackRevealBounds,
                        onDone = ::finishPackOpeningToMenu,
                    )
                }
            }
        }

        if (launchLogoAlpha > 0.01f && currentScene == AppScene.Login) {
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
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
