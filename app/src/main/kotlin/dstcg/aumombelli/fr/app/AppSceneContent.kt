package fr.aumombelli.dstcg.app

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import fr.aumombelli.dstcg.AppContainer
import fr.aumombelli.dstcg.model.NewPlayerOnboardingStep
import fr.aumombelli.dstcg.model.TradeCardCandidate
import fr.aumombelli.dstcg.ui.motion.AppScene
import fr.aumombelli.dstcg.ui.motion.BrandLogoVariant
import kotlinx.coroutines.CoroutineScope

@Composable
internal fun AppSceneContent(
    appContainer: AppContainer,
    activity: Activity?,
    sceneState: AppSceneUiState,
    onboardingCoordinator: NewPlayerOnboardingCoordinator,
    onboardingStep: NewPlayerOnboardingStep?,
    blockingModalSpec: NewPlayerBlockingModalSpec?,
    selectedTradeCandidate: MutableState<TradeCardCandidate?>,
    hasEnteredHomeOnce: MutableState<Boolean>,
    homeContentEntranceSettled: MutableState<Boolean>,
    homeLogoVariant: BrandLogoVariant,
    transitions: AppSceneTransitionController,
    scope: CoroutineScope,
    updateSceneState: ((AppSceneUiState) -> AppSceneUiState) -> Unit,
) {
    when (sceneState.currentScene) {
        AppScene.Home -> HomeScene(
            appContainer = appContainer,
            activity = activity,
            sceneState = sceneState,
            onboardingCoordinator = onboardingCoordinator,
            onboardingStep = onboardingStep,
            hasEnteredHomeOnce = hasEnteredHomeOnce,
            homeContentEntranceSettled = homeContentEntranceSettled,
            homeLogoVariant = homeLogoVariant,
            transitions = transitions,
            scope = scope,
            updateSceneState = updateSceneState,
        )

        AppScene.Library -> LibraryScene(
            appContainer = appContainer,
            sceneState = sceneState,
            onboardingCoordinator = onboardingCoordinator,
            onboardingStep = onboardingStep,
            blockingModalSpec = blockingModalSpec,
            selectedTradeCandidate = selectedTradeCandidate,
            transitions = transitions,
            scope = scope,
            updateSceneState = updateSceneState,
        )

        AppScene.Crafting -> CraftingScene(
            appContainer = appContainer,
            sceneState = sceneState,
            onboardingCoordinator = onboardingCoordinator,
            onboardingStep = onboardingStep,
            blockingModalSpec = blockingModalSpec,
            transitions = transitions,
            scope = scope,
            updateSceneState = updateSceneState,
        )

        AppScene.Equipment -> EquipmentScene(
            appContainer = appContainer,
            sceneState = sceneState,
            onboardingCoordinator = onboardingCoordinator,
            onboardingStep = onboardingStep,
            transitions = transitions,
            scope = scope,
            updateSceneState = updateSceneState,
        )

        AppScene.BadgeBook -> BadgeBookScene(
            appContainer = appContainer,
            sceneState = sceneState,
            transitions = transitions,
            scope = scope,
        )

        AppScene.MiniGamesMenu -> MiniGamesScene(
            sceneState = sceneState,
            transitions = transitions,
            scope = scope,
        )

        AppScene.PackSelection,
        AppScene.PackOpening,
        -> PackScene(
            appContainer = appContainer,
            sceneState = sceneState,
            onboardingCoordinator = onboardingCoordinator,
            onboardingStep = onboardingStep,
            transitions = transitions,
            scope = scope,
            updateSceneState = updateSceneState,
        )
    }
}
