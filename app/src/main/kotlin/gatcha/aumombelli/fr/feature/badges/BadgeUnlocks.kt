package fr.aumombelli.gatcha.feature.badges

import fr.aumombelli.gatcha.model.CardDefinition
import fr.aumombelli.gatcha.model.ExtensionDefinition
import fr.aumombelli.gatcha.model.StandaloneProgress
import fr.aumombelli.gatcha.model.VariantProfile

internal fun buildNewlyUnlockedBadges(
    extensions: List<ExtensionDefinition>,
    cards: List<CardDefinition>,
    variantProfiles: List<VariantProfile>,
    beforeProgress: StandaloneProgress,
    afterProgress: StandaloneProgress,
): List<BadgeItem> {
    val beforeUnlockedIds = buildUnlockedBadgeIds(
        extensions = extensions,
        cards = cards,
        variantProfiles = variantProfiles,
        progress = beforeProgress,
    )

    return sortBadgeCelebrationItems(
        buildUnlockedBadges(
            extensions = extensions,
            cards = cards,
            variantProfiles = variantProfiles,
            progress = afterProgress,
        ).filter { badge -> badge.id !in beforeUnlockedIds },
    )
}

internal fun sortBadgeCelebrationItems(badges: List<BadgeItem>): List<BadgeItem> = badges.sortedWith(
    compareByDescending<BadgeItem> { badgeRequirementPriority(it.requirementType) }
        .thenByDescending { skyQualityCelebrationPriority(it.skyQualityCode) }
        .thenBy { it.extensionName }
        .thenBy { it.title },
)

private fun badgeRequirementPriority(requirementType: BadgeRequirementType): Int = when (requirementType) {
    BadgeRequirementType.PerfectCollection -> 5
    BadgeRequirementType.MountainHolographic -> 4
    BadgeRequirementType.Holographic -> 3
    BadgeRequirementType.FirstPackOpened -> 2
    BadgeRequirementType.SkyQuality -> 1
}

private fun skyQualityCelebrationPriority(skyQualityCode: String?): Int = when (skyQualityCode) {
    "mountain" -> 4
    "rural" -> 3
    "suburban" -> 2
    "city" -> 1
    else -> 0
}

private fun buildUnlockedBadgeIds(
    extensions: List<ExtensionDefinition>,
    cards: List<CardDefinition>,
    variantProfiles: List<VariantProfile>,
    progress: StandaloneProgress,
): Set<String> = buildUnlockedBadges(
    extensions = extensions,
    cards = cards,
    variantProfiles = variantProfiles,
    progress = progress,
).mapTo(mutableSetOf()) { badge -> badge.id }

private fun buildUnlockedBadges(
    extensions: List<ExtensionDefinition>,
    cards: List<CardDefinition>,
    variantProfiles: List<VariantProfile>,
    progress: StandaloneProgress,
): List<BadgeItem> = buildBadgeBookSections(
    extensions = extensions,
    cards = cards,
    variantProfiles = variantProfiles,
    progress = progress,
).flatMap { section ->
    section.badges.filter(BadgeItem::isUnlocked)
}
