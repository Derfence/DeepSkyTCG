package fr.aumombelli.dstcg.feature.badges

import fr.aumombelli.dstcg.model.CardDefinition
import fr.aumombelli.dstcg.model.EquipmentCardDefinition
import fr.aumombelli.dstcg.model.ExtensionDefinition
import fr.aumombelli.dstcg.model.StandaloneProgress
import fr.aumombelli.dstcg.model.VariantProfile

internal fun buildNewlyUnlockedBadges(
    extensions: List<ExtensionDefinition>,
    cards: List<CardDefinition>,
    equipmentCards: List<EquipmentCardDefinition>,
    variantProfiles: List<VariantProfile>,
    beforeProgress: StandaloneProgress,
    afterProgress: StandaloneProgress,
): List<BadgeItem> {
    val newlyUnlockedIds = buildNewlyUnlockedBadgeIds(
        extensions = extensions,
        cards = cards,
        equipmentCards = equipmentCards,
        variantProfiles = variantProfiles,
        beforeProgress = beforeProgress,
        afterProgress = afterProgress,
    )

    return sortBadgeCelebrationItems(
        buildUnlockedBadges(
            extensions = extensions,
            cards = cards,
            equipmentCards = equipmentCards,
            variantProfiles = variantProfiles,
            progress = afterProgress,
        ).filter { badge -> badge.id in newlyUnlockedIds },
    )
}

internal fun buildNewlyUnlockedBadgeIds(
    extensions: List<ExtensionDefinition>,
    cards: List<CardDefinition>,
    equipmentCards: List<EquipmentCardDefinition>,
    variantProfiles: List<VariantProfile>,
    beforeProgress: StandaloneProgress,
    afterProgress: StandaloneProgress,
): Set<String> {
    val beforeUnlockedIds = buildUnlockedBadgeIds(
        extensions = extensions,
        cards = cards,
        equipmentCards = equipmentCards,
        variantProfiles = variantProfiles,
        progress = beforeProgress,
    )

    return buildUnlockedBadgeIds(
        extensions = extensions,
        cards = cards,
        equipmentCards = equipmentCards,
        variantProfiles = variantProfiles,
        progress = afterProgress,
    ).filterNotTo(mutableSetOf()) { badgeId -> badgeId in beforeUnlockedIds }
}

internal fun sortBadgeCelebrationItems(badges: List<BadgeItem>): List<BadgeItem> = badges.sortedWith(
    compareByDescending<BadgeItem> { badgeRequirementPriority(it.requirementType) }
        .thenByDescending { skyQualityCelebrationPriority(it.skyQualityCode) }
        .thenBy { it.extensionName }
        .thenBy { it.title },
)

private fun badgeRequirementPriority(requirementType: BadgeRequirementType): Int = when (requirementType) {
    BadgeRequirementType.PerfectCollection -> 10
    BadgeRequirementType.HolographicStamped -> 9
    BadgeRequirementType.Stamped -> 8
    BadgeRequirementType.EquipmentThreeLevelThreeTypesActiveSimultaneously -> 7
    BadgeRequirementType.EquipmentThreeTypesActiveSimultaneously -> 6
    BadgeRequirementType.EquipmentAllCardsActivatedOnce -> 5
    BadgeRequirementType.EquipmentActivations100 -> 4
    BadgeRequirementType.EquipmentAffectedPacks100 -> 3
    BadgeRequirementType.FirstPackOpened -> 2
    BadgeRequirementType.SkyQuality -> 1
}

private fun skyQualityCelebrationPriority(skyQualityCode: String?): Int = when (skyQualityCode) {
    "holographic" -> 5
    "mountain" -> 4
    "rural" -> 3
    "suburban" -> 2
    "city" -> 1
    else -> 0
}

private fun buildUnlockedBadgeIds(
    extensions: List<ExtensionDefinition>,
    cards: List<CardDefinition>,
    equipmentCards: List<EquipmentCardDefinition>,
    variantProfiles: List<VariantProfile>,
    progress: StandaloneProgress,
): Set<String> = buildUnlockedBadges(
    extensions = extensions,
    cards = cards,
    equipmentCards = equipmentCards,
    variantProfiles = variantProfiles,
    progress = progress,
).mapTo(mutableSetOf()) { badge -> badge.id }

private fun buildUnlockedBadges(
    extensions: List<ExtensionDefinition>,
    cards: List<CardDefinition>,
    equipmentCards: List<EquipmentCardDefinition>,
    variantProfiles: List<VariantProfile>,
    progress: StandaloneProgress,
): List<BadgeItem> = buildBadgeBookSections(
    extensions = extensions,
    cards = cards,
    equipmentCards = equipmentCards,
    variantProfiles = variantProfiles,
    progress = progress,
).flatMap { section ->
    section.badges.filter(BadgeItem::isUnlocked)
}
