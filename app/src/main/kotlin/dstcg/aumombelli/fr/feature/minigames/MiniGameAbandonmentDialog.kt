package fr.aumombelli.dstcg.feature.minigames

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag

@Composable
internal fun MiniGameAbandonmentDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Abandonner la partie ?") },
        text = {
            Text(
                "Si tu quittes maintenant, le jeu sera considéré comme abandonné " +
                    "et tu ne pourras pas y rejouer aujourd'hui.",
            )
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                modifier = Modifier.testTag("mini-game-abandonment-confirm"),
            ) {
                Text("Abandonner")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("mini-game-abandonment-cancel"),
            ) {
                Text("Continuer la partie")
            }
        },
        modifier = Modifier.testTag("mini-game-abandonment-dialog"),
    )
}

internal fun MiniGamesScreenUiState.requiresAbandonmentConfirmation(): Boolean = when (this) {
    is MiniGamesScreenUiState.QuizPlaying,
    is MiniGamesScreenUiState.MemoryPlaying,
    is MiniGamesScreenUiState.TimelinePlaying,
    is MiniGamesScreenUiState.ObservatoryPlaying -> true

    else -> false
}
