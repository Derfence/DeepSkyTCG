package fr.aumombelli.dstcg.feature.library

import fr.aumombelli.dstcg.model.CardDefinition
import fr.aumombelli.dstcg.model.ExtensionDefinition
import fr.aumombelli.dstcg.model.LibraryCardItem
import fr.aumombelli.dstcg.model.LibrarySection
import fr.aumombelli.dstcg.model.OwnedCollection
import fr.aumombelli.dstcg.model.VariantProfile
import fr.aumombelli.dstcg.model.ownedCountFor
import fr.aumombelli.dstcg.model.raritySortPriority
import fr.aumombelli.dstcg.model.toDisplayVariants

internal fun buildLibrarySections(
    extensions: List<ExtensionDefinition>,
    cards: List<CardDefinition>,
    variantProfiles: List<VariantProfile>,
    collection: OwnedCollection,
    newCardIds: Set<String> = emptySet(),
): List<LibrarySection> {
    val variantProfilesById = variantProfiles.associateBy { it.id }

    return extensions.map { extension ->
        LibrarySection(
            extension = extension,
            cards = cards
                .filter { it.extensionId == extension.id }
                .sortedWith(
                    compareBy(
                        { raritySortPriority(it.rarityLabel) },
                        { it.id },
                    ),
                )
                .map { card ->
                    val availableVariants = collection.cards[card.id]
                        ?.toDisplayVariants(
                            checkNotNull(variantProfilesById[card.variantProfileId]) {
                                "Profil de variante inconnu '${card.variantProfileId}' pour '${card.id}'."
                            },
                        )
                        .orEmpty()
                    LibraryCardItem(
                        definition = card,
                        extensionName = extension.name,
                        ownedCount = collection.ownedCountFor(card.id),
                        showNewIndicator = card.id in newCardIds,
                        availableVariants = availableVariants,
                    )
                },
        )
    }
}
