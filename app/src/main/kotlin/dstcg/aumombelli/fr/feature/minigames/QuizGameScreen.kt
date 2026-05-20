package fr.aumombelli.dstcg.feature.minigames

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import fr.aumombelli.dstcg.model.DisplayCard
import fr.aumombelli.dstcg.model.MiniGameDifficulty
import fr.aumombelli.dstcg.ui.component.AstroCardPreviewSurface
import fr.aumombelli.dstcg.ui.component.AstroCardSurfaceMode
import fr.aumombelli.dstcg.ui.component.CardArtBackground
import fr.aumombelli.dstcg.ui.component.SceneNavigationButton
import fr.aumombelli.dstcg.ui.component.SceneNavigationIcon
import fr.aumombelli.dstcg.ui.component.TRADING_CARD_WIDTH_OVER_HEIGHT
import fr.aumombelli.dstcg.ui.motion.SkyBackdropVariant
import fr.aumombelli.dstcg.ui.screen.dstcgContentInsetsPadding
import fr.aumombelli.dstcg.ui.theme.skyQualityPalette
import kotlinx.coroutines.delay

@Composable
internal fun QuizGameScreen(
    state: MiniGamesUiState,
    onBackToMenu: () -> Unit,
    onSelectDifficulty: (MiniGameDifficulty) -> Unit,
    onSelectAnswer: (Int) -> Unit,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val resultScreen = state.screen as? MiniGamesScreenUiState.QuizResult
    val playingCue = (state.screen as? MiniGamesScreenUiState.QuizPlaying)?.feedbackEvent

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("quiz-screen"),
    ) {
        MiniGameSceneBackdrop(
            variant = SkyBackdropVariant.City,
            sparkleBoost = if (state.screen is MiniGamesScreenUiState.QuizPlaying) 0.24f else 0.16f,
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
                testTag = "quiz-back",
                modifier = Modifier.align(Alignment.TopStart),
            )

            Text(
                text = "Quiz",
                color = Color.White,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 4.dp),
            )

            when (val screen = state.screen) {
                MiniGamesScreenUiState.Menu -> Unit
                is MiniGamesScreenUiState.QuizDifficultySelection -> QuizDifficultySelection(
                    card = screen.card,
                    choices = state.quizDifficultyChoices,
                    isLoading = state.isLoading,
                    onSelectDifficulty = onSelectDifficulty,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .fillMaxWidth(),
                )

                is MiniGamesScreenUiState.QuizPlaying -> QuizPlayingPanel(
                    playing = screen,
                    onSelectAnswer = onSelectAnswer,
                    onContinue = onContinue,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .fillMaxSize(),
                )

                is MiniGamesScreenUiState.QuizResult -> QuizResultPanel(
                    result = screen,
                    onBackToMenu = onBackToMenu,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .fillMaxSize(),
                )

                is MiniGamesScreenUiState.QuizUnavailable -> QuizUnavailablePanel(
                    message = screen.message,
                    onBackToMenu = onBackToMenu,
                    modifier = Modifier.align(Alignment.Center),
                )

                else -> Unit
            }
        }
    }
}

@Composable
private fun QuizDifficultySelection(
    card: DisplayCard,
    choices: List<QuizDifficultyChoiceUi>,
    isLoading: Boolean,
    onSelectDifficulty: (MiniGameDifficulty) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .testTag("quiz-difficulty-selection"),
    ) {
        Spacer(modifier = Modifier.height(56.dp))
        AstroCardPreviewSurface(
            displayCard = card,
            mode = AstroCardSurfaceMode.Preview,
            modifier = Modifier
                .widthIn(max = 320.dp)
                .fillMaxWidth(0.68f)
                .testTag("quiz-card-preview"),
        )
        choices.forEach { choice ->
            QuizDifficultyRow(
                choice = choice,
                enabled = choice.enabled && !isLoading,
                onClick = { onSelectDifficulty(choice.difficulty) },
            )
        }
    }
}

