package fr.aumombelli.dstcg.feature.minigames

import fr.aumombelli.dstcg.model.MiniGameDifficulty
import fr.aumombelli.dstcg.model.MiniGameId
import fr.aumombelli.dstcg.model.MiniGamesProgress
import fr.aumombelli.dstcg.model.dailyStateFor
import fr.aumombelli.dstcg.model.unlockedDifficultyFor

internal fun fr.aumombelli.dstcg.data.MiniGamesState.toUiState(
    screen: MiniGamesScreenUiState,
): MiniGamesUiState = progress.toUiState(
    todayUtc = todayUtc,
    screen = screen,
)

internal fun MiniGamesProgress.toUiState(
    todayUtc: String,
    screen: MiniGamesScreenUiState,
): MiniGamesUiState {
    val quizDailyState = dailyStateFor(MiniGameId.Quiz, todayUtc)
    val quizUnlockedDifficulty = unlockedDifficultyFor(MiniGameId.Quiz)
    val quizRewardLabel = quizDailyState.reward?.let(::formatReward)
    val quizPlayedToday = quizDailyState.hasPlayed || quizDailyState.reward != null
    val memoryDailyState = dailyStateFor(MiniGameId.Memory, todayUtc)
    val memoryUnlockedDifficulty = unlockedDifficultyFor(MiniGameId.Memory)
    val memoryRewardLabel = memoryDailyState.reward?.let(::formatReward)
    val memoryPlayedToday = memoryDailyState.hasPlayed || memoryDailyState.reward != null
    val timelineDailyState = dailyStateFor(MiniGameId.Timeline, todayUtc)
    val timelineUnlockedDifficulty = unlockedDifficultyFor(MiniGameId.Timeline)
    val timelineRewardLabel = timelineDailyState.reward?.let(::formatReward)
    val timelinePlayedToday = timelineDailyState.hasPlayed || timelineDailyState.reward != null
    val observatoryDailyState = dailyStateFor(MiniGameId.Observatory, todayUtc)
    val observatoryUnlockedDifficulty = unlockedDifficultyFor(MiniGameId.Observatory)
    val observatoryRewardLabel = observatoryDailyState.reward?.let(::formatReward)
    val observatoryPlayedToday = observatoryDailyState.hasPlayed || observatoryDailyState.reward != null
    return MiniGamesUiState(
        isLoading = false,
        todayUtc = todayUtc,
        quizStatusLabel = when {
            quizRewardLabel != null -> "Joué aujourd'hui - $quizRewardLabel gagnées"
            quizPlayedToday -> "Essai utilisé aujourd'hui"
            else -> "Disponible - ${quizUnlockedDifficulty.displayName}"
        },
        quizPlayedToday = quizPlayedToday,
        quizRewardLabel = quizRewardLabel,
        quizDifficultyChoices = MiniGameDifficulty.entries.map { difficulty ->
            val unlocked = difficulty.level <= quizUnlockedDifficulty.level
            val spec = QuizDifficultySpec.forDifficulty(difficulty)
            QuizDifficultyChoiceUi(
                difficulty = difficulty,
                title = difficulty.displayName,
                questionLabel = spec.questionLabel,
                rewardLabel = formatReward(difficulty.reward),
                enabled = unlocked && !quizPlayedToday,
                locked = !unlocked,
                statusLabel = when {
                    quizPlayedToday -> "Déjà joué"
                    unlocked -> "Disponible"
                    else -> "À débloquer"
                },
            )
        },
        memoryStatusLabel = when {
            memoryRewardLabel != null -> "Joué aujourd'hui - $memoryRewardLabel gagnées"
            memoryPlayedToday -> "Essai utilisé aujourd'hui"
            else -> "Disponible - ${memoryUnlockedDifficulty.displayName}"
        },
        memoryPlayedToday = memoryPlayedToday,
        memoryRewardLabel = memoryRewardLabel,
        memoryDifficultyChoices = MiniGameDifficulty.entries.map { difficulty ->
            val unlocked = difficulty.level <= memoryUnlockedDifficulty.level
            val spec = MemoryDifficultySpec.forDifficulty(difficulty)
            MemoryDifficultyChoiceUi(
                difficulty = difficulty,
                title = difficulty.displayName,
                gridLabel = spec.gridLabel,
                rewardLabel = formatReward(difficulty.reward),
                enabled = unlocked && !memoryPlayedToday,
                locked = !unlocked,
                statusLabel = when {
                    memoryPlayedToday -> "Déjà joué"
                    unlocked -> "Disponible"
                    else -> "À débloquer"
                },
            )
        },
        timelineStatusLabel = when {
            timelineRewardLabel != null -> "Joué aujourd'hui - $timelineRewardLabel gagnées"
            timelinePlayedToday -> "Essai utilisé aujourd'hui"
            else -> "Disponible - ${timelineUnlockedDifficulty.displayName}"
        },
        timelinePlayedToday = timelinePlayedToday,
        timelineRewardLabel = timelineRewardLabel,
        timelineDifficultyChoices = MiniGameDifficulty.entries.map { difficulty ->
            val unlocked = difficulty.level <= timelineUnlockedDifficulty.level
            val spec = TimelineDifficultySpec.forDifficulty(difficulty)
            TimelineDifficultyChoiceUi(
                difficulty = difficulty,
                title = difficulty.displayName,
                comparisonLabel = spec.comparisonLabel,
                rewardLabel = formatReward(difficulty.reward),
                enabled = unlocked && !timelinePlayedToday,
                locked = !unlocked,
                statusLabel = when {
                    timelinePlayedToday -> "Déjà joué"
                    unlocked -> "Disponible"
                    else -> "À débloquer"
                },
            )
        },
        observatoryStatusLabel = when {
            observatoryRewardLabel != null -> "Joué aujourd'hui - $observatoryRewardLabel gagnées"
            observatoryPlayedToday -> "Essai utilisé aujourd'hui"
            else -> "Disponible - ${observatoryUnlockedDifficulty.displayName}"
        },
        observatoryPlayedToday = observatoryPlayedToday,
        observatoryRewardLabel = observatoryRewardLabel,
        observatoryDifficultyChoices = MiniGameDifficulty.entries.map { difficulty ->
            val unlocked = difficulty.level <= observatoryUnlockedDifficulty.level
            val spec = ObservatoryDifficultySpec.forDifficulty(difficulty)
            ObservatoryDifficultyChoiceUi(
                difficulty = difficulty,
                title = difficulty.displayName,
                targetLabel = spec.targetLabel,
                precisionLabel = spec.precisionLabel,
                rewardLabel = formatReward(difficulty.reward),
                enabled = unlocked && !observatoryPlayedToday,
                locked = !unlocked,
                statusLabel = when {
                    observatoryPlayedToday -> "Déjà joué"
                    unlocked -> "Disponible"
                    else -> "À débloquer"
                },
            )
        },
        screen = screen,
    )
}
