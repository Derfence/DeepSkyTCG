package fr.aumombelli.dstcg.feature.minigames

import fr.aumombelli.dstcg.data.MiniGameDeterministicDrawPolicy
import fr.aumombelli.dstcg.data.ownedMiniGameVariants
import fr.aumombelli.dstcg.data.stableMiniGameHash
import fr.aumombelli.dstcg.model.CardDefinition
import fr.aumombelli.dstcg.model.DisplayCard
import fr.aumombelli.dstcg.model.DisplayCardVariant
import fr.aumombelli.dstcg.model.ExtensionDefinition
import fr.aumombelli.dstcg.model.MiniGameDifficulty
import fr.aumombelli.dstcg.model.MiniGameOwnedVariantRef
import fr.aumombelli.dstcg.model.MiniGameResolvedCardRef
import fr.aumombelli.dstcg.model.OwnedCollection
import fr.aumombelli.dstcg.model.VariantProfile
import fr.aumombelli.dstcg.model.requireFinishDefinition
import fr.aumombelli.dstcg.model.requireSkyQualityDefinition
import fr.aumombelli.dstcg.model.toDisplayCard
import fr.aumombelli.dstcg.model.toDisplayVariant

internal data class MemoryDifficultySpec(
    val difficulty: MiniGameDifficulty,
    val rows: Int,
    val columns: Int,
) {
    val cellCount: Int = rows * columns
    val pairCount: Int = cellCount / 2
    val hasSingleton: Boolean = cellCount % 2 == 1
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
    HolographicSingleton,
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
    val isVisualHolographicFallback: Boolean = false,
)

internal data class MemoryBoard(
    val difficulty: MiniGameDifficulty,
    val rows: Int,
    val columns: Int,
    val cards: List<MemoryCardFace>,
) {
    val cellCount: Int = rows * columns
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
    val usedPairCardKeys = selectedPairs.map { it.identity.cardKey }.toSet()
    val faces = selectedPairs.flatMapIndexed { index, entry ->
        listOf(
            entry.toFace(id = "pair-$index-a", role = MemoryCardRole.Pair),
            entry.toFace(id = "pair-$index-b", role = MemoryCardRole.Pair),
        )
    }.toMutableList()

    if (spec.hasSingleton) {
        val singleton = selectHolographicSingleton(
            dateUtc = dateUtc,
            difficulty = difficulty,
            catalog = catalog,
            collection = collection,
            usedPairCardKeys = usedPairCardKeys,
        ) ?: return MemoryBoardBuildResult.Unavailable(
            message = "Pas assez de cartes dans ta bibliothèque pour ajouter la carte holographique seule.",
        )
        faces += singleton.copy(id = "holographic-singleton")
    }

    val shuffled = faces.sortedWith(
        compareBy<MemoryCardFace> {
            stableMiniGameHash(
                "memory-board",
                "v${MiniGameDeterministicDrawPolicy.CurrentAlgorithmVersion}",
                dateUtc,
                difficulty.name,
                it.id,
                it.identity.key,
            )
        }.thenBy { it.id },
    )

    return MemoryBoardBuildResult.Ready(
        board = MemoryBoard(
            difficulty = difficulty,
            rows = spec.rows,
            columns = spec.columns,
            cards = shuffled,
        ),
    )
}

private fun selectHolographicSingleton(
    dateUtc: String,
    difficulty: MiniGameDifficulty,
    catalog: MemoryCatalogLookup,
    collection: OwnedCollection,
    usedPairCardKeys: Set<String>,
): MemoryCardFace? {
    val ownedEntries = collection.ownedMiniGameVariants(catalog.cards)
        .mapNotNull { catalog.entryFor(it) }
    val actualHolographic = ownedEntries
        .filter { it.displayCard.activeVariant.isHolographic }
        .filter { it.identity.cardKey !in usedPairCardKeys }
        .stableMemoryOrder(dateUtc, difficulty, "actual-holographic")
        .firstOrNull()

    if (actualHolographic != null) {
        return actualHolographic.toFace(
            id = "holographic-singleton",
            role = MemoryCardRole.HolographicSingleton,
        )
    }

    return ownedEntries
        .mapNotNull { it.asVisualHolographicFallback(catalog) }
        .filter { it.identity.cardKey !in usedPairCardKeys }
        .stableMemoryOrder(dateUtc, difficulty, "visual-holographic")
        .firstOrNull()
        ?.toFace(
            id = "holographic-singleton",
            role = MemoryCardRole.HolographicSingleton,
            isVisualHolographicFallback = true,
        )
}

private fun List<MemoryCardEntry>.stableMemoryOrder(
    dateUtc: String,
    difficulty: MiniGameDifficulty,
    purpose: String,
): List<MemoryCardEntry> = sortedWith(
    compareBy<MemoryCardEntry> {
        stableMiniGameHash(
            "memory-singleton",
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

    fun visualHolographicVariantFor(entry: MemoryCardEntry): DisplayCardVariant? {
        val profile = variantProfilesById[entry.definition.variantProfileId] ?: return null
        val holographicSky = profile.skyQualities.firstOrNull { it.isHolographic } ?: return null
        profile.requireFinishDefinition(entry.identity.finish)
        profile.requireSkyQualityDefinition(holographicSky.code)
        return profile.toDisplayVariant(
            skyQuality = holographicSky.code,
            finish = entry.identity.finish,
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
        isVisualHolographicFallback: Boolean = false,
    ): MemoryCardFace = MemoryCardFace(
        id = id,
        identity = identity,
        displayCard = displayCard,
        role = role,
        isVisualHolographicFallback = isVisualHolographicFallback,
    )

    fun asVisualHolographicFallback(catalog: MemoryCatalogLookup): MemoryCardEntry? {
        val holographicVariant = catalog.visualHolographicVariantFor(this) ?: return null
        return copy(
            identity = identity.copy(skyQuality = holographicVariant.skyQuality),
            displayCard = definition.toDisplayCard(
                extensionName = extensionName,
                activeVariant = holographicVariant,
            ),
        )
    }
}
