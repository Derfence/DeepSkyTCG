package fr.aumombelli.gatcha.ui.screen

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import fr.aumombelli.gatcha.ui.theme.AuroraTeal
import fr.aumombelli.gatcha.ui.theme.EmberGold
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
    val formAlpha by animateFloatAsState(
        targetValue = if (contentVisible) 1f else 0f,
        animationSpec = tween(durationMillis = 520, easing = FastOutSlowInEasing),
        label = "login-form-alpha",
    )
    val backgroundBrush = if (showBackground) {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFF08101D),
                Color(0xFF12243F),
                Color(0xFF1A3052),
            ),
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                Color.Transparent,
                Color.Transparent,
            ),
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(brush = backgroundBrush),
    ) {
        Card(
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier
                .align(Alignment.Center)
                .onGloballyPositioned { coordinates ->
                    onFormTopChanged(coordinates.positionInRoot().y)
                }
                .graphicsLayer {
                    alpha = formAlpha
                }
                .gatchaContentInsetsPadding(includeBottom = true)
                .padding(24.dp),
        ) {
            val fieldsEnabled = !state.isLoading && !state.isTransitioningToMenu
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp)
                    .testTag("app-launch-login-form"),
            ) {
                Text(
                    text = "Gatcha",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.testTag("login-title"),
                )
                Text(
                    text = "Connecte-toi pour retrouver ta collection ou creer un nouveau compte.",
                    style = MaterialTheme.typography.bodyMedium,
                )

                OutlinedTextField(
                    value = state.username,
                    onValueChange = onUsernameChange,
                    label = { Text("Username") },
                    singleLine = true,
                    enabled = fieldsEnabled,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("login-username"),
                )

                if (state.isCreateMode) {
                    OutlinedTextField(
                        value = state.email,
                        onValueChange = onEmailChange,
                        label = { Text("Email") },
                        singleLine = true,
                        enabled = fieldsEnabled,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("login-email"),
                    )
                }

                OutlinedTextField(
                    value = state.password,
                    onValueChange = onPasswordChange,
                    label = { Text("Password") },
                    singleLine = true,
                    enabled = fieldsEnabled,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("login-password"),
                )

                state.errorMessage?.let { error ->
                    Text(
                        text = error,
                        color = Color(0xFFFF7A7A),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.testTag("login-error"),
                    )
                }

                Button(
                    onClick = onSubmit,
                    enabled = fieldsEnabled,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("login-submit"),
                ) {
                    if (state.isLoading || state.isTransitioningToMenu) {
                        CircularProgressIndicator(
                            color = AuroraTeal,
                            strokeWidth = 2.dp,
                            modifier = Modifier.padding(vertical = 4.dp),
                        )
                    } else {
                        Text(if (state.isCreateMode) "Create account" else "Login")
                    }
                }

                TextButton(
                    onClick = onModeToggle,
                    enabled = fieldsEnabled,
                    modifier = Modifier
                        .align(Alignment.End)
                        .testTag("login-toggle-mode"),
                ) {
                    Text(
                        text = if (state.isCreateMode) "I already have an account" else "Create a new account",
                        color = EmberGold,
                    )
                }
            }
        }
    }
}
