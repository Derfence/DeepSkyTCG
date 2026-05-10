package fr.aumombelli.dstcg.model

import kotlinx.serialization.Serializable

@Serializable
enum class MiniGameId {
    Quiz,
    Memory,
    Timeline,
    Observatory,
}

@Serializable
enum class MiniGameDifficulty(
    val level: Int,
    val displayName: String,
    val rewardMinutes: Long,
) {
    Apprentice(level = 1, displayName = "Apprenti", rewardMinutes = 15L),
    Observer(level = 2, displayName = "Observateur", rewardMinutes = 30L),
    Scientist(level = 3, displayName = "Scientifique", rewardMinutes = 45L),
    Explorer(level = 4, displayName = "Explorateur", rewardMinutes = 60L),
    ;

    val reward: MiniGameReward
        get() = MiniGameReward(reductionMinutes = rewardMinutes)

    fun next(): MiniGameDifficulty? =
        entries.firstOrNull { it.level == level + 1 }

    companion object {
        val Default: MiniGameDifficulty = Apprentice
    }
}

@Serializable
data class MiniGameReward(
    val reductionMinutes: Long,
)

@Serializable
data class MiniGameGlobalCardRef(
    val cardId: String,
    val extensionId: String,
)

@Serializable
data class MiniGameOwnedVariantRef(
    val cardId: String,
    val extensionId: String,
    val skyQuality: String,
    val finish: String,
)

@Serializable
enum class MiniGameCardResolutionSource {
    GlobalCard,
    SameExtensionFallback,
    AnyExtensionFallback,
}

@Serializable
data class MiniGameResolvedCardRef(
    val globalCard: MiniGameGlobalCardRef,
    val ownedVariant: MiniGameOwnedVariantRef,
    val source: MiniGameCardResolutionSource,
)

@Serializable
data class MiniGameDailyState(
    val dateUtc: String? = null,
    val hasPlayed: Boolean = false,
    val reward: MiniGameReward? = null,
    val resolvedCards: List<MiniGameResolvedCardRef> = emptyList(),
)

@Serializable
data class MiniGamesProgress(
    val dailyStates: Map<MiniGameId, MiniGameDailyState> = emptyMap(),
    val unlockedDifficulties: Map<MiniGameId, MiniGameDifficulty> = emptyMap(),
)

fun MiniGamesProgress.dailyStateFor(
    miniGameId: MiniGameId,
    dateUtc: String,
): MiniGameDailyState =
    dailyStates[miniGameId]
        ?.takeIf { it.dateUtc == dateUtc }
        ?: MiniGameDailyState(dateUtc = dateUtc)

fun MiniGamesProgress.withDailyState(
    miniGameId: MiniGameId,
    dailyState: MiniGameDailyState,
): MiniGamesProgress = copy(
    dailyStates = dailyStates.toMutableMap().also { states ->
        states[miniGameId] = dailyState
    },
)

fun MiniGamesProgress.unlockedDifficultyFor(miniGameId: MiniGameId): MiniGameDifficulty =
    unlockedDifficulties[miniGameId] ?: MiniGameDifficulty.Default

fun MiniGamesProgress.withUnlockedDifficulty(
    miniGameId: MiniGameId,
    difficulty: MiniGameDifficulty,
): MiniGamesProgress = copy(
    unlockedDifficulties = unlockedDifficulties.toMutableMap().also { difficulties ->
        val current = unlockedDifficultyFor(miniGameId)
        difficulties[miniGameId] = if (difficulty.level > current.level) difficulty else current
    },
)
