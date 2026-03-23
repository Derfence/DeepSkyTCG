package gatcha.aumombelli.fr

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import gatcha.aumombelli.fr.ui.navigation.AppDestination
import gatcha.aumombelli.fr.ui.screen.LibraryScreen
import gatcha.aumombelli.fr.ui.screen.LoginScreen
import gatcha.aumombelli.fr.ui.screen.MainMenuScreen
import gatcha.aumombelli.fr.ui.screen.PackOpeningScreen
import gatcha.aumombelli.fr.ui.screen.PackSelectionScreen
import gatcha.aumombelli.fr.ui.viewmodel.GatchaViewModelFactory
import gatcha.aumombelli.fr.ui.viewmodel.LibraryViewModel
import gatcha.aumombelli.fr.ui.viewmodel.LoginEvent
import gatcha.aumombelli.fr.ui.viewmodel.LoginViewModel
import gatcha.aumombelli.fr.ui.viewmodel.PackEvent
import gatcha.aumombelli.fr.ui.viewmodel.PackOpeningViewModel
import gatcha.aumombelli.fr.ui.viewmodel.PackViewModel

@Composable
fun GatchaApp(appContainer: AppContainer) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = AppDestination.Login.route,
    ) {
        composable(AppDestination.Login.route) {
            val viewModel: LoginViewModel = viewModel(
                factory = GatchaViewModelFactory {
                    LoginViewModel(
                        apiService = appContainer.apiService,
                        sessionRepository = appContainer.sessionRepository,
                        collectionRepository = appContainer.collectionRepository,
                    )
                },
            )
            val uiState by viewModel.uiState.collectAsState()

            LaunchedEffect(viewModel) {
                viewModel.events.collect { event ->
                    when (event) {
                        LoginEvent.NavigateToMenu -> {
                            navController.navigate(AppDestination.MainMenu.route) {
                                popUpTo(AppDestination.Login.route) {
                                    inclusive = true
                                }
                            }
                        }
                    }
                }
            }

            LoginScreen(
                state = uiState,
                onUsernameChange = viewModel::updateUsername,
                onEmailChange = viewModel::updateEmail,
                onPasswordChange = viewModel::updatePassword,
                onModeToggle = viewModel::toggleMode,
                onSubmit = viewModel::submit,
            )
        }

        composable(AppDestination.MainMenu.route) {
            MainMenuScreen(
                onOpenPack = { navController.navigate(AppDestination.PackSelection.route) },
                onOpenLibrary = { navController.navigate(AppDestination.Library.route) },
                onLogout = {
                    appContainer.sessionRepository.clearActiveSession()
                    navController.navigate(AppDestination.Login.route) {
                        popUpTo(AppDestination.MainMenu.route) {
                            inclusive = true
                        }
                    }
                },
            )
        }

        composable(AppDestination.Library.route) {
            val viewModel: LibraryViewModel = viewModel(
                factory = GatchaViewModelFactory {
                    LibraryViewModel(
                        catalogRepository = appContainer.catalogRepository,
                        collectionRepository = appContainer.collectionRepository,
                    )
                },
            )
            val uiState by viewModel.uiState.collectAsState()

            LibraryScreen(
                state = uiState,
                onBack = { navController.popBackStack() },
                onRefresh = viewModel::refresh,
            )
        }

        composable(AppDestination.PackSelection.route) {
            val viewModel: PackViewModel = viewModel(
                factory = GatchaViewModelFactory {
                    PackViewModel(
                        catalogRepository = appContainer.catalogRepository,
                        collectionRepository = appContainer.collectionRepository,
                        packRepository = appContainer.packRepository,
                        sessionRepository = appContainer.sessionRepository,
                    )
                },
            )
            val uiState by viewModel.uiState.collectAsState()

            LaunchedEffect(viewModel) {
                viewModel.events.collect { event ->
                    when (event) {
                        PackEvent.NavigateToOpening -> navController.navigate(AppDestination.PackOpening.route)
                    }
                }
            }

            PackSelectionScreen(
                state = uiState,
                onBack = { navController.popBackStack() },
                onRefresh = viewModel::refresh,
                onOpenPack = viewModel::openPack,
            )
        }

        composable(AppDestination.PackOpening.route) {
            val viewModel: PackOpeningViewModel = viewModel(
                factory = GatchaViewModelFactory {
                    PackOpeningViewModel(appContainer.packRepository)
                },
            )
            val uiState by viewModel.uiState.collectAsState()

            PackOpeningScreen(
                state = uiState,
                onDone = {
                    navController.popBackStack(AppDestination.MainMenu.route, inclusive = false)
                },
            )
        }
    }
}
