package fr.aumombelli.dstcg.feature.badges

data class BadgeBookUiState(
    val isLoading: Boolean = true,
    val sections: List<BadgeSection> = emptyList(),
    val errorMessage: String? = null,
)

enum class BadgeSectionType {
    General,
    Extension,
}

data class BadgeSection(
    val extensionId: String,
    val extensionName: String,
    val badges: List<BadgeItem>,
    val sectionType: BadgeSectionType = BadgeSectionType.Extension,
) {
    val unlockedCount: Int
        get() = badges.count { it.isUnlocked }
}

enum class BadgeRequirementType {
    FirstPackOpened,
    EpicBoostedPackOpened,
    EquipmentAllCardsActivatedOnce,
    EquipmentThreeTypesActiveSimultaneously,
    EquipmentThreeLevelThreeTypesActiveSimultaneously,
    EquipmentAffectedPacks100,
    EquipmentActivations100,
    SkyQuality,
    Stamped,
    HolographicStamped,
    PerfectCollection,
}

enum class BadgeCenterMarkKind {
    ExtensionLogo,
    GeneralLogo,
    EquipmentMountGlyph,
}

data class BadgeProgress(
    val matchedCards: Int,
    val totalCards: Int,
    val unitLabel: String = "cartes valides",
) {
    val isComplete: Boolean
        get() = totalCards > 0 && matchedCards >= totalCards

    val label: String
        get() = "$matchedCards / $totalCards $unitLabel"
}

data class BadgeItem(
    val id: String,
    val extensionId: String,
    val extensionName: String,
    val title: String,
    val description: String,
    val requirementType: BadgeRequirementType,
    val progress: BadgeProgress,
    val skyQualityCode: String? = null,
    val centerMarkKind: BadgeCenterMarkKind = BadgeCenterMarkKind.ExtensionLogo,
) {
    val isUnlocked: Boolean
        get() = progress.isComplete
}
