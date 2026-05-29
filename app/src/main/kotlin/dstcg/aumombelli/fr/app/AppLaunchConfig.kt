package fr.aumombelli.dstcg.app

import fr.aumombelli.dstcg.ui.motion.AppScene

internal const val AppLaunchSceneExtraKey = "fr.aumombelli.dstcg.extra.LAUNCH_SCENE"
internal const val AppResetProgressExtraKey = "fr.aumombelli.dstcg.extra.RESET_PROGRESS"

enum class AppLaunchScene(
    val wireValue: String,
) {
    Start("start"),
    Home("home"),
}

data class AppLaunchConfig(
    val scene: AppLaunchScene = AppLaunchScene.Start,
    val resetProgressOnLaunch: Boolean = false,
)

internal fun parseAppLaunchConfig(
    rawSceneValue: String?,
    resetProgressOnLaunch: Boolean,
): AppLaunchConfig {
    val scene = when (rawSceneValue) {
        AppLaunchScene.Home.wireValue,
        "main-menu",
        -> AppLaunchScene.Home

        AppLaunchScene.Start.wireValue -> AppLaunchScene.Start
        else -> AppLaunchScene.Start
    }
    return AppLaunchConfig(
        scene = scene,
        resetProgressOnLaunch = resetProgressOnLaunch,
    )
}

internal fun initialAppSceneUiState(launchConfig: AppLaunchConfig): AppSceneUiState = when (launchConfig.scene) {
    AppLaunchScene.Start -> AppSceneUiState(
        onboardingHintsVisible = false,
    )
    AppLaunchScene.Home -> AppSceneUiState(
        currentScene = AppScene.Home,
        homeContentVisible = !launchConfig.resetProgressOnLaunch,
    )
}
