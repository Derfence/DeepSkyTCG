package fr.aumombelli.dstcg.app

import fr.aumombelli.dstcg.model.CraftingMode
import fr.aumombelli.dstcg.model.NewPlayerOnboardingStep

internal object NewPlayerOnboardingInteractionPolicy {
    fun showsPackOpeningDismissHint(step: NewPlayerOnboardingStep?): Boolean =
        step == NewPlayerOnboardingStep.ViewLibrary

    fun isBlockingStep(step: NewPlayerOnboardingStep?): Boolean = when (step) {
        null,
        NewPlayerOnboardingStep.OpenSecondPackMenu,
        NewPlayerOnboardingStep.AwaitCraftingEligibility,
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
        NewPlayerOnboardingStep.ViewCraftingMenu,
        NewPlayerOnboardingStep.LearnCraftingTools,
        NewPlayerOnboardingStep.UseSkyDarkening,
        NewPlayerOnboardingStep.ShowConclusion,
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
        NewPlayerOnboardingStep.AwaitCraftingEligibility,
        NewPlayerOnboardingStep.Completed,
        -> true

        NewPlayerOnboardingStep.ShowWelcomeIntro,
        NewPlayerOnboardingStep.ViewLibrary,
        NewPlayerOnboardingStep.LearnLibraryVariants,
        NewPlayerOnboardingStep.ViewBadges,
        NewPlayerOnboardingStep.ViewEquipmentMenu,
        NewPlayerOnboardingStep.ActivateFirstEquipment,
        NewPlayerOnboardingStep.ViewCraftingMenu,
        NewPlayerOnboardingStep.LearnCraftingTools,
        NewPlayerOnboardingStep.UseSkyDarkening,
        NewPlayerOnboardingStep.ShowConclusion,
        -> false
    }

    fun allowsHomeLibrary(step: NewPlayerOnboardingStep?): Boolean = when (step) {
        null,
        NewPlayerOnboardingStep.ViewLibrary,
        NewPlayerOnboardingStep.LearnLibraryVariants,
        NewPlayerOnboardingStep.OpenSecondPackMenu,
        NewPlayerOnboardingStep.AwaitCraftingEligibility,
        NewPlayerOnboardingStep.Completed,
        -> true

        NewPlayerOnboardingStep.ShowWelcomeIntro,
        NewPlayerOnboardingStep.OpenFirstPackMenu,
        NewPlayerOnboardingStep.SelectFirstExtension,
        NewPlayerOnboardingStep.SelectFirstBooster,
        NewPlayerOnboardingStep.ViewBadges,
        NewPlayerOnboardingStep.ViewEquipmentMenu,
        NewPlayerOnboardingStep.ActivateFirstEquipment,
        NewPlayerOnboardingStep.ViewCraftingMenu,
        NewPlayerOnboardingStep.LearnCraftingTools,
        NewPlayerOnboardingStep.UseSkyDarkening,
        NewPlayerOnboardingStep.ShowConclusion,
        -> false
    }

    fun allowsHomeBadgeBook(step: NewPlayerOnboardingStep?): Boolean = when (step) {
        null,
        NewPlayerOnboardingStep.ViewBadges,
        NewPlayerOnboardingStep.OpenSecondPackMenu,
        NewPlayerOnboardingStep.AwaitCraftingEligibility,
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
        NewPlayerOnboardingStep.ViewCraftingMenu,
        NewPlayerOnboardingStep.LearnCraftingTools,
        NewPlayerOnboardingStep.UseSkyDarkening,
        NewPlayerOnboardingStep.ShowConclusion,
        -> false
    }

    fun allowsHomeCrafting(step: NewPlayerOnboardingStep?): Boolean = when (step) {
        null,
        NewPlayerOnboardingStep.ViewCraftingMenu,
        NewPlayerOnboardingStep.LearnCraftingTools,
        NewPlayerOnboardingStep.UseSkyDarkening,
        NewPlayerOnboardingStep.Completed,
        -> true

        NewPlayerOnboardingStep.ShowWelcomeIntro,
        NewPlayerOnboardingStep.OpenFirstPackMenu,
        NewPlayerOnboardingStep.SelectFirstExtension,
        NewPlayerOnboardingStep.SelectFirstBooster,
        NewPlayerOnboardingStep.ViewLibrary,
        NewPlayerOnboardingStep.LearnLibraryVariants,
        NewPlayerOnboardingStep.ViewBadges,
        NewPlayerOnboardingStep.OpenSecondPackMenu,
        NewPlayerOnboardingStep.ViewEquipmentMenu,
        NewPlayerOnboardingStep.ActivateFirstEquipment,
        NewPlayerOnboardingStep.AwaitCraftingEligibility,
        NewPlayerOnboardingStep.ShowConclusion,
        -> false
    }

