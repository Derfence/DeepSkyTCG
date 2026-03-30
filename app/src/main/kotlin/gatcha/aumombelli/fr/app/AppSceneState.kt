package fr.aumombelli.gatcha.app

import androidx.compose.ui.geometry.Rect
import fr.aumombelli.gatcha.feature.badges.BadgeItem
import fr.aumombelli.gatcha.ui.motion.AppScene
import fr.aumombelli.gatcha.ui.motion.PackRevealBounds

internal data class AppSceneUiState(
    val currentScene: AppScene = AppScene.Start,
    val launchLogoVisible: Boolean = false,
    val launchLogoRaised: Boolean = false,
    val startCardVisible: Boolean = false,
    val menuContentVisible: Boolean = false,
    val libraryContentVisible: Boolean = false,
    val badgeBookContentVisible: Boolean = false,
    val packSceneVisible: Boolean = false,
    val packExtensionListVisible: Boolean = false,
    val transitionLocked: Boolean = false,
    val rootHeightPx: Float = 0f,
    val startCardTopPx: Float = 0f,
    val libraryRefreshSignal: Int = 0,
    val badgeBookRefreshSignal: Int = 0,
    val packRefreshSignal: Int = 0,
    val packReadySignal: Int = 0,
    val selectedPackRevealBounds: PackRevealBounds? = null,
    val packOpeningExitSignal: Int = 0,
    val pendingBadgeCelebration: List<BadgeItem> = emptyList(),
    val menuBadgeButtonBounds: Rect? = null,
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

internal fun AppSceneUiState.hideBadgeBookContent(): AppSceneUiState = copy(badgeBookContentVisible = false)

internal fun AppSceneUiState.showBadgeBookContent(): AppSceneUiState = copy(badgeBookContentVisible = true)

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

internal fun AppSceneUiState.prepareLibraryEntry(nextLibraryRefreshSignal: Int): AppSceneUiState = copy(
    menuContentVisible = false,
    libraryContentVisible = false,
    libraryRefreshSignal = nextLibraryRefreshSignal,
)

internal fun AppSceneUiState.enterLibrary(): AppSceneUiState = copy(currentScene = AppScene.Library)

internal fun AppSceneUiState.prepareBadgeBookEntry(nextBadgeBookRefreshSignal: Int): AppSceneUiState = copy(
    menuContentVisible = false,
    badgeBookContentVisible = false,
    badgeBookRefreshSignal = nextBadgeBookRefreshSignal,
)

internal fun AppSceneUiState.enterBadgeBook(): AppSceneUiState = copy(currentScene = AppScene.BadgeBook)

internal fun AppSceneUiState.preparePackSelection(nextPackRefreshSignal: Int): AppSceneUiState = copy(
    currentScene = AppScene.PackSelection,
    menuContentVisible = false,
    packSceneVisible = false,
    packExtensionListVisible = false,
    packRefreshSignal = nextPackRefreshSignal,
    packReadySignal = 0,
    selectedPackRevealBounds = null,
    packOpeningExitSignal = 0,
    pendingBadgeCelebration = emptyList(),
)

internal fun AppSceneUiState.switchPackSelectionToMenu(): AppSceneUiState = copy(
    currentScene = AppScene.MainMenu,
    selectedPackRevealBounds = null,
    packOpeningExitSignal = 0,
    pendingBadgeCelebration = emptyList(),
)

internal fun AppSceneUiState.enterPackOpening(): AppSceneUiState = copy(
    currentScene = AppScene.PackOpening,
    packSceneVisible = false,
    packExtensionListVisible = false,
    packOpeningExitSignal = 0,
)

internal fun AppSceneUiState.preparePackOpeningReturnToMenu(): AppSceneUiState = copy(
    currentScene = AppScene.MainMenu,
    menuContentVisible = false,
    packSceneVisible = false,
    packExtensionListVisible = false,
    selectedPackRevealBounds = null,
    packOpeningExitSignal = 0,
)

internal fun AppSceneUiState.finishPackOpeningToMenu(): AppSceneUiState = preparePackOpeningReturnToMenu().copy(
    menuContentVisible = true,
)

internal fun AppSceneUiState.withPackRevealBounds(bounds: PackRevealBounds?): AppSceneUiState =
    copy(selectedPackRevealBounds = bounds)

internal fun AppSceneUiState.registerPackReady(newlyUnlockedBadges: List<BadgeItem>): AppSceneUiState = copy(
    packReadySignal = packReadySignal + 1,
    pendingBadgeCelebration = newlyUnlockedBadges,
)

internal fun AppSceneUiState.requestPackOpeningExit(): AppSceneUiState = copy(
    packOpeningExitSignal = packOpeningExitSignal + 1,
)

internal fun AppSceneUiState.clearPendingBadgeCelebration(): AppSceneUiState = copy(
    pendingBadgeCelebration = emptyList(),
)

internal fun AppSceneUiState.withMenuBadgeButtonBounds(bounds: Rect?): AppSceneUiState = copy(
    menuBadgeButtonBounds = bounds,
)
