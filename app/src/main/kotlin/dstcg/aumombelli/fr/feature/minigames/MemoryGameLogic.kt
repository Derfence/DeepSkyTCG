package fr.aumombelli.dstcg.feature.minigames

import fr.aumombelli.dstcg.data.MiniGameDeterministicDrawPolicy
import fr.aumombelli.dstcg.data.ownedMiniGameVariants
import fr.aumombelli.dstcg.data.stableMiniGameHash
import fr.aumombelli.dstcg.model.CardDefinition
import fr.aumombelli.dstcg.model.DisplayCard
import fr.aumombelli.dstcg.model.ExtensionDefinition
import fr.aumombelli.dstcg.model.MiniGameDifficulty
import fr.aumombelli.dstcg.model.MiniGameOwnedVariantRef
import fr.aumombelli.dstcg.model.MiniGameResolvedCardRef
import fr.aumombelli.dstcg.model.OwnedCollection
import fr.aumombelli.dstcg.model.VariantProfile
import fr.aumombelli.dstcg.model.toDisplayCard
import fr.aumombelli.dstcg.model.toDisplayVariant

internal data class MemoryDifficultySpec(
    val difficulty: MiniGameDifficulty,
    val rows: Int,
    val columns: Int,
) {
    val cellCount: Int = rows * columns
    val pairCount: Int = cellCount / 2
    val hasHole: Boolean = cellCount % 2 == 1
    val gridLabel: String = "${columns}x$rows"

    companion object {
        fun forDifficulty(difficulty: MiniGameDifficulty): MemoryDifficultySpec = when (difficulty) {
            MiniGameDifficulty.Apprentice -> MemoryDifficultySpec(difficulty, rows = 2, columns = 2)
            MiniGameDifficulty.Observer -> MemoryDifficultySpec(difficulty, rows = 3, columns = 3)
            MiniGameDifficulty.Scientist -> MemoryDifficultySpec(difficulty, rows = 4, columns = 4)
            MiniGameDifficulty.Explorer -> MemoryDifficultySpec(difficulty, rows = 5, columns = 5)
        }
    }
}

internal enum class MemoryCardRole {
    Pair,
}

internal data class MemoryCardIdentity(
    val cardId: String,
    val extensionId: String,
    val skyQuality: String,
    val finish: String,
) {
    val cardKey: String = "$extensionId::$cardId"
    val key: String = "$extensionId::$cardId::$skyQuality::$finish"
}

internal data class MemoryCardFace(
    val id: String,
    val identity: MemoryCardIdentity,
    val displayCard: DisplayCard,
    val role: MemoryCardRole,
)

internal sealed interface MemoryBoardCell {
    val id: String

    data class Card(
        val face: MemoryCardFace,
    ) : MemoryBoardCell {
        override val id: String = face.id
    }

    data class Hole(
        override val id: String = "hole",
    ) : MemoryBoardCell
}

internal data class MemoryBoard(
    val difficulty: MiniGameDifficulty,
    val rows: Int,
    val columns: Int,
    val cells: List<MemoryBoardCell>,
) {
    val cellCount: Int = rows * columns
    val cards: List<MemoryCardFace> = cells.mapNotNull { cell ->
        (cell as? MemoryBoardCell.Card)?.face
    }
    val playableCellCount: Int = cards.size
}

internal sealed interface MemoryBoardBuildResult {
    data class Ready(
        val board: MemoryBoard,
    ) : MemoryBoardBuildResult

    data class Unavailable(
        val message: String,
    ) : MemoryBoardBuildResult
}

