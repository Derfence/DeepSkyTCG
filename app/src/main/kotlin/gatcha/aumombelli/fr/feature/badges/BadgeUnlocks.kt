package fr.aumombelli.gatcha.feature.badges

import fr.aumombelli.gatcha.model.CardDefinition
import fr.aumombelli.gatcha.model.ExtensionDefinition
import fr.aumombelli.gatcha.model.OwnedCollection
import fr.aumombelli.gatcha.model.VariantProfile

internal fun buildNewlyUnlockedBadges(
    extensions: List<ExtensionDefinition>,
    cards: List<CardDefinition>,
    variantProfiles: List<VariantProfile>,
    beforeCollection: OwnedCollection,
    afterCollection: OwnedCollection,
): List<BadgeItem> {
    val beforeUnlockedIds = buildBadgeBookSections(
        extensions = extensions,
        cards = cards,
        variantProfiles = variantProfiles,
        collection = beforeCollection,
    ).flatMap { section ->
        section.badges.filter(BadgeItem::isUnlocked).map(BadgeItem::id)
    }.toSet()

    return sortBadgeCelebrationItems(
        buildBadgeBookSections(
            extensions = extensions,
            cards = cards,
            variantProfiles = variantProfiles,
            collection = afterCollection,
        ).flatMap { section ->
            section.badges.filter { badge ->
                badge.isUnlocked && badge.id !in beforeUnlockedIds
            }
        },
    )
}

internal fun sortBadgeCelebrationItems(badges: List<BadgeItem>): List<BadgeItem> = badges.sortedWith(
    compareByDescending<BadgeItem> { badgeRequirementPriority(it.requirementType) }
        .thenByDescending { skyQualityCelebrationPriority(it.skyQualityCode) }
        .thenBy { it.extensionName }
        .thenBy { it.title },
)

private fun badgeRequirementPriority(requirementType: BadgeRequirementType): Int = when (requirementType) {
    BadgeRequirementType.PerfectCollection -> 4
    BadgeRequirementType.MountainHolographic -> 3
    BadgeRequirementType.Holographic -> 2
    BadgeRequirementType.SkyQuality -> 1
}

private fun skyQualityCelebrationPriority(skyQualityCode: String?): Int = when (skyQualityCode) {
    "mountain" -> 4
    "rural" -> 3
    "suburban" -> 2
    "city" -> 1
    else -> 0
}
