package fr.aumombelli.gatcha.ui.screen

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF08101D),
                        Color(0xFF12243F),
                        Color(0xFF1A3052),
                    ),
                ),
            ),
    ) {
        Card(
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier
                .align(Alignment.Center)
                .padding(24.dp),
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
            ) {
                Text(
                    text = "Gatcha",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "Connecte-toi pour retrouver ta collection ou créer un nouveau compte.",
                    style = MaterialTheme.typography.bodyMedium,
                )

                OutlinedTextField(
                    value = state.username,
                    onValueChange = onUsernameChange,
                    label = { Text("Username") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                if (state.isCreateMode) {
                    OutlinedTextField(
                        value = state.email,
                        onValueChange = onEmailChange,
                        label = { Text("Email") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                OutlinedTextField(
                    value = state.password,
                    onValueChange = onPasswordChange,
                    label = { Text("Password") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                )

                state.errorMessage?.let { error ->
                    Text(
                        text = error,
                        color = Color(0xFFFF7A7A),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }

                Button(
                    onClick = onSubmit,
                    enabled = !state.isLoading,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (state.isLoading) {
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
                    modifier = Modifier.align(Alignment.End),
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
