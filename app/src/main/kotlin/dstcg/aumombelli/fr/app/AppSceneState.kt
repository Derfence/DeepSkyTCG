package fr.aumombelli.dstcg.app

import androidx.compose.ui.geometry.Rect
import fr.aumombelli.dstcg.feature.badges.BadgeItem
import fr.aumombelli.dstcg.ui.motion.AppScene
import fr.aumombelli.dstcg.ui.motion.PackRevealBounds

internal data class AppSceneUiState(
    val currentScene: AppScene = AppScene.Home,
    val launchLogoVisible: Boolean = false,
    val launchLogoRaised: Boolean = false,
    val homeContentVisible: Boolean = false,
    val libraryContentVisible: Boolean = false,
    val badgeBookContentVisible: Boolean = false,
    val packSceneVisible: Boolean = false,
    val packExtensionListVisible: Boolean = false,
    val transitionLocked: Boolean = false,
    val rootHeightPx: Float = 0f,
    val homeHeroCardTopPx: Float = 0f,
    val libraryRefreshSignal: Int = 0,
    val badgeBookRefreshSignal: Int = 0,
    val packRefreshSignal: Int = 0,
    val packReadySignal: Int = 0,
    val selectedPackRevealBounds: PackRevealBounds? = null,
    val packOpeningExitSignal: Int = 0,
    val pendingBadgeCelebration: List<BadgeItem> = emptyList(),
    val badgeCelebrationDeferred: Boolean = false,
    val onboardingHintsVisible: Boolean = true,
    val coachmarkTargetBounds: Map<NewPlayerOnboardingTarget, Rect> = emptyMap(),
)

internal fun AppSceneUiState.withRootHeight(heightPx: Float): AppSceneUiState = copy(rootHeightPx = heightPx)

internal fun AppSceneUiState.withHomeHeroCardTop(topPx: Float): AppSceneUiState = copy(homeHeroCardTopPx = topPx)

internal fun AppSceneUiState.resetLaunchSequence(): AppSceneUiState = copy(
    launchLogoVisible = false,
    launchLogoRaised = false,
    homeContentVisible = false,
)

internal fun AppSceneUiState.showLaunchLogo(): AppSceneUiState = copy(launchLogoVisible = true)

internal fun AppSceneUiState.raiseLaunchLogo(): AppSceneUiState = copy(launchLogoRaised = true)

internal fun AppSceneUiState.hideLaunchLogo(): AppSceneUiState = copy(launchLogoVisible = false)

internal fun AppSceneUiState.hideHomeContent(): AppSceneUiState = copy(homeContentVisible = false)

internal fun AppSceneUiState.showHomeContent(): AppSceneUiState = copy(homeContentVisible = true)

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

internal fun AppSceneUiState.lockTransitions(): AppSceneUiState = copy(
    transitionLocked = true,
    onboardingHintsVisible = false,
)

internal fun AppSceneUiState.unlockTransitions(): AppSceneUiState = copy(transitionLocked = false)

internal fun AppSceneUiState.showOnboardingHints(): AppSceneUiState = copy(onboardingHintsVisible = true)

internal fun AppSceneUiState.hideOnboardingHints(): AppSceneUiState = copy(onboardingHintsVisible = false)

internal fun AppSceneUiState.enterHome(): AppSceneUiState = copy(currentScene = AppScene.Home)

internal fun AppSceneUiState.prepareLibraryEntry(nextLibraryRefreshSignal: Int): AppSceneUiState = copy(
    homeContentVisible = false,
    libraryContentVisible = false,
    libraryRefreshSignal = nextLibraryRefreshSignal,
)

internal fun AppSceneUiState.enterLibrary(): AppSceneUiState = copy(currentScene = AppScene.Library)

internal fun AppSceneUiState.prepareBadgeBookEntry(nextBadgeBookRefreshSignal: Int): AppSceneUiState = copy(
    homeContentVisible = false,
    badgeBookContentVisible = false,
    badgeBookRefreshSignal = nextBadgeBookRefreshSignal,
)

internal fun AppSceneUiState.enterBadgeBook(): AppSceneUiState = copy(currentScene = AppScene.BadgeBook)

internal fun AppSceneUiState.preparePackSelection(nextPackRefreshSignal: Int): AppSceneUiState = copy(
    currentScene = AppScene.PackSelection,
    homeContentVisible = false,
    packSceneVisible = false,
    packExtensionListVisible = false,
    packRefreshSignal = nextPackRefreshSignal,
    packReadySignal = 0,
    selectedPackRevealBounds = null,
    packOpeningExitSignal = 0,
)

internal fun AppSceneUiState.switchPackSelectionToHome(): AppSceneUiState = copy(
    currentScene = AppScene.Home,
    launchLogoVisible = true,
    launchLogoRaised = true,
    selectedPackRevealBounds = null,
    packOpeningExitSignal = 0,
)

internal fun AppSceneUiState.enterPackOpening(): AppSceneUiState = copy(
    currentScene = AppScene.PackOpening,
    packSceneVisible = false,
    packExtensionListVisible = false,
    packOpeningExitSignal = 0,
)

internal fun AppSceneUiState.preparePackOpeningReturnToHome(): AppSceneUiState = copy(
    currentScene = AppScene.Home,
    launchLogoVisible = true,
    launchLogoRaised = true,
    homeContentVisible = false,
    packSceneVisible = false,
    packExtensionListVisible = false,
    selectedPackRevealBounds = null,
    packOpeningExitSignal = 0,
)

internal fun AppSceneUiState.finishPackOpeningToHome(): AppSceneUiState = preparePackOpeningReturnToHome().copy(
    homeContentVisible = true,
)

internal fun AppSceneUiState.withPackRevealBounds(bounds: PackRevealBounds?): AppSceneUiState =
    copy(selectedPackRevealBounds = bounds)

internal fun AppSceneUiState.registerPackReady(
    newlyUnlockedBadges: List<BadgeItem>,
    deferBadgeCelebration: Boolean,
): AppSceneUiState = copy(
    packReadySignal = packReadySignal + 1,
    pendingBadgeCelebration = newlyUnlockedBadges,
    badgeCelebrationDeferred = newlyUnlockedBadges.isNotEmpty() && deferBadgeCelebration,
)

internal fun AppSceneUiState.requestPackOpeningExit(): AppSceneUiState = copy(
    packOpeningExitSignal = packOpeningExitSignal + 1,
)

internal fun AppSceneUiState.clearPendingBadgeCelebration(): AppSceneUiState = copy(
    pendingBadgeCelebration = emptyList(),
    badgeCelebrationDeferred = false,
)

internal fun AppSceneUiState.resumePendingBadgeCelebration(): AppSceneUiState = copy(
    badgeCelebrationDeferred = false,
)

internal fun AppSceneUiState.withCoachmarkTargetBounds(
    target: NewPlayerOnboardingTarget,
    bounds: Rect?,
): AppSceneUiState = copy(
    coachmarkTargetBounds = coachmarkTargetBounds.toMutableMap().apply {
        if (bounds == null) {
            remove(target)
        } else {
            put(target, bounds)
        }
    },
)
