package fr.aumombelli.dstcg.app

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import fr.aumombelli.dstcg.AppContainer
import fr.aumombelli.dstcg.feature.minigames.MemoryGameScreen
import fr.aumombelli.dstcg.feature.minigames.MiniGamesMenuScreen
import fr.aumombelli.dstcg.feature.minigames.MiniGamesScreenUiState
import fr.aumombelli.dstcg.feature.minigames.MiniGamesViewModel
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
    val navigateBackToHome: () -> Unit = {
        if (backAllowed) {
            scope.launch { transitions.animateMiniGamesMenuToHome() }
        }
    }
    val navigateBack: () -> Unit = {
        if (uiState.screen is MiniGamesScreenUiState.Menu) {
            navigateBackToHome()
        } else {
            miniGamesViewModel.backToMenu()
        }
    }

    BackHandler(enabled = backAllowed) {
        navigateBack()
    }

    when (uiState.screen) {
        is MiniGamesScreenUiState.Menu -> MiniGamesMenuScreen(
            state = uiState,
            onBack = navigateBackToHome,
            onOpenQuiz = miniGamesViewModel::openQuiz,
            onOpenMemory = miniGamesViewModel::openMemory,
            onResetDailyAttempts = miniGamesViewModel::resetDailyAttemptsForDebug,
            onOpenTimeline = miniGamesViewModel::openTimeline,
            contentVisible = sceneState.miniGamesMenuContentVisible,
            interactionsEnabled = !sceneState.transitionLocked,
        )

        is MiniGamesScreenUiState.QuizDifficultySelection,
        is MiniGamesScreenUiState.QuizPlaying,
        is MiniGamesScreenUiState.QuizResult,
        is MiniGamesScreenUiState.QuizUnavailable -> QuizGameScreen(
            state = uiState,
            onBackToMenu = miniGamesViewModel::backToMenu,
            onSelectDifficulty = miniGamesViewModel::selectQuizDifficulty,
            onSelectAnswer = miniGamesViewModel::selectQuizAnswer,
            onContinue = miniGamesViewModel::continueQuiz,
        )

        is MiniGamesScreenUiState.TimelinePlaying,
        is MiniGamesScreenUiState.TimelineResult,
        is MiniGamesScreenUiState.TimelineUnavailable -> TimelineGameScreen(
            state = uiState,
            onBackToMenu = miniGamesViewModel::backToMenu,
            onPlaceCard = miniGamesViewModel::placeTimelineCard,
            onValidate = miniGamesViewModel::validateTimeline,
        )

        else -> MemoryGameScreen(
            state = uiState,
            onBackToMenu = miniGamesViewModel::backToMenu,
            onSelectDifficulty = miniGamesViewModel::selectMemoryDifficulty,
            onSelectCell = miniGamesViewModel::selectMemoryCell,
        )
    }
}