internal fun buildMemoryBoard(
    difficulty: MiniGameDifficulty,
    dateUtc: String,
    resolvedPairCards: List<MiniGameResolvedCardRef>,
    cards: List<CardDefinition>,
    extensions: List<ExtensionDefinition>,
    variantProfiles: List<VariantProfile>,
    collection: OwnedCollection,
): MemoryBoardBuildResult {
    val spec = MemoryDifficultySpec.forDifficulty(difficulty)
    val catalog = MemoryCatalogLookup(
        cards = cards,
        extensions = extensions,
        variantProfiles = variantProfiles,
    )
    val resolvedPairEntries = resolvedPairCards
        .mapNotNull { catalog.entryFor(it.ownedVariant) }
        .distinctBy { it.identity.cardKey }
    val pairEntries = if (resolvedPairEntries.size >= spec.pairCount) {
        resolvedPairEntries
    } else {
        val resolvedCardKeys = resolvedPairEntries.map { it.identity.cardKey }.toSet()
        resolvedPairEntries + collection.ownedMiniGameVariants(cards)
            .mapNotNull { catalog.entryFor(it) }
            .distinctBy { it.identity.cardKey }
            .filter { it.identity.cardKey !in resolvedCardKeys }
            .stableMemoryOrder(dateUtc, difficulty, "pair-fill")
    }

    if (pairEntries.size < spec.pairCount) {
        return MemoryBoardBuildResult.Unavailable(
            message = "Pas assez de cartes distinctes dans ta bibliothèque pour cette grille.",
        )
    }

    val selectedPairs = pairEntries.take(spec.pairCount)
    val cells: MutableList<MemoryBoardCell> = selectedPairs.flatMapIndexed { index, entry ->
        listOf(
            MemoryBoardCell.Card(entry.toFace(id = "pair-$index-a", role = MemoryCardRole.Pair)),
            MemoryBoardCell.Card(entry.toFace(id = "pair-$index-b", role = MemoryCardRole.Pair)),
        )
    }.toMutableList()

    if (spec.hasHole) {
        cells += MemoryBoardCell.Hole()
    }

    val shuffled = cells.sortedWith(
        compareBy<MemoryBoardCell> {
            stableMiniGameHash(
                "memory-board",
                "v${MiniGameDeterministicDrawPolicy.CurrentAlgorithmVersion}",
                dateUtc,
                difficulty.name,
                it.id,
                it.shuffleKey,
            )
        }.thenBy { it.id },
    )

    return MemoryBoardBuildResult.Ready(
        board = MemoryBoard(
            difficulty = difficulty,
            rows = spec.rows,
            columns = spec.columns,
            cells = shuffled,
        ),
    )
}

private val MemoryBoardCell.shuffleKey: String
    get() = when (this) {
        is MemoryBoardCell.Card -> face.identity.key
        is MemoryBoardCell.Hole -> id
    }

private fun List<MemoryCardEntry>.stableMemoryOrder(
    dateUtc: String,
    difficulty: MiniGameDifficulty,
    purpose: String,
): List<MemoryCardEntry> = sortedWith(
    compareBy<MemoryCardEntry> {
        stableMiniGameHash(
            "memory-card-order",
            "v${MiniGameDeterministicDrawPolicy.CurrentAlgorithmVersion}",
            purpose,
            dateUtc,
            difficulty.name,
            it.identity.key,
        )
    }.thenBy { it.identity.key },
)

private data class MemoryCatalogLookup(
    val cards: List<CardDefinition>,
    val extensions: List<ExtensionDefinition>,
    val variantProfiles: List<VariantProfile>,
) {
    private val cardsById = cards.associateBy(CardDefinition::id)
    private val extensionNamesById = extensions.associate { it.id to it.name }
    private val variantProfilesById = variantProfiles.associateBy(VariantProfile::id)

    fun entryFor(variant: MiniGameOwnedVariantRef): MemoryCardEntry? {
        val definition = cardsById[variant.cardId] ?: return null
        val profile = variantProfilesById[definition.variantProfileId] ?: return null
        return MemoryCardEntry(
            definition = definition,
            extensionName = extensionNamesById[definition.extensionId] ?: definition.extensionId,
            identity = MemoryCardIdentity(
                cardId = variant.cardId,
                extensionId = variant.extensionId,
                skyQuality = variant.skyQuality,
                finish = variant.finish,
            ),
            displayCard = definition.toDisplayCard(
                extensionName = extensionNamesById[definition.extensionId] ?: definition.extensionId,
                activeVariant = profile.toDisplayVariant(
                    skyQuality = variant.skyQuality,
                    finish = variant.finish,
                ),
            ),
        )
    }

}

private data class MemoryCardEntry(
    val definition: CardDefinition,
    val extensionName: String,
    val identity: MemoryCardIdentity,
    val displayCard: DisplayCard,
) {
    fun toFace(
        id: String,
        role: MemoryCardRole,
    ): MemoryCardFace = MemoryCardFace(
        id = id,
        identity = identity,
        displayCard = displayCard,
        role = role,
    )
}
