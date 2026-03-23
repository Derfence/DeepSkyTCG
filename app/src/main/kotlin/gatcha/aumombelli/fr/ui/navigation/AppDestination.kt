package gatcha.aumombelli.fr.ui.navigation

sealed class AppDestination(val route: String) {
    data object Login : AppDestination("login")
    data object MainMenu : AppDestination("main_menu")
    data object Library : AppDestination("library")
    data object PackSelection : AppDestination("pack_selection")
    data object PackOpening : AppDestination("pack_opening")
}
