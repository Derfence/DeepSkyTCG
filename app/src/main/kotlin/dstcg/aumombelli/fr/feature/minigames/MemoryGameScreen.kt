package fr.aumombelli.dstcg.feature.minigames

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import fr.aumombelli.dstcg.model.MiniGameDifficulty
import fr.aumombelli.dstcg.ui.component.SceneNavigationButton
import fr.aumombelli.dstcg.ui.component.SceneNavigationIcon
import fr.aumombelli.dstcg.ui.motion.SkyBackdropVariant
import fr.aumombelli.dstcg.ui.screen.dstcgContentInsetsPadding

@Composable
internal fun MemoryGameScreen(
    state: MiniGamesUiState,
    onBackToMenu: () -> Unit,
    onSelectDifficulty: (MiniGameDifficulty) -> Unit,
    onSelectCell: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val resultScreen = state.screen as? MiniGamesScreenUiState.MemoryResult
    val playingCue = (state.screen as? MiniGamesScreenUiState.MemoryPlaying)?.feedbackEvent

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("memory-screen"),
    ) {
        MiniGameSceneBackdrop(
            variant = SkyBackdropVariant.Mountain,
            sparkleBoost = if (state.screen is MiniGamesScreenUiState.MemoryPlaying) 0.28f else 0.18f,
            modifier = Modifier.fillMaxSize(),
        )
        MiniGameFeedbackOverlay(
            cue = playingCue ?: resultScreen?.feedbackEvent,
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .dstcgContentInsetsPadding(includeBottom = true)
                .padding(horizontal = 18.dp, vertical = 18.dp),
        ) {
            SceneNavigationButton(
                icon = SceneNavigationIcon.Back,
                onClick = onBackToMenu,
                contentDescription = "Retour au menu des mini-jeux",
                testTag = "memory-back",
                modifier = Modifier.align(Alignment.TopStart),
            )

            Text(
                text = "Memory",
                color = Color.White,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 4.dp),
            )

            when (val screen = state.screen) {
                MiniGamesScreenUiState.Menu -> Unit
                MiniGamesScreenUiState.MemoryDifficultySelection -> MemoryDifficultySelection(
                    choices = state.memoryDifficultyChoices,
                    isLoading = state.isLoading,
                    onSelectDifficulty = onSelectDifficulty,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .fillMaxWidth(),
                )

                is MiniGamesScreenUiState.MemoryPlaying -> MemoryPlayingBoard(
                    playing = screen,
                    onSelectCell = onSelectCell,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .fillMaxSize(),
                )

                is MiniGamesScreenUiState.MemoryResult -> MemoryResultPanel(
                    result = screen,
                    onBackToMenu = onBackToMenu,
                    modifier = Modifier
                        .align(Alignment.Center),
                )

                is MiniGamesScreenUiState.MemoryUnavailable -> MemoryUnavailablePanel(
                    message = screen.message,
                    onBackToMenu = onBackToMenu,
                    modifier = Modifier
                        .align(Alignment.Center),
                )

                else -> Unit
            }
        }
    }
}

@Composable
private fun MemoryDifficultySelection(
    choices: List<MemoryDifficultyChoiceUi>,
    isLoading: Boolean,
    onSelectDifficulty: (MiniGameDifficulty) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .testTag("memory-difficulty-selection"),
    ) {
        Spacer(modifier = Modifier.height(56.dp))
        Text(
            text = "Choisis une grille",
            color = Color.White,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        choices.forEach { choice ->
            MemoryDifficultyRow(
                choice = choice,
                enabled = choice.enabled && !isLoading,
                onClick = { onSelectDifficulty(choice.difficulty) },
            )
        }
    }
}

