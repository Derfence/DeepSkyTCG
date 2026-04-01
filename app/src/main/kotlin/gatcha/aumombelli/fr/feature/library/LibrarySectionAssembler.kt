package fr.aumombelli.gatcha.feature.library

import fr.aumombelli.gatcha.model.CardDefinition
import fr.aumombelli.gatcha.model.ExtensionDefinition
import fr.aumombelli.gatcha.model.LibraryCardItem
import fr.aumombelli.gatcha.model.LibrarySection
import fr.aumombelli.gatcha.model.OwnedCollection
import fr.aumombelli.gatcha.model.VariantProfile
import fr.aumombelli.gatcha.model.ownedCountFor
import fr.aumombelli.gatcha.model.raritySortPriority
import fr.aumombelli.gatcha.model.toDisplayVariants

internal fun buildLibrarySections(
    extensions: List<ExtensionDefinition>,
    cards: List<CardDefinition>,
    variantProfiles: List<VariantProfile>,
    collection: OwnedCollection,
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
                        availableVariants = availableVariants,
                    )
                },
        )
    }
}
