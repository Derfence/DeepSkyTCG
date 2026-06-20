package fr.aumombelli.dstcg.app

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import fr.aumombelli.dstcg.AppContainer
import fr.aumombelli.dstcg.audio.SoundCue
import fr.aumombelli.dstcg.feature.library.LibraryScreen
import fr.aumombelli.dstcg.feature.library.LibraryViewModel
import fr.aumombelli.dstcg.feature.trade.TradeScreen
import fr.aumombelli.dstcg.feature.trade.TradeViewModel
import fr.aumombelli.dstcg.model.NewPlayerOnboardingStep
import fr.aumombelli.dstcg.model.TradeCardCandidate
import fr.aumombelli.dstcg.ui.viewmodel.DstcgViewModelFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
internal fun LibraryScene(
    appContainer: AppContainer,
    sceneState: AppSceneUiState,
    onboardingCoordinator: NewPlayerOnboardingCoordinator,
    onboardingStep: NewPlayerOnboardingStep?,
    blockingModalSpec: NewPlayerBlockingModalSpec?,
    selectedTradeCandidate: MutableState<TradeCardCandidate?>,
    transitions: AppSceneTransitionController,
    scope: CoroutineScope,
    updateSceneState: ((AppSceneUiState) -> AppSceneUiState) -> Unit,
) {
    val libraryViewModel: LibraryViewModel = viewModel(
        key = "library",
        factory = DstcgViewModelFactory {
            LibraryViewModel(
                catalogRepository = appContainer.catalogRepository,
                collectionRepository = appContainer.collectionRepository,
                progressRepository = appContainer.progressRepository,
            )
        },
    )
    val uiState by libraryViewModel.uiState.collectAsState()

    LaunchedEffect(sceneState.libraryRefreshSignal) {
        if (!uiState.isLoading || uiState.sections.isNotEmpty() || uiState.errorMessage != null) {
            libraryViewModel.refresh()
        }
    }

    val libraryBackVisible = onboardingStep != NewPlayerOnboardingStep.LearnLibraryVariants
    val libraryBackAllowed = libraryBackVisible && !sceneState.transitionLocked
    val navigateBackToHome: () -> Unit = {
        if (libraryBackAllowed) {
            appContainer.audioController.play(SoundCue.UiNavigate)
            scope.launch { transitions.animateLibraryToHome() }
        }
    }

    BackHandler(enabled = libraryBackAllowed) {
        navigateBackToHome()
    }

    LibraryScreen(
        state = uiState,
        onRefresh = libraryViewModel::refresh,
        onBack = if (libraryBackVisible) navigateBackToHome else null,
        onOpenTrade = { candidate ->
            if (onboardingStep != NewPlayerOnboardingStep.LearnLibraryVariants) {
                selectedTradeCandidate.value = candidate
            }
        },
        contentVisible = sceneState.libraryContentVisible,
        interactionsEnabled = onboardingStep != NewPlayerOnboardingStep.LearnLibraryVariants,
        showOnboardingHint = onboardingCoordinator.uiState.libraryCardHintVisible &&
            sceneState.onboardingHintsVisible,
        onOnboardingHintConsumed = onboardingCoordinator::onLibraryCardHintConsumed,
        showOnboardingVariantWalkthrough =
            blockingModalSpec?.kind == NewPlayerBlockingModalKind.LibraryVariants,
        onOnboardingVariantWalkthroughCompleted = {
            scope.launch {
                val shouldResumeBadgeCelebration =
                    onboardingCoordinator.onLibraryVariantWalkthroughCompleted()
                if (shouldResumeBadgeCelebration) {
                    updateSceneState { it.resumePendingBadgeCelebration() }
                }
            }
        },
    )

    selectedTradeCandidate.value?.let { tradeCandidate ->
        val tradeViewModel: TradeViewModel = viewModel(
            key = "trade-${tradeCandidate.card.id}-${tradeCandidate.variant.key}",
            factory = DstcgViewModelFactory {
                TradeViewModel(
                    selectedCandidate = tradeCandidate,
                    tradeRepository = appContainer.tradeRepository,
                )
            },
        )
        TradeScreen(
            viewModel = tradeViewModel,
            tradeGateway = appContainer.tradeRepository,
            onDismiss = {
                selectedTradeCandidate.value = null
                libraryViewModel.refresh()
            },
        )
    }
}
