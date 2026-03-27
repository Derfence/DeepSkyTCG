package fr.aumombelli.gatcha.feature.badges

data class BadgeBookUiState(
    val isLoading: Boolean = true,
    val sections: List<BadgeSection> = emptyList(),
    val errorMessage: String? = null,
)

data class BadgeSection(
    val extensionId: String,
    val extensionName: String,
    val badges: List<BadgeItem>,
) {
    val unlockedCount: Int
        get() = badges.count { it.isUnlocked }
}

enum class BadgeRequirementType {
    SkyQuality,
    Holographic,
    MountainHolographic,
    PerfectCollection,
}

data class BadgeProgress(
    val matchedCards: Int,
    val totalCards: Int,
) {
    val isComplete: Boolean
        get() = totalCards > 0 && matchedCards >= totalCards

    val label: String
        get() = "$matchedCards / $totalCards cartes valides"
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
) {
    val isUnlocked: Boolean
        get() = progress.isComplete
}
