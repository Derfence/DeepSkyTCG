package fr.aumombelli.gatcha.app

import fr.aumombelli.gatcha.ui.motion.AppScene
import fr.aumombelli.gatcha.ui.motion.PackRevealBounds

internal data class AppSceneUiState(
    val currentScene: AppScene = AppScene.Start,
    val launchLogoVisible: Boolean = false,
    val launchLogoRaised: Boolean = false,
    val startCardVisible: Boolean = false,
    val menuContentVisible: Boolean = false,
    val libraryContentVisible: Boolean = false,
    val packSceneVisible: Boolean = false,
    val packExtensionListVisible: Boolean = false,
    val transitionLocked: Boolean = false,
    val rootHeightPx: Float = 0f,
    val startCardTopPx: Float = 0f,
    val libraryViewModelKey: Int = 0,
    val packFlowKey: Int = 0,
    val packReadySignal: Int = 0,
    val selectedPackRevealBounds: PackRevealBounds? = null,
)

internal fun AppSceneUiState.withRootHeight(heightPx: Float): AppSceneUiState = copy(rootHeightPx = heightPx)

internal fun AppSceneUiState.withStartCardTop(topPx: Float): AppSceneUiState = copy(startCardTopPx = topPx)

internal fun AppSceneUiState.resetLaunchSequence(): AppSceneUiState = copy(
    launchLogoVisible = false,
    launchLogoRaised = false,
    startCardVisible = false,
)

internal fun AppSceneUiState.showLaunchLogo(): AppSceneUiState = copy(launchLogoVisible = true)

internal fun AppSceneUiState.raiseLaunchLogo(): AppSceneUiState = copy(launchLogoRaised = true)

internal fun AppSceneUiState.showStartCard(): AppSceneUiState = copy(startCardVisible = true)

internal fun AppSceneUiState.hideStartCard(): AppSceneUiState = copy(startCardVisible = false)

internal fun AppSceneUiState.hideLaunchLogo(): AppSceneUiState = copy(launchLogoVisible = false)

internal fun AppSceneUiState.hideMenuContent(): AppSceneUiState = copy(menuContentVisible = false)

internal fun AppSceneUiState.showMenuContent(): AppSceneUiState = copy(menuContentVisible = true)

internal fun AppSceneUiState.hideLibraryContent(): AppSceneUiState = copy(libraryContentVisible = false)

internal fun AppSceneUiState.showLibraryContent(): AppSceneUiState = copy(libraryContentVisible = true)

internal fun AppSceneUiState.hidePackSelectionScene(): AppSceneUiState = copy(
    packSceneVisible = false,
    packExtensionListVisible = false,
)

internal fun AppSceneUiState.showPackScene(): AppSceneUiState = copy(packSceneVisible = true)

internal fun AppSceneUiState.showPackExtensionList(): AppSceneUiState = copy(packExtensionListVisible = true)

internal fun AppSceneUiState.hidePackExtensionList(): AppSceneUiState = copy(packExtensionListVisible = false)

internal fun AppSceneUiState.lockTransitions(): AppSceneUiState = copy(transitionLocked = true)

internal fun AppSceneUiState.unlockTransitions(): AppSceneUiState = copy(transitionLocked = false)

internal fun AppSceneUiState.enterMainMenu(): AppSceneUiState = copy(currentScene = AppScene.MainMenu)

internal fun AppSceneUiState.prepareLibraryEntry(nextLibraryViewModelKey: Int): AppSceneUiState = copy(
    menuContentVisible = false,
    libraryContentVisible = false,
    libraryViewModelKey = nextLibraryViewModelKey,
)

internal fun AppSceneUiState.enterLibrary(): AppSceneUiState = copy(currentScene = AppScene.Library)

internal fun AppSceneUiState.preparePackSelection(nextPackFlowKey: Int): AppSceneUiState = copy(
    currentScene = AppScene.PackSelection,
    menuContentVisible = false,
    packSceneVisible = false,
    packExtensionListVisible = false,
    packFlowKey = nextPackFlowKey,
    packReadySignal = 0,
    selectedPackRevealBounds = null,
)

internal fun AppSceneUiState.switchPackSelectionToMenu(): AppSceneUiState = copy(
    currentScene = AppScene.MainMenu,
    selectedPackRevealBounds = null,
)

internal fun AppSceneUiState.enterPackOpening(): AppSceneUiState = copy(
    currentScene = AppScene.PackOpening,
    packSceneVisible = false,
    packExtensionListVisible = false,
)

internal fun AppSceneUiState.finishPackOpeningToMenu(): AppSceneUiState = copy(
    currentScene = AppScene.MainMenu,
    menuContentVisible = true,
    packSceneVisible = false,
    packExtensionListVisible = false,
    selectedPackRevealBounds = null,
)

internal fun AppSceneUiState.withPackRevealBounds(bounds: PackRevealBounds?): AppSceneUiState =
    copy(selectedPackRevealBounds = bounds)

internal fun AppSceneUiState.registerPackReady(): AppSceneUiState = copy(
    packReadySignal = packReadySignal + 1,
)
