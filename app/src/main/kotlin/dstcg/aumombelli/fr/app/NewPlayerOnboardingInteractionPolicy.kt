package fr.aumombelli.dstcg.app

import fr.aumombelli.dstcg.model.NewPlayerOnboardingStep

internal object NewPlayerOnboardingInteractionPolicy {
    fun showsPackOpeningDismissHint(step: NewPlayerOnboardingStep?): Boolean =
        step == NewPlayerOnboardingStep.ViewLibrary

    fun isBlockingStep(step: NewPlayerOnboardingStep?): Boolean = when (step) {
        null,
        NewPlayerOnboardingStep.OpenSecondPackMenu,
        NewPlayerOnboardingStep.Completed,
        -> false

        NewPlayerOnboardingStep.ShowWelcomeIntro,
        NewPlayerOnboardingStep.OpenFirstPackMenu,
        NewPlayerOnboardingStep.SelectFirstExtension,
        NewPlayerOnboardingStep.SelectFirstBooster,
        NewPlayerOnboardingStep.ViewLibrary,
        NewPlayerOnboardingStep.LearnLibraryVariants,
        NewPlayerOnboardingStep.ViewBadges,
        NewPlayerOnboardingStep.ViewEquipmentMenu,
        NewPlayerOnboardingStep.ActivateFirstEquipment,
        -> true
    }

    fun allowsHomeExit(step: NewPlayerOnboardingStep?): Boolean = !isBlockingStep(step)

    fun allowsHomeAuxiliaryActions(step: NewPlayerOnboardingStep?): Boolean = !isBlockingStep(step)

    fun allowsHomeOpenPack(step: NewPlayerOnboardingStep?): Boolean = when (step) {
        null,
        NewPlayerOnboardingStep.OpenFirstPackMenu,
        NewPlayerOnboardingStep.SelectFirstExtension,
        NewPlayerOnboardingStep.SelectFirstBooster,
        NewPlayerOnboardingStep.OpenSecondPackMenu,
        NewPlayerOnboardingStep.Completed,
        -> true

        NewPlayerOnboardingStep.ShowWelcomeIntro,
        NewPlayerOnboardingStep.ViewLibrary,
        NewPlayerOnboardingStep.LearnLibraryVariants,
        NewPlayerOnboardingStep.ViewBadges,
        NewPlayerOnboardingStep.ViewEquipmentMenu,
        NewPlayerOnboardingStep.ActivateFirstEquipment,
        -> false
    }

    fun allowsHomeLibrary(step: NewPlayerOnboardingStep?): Boolean = when (step) {
        null,
        NewPlayerOnboardingStep.ViewLibrary,
        NewPlayerOnboardingStep.LearnLibraryVariants,
        NewPlayerOnboardingStep.OpenSecondPackMenu,
        NewPlayerOnboardingStep.Completed,
        -> true

        NewPlayerOnboardingStep.ShowWelcomeIntro,
        NewPlayerOnboardingStep.OpenFirstPackMenu,
        NewPlayerOnboardingStep.SelectFirstExtension,
        NewPlayerOnboardingStep.SelectFirstBooster,
        NewPlayerOnboardingStep.ViewBadges,
        NewPlayerOnboardingStep.ViewEquipmentMenu,
        NewPlayerOnboardingStep.ActivateFirstEquipment,
        -> false
    }

    fun allowsHomeBadgeBook(step: NewPlayerOnboardingStep?): Boolean = when (step) {
        null,
        NewPlayerOnboardingStep.ViewBadges,
        NewPlayerOnboardingStep.OpenSecondPackMenu,
        NewPlayerOnboardingStep.Completed,
        -> true

        NewPlayerOnboardingStep.ShowWelcomeIntro,
        NewPlayerOnboardingStep.OpenFirstPackMenu,
        NewPlayerOnboardingStep.SelectFirstExtension,
        NewPlayerOnboardingStep.SelectFirstBooster,
        NewPlayerOnboardingStep.ViewLibrary,
        NewPlayerOnboardingStep.LearnLibraryVariants,
        NewPlayerOnboardingStep.ViewEquipmentMenu,
        NewPlayerOnboardingStep.ActivateFirstEquipment,
        -> false
    }

    fun allowsHomeEquipment(step: NewPlayerOnboardingStep?): Boolean = when (step) {
        null,
        NewPlayerOnboardingStep.OpenSecondPackMenu,
        NewPlayerOnboardingStep.ViewEquipmentMenu,
        NewPlayerOnboardingStep.ActivateFirstEquipment,
        NewPlayerOnboardingStep.Completed,
        -> true

        NewPlayerOnboardingStep.ShowWelcomeIntro,
        NewPlayerOnboardingStep.OpenFirstPackMenu,
        NewPlayerOnboardingStep.SelectFirstExtension,
        NewPlayerOnboardingStep.SelectFirstBooster,
        NewPlayerOnboardingStep.ViewLibrary,
        NewPlayerOnboardingStep.LearnLibraryVariants,
        NewPlayerOnboardingStep.ViewBadges,
        -> false
    }

