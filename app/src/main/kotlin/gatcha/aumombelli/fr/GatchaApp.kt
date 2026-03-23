package fr.aumombelli.gatcha

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import fr.aumombelli.gatcha.ui.navigation.AppDestination
import fr.aumombelli.gatcha.ui.screen.LibraryScreen
import fr.aumombelli.gatcha.ui.screen.LoginScreen
import fr.aumombelli.gatcha.ui.screen.MainMenuScreen
import fr.aumombelli.gatcha.ui.screen.PackOpeningScreen
import fr.aumombelli.gatcha.ui.screen.PackSelectionScreen
import fr.aumombelli.gatcha.ui.viewmodel.GatchaViewModelFactory
import fr.aumombelli.gatcha.ui.viewmodel.LibraryViewModel
import fr.aumombelli.gatcha.ui.viewmodel.LoginEvent
import fr.aumombelli.gatcha.ui.viewmodel.LoginViewModel
import fr.aumombelli.gatcha.ui.viewmodel.PackEvent
import fr.aumombelli.gatcha.ui.viewmodel.PackOpeningViewModel
import fr.aumombelli.gatcha.ui.viewmodel.PackViewModel

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
