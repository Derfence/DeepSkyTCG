package fr.aumombelli.dstcg.model

import kotlinx.serialization.Serializable

@Serializable
enum class NewPlayerOnboardingStep {
    OpenFirstPackMenu,
    SelectFirstExtension,
    SelectFirstBooster,
    ViewLibrary,
    ViewBadges,
    Completed,
}

internal fun NewPlayerOnboardingStep.normalizedForProgress(
    openedPackCount: Int,
    collection: OwnedCollection,
    isLegacySnapshot: Boolean,
): NewPlayerOnboardingStep {
    if (!isLegacySnapshot) return this
    return if (openedPackCount > 0 || collection.cards.isNotEmpty()) {
        NewPlayerOnboardingStep.Completed
    } else {
        NewPlayerOnboardingStep.OpenFirstPackMenu
    }
}
