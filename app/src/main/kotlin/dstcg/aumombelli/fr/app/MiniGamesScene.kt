package fr.aumombelli.dstcg.app

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import fr.aumombelli.dstcg.AppContainer
import fr.aumombelli.dstcg.audio.SoundCue
import fr.aumombelli.dstcg.feature.minigames.MemoryGameScreen
import fr.aumombelli.dstcg.feature.minigames.MiniGamesMenuScreen
import fr.aumombelli.dstcg.feature.minigames.MiniGamesScreenUiState
import fr.aumombelli.dstcg.feature.minigames.MiniGamesViewModel
import fr.aumombelli.dstcg.feature.minigames.ObservatoryGameScreen
import fr.aumombelli.dstcg.feature.minigames.QuizGameScreen
import fr.aumombelli.dstcg.feature.minigames.TimelineGameScreen
import fr.aumombelli.dstcg.ui.viewmodel.DstcgViewModelFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
internal fun MiniGamesScene(
    appContainer: AppContainer,
    sceneState: AppSceneUiState,
    transitions: AppSceneTransitionController,
    scope: CoroutineScope,
) {
    val miniGamesViewModel: MiniGamesViewModel = viewModel(
        key = "mini-games",
        factory = DstcgViewModelFactory {
            MiniGamesViewModel(
                miniGamesRepository = appContainer.miniGamesRepository,
                catalogRepository = appContainer.catalogRepository,
                progressRepository = appContainer.progressRepository,
            )
        },
    )
    val uiState by miniGamesViewModel.uiState.collectAsState()

    LaunchedEffect(sceneState.miniGamesMenuContentVisible) {
        if (sceneState.miniGamesMenuContentVisible && uiState.screen is MiniGamesScreenUiState.Menu) {
            miniGamesViewModel.refresh()
        }
    }

    val backAllowed = !sceneState.transitionLocked
    val playNavigationSound = {
        appContainer.audioController.play(SoundCue.UiNavigate)
    }
    val navigateBackToHome: () -> Unit = {
        if (backAllowed) {
            playNavigationSound()
            scope.launch { transitions.animateMiniGamesMenuToHome() }
        }
    }
    val navigateBackToMiniGamesMenu: () -> Unit = {
        if (backAllowed) {
            playNavigationSound()
            miniGamesViewModel.backToMenu()
        }
    }
    val navigateBack: () -> Unit = {
        if (uiState.screen is MiniGamesScreenUiState.Menu) {
            navigateBackToHome()
        } else {
            navigateBackToMiniGamesMenu()
        }
    }
    val openMiniGame: (() -> Unit) -> Unit = { open ->
        if (backAllowed) {
            playNavigationSound()
            open()
        }
    }

    BackHandler(enabled = backAllowed) {
        navigateBack()
    }

    when (uiState.screen) {
        is MiniGamesScreenUiState.Menu -> MiniGamesMenuScreen(
            state = uiState,
            onBack = navigateBackToHome,
            onOpenQuiz = { openMiniGame(miniGamesViewModel::openQuiz) },
            onOpenMemory = { openMiniGame(miniGamesViewModel::openMemory) },
            onOpenTimeline = { openMiniGame(miniGamesViewModel::openTimeline) },
            onOpenObservatory = { openMiniGame(miniGamesViewModel::openObservatory) },
            contentVisible = sceneState.miniGamesMenuContentVisible,
            interactionsEnabled = !sceneState.transitionLocked,
        )

        is MiniGamesScreenUiState.QuizDifficultySelection,
        is MiniGamesScreenUiState.QuizPlaying,
        is MiniGamesScreenUiState.QuizResult,
        is MiniGamesScreenUiState.QuizUnavailable -> QuizGameScreen(
            state = uiState,
            onBackToMenu = navigateBackToMiniGamesMenu,
            onSelectDifficulty = miniGamesViewModel::selectQuizDifficulty,
            onSelectAnswer = miniGamesViewModel::selectQuizAnswer,
            onContinue = miniGamesViewModel::continueQuiz,
        )

        is MiniGamesScreenUiState.TimelineDifficultySelection,
        is MiniGamesScreenUiState.TimelinePlaying,
        is MiniGamesScreenUiState.TimelineResult,
        is MiniGamesScreenUiState.TimelineUnavailable -> TimelineGameScreen(
            state = uiState,
            onBackToMenu = navigateBackToMiniGamesMenu,
            onSelectDifficulty = miniGamesViewModel::selectTimelineDifficulty,
            onPlaceCard = miniGamesViewModel::placeTimelineCard,
            onReturnCardToHand = miniGamesViewModel::returnTimelineCardToHand,
            onValidate = miniGamesViewModel::validateTimeline,
        )

        is MiniGamesScreenUiState.ObservatoryDifficultySelection,
        is MiniGamesScreenUiState.ObservatoryPlaying,
        is MiniGamesScreenUiState.ObservatoryResult,
        is MiniGamesScreenUiState.ObservatoryUnavailable -> ObservatoryGameScreen(
            state = uiState,
            onBackToMenu = navigateBackToMiniGamesMenu,
            onSelectDifficulty = miniGamesViewModel::selectObservatoryDifficulty,
            onSetDomeProgress = miniGamesViewModel::setObservatoryDomeProgress,
            onValidateDomeProgress = miniGamesViewModel::validateObservatoryDomeProgress,
            onSetAzimuth = miniGamesViewModel::setObservatoryAzimuth,
            onSetAltitude = miniGamesViewModel::setObservatoryAltitude,
            onValidateAlignment = miniGamesViewModel::validateObservatoryAlignment,
            onSetFocus = miniGamesViewModel::setObservatoryFocus,
            onValidateFocus = miniGamesViewModel::validateObservatoryFocus,
            onScrubCloud = miniGamesViewModel::scrubObservatoryCloud,
            onCapture = miniGamesViewModel::captureObservatoryTarget,
        )

        else -> MemoryGameScreen(
            state = uiState,
            onBackToMenu = navigateBackToMiniGamesMenu,
            onSelectDifficulty = miniGamesViewModel::selectMemoryDifficulty,
            onSelectCell = miniGamesViewModel::selectMemoryCell,
        )
    }
}
