package fr.aumombelli.dstcg.model

import kotlinx.serialization.Serializable

@Serializable
enum class NewPlayerOnboardingStep {
    ShowWelcomeIntro,
    OpenFirstPackMenu,
    SelectFirstExtension,
    SelectFirstBooster,
    ViewLibrary,
    LearnLibraryVariants,
    ViewBadges,
    OpenSecondPackMenu,
    ViewEquipmentMenu,
    ActivateFirstEquipment,
    AwaitCraftingEligibility,
    ViewCraftingMenu,
    UseSkyDarkening,
    ShowConclusion,
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
        NewPlayerOnboardingStep.ShowWelcomeIntro
    }
}