    fun allowsHomeEquipment(step: NewPlayerOnboardingStep?): Boolean = when (step) {
        null,
        NewPlayerOnboardingStep.OpenSecondPackMenu,
        NewPlayerOnboardingStep.ViewEquipmentMenu,
        NewPlayerOnboardingStep.ActivateFirstEquipment,
        NewPlayerOnboardingStep.AwaitCraftingEligibility,
        NewPlayerOnboardingStep.Completed,
        -> true

        NewPlayerOnboardingStep.ShowWelcomeIntro,
        NewPlayerOnboardingStep.OpenFirstPackMenu,
        NewPlayerOnboardingStep.SelectFirstExtension,
        NewPlayerOnboardingStep.SelectFirstBooster,
        NewPlayerOnboardingStep.ViewLibrary,
        NewPlayerOnboardingStep.LearnLibraryVariants,
        NewPlayerOnboardingStep.ViewBadges,
        NewPlayerOnboardingStep.ViewCraftingMenu,
        NewPlayerOnboardingStep.LearnCraftingTools,
        NewPlayerOnboardingStep.UseSkyDarkening,
        NewPlayerOnboardingStep.ShowConclusion,
        -> false
    }

    fun allowsHomeMiniGames(step: NewPlayerOnboardingStep?): Boolean = when (step) {
        null,
        NewPlayerOnboardingStep.Completed,
        -> true

        NewPlayerOnboardingStep.ShowWelcomeIntro,
        NewPlayerOnboardingStep.OpenFirstPackMenu,
        NewPlayerOnboardingStep.SelectFirstExtension,
        NewPlayerOnboardingStep.SelectFirstBooster,
        NewPlayerOnboardingStep.ViewLibrary,
        NewPlayerOnboardingStep.LearnLibraryVariants,
        NewPlayerOnboardingStep.ViewBadges,
        NewPlayerOnboardingStep.OpenSecondPackMenu,
        NewPlayerOnboardingStep.ViewEquipmentMenu,
        NewPlayerOnboardingStep.ActivateFirstEquipment,
        NewPlayerOnboardingStep.AwaitCraftingEligibility,
        NewPlayerOnboardingStep.ViewCraftingMenu,
        NewPlayerOnboardingStep.LearnCraftingTools,
        NewPlayerOnboardingStep.UseSkyDarkening,
        NewPlayerOnboardingStep.ShowConclusion,
        -> false
    }

    fun allowsPackSelectionBack(step: NewPlayerOnboardingStep?): Boolean = when (step) {
        null,
        NewPlayerOnboardingStep.OpenSecondPackMenu,
        NewPlayerOnboardingStep.AwaitCraftingEligibility,
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
        NewPlayerOnboardingStep.ViewCraftingMenu,
        NewPlayerOnboardingStep.LearnCraftingTools,
        NewPlayerOnboardingStep.UseSkyDarkening,
        NewPlayerOnboardingStep.ShowConclusion,
        -> false
    }

    fun allowsPackSelectionExtensionSelection(step: NewPlayerOnboardingStep?): Boolean = when (step) {
        null,
        NewPlayerOnboardingStep.OpenFirstPackMenu,
        NewPlayerOnboardingStep.SelectFirstExtension,
        NewPlayerOnboardingStep.SelectFirstBooster,
        NewPlayerOnboardingStep.OpenSecondPackMenu,
        NewPlayerOnboardingStep.AwaitCraftingEligibility,
        NewPlayerOnboardingStep.Completed,
        -> true

        NewPlayerOnboardingStep.ShowWelcomeIntro,
        NewPlayerOnboardingStep.ViewLibrary,
        NewPlayerOnboardingStep.LearnLibraryVariants,
        NewPlayerOnboardingStep.ViewBadges,
        NewPlayerOnboardingStep.ViewEquipmentMenu,
        NewPlayerOnboardingStep.ActivateFirstEquipment,
        NewPlayerOnboardingStep.ViewCraftingMenu,
        NewPlayerOnboardingStep.LearnCraftingTools,
        NewPlayerOnboardingStep.UseSkyDarkening,
        NewPlayerOnboardingStep.ShowConclusion,
        -> false
    }

