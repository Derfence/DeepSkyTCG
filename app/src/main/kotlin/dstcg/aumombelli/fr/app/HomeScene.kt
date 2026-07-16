package fr.aumombelli.dstcg.app

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import fr.aumombelli.dstcg.AppContainer
import fr.aumombelli.dstcg.audio.SoundCue
import fr.aumombelli.dstcg.data.BackupRepository
import fr.aumombelli.dstcg.data.readBackupBytesLimited
import fr.aumombelli.dstcg.data.writeBackupDocument
import fr.aumombelli.dstcg.feature.backup.BackupViewModel
import fr.aumombelli.dstcg.feature.home.HomeScreen
import fr.aumombelli.dstcg.feature.home.HomeViewModel
import fr.aumombelli.dstcg.model.NewPlayerOnboardingStep
import fr.aumombelli.dstcg.ui.motion.BrandLogoVariant
import fr.aumombelli.dstcg.ui.viewmodel.DstcgViewModelFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
internal fun HomeScene(
    appContainer: AppContainer,
    activity: Activity?,
    sceneState: AppSceneUiState,
    onboardingCoordinator: NewPlayerOnboardingCoordinator,
    onboardingStep: NewPlayerOnboardingStep?,
    hasEnteredHomeOnce: MutableState<Boolean>,
    homeContentEntranceSettled: MutableState<Boolean>,
    homeLogoVariant: BrandLogoVariant,
    transitions: AppSceneTransitionController,
    scope: CoroutineScope,
    updateSceneState: ((AppSceneUiState) -> AppSceneUiState) -> Unit,
) {
    val homeViewModel: HomeViewModel = viewModel(
        key = "home",
        factory = DstcgViewModelFactory {
            HomeViewModel(
                progressRepository = appContainer.progressRepository,
                craftingRepository = appContainer.craftingRepository,
            )
        },
    )
    val uiState by homeViewModel.uiState.collectAsState()
    val backupViewModel: BackupViewModel = viewModel(
        key = "backup",
        factory = DstcgViewModelFactory {
            BackupViewModel(appContainer.backupGateway)
        },
    )
    val backupUiState by backupViewModel.uiState.collectAsState()
    val audioSettings by appContainer.audioController.settings.collectAsState()
    val context = LocalContext.current
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream"),
    ) { uri ->
        val document = backupUiState.exportDocument
        if (uri == null || document == null) {
            backupViewModel.consumeExportDocument(saved = false)
        } else {
            backupViewModel.beginExportDocumentWrite()
            scope.launch {
                runCatching {
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openOutputStream(uri, "w")?.use { output ->
                            output.writeBackupDocument(document.bytes)
                        } ?: error("Le fichier de destination n'a pas pu être ouvert.")
                    }
                }.onSuccess {
                    backupViewModel.consumeExportDocument(saved = true)
                }.onFailure { exception ->
                    backupViewModel.reportDocumentWriteFailure(
                        exception.message ?: "Impossible d'écrire la sauvegarde.",
                    )
                }
            }
        }
    }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            backupViewModel.beginImportDocumentRead()
            scope.launch {
                runCatching {
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openInputStream(uri)?.use { input ->
                            input.readBackupBytesLimited(BackupRepository.MAX_BACKUP_SIZE_BYTES)
                        } ?: error("Le fichier sélectionné n'a pas pu être ouvert.")
                    }
                }.onSuccess(backupViewModel::acceptImportDocument)
                    .onFailure { exception ->
                        backupViewModel.reportDocumentReadFailure(
                            exception.message ?: "Impossible de lire la sauvegarde.",
                        )
                    }
            }
        } else {
            backupViewModel.cancelImportDocumentSelection()
        }
    }

    LaunchedEffect(backupUiState.exportDocument) {
        backupUiState.exportDocument?.let { exportLauncher.launch(it.fileName) }
    }

    LaunchedEffect(backupUiState.openDocumentRequestId) {
        if (backupUiState.openDocumentRequestId > 0) {
            importLauncher.launch(arrayOf("application/octet-stream", "application/json", "text/plain"))
        }
    }

    LaunchedEffect(backupUiState.importCompletedId) {
        if (backupUiState.importCompletedId > 0) {
            homeViewModel.refresh()
            onboardingCoordinator.syncFromProgress()
        }
    }

    LaunchedEffect(Unit) {
        if (hasEnteredHomeOnce.value) {
            homeViewModel.refresh()
        } else {
            hasEnteredHomeOnce.value = true
        }
    }

    BackHandler(
        enabled = !sceneState.transitionLocked &&
            NewPlayerOnboardingInteractionPolicy.allowsHomeExit(onboardingStep),
    ) {
        activity?.finish()
    }

    HomeScreen(
        state = uiState,
        onOpenPack = {
            if (
                !sceneState.transitionLocked &&
                NewPlayerOnboardingInteractionPolicy.allowsHomeOpenPack(onboardingStep)
            ) {
                scope.launch {
                    onboardingCoordinator.onHomeOpenPackSelected()
                    transitions.animateHomeToPackSelection()
                }
            }
        },
        onOpenLibrary = {
            if (
                !sceneState.transitionLocked &&
                uiState.isLibraryMenuVisible &&
                NewPlayerOnboardingInteractionPolicy.allowsHomeLibrary(onboardingStep)
            ) {
                appContainer.audioController.play(SoundCue.UiNavigate)
                homeViewModel.markLibrarySeen()
                scope.launch {
                    val shouldResumeBadgeCelebration = onboardingCoordinator.onLibraryOpened()
                    if (shouldResumeBadgeCelebration) {
                        updateSceneState { it.resumePendingBadgeCelebration() }
                    }
                    transitions.animateHomeToLibrary()
                }
            }
        },
        onOpenCrafting = {
            if (
                !sceneState.transitionLocked &&
                uiState.isCraftingMenuAvailable &&
                NewPlayerOnboardingInteractionPolicy.allowsHomeCrafting(onboardingStep)
            ) {
                appContainer.audioController.play(SoundCue.UiNavigate)
                scope.launch {
                    onboardingCoordinator.onCraftingOpened()
                    transitions.animateHomeToCrafting()
                }
            }
        },
        onOpenEquipment = {
            if (
                !sceneState.transitionLocked &&
                NewPlayerOnboardingInteractionPolicy.allowsHomeEquipment(onboardingStep)
            ) {
                appContainer.audioController.play(SoundCue.UiNavigate)
                homeViewModel.markEquipmentSeen()
                scope.launch {
                    onboardingCoordinator.onEquipmentOpened()
                    transitions.animateHomeToEquipment()
                }
            }
        },
        onOpenBadgeBook = {
            if (
                !sceneState.transitionLocked &&
                uiState.isBadgeBookMenuVisible &&
                NewPlayerOnboardingInteractionPolicy.allowsHomeBadgeBook(onboardingStep)
            ) {
                appContainer.audioController.play(SoundCue.UiNavigate)
                homeViewModel.markBadgeBookSeen()
                scope.launch {
                    onboardingCoordinator.onBadgeBookOpened()
                    transitions.animateHomeToBadgeBook()
                }
            }
        },
        onOpenMiniGamesMenu = {
            if (
                !sceneState.transitionLocked &&
                uiState.isMiniGamesMenuVisible &&
                NewPlayerOnboardingInteractionPolicy.allowsHomeMiniGames(onboardingStep)
            ) {
                appContainer.audioController.play(SoundCue.UiNavigate)
                homeViewModel.markMiniGamesSeen()
                scope.launch {
                    onboardingCoordinator.onMiniGamesMenuOpened()
                    transitions.animateHomeToMiniGamesMenu()
                }
            }
        },
        onResetProgress = homeViewModel::resetProgress,
        onResetNewPlayerOnboarding = {
            homeViewModel.resetNewPlayerOnboarding(
                onResetCompleted = {
                    scope.launch {
                        onboardingCoordinator.syncFromProgress()
                    }
                },
            )
        },
        backupState = backupUiState,
        onRequestBackupExport = backupViewModel::requestExport,
        onRequestBackupImport = backupViewModel::requestImportDocument,
        onSubmitBackupExportPassword = backupViewModel::submitExportPassword,
        onSubmitBackupImportPassword = backupViewModel::submitImportPassword,
        onConfirmBackupImport = backupViewModel::confirmImport,
        onDismissBackupDialog = backupViewModel::dismissDialog,
        soundEnabled = audioSettings.enabled,
        onSoundEnabledChange = { enabled ->
            scope.launch {
                appContainer.audioController.setEnabled(enabled)
            }
        },
        showBackground = false,
        contentVisible = sceneState.homeContentVisible,
        interactionsEnabled = !sceneState.transitionLocked,
        allowAuxiliaryActions = NewPlayerOnboardingInteractionPolicy
            .allowsHomeAuxiliaryActions(onboardingStep),
        showMiniGamesDiscoveryHint = onboardingStep == NewPlayerOnboardingStep.DiscoverMiniGames &&
            sceneState.onboardingHintsVisible &&
            uiState.isMiniGamesMenuVisible,
        homeLogoVariant = homeLogoVariant,
        onHomeLogoLayoutChanged = { badgeCenterYInRootPx, landingSizePx ->
            updateSceneState {
                it.withHomeLogoBadgeLayout(
                    centerYPx = badgeCenterYInRootPx,
                    landingSizePx = landingSizePx,
                )
            }
        },
        onContentEntranceSettledChanged = { settled ->
            homeContentEntranceSettled.value = settled
        },
        onCoachmarkTargetBoundsChanged = { target, bounds ->
            updateSceneState { it.withCoachmarkTargetBounds(target, bounds) }
        },
    )
}