@Composable
private fun QuizDifficultyRow(
    choice: QuizDifficultyChoiceUi,
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
                    text = "${choice.questionLabel} - max ${choice.rewardLabel}",
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
private fun QuizPlayingPanel(
    playing: MiniGamesScreenUiState.QuizPlaying,
    onSelectAnswer: (Int) -> Unit,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val entranceProgress = remember { Animatable(1f) }
    LaunchedEffect(playing.questionIndex, playing.prompt) {
        entranceProgress.snapTo(0f)
        delay(QuizQuestionHoldMillis)
        entranceProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 820, easing = FastOutSlowInEasing),
        )
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .testTag("quiz-playing"),
    ) {
        val questionProgress = entranceProgress.value
        val answersProgress = ((questionProgress - 0.72f) / 0.28f).coerceIn(0f, 1f)
        val questionStartCenterY = maxHeight * 0.46f
        val questionEndCenterY = maxHeight * 0.34f
        val questionCenterY = questionStartCenterY + ((questionEndCenterY - questionStartCenterY) * questionProgress)
        val answersOffsetY = 72.dp * (1f - answersProgress)
        val observationWidth = when {
            maxWidth < 360.dp -> 104.dp
            maxWidth < 520.dp -> 124.dp
            else -> 152.dp
        }
        val observationHeight = observationWidth / TRADING_CARD_WIDTH_OVER_HEIGHT
        val questionLaneWidth = (maxWidth - observationWidth - 18.dp)
            .coerceAtLeast(maxWidth * 0.54f)
            .coerceAtMost(maxWidth)
        val questionBandHeight = when {
            maxHeight < 620.dp -> 108.dp
            maxWidth < 420.dp -> 132.dp
            else -> 148.dp
        }
        val questionTopY = (questionCenterY - (questionBandHeight / 2f))
            .coerceIn(0.dp, maxOf(0.dp, maxHeight - questionBandHeight))
        val observationTopY = (questionEndCenterY - (observationHeight / 2f))
            .coerceIn(0.dp, maxOf(0.dp, maxHeight - observationHeight))

        QuizHud(
            playing = playing,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(top = 58.dp)
                .testTag("quiz-fixed-hud"),
        )

        QuizObservationImage(
            card = playing.card,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(y = observationTopY)
                .width(observationWidth)
                .testTag("quiz-observation-image"),
        )

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(y = questionTopY)
                .width(questionLaneWidth)
                .height(questionBandHeight)
                .padding(start = 6.dp, end = 12.dp)
                .testTag("quiz-question-prompt"),
        ) {
            Text(
                text = playing.prompt,
                color = Color.White,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(y = answersOffsetY)
                .graphicsLayer { alpha = answersProgress }
                .verticalScroll(rememberScrollState())
                .padding(bottom = 6.dp),
        ) {
            MiniGameBoardSurface(
                modifier = Modifier
                    .widthIn(max = 560.dp)
                    .fillMaxWidth(),
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    playing.answers.forEach { answer ->
                        QuizAnswerRow(
                            answer = answer,
                            enabled = !playing.canAdvance,
                            onClick = { onSelectAnswer(answer.index) },
                        )
                    }
                    Button(
                        onClick = onContinue,
                        enabled = playing.canAdvance,
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .testTag("quiz-continue"),
                    ) {
                        Text(
                            text = if (playing.questionIndex == playing.questionCount - 1) {
                                "Voir le résultat"
                            } else {
                                "Question suivante"
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuizObservationImage(
    card: DisplayCard,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(16.dp)
    Surface(
        shape = shape,
        color = Color(0xCC0A1724),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.32f)),
        shadowElevation = 6.dp,
        modifier = modifier.aspectRatio(TRADING_CARD_WIDTH_OVER_HEIGHT),
    ) {
        CardArtBackground(
            definition = card.definition,
            mode = AstroCardSurfaceMode.Thumbnail,
            palette = skyQualityPalette(card.activeVariant.skyQuality),
            artShape = shape,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
private fun QuizHud(
    playing: MiniGamesScreenUiState.QuizPlaying,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.testTag("quiz-hud"),
    ) {
        MiniGameHudPill(
            label = "Gain max",
            value = playing.rewardLabel,
            tint = Color(0xFFF6C75D),
            modifier = Modifier.weight(1f),
        )
        MiniGameHudPill(
            label = "Score",
            value = "${playing.score}/${playing.questionCount}",
            tint = Color(0xFF75E0C2),
            modifier = Modifier.weight(1f),
        )
        MiniGameHudPill(
            label = "Question",
            value = "${playing.questionIndex + 1}/${playing.questionCount}",
            tint = Color(0xFF9AEAFF),
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun QuizAnswerRow(
    answer: QuizAnswerUi,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val color = when (answer.state) {
        QuizAnswerState.Idle -> Color(0xDD10283A)
        QuizAnswerState.SelectedCorrect,
        QuizAnswerState.Correct,
        -> Color(0xDD164E42)
        QuizAnswerState.SelectedWrong -> Color(0xDD672A2E)
    }
    val label = when (answer.state) {
        QuizAnswerState.SelectedCorrect -> "Bonne réponse"
        QuizAnswerState.SelectedWrong -> "Ta réponse"
        QuizAnswerState.Correct -> "Réponse correcte"
        QuizAnswerState.Idle -> null
    }

    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        color = color,
        contentColor = Color.White,
        modifier = Modifier
            .fillMaxWidth()
            .testTag(answer.testTag),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(3.dp),
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
        ) {
            Text(
                text = answer.text,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
            )
            label?.let {
                Text(
                    text = it,
                    color = Color(0xFFD6E7F7),
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
    }
}

@Composable
private fun QuizResultPanel(
    result: MiniGamesScreenUiState.QuizResult,
    onBackToMenu: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxSize()
            .testTag("quiz-result"),
    ) {
        BoxWithConstraints(
            contentAlignment = Alignment.BottomCenter,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(top = 58.dp, bottom = 8.dp),
        ) {
            result.card?.let { card ->
                val cardWidth = minOf(
                    maxWidth,
                    maxHeight * TRADING_CARD_WIDTH_OVER_HEIGHT,
                )
                AstroCardPreviewSurface(
                    displayCard = card,
                    mode = AstroCardSurfaceMode.Preview,
                    modifier = Modifier
                        .width(cardWidth)
                        .testTag("quiz-result-card"),
                )
            }
        }

        MiniGameBoardSurface(
            modifier = Modifier
                .widthIn(max = 560.dp)
                .heightIn(max = 360.dp)
                .fillMaxWidth(),
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
            ) {
                Text(
                    text = "Quiz terminé",
                    color = Color.White,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                result.card?.let { card ->
                    Text(
                        text = card.definition.name,
                        color = Color(0xFFD6E7F7),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    MiniGameHudPill(
                        label = result.difficultyName,
                        value = result.scoreLabel,
                        tint = Color(0xFF75E0C2),
                        modifier = Modifier.weight(1f),
                    )
                    MiniGameHudPill(
                        label = "Gain",
                        value = result.rewardLabel,
                        tint = Color(0xFFF6C75D),
                        modifier = Modifier.weight(1f),
                    )
                }
                result.corrections.forEachIndexed { index, correction ->
                    QuizCorrectionRow(
                        index = index,
                        correction = correction,
                    )
                }
                result.nextDifficultyName?.let { next ->
                    Text(
                        text = "$next débloqué",
                        color = Color(0xFF88E6D2),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}

@Composable
private fun QuizCorrectionRow(
    index: Int,
    correction: QuizCorrectionUi,
) {
    val tint = if (correction.isCorrect) Color(0xFF88E6D2) else Color(0xFFFFC4BD)
    Column(
        verticalArrangement = Arrangement.spacedBy(3.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("quiz-correction-$index"),
    ) {
        Text(
            text = correction.prompt,
            color = Color.White,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = if (correction.isCorrect) {
                correction.correctAnswer
            } else {
                "${correction.selectedAnswer} → ${correction.correctAnswer}"
            },
            color = tint,
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            text = correction.explanation,
            color = Color(0xFFD8E8F1),
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun QuizUnavailablePanel(
    message: String,
    onBackToMenu: () -> Unit,
    modifier: Modifier = Modifier,
) {
    MiniGameBoardSurface(
        modifier = modifier
            .widthIn(max = 460.dp)
            .fillMaxWidth()
            .testTag("quiz-unavailable"),
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
                modifier = Modifier.testTag("quiz-unavailable-back"),
            ) {
                Text("Retour au menu")
            }
        }
    }
}

private const val QuizQuestionHoldMillis: Long = 650L
