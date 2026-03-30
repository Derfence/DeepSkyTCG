package fr.aumombelli.gatcha.app

import fr.aumombelli.gatcha.ui.motion.AppScene

internal const val AppLaunchSceneExtraKey = "fr.aumombelli.gatcha.extra.LAUNCH_SCENE"
internal const val AppResetProgressExtraKey = "fr.aumombelli.gatcha.extra.RESET_PROGRESS"

enum class AppLaunchScene(
    val wireValue: String,
) {
    Start("start"),
    MainMenu("main-menu"),
}

data class AppLaunchConfig(
    val scene: AppLaunchScene = AppLaunchScene.Start,
    val resetProgressOnLaunch: Boolean = false,
)

internal fun parseAppLaunchConfig(
    rawSceneValue: String?,
    resetProgressOnLaunch: Boolean,
): AppLaunchConfig = AppLaunchConfig(
    scene = AppLaunchScene.entries.firstOrNull { it.wireValue == rawSceneValue } ?: AppLaunchScene.Start,
    resetProgressOnLaunch = resetProgressOnLaunch,
)

internal fun initialAppSceneUiState(launchConfig: AppLaunchConfig): AppSceneUiState = when (launchConfig.scene) {
    AppLaunchScene.Start -> AppSceneUiState()
    AppLaunchScene.MainMenu -> AppSceneUiState(
        currentScene = AppScene.MainMenu,
        menuContentVisible = !launchConfig.resetProgressOnLaunch,
    )
}
