package fr.aumombelli.dstcg.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put

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
        get() = MiniGameReward.fromMinutes(rewardMinutes)

    fun next(): MiniGameDifficulty? =
        entries.firstOrNull { it.level == level + 1 }

    companion object {
        val Default: MiniGameDifficulty = Apprentice
    }
}

@Serializable(with = MiniGameRewardSerializer::class)
data class MiniGameReward(
    val reductionSeconds: Long,
) {
    val reductionMinutes: Long
        get() = reductionSeconds / 60L

    companion object {
        fun fromMinutes(minutes: Long): MiniGameReward =
            MiniGameReward(reductionSeconds = minutes * 60L)

        fun fromSeconds(seconds: Long): MiniGameReward =
            MiniGameReward(reductionSeconds = seconds)
    }
}

internal object MiniGameRewardSerializer : KSerializer<MiniGameReward> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("MiniGameReward") {
        element<Long>("reductionSeconds", isOptional = true)
        element<Long>("reductionMinutes", isOptional = true)
    }

    override fun deserialize(decoder: Decoder): MiniGameReward {
        val jsonDecoder = decoder as? JsonDecoder
            ?: throw SerializationException("MiniGameReward can only be decoded from JSON.")
        val jsonObject = jsonDecoder.decodeJsonElement().jsonObject
        val seconds = jsonObject["reductionSeconds"]?.jsonPrimitive?.longOrNull
            ?: jsonObject["reductionMinutes"]?.jsonPrimitive?.longOrNull?.times(60L)
            ?: 0L
        return MiniGameReward.fromSeconds(seconds)
    }

    override fun serialize(encoder: Encoder, value: MiniGameReward) {
        val jsonEncoder = encoder as? JsonEncoder
            ?: throw SerializationException("MiniGameReward can only be encoded to JSON.")
        jsonEncoder.encodeJsonElement(
            buildJsonObject {
                put("reductionSeconds", value.reductionSeconds)
            },
        )
    }
}

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