    fun allowsPackSelectionBoosterSelection(step: NewPlayerOnboardingStep?): Boolean = when (step) {
        null,
        NewPlayerOnboardingStep.SelectFirstBooster,
        NewPlayerOnboardingStep.OpenSecondPackMenu,
        NewPlayerOnboardingStep.AwaitCraftingEligibility,
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
        NewPlayerOnboardingStep.ViewCraftingMenu,
        NewPlayerOnboardingStep.LearnCraftingTools,
        NewPlayerOnboardingStep.UseSkyDarkening,
        NewPlayerOnboardingStep.ShowConclusion,
        -> false
    }

    fun allowsEquipmentBack(step: NewPlayerOnboardingStep?): Boolean = when (step) {
        null,
        NewPlayerOnboardingStep.OpenSecondPackMenu,
        NewPlayerOnboardingStep.AwaitCraftingEligibility,
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
        NewPlayerOnboardingStep.ViewCraftingMenu,
        NewPlayerOnboardingStep.LearnCraftingTools,
        NewPlayerOnboardingStep.UseSkyDarkening,
        NewPlayerOnboardingStep.ShowConclusion,
        -> false
    }

    fun allowsEquipmentActivation(step: NewPlayerOnboardingStep?): Boolean = when (step) {
        null,
        NewPlayerOnboardingStep.OpenSecondPackMenu,
        NewPlayerOnboardingStep.ViewEquipmentMenu,
        NewPlayerOnboardingStep.ActivateFirstEquipment,
        NewPlayerOnboardingStep.AwaitCraftingEligibility,
        NewPlayerOnboardingStep.Completed,
        -> true

        NewPlayerOnboardingStep.ShowWelcomeIntro,
        NewPlayerOnboardingStep.OpenFirstPackMenu,
        NewPlayerOnboardingStep.SelectFirstExtension,
        NewPlayerOnboardingStep.SelectFirstBooster,
        NewPlayerOnboardingStep.ViewLibrary,
        NewPlayerOnboardingStep.LearnLibraryVariants,
        NewPlayerOnboardingStep.ViewBadges,
        NewPlayerOnboardingStep.ViewCraftingMenu,
        NewPlayerOnboardingStep.LearnCraftingTools,
        NewPlayerOnboardingStep.UseSkyDarkening,
        NewPlayerOnboardingStep.ShowConclusion,
        -> false
    }

    fun allowsCraftingBack(step: NewPlayerOnboardingStep?): Boolean = when (step) {
        NewPlayerOnboardingStep.ViewCraftingMenu,
        NewPlayerOnboardingStep.LearnCraftingTools,
        NewPlayerOnboardingStep.UseSkyDarkening,
        -> false

        null,
        NewPlayerOnboardingStep.ShowWelcomeIntro,
        NewPlayerOnboardingStep.OpenFirstPackMenu,
        NewPlayerOnboardingStep.SelectFirstExtension,
        NewPlayerOnboardingStep.SelectFirstBooster,
        NewPlayerOnboardingStep.ViewLibrary,
        NewPlayerOnboardingStep.LearnLibraryVariants,
        NewPlayerOnboardingStep.ViewBadges,
        NewPlayerOnboardingStep.OpenSecondPackMenu,
        NewPlayerOnboardingStep.ViewEquipmentMenu,
        NewPlayerOnboardingStep.ActivateFirstEquipment,
        NewPlayerOnboardingStep.AwaitCraftingEligibility,
        NewPlayerOnboardingStep.ShowConclusion,
        NewPlayerOnboardingStep.Completed,
        -> true
    }

    fun allowsCraftingModeSelection(step: NewPlayerOnboardingStep?, mode: CraftingMode): Boolean = when (step) {
        NewPlayerOnboardingStep.LearnCraftingTools,
        NewPlayerOnboardingStep.ShowConclusion,
        -> false

        NewPlayerOnboardingStep.ViewCraftingMenu,
        NewPlayerOnboardingStep.UseSkyDarkening -> mode == CraftingMode.DarkenSky
        else -> true
    }

    fun allowsCraftingApplication(step: NewPlayerOnboardingStep?, mode: CraftingMode?): Boolean = when (step) {
        NewPlayerOnboardingStep.LearnCraftingTools,
        NewPlayerOnboardingStep.ShowConclusion,
        -> false

        NewPlayerOnboardingStep.ViewCraftingMenu,
        NewPlayerOnboardingStep.UseSkyDarkening -> mode == CraftingMode.DarkenSky
        else -> true
    }
}