    fun allowsPackSelectionBack(step: NewPlayerOnboardingStep?): Boolean = when (step) {
        null,
        NewPlayerOnboardingStep.OpenSecondPackMenu,
        NewPlayerOnboardingStep.Completed,
        -> true

        NewPlayerOnboardingStep.ShowWelcomeIntro,
        NewPlayerOnboardingStep.OpenFirstPackMenu,
        NewPlayerOnboardingStep.SelectFirstExtension,
        NewPlayerOnboardingStep.SelectFirstBooster,
        NewPlayerOnboardingStep.ViewLibrary,
        NewPlayerOnboardingStep.LearnLibraryVariants,
        NewPlayerOnboardingStep.ViewBadges,
        NewPlayerOnboardingStep.ViewEquipmentMenu,
        NewPlayerOnboardingStep.ActivateFirstEquipment,
        -> false
    }

    fun allowsPackSelectionExtensionSelection(step: NewPlayerOnboardingStep?): Boolean = when (step) {
        null,
        NewPlayerOnboardingStep.OpenFirstPackMenu,
        NewPlayerOnboardingStep.SelectFirstExtension,
        NewPlayerOnboardingStep.SelectFirstBooster,
        NewPlayerOnboardingStep.OpenSecondPackMenu,
        NewPlayerOnboardingStep.Completed,
        -> true

        NewPlayerOnboardingStep.ShowWelcomeIntro,
        NewPlayerOnboardingStep.ViewLibrary,
        NewPlayerOnboardingStep.LearnLibraryVariants,
        NewPlayerOnboardingStep.ViewBadges,
        NewPlayerOnboardingStep.ViewEquipmentMenu,
        NewPlayerOnboardingStep.ActivateFirstEquipment,
        -> false
    }

    fun allowsPackSelectionBoosterSelection(step: NewPlayerOnboardingStep?): Boolean = when (step) {
        null,
        NewPlayerOnboardingStep.SelectFirstBooster,
        NewPlayerOnboardingStep.OpenSecondPackMenu,
        NewPlayerOnboardingStep.Completed,
        -> true

        NewPlayerOnboardingStep.ShowWelcomeIntro,
        NewPlayerOnboardingStep.OpenFirstPackMenu,
        NewPlayerOnboardingStep.SelectFirstExtension,
        NewPlayerOnboardingStep.ViewLibrary,
        NewPlayerOnboardingStep.LearnLibraryVariants,
        NewPlayerOnboardingStep.ViewBadges,
        NewPlayerOnboardingStep.ViewEquipmentMenu,
        NewPlayerOnboardingStep.ActivateFirstEquipment,
        -> false
    }

    fun allowsEquipmentBack(step: NewPlayerOnboardingStep?): Boolean = when (step) {
        null,
        NewPlayerOnboardingStep.OpenSecondPackMenu,
        NewPlayerOnboardingStep.Completed,
        -> true

        NewPlayerOnboardingStep.ShowWelcomeIntro,
        NewPlayerOnboardingStep.OpenFirstPackMenu,
        NewPlayerOnboardingStep.SelectFirstExtension,
        NewPlayerOnboardingStep.SelectFirstBooster,
        NewPlayerOnboardingStep.ViewLibrary,
        NewPlayerOnboardingStep.LearnLibraryVariants,
        NewPlayerOnboardingStep.ViewBadges,
        NewPlayerOnboardingStep.ViewEquipmentMenu,
        NewPlayerOnboardingStep.ActivateFirstEquipment,
        -> false
    }

    fun allowsEquipmentActivation(step: NewPlayerOnboardingStep?): Boolean = when (step) {
        null,
        NewPlayerOnboardingStep.OpenSecondPackMenu,
        NewPlayerOnboardingStep.ViewEquipmentMenu,
        NewPlayerOnboardingStep.ActivateFirstEquipment,
        NewPlayerOnboardingStep.Completed,
        -> true

        NewPlayerOnboardingStep.ShowWelcomeIntro,
        NewPlayerOnboardingStep.OpenFirstPackMenu,
        NewPlayerOnboardingStep.SelectFirstExtension,
        NewPlayerOnboardingStep.SelectFirstBooster,
        NewPlayerOnboardingStep.ViewLibrary,
        NewPlayerOnboardingStep.LearnLibraryVariants,
        NewPlayerOnboardingStep.ViewBadges,
        -> false
    }
}