@Composable
private fun MemoryDifficultyRow(
    choice: MemoryDifficultyChoiceUi,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(14.dp),
        color = if (enabled) Color(0xDD10283A) else Color(0x99101A25),
        contentColor = Color.White,
        modifier = Modifier
            .fillMaxWidth()
            .testTag(choice.testTag),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = choice.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "${choice.gridLabel} - ${choice.rewardLabel}",
                    color = Color(0xFFD6E7F7),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Text(
                text = choice.statusLabel,
                color = if (enabled) Color(0xFF88E6D2) else Color(0xFF9BA9B7),
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}

@Composable
private fun MemoryPlayingBoard(
    playing: MiniGamesScreenUiState.MemoryPlaying,
    onSelectCell: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxSize()
            .testTag("memory-playing"),
    ) {
        Spacer(modifier = Modifier.height(60.dp))
        Text(
            text = "${playing.difficultyName} - ${playing.gridLabel}",
            color = Color.White,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
        MemoryHud(
            playing = playing,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp, bottom = 14.dp),
        )
        BoxWithConstraints(
            modifier = Modifier
                .widthIn(max = 560.dp)
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center,
        ) {
            val gap = 6.dp
            val boardPadding = 10.dp
            val rows = playing.cells.chunked(playing.columns)
            val rowCount = rows.size.coerceAtLeast(1)
            val availableGridHeight = maxHeight -
                boardPadding * 2 -
                gap * (rowCount - 1)
            val cardHeight = (availableGridHeight / rowCount.toFloat())
                .coerceAtLeast(44.dp)

            MiniGameBoardSurface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(maxHeight),
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(gap),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    rows.forEach { row ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(gap),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(cardHeight),
                        ) {
                            row.forEach { cell ->
                                MemoryAnimatedCardTile(
                                    cell = cell,
                                    enabled = !playing.inputLocked && cell.state == MemoryCellState.Hidden,
                                    onClick = { onSelectCell(cell.index) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight(),
                                )
                            }
                            repeat(playing.columns - row.size) {
                                Spacer(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight(),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MemoryHud(
    playing: MiniGamesScreenUiState.MemoryPlaying,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.testTag("memory-hud"),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            MiniGameHudPill(
                label = "Gain",
                value = playing.rewardLabel,
                tint = Color(0xFFF6C75D),
                modifier = Modifier.weight(1f),
            )
            MiniGameHudPill(
                label = "Cartes",
                value = "${playing.matchedCount}/${playing.totalCount}",
                tint = Color(0xFF75E0C2),
                modifier = Modifier.weight(1f),
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            MiniGameHudPill(
                label = "Coups",
                value = playing.moves.toString(),
                tint = Color(0xFFD6E7F7),
                modifier = Modifier.weight(1f),
            )
            MiniGameHudPill(
                label = "Serie",
                value = "${playing.currentStreak}/${playing.bestStreak}",
                tint = Color(0xFF9AEAFF),
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun MemoryResultPanel(
    result: MiniGamesScreenUiState.MemoryResult,
    onBackToMenu: () -> Unit,
    modifier: Modifier = Modifier,
) {
    MiniGameBoardSurface(
        modifier = modifier
            .widthIn(max = 460.dp)
            .fillMaxWidth()
            .testTag("memory-result"),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = "Grille terminée",
                color = Color.White,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            MiniGameHudPill(
                label = result.difficultyName,
                value = result.rewardLabel,
                tint = Color(0xFFF6C75D),
            )
            result.nextDifficultyName?.let { next ->
                Text(
                    text = "$next débloqué",
                    color = Color(0xFF88E6D2),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Button(
                onClick = onBackToMenu,
                modifier = Modifier.testTag("memory-result-back"),
            ) {
                Text("Retour au menu")
            }
        }
    }
}

@Composable
private fun MemoryUnavailablePanel(
    message: String,
    onBackToMenu: () -> Unit,
    modifier: Modifier = Modifier,
) {
    MiniGameBoardSurface(
        modifier = modifier
            .widthIn(max = 460.dp)
            .fillMaxWidth()
            .testTag("memory-unavailable"),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = message,
                color = Color.White,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleMedium,
            )
            Button(
                onClick = onBackToMenu,
                modifier = Modifier.testTag("memory-unavailable-back"),
            ) {
                Text("Retour au menu")
            }
        }
    }
}
