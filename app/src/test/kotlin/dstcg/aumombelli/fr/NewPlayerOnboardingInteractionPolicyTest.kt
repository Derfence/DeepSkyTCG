package fr.aumombelli.dstcg

import fr.aumombelli.dstcg.app.NewPlayerOnboardingInteractionPolicy
import fr.aumombelli.dstcg.model.CraftingMode
import fr.aumombelli.dstcg.model.NewPlayerOnboardingStep
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NewPlayerOnboardingInteractionPolicyTest {
    @Test
    fun `open second pack menu is a free pause state`() {
        val step = NewPlayerOnboardingStep.OpenSecondPackMenu

        assertFalse(NewPlayerOnboardingInteractionPolicy.isBlockingStep(step))
        assertTrue(NewPlayerOnboardingInteractionPolicy.allowsHomeExit(step))
        assertTrue(NewPlayerOnboardingInteractionPolicy.allowsHomeAuxiliaryActions(step))
        assertTrue(NewPlayerOnboardingInteractionPolicy.allowsHomeOpenPack(step))
        assertTrue(NewPlayerOnboardingInteractionPolicy.allowsHomeLibrary(step))
        assertTrue(NewPlayerOnboardingInteractionPolicy.allowsHomeBadgeBook(step))
        assertFalse(NewPlayerOnboardingInteractionPolicy.allowsHomeCrafting(step))
        assertTrue(NewPlayerOnboardingInteractionPolicy.allowsHomeEquipment(step))
        assertTrue(NewPlayerOnboardingInteractionPolicy.allowsPackSelectionBack(step))
        assertTrue(NewPlayerOnboardingInteractionPolicy.allowsPackSelectionExtensionSelection(step))
        assertTrue(NewPlayerOnboardingInteractionPolicy.allowsPackSelectionBoosterSelection(step))
        assertTrue(NewPlayerOnboardingInteractionPolicy.allowsEquipmentBack(step))
        assertTrue(NewPlayerOnboardingInteractionPolicy.allowsEquipmentActivation(step))
    }

    @Test
    fun `await crafting eligibility is a free pause state with crafting still locked`() {
        val step = NewPlayerOnboardingStep.AwaitCraftingEligibility

        assertFalse(NewPlayerOnboardingInteractionPolicy.isBlockingStep(step))
        assertTrue(NewPlayerOnboardingInteractionPolicy.allowsHomeExit(step))
        assertTrue(NewPlayerOnboardingInteractionPolicy.allowsHomeAuxiliaryActions(step))
        assertTrue(NewPlayerOnboardingInteractionPolicy.allowsHomeOpenPack(step))
        assertTrue(NewPlayerOnboardingInteractionPolicy.allowsHomeLibrary(step))
        assertTrue(NewPlayerOnboardingInteractionPolicy.allowsHomeBadgeBook(step))
        assertTrue(NewPlayerOnboardingInteractionPolicy.allowsHomeEquipment(step))
        assertFalse(NewPlayerOnboardingInteractionPolicy.allowsHomeCrafting(step))
    }

    @Test
    fun `view badges keeps home limited to the badge path`() {
        val step = NewPlayerOnboardingStep.ViewBadges

        assertTrue(NewPlayerOnboardingInteractionPolicy.isBlockingStep(step))
        assertFalse(NewPlayerOnboardingInteractionPolicy.allowsHomeExit(step))
        assertFalse(NewPlayerOnboardingInteractionPolicy.allowsHomeAuxiliaryActions(step))
        assertFalse(NewPlayerOnboardingInteractionPolicy.allowsHomeOpenPack(step))
        assertFalse(NewPlayerOnboardingInteractionPolicy.allowsHomeLibrary(step))
        assertTrue(NewPlayerOnboardingInteractionPolicy.allowsHomeBadgeBook(step))
        assertFalse(NewPlayerOnboardingInteractionPolicy.allowsHomeEquipment(step))
    }

    @Test
    fun `show welcome intro blocks leaving home until onboarding starts`() {
        val step = NewPlayerOnboardingStep.ShowWelcomeIntro

        assertTrue(NewPlayerOnboardingInteractionPolicy.isBlockingStep(step))
        assertFalse(NewPlayerOnboardingInteractionPolicy.allowsHomeExit(step))
        assertFalse(NewPlayerOnboardingInteractionPolicy.allowsHomeAuxiliaryActions(step))
        assertFalse(NewPlayerOnboardingInteractionPolicy.allowsHomeOpenPack(step))
        assertFalse(NewPlayerOnboardingInteractionPolicy.allowsHomeLibrary(step))
        assertFalse(NewPlayerOnboardingInteractionPolicy.allowsHomeBadgeBook(step))
        assertFalse(NewPlayerOnboardingInteractionPolicy.allowsHomeEquipment(step))
        assertFalse(NewPlayerOnboardingInteractionPolicy.allowsHomeCrafting(step))
    }

    @Test
    fun `learn library variants keeps home limited to reopening the library`() {
        val step = NewPlayerOnboardingStep.LearnLibraryVariants

        assertTrue(NewPlayerOnboardingInteractionPolicy.isBlockingStep(step))
        assertFalse(NewPlayerOnboardingInteractionPolicy.allowsHomeExit(step))
        assertFalse(NewPlayerOnboardingInteractionPolicy.allowsHomeOpenPack(step))
        assertTrue(NewPlayerOnboardingInteractionPolicy.allowsHomeLibrary(step))
        assertFalse(NewPlayerOnboardingInteractionPolicy.allowsHomeBadgeBook(step))
        assertFalse(NewPlayerOnboardingInteractionPolicy.allowsHomeEquipment(step))
    }

    @Test
    fun `activate first equipment keeps equipment activation available while blocking exits`() {
        val step = NewPlayerOnboardingStep.ActivateFirstEquipment

        assertTrue(NewPlayerOnboardingInteractionPolicy.isBlockingStep(step))
        assertFalse(NewPlayerOnboardingInteractionPolicy.allowsHomeExit(step))
        assertTrue(NewPlayerOnboardingInteractionPolicy.allowsHomeEquipment(step))
        assertFalse(NewPlayerOnboardingInteractionPolicy.allowsEquipmentBack(step))
        assertTrue(NewPlayerOnboardingInteractionPolicy.allowsEquipmentActivation(step))
    }

    @Test
    fun `view crafting menu keeps home limited to crafting`() {
        val step = NewPlayerOnboardingStep.ViewCraftingMenu

        assertTrue(NewPlayerOnboardingInteractionPolicy.isBlockingStep(step))
        assertFalse(NewPlayerOnboardingInteractionPolicy.allowsHomeOpenPack(step))
        assertFalse(NewPlayerOnboardingInteractionPolicy.allowsHomeLibrary(step))
        assertFalse(NewPlayerOnboardingInteractionPolicy.allowsHomeBadgeBook(step))
        assertFalse(NewPlayerOnboardingInteractionPolicy.allowsHomeEquipment(step))
        assertTrue(NewPlayerOnboardingInteractionPolicy.allowsHomeCrafting(step))
        assertFalse(NewPlayerOnboardingInteractionPolicy.allowsCraftingBack(step))
        assertTrue(NewPlayerOnboardingInteractionPolicy.allowsCraftingModeSelection(step, CraftingMode.DarkenSky))
        assertFalse(NewPlayerOnboardingInteractionPolicy.allowsCraftingModeSelection(step, CraftingMode.SpaceAgency))
        assertTrue(NewPlayerOnboardingInteractionPolicy.allowsCraftingApplication(step, CraftingMode.DarkenSky))
        assertFalse(NewPlayerOnboardingInteractionPolicy.allowsCraftingApplication(step, CraftingMode.SpaceAgency))
    }

    @Test
    fun `learn crafting tools keeps crafting modal blocking until acknowledged`() {
        val step = NewPlayerOnboardingStep.LearnCraftingTools

        assertTrue(NewPlayerOnboardingInteractionPolicy.isBlockingStep(step))
        assertFalse(NewPlayerOnboardingInteractionPolicy.allowsHomeOpenPack(step))
        assertFalse(NewPlayerOnboardingInteractionPolicy.allowsHomeLibrary(step))
        assertFalse(NewPlayerOnboardingInteractionPolicy.allowsHomeBadgeBook(step))
        assertFalse(NewPlayerOnboardingInteractionPolicy.allowsHomeEquipment(step))
        assertTrue(NewPlayerOnboardingInteractionPolicy.allowsHomeCrafting(step))
        assertFalse(NewPlayerOnboardingInteractionPolicy.allowsCraftingBack(step))
        assertFalse(NewPlayerOnboardingInteractionPolicy.allowsCraftingModeSelection(step, CraftingMode.DarkenSky))
        assertFalse(NewPlayerOnboardingInteractionPolicy.allowsCraftingModeSelection(step, CraftingMode.SpaceAgency))
        assertFalse(NewPlayerOnboardingInteractionPolicy.allowsCraftingApplication(step, CraftingMode.DarkenSky))
        assertFalse(NewPlayerOnboardingInteractionPolicy.allowsCraftingApplication(step, CraftingMode.SpaceAgency))
    }

    @Test
    fun `use sky darkening restricts crafting to the darken sky recipe`() {
        val step = NewPlayerOnboardingStep.UseSkyDarkening

        assertTrue(NewPlayerOnboardingInteractionPolicy.isBlockingStep(step))
        assertFalse(NewPlayerOnboardingInteractionPolicy.allowsCraftingBack(step))
        assertTrue(NewPlayerOnboardingInteractionPolicy.allowsCraftingModeSelection(step, CraftingMode.DarkenSky))
        assertFalse(NewPlayerOnboardingInteractionPolicy.allowsCraftingModeSelection(step, CraftingMode.SpaceAgency))
        assertTrue(NewPlayerOnboardingInteractionPolicy.allowsCraftingApplication(step, CraftingMode.DarkenSky))
        assertFalse(NewPlayerOnboardingInteractionPolicy.allowsCraftingApplication(step, CraftingMode.SpaceAgency))
    }

    @Test
    fun `discover mini games blocks home except mini games and allows crafting back`() {
        val step = NewPlayerOnboardingStep.DiscoverMiniGames

        assertTrue(NewPlayerOnboardingInteractionPolicy.isBlockingStep(step))
        assertFalse(NewPlayerOnboardingInteractionPolicy.allowsHomeExit(step))
        assertFalse(NewPlayerOnboardingInteractionPolicy.allowsHomeAuxiliaryActions(step))
        assertFalse(NewPlayerOnboardingInteractionPolicy.allowsHomeOpenPack(step))
        assertFalse(NewPlayerOnboardingInteractionPolicy.allowsHomeLibrary(step))
        assertFalse(NewPlayerOnboardingInteractionPolicy.allowsHomeBadgeBook(step))
        assertFalse(NewPlayerOnboardingInteractionPolicy.allowsHomeEquipment(step))
        assertFalse(NewPlayerOnboardingInteractionPolicy.allowsHomeCrafting(step))
        assertTrue(NewPlayerOnboardingInteractionPolicy.allowsHomeMiniGames(step))
        assertTrue(NewPlayerOnboardingInteractionPolicy.allowsCraftingBack(step))
        assertFalse(NewPlayerOnboardingInteractionPolicy.allowsCraftingModeSelection(step, CraftingMode.DarkenSky))
        assertFalse(NewPlayerOnboardingInteractionPolicy.allowsCraftingApplication(step, CraftingMode.DarkenSky))
    }

    @Test
    fun `show conclusion allows returning home while keeping crafting actions locked`() {
        val step = NewPlayerOnboardingStep.ShowConclusion

        assertTrue(NewPlayerOnboardingInteractionPolicy.isBlockingStep(step))
        assertFalse(NewPlayerOnboardingInteractionPolicy.allowsHomeExit(step))
        assertTrue(NewPlayerOnboardingInteractionPolicy.allowsCraftingBack(step))
        assertFalse(NewPlayerOnboardingInteractionPolicy.allowsCraftingModeSelection(step, CraftingMode.DarkenSky))
        assertFalse(NewPlayerOnboardingInteractionPolicy.allowsCraftingApplication(step, CraftingMode.DarkenSky))
    }

    @Test
    fun `select first booster keeps pack progression available but blocks leaving the scene`() {
        val step = NewPlayerOnboardingStep.SelectFirstBooster

        assertTrue(NewPlayerOnboardingInteractionPolicy.isBlockingStep(step))
        assertFalse(NewPlayerOnboardingInteractionPolicy.allowsPackSelectionBack(step))
        assertTrue(NewPlayerOnboardingInteractionPolicy.allowsPackSelectionExtensionSelection(step))
        assertTrue(NewPlayerOnboardingInteractionPolicy.allowsPackSelectionBoosterSelection(step))
    }

    @Test
    fun `pack opening dismiss hint is reserved for the first opened pack`() {
        assertTrue(
            NewPlayerOnboardingInteractionPolicy.showsPackOpeningDismissHint(
                NewPlayerOnboardingStep.ViewLibrary,
            ),
        )
        assertFalse(
            NewPlayerOnboardingInteractionPolicy.showsPackOpeningDismissHint(
                NewPlayerOnboardingStep.OpenSecondPackMenu,
            ),
        )
        assertFalse(
            NewPlayerOnboardingInteractionPolicy.showsPackOpeningDismissHint(
                NewPlayerOnboardingStep.ViewEquipmentMenu,
            ),
        )
        assertFalse(
            NewPlayerOnboardingInteractionPolicy.showsPackOpeningDismissHint(
                NewPlayerOnboardingStep.ShowConclusion,
            ),
        )
        assertFalse(
            NewPlayerOnboardingInteractionPolicy.showsPackOpeningDismissHint(
                NewPlayerOnboardingStep.Completed,
            ),
        )
        assertFalse(NewPlayerOnboardingInteractionPolicy.showsPackOpeningDismissHint(null))
    }
}
