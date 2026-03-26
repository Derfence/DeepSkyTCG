package fr.aumombelli.gatcha.ui.screen

import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import fr.aumombelli.gatcha.feature.auth.LoginScreen as AuthFeatureLoginScreen
import fr.aumombelli.gatcha.ui.viewmodel.LoginUiState

@Composable
fun LoginScreen(
    state: LoginUiState,
    onUsernameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onModeToggle: () -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
    showBackground: Boolean = true,
    contentVisible: Boolean = true,
    onFormTopChanged: (Float) -> Unit = {},
) {
    AuthFeatureLoginScreen(
        state = state,
        onUsernameChange = onUsernameChange,
        onEmailChange = onEmailChange,
        onPasswordChange = onPasswordChange,
        onModeToggle = onModeToggle,
        onSubmit = onSubmit,
        modifier = modifier,
        showBackground = showBackground,
        contentVisible = contentVisible,
        onFormTopChanged = onFormTopChanged,
    )
}
