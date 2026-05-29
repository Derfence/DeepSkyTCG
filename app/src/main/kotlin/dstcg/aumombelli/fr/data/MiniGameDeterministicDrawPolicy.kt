package fr.aumombelli.dstcg.data

import fr.aumombelli.dstcg.model.CardDefinition
import fr.aumombelli.dstcg.model.MiniGameGlobalCardRef
import fr.aumombelli.dstcg.model.MiniGameId
import java.security.MessageDigest

class MiniGameDeterministicDrawPolicy(
    val algorithmVersion: Int = CurrentAlgorithmVersion,
) {
    fun drawGlobalCard(
        cards: List<CardDefinition>,
        miniGameId: MiniGameId,
        dateUtc: String,
        slot: Int,
        extensionId: String? = null,
    ): MiniGameGlobalCardRef? =
        rankedGlobalCandidates(
            cards = cards,
            miniGameId = miniGameId,
            dateUtc = dateUtc,
            slot = slot,
            extensionId = extensionId,
        ).firstOrNull()?.toGlobalRef()

    fun drawGlobalCards(
        cards: List<CardDefinition>,
        miniGameId: MiniGameId,
        dateUtc: String,
        slotCount: Int,
        extensionId: String? = null,
    ): List<MiniGameGlobalCardRef> {
        if (slotCount <= 0) return emptyList()
        val selectedCardIds = mutableSetOf<String>()
        val selected = mutableListOf<MiniGameGlobalCardRef>()

        repeat(slotCount) { slot ->
            val ranked = rankedGlobalCandidates(
                cards = cards,
                miniGameId = miniGameId,
                dateUtc = dateUtc,
                slot = slot,
                extensionId = extensionId,
            )
            val picked = ranked.firstOrNull { it.id !in selectedCardIds }
                ?: ranked.firstOrNull()
                ?: return@repeat
            selectedCardIds += picked.id
            selected += picked.toGlobalRef()
        }

        return selected
    }

    private fun rankedGlobalCandidates(
        cards: List<CardDefinition>,
        miniGameId: MiniGameId,
        dateUtc: String,
        slot: Int,
        extensionId: String?,
    ): List<CardDefinition> {
        val targetExtensionKey = extensionId ?: AnyExtensionKey
        return cards
            .asSequence()
            .filter { extensionId == null || it.extensionId == extensionId }
            .sortedWith(
                compareBy<CardDefinition> {
                    stableMiniGameHash(
                        "global-card",
                        "v$algorithmVersion",
                        miniGameId.name,
                        dateUtc,
                        slot.coerceAtLeast(0).toString(),
                        targetExtensionKey,
                        it.extensionId,
                        it.id,
                    )
                }.thenBy { it.id },
            )
            .toList()
    }

    private fun CardDefinition.toGlobalRef(): MiniGameGlobalCardRef =
        MiniGameGlobalCardRef(
            cardId = id,
            extensionId = extensionId,
        )

    companion object {
        const val CurrentAlgorithmVersion: Int = 1
        private const val AnyExtensionKey = "*"
    }
}

internal fun stableMiniGameHash(vararg parts: String): String {
    val input = parts.joinToString(separator = "|")
    val digest = MessageDigest.getInstance("SHA-256").digest(input.encodeToByteArray())
    return digest.joinToString(separator = "") { byte ->
        (byte.toInt() and 0xFF).toString(radix = 16).padStart(length = 2, padChar = '0')
    }
}
