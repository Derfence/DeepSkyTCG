package fr.aumombelli.dstcg

import fr.aumombelli.dstcg.model.NewPlayerOnboardingContent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CraftingOnboardingToolsContentTest {
    @Test
    fun `crafting tools walkthrough exposes both tools and explicit costs`() {
        val pages = NewPlayerOnboardingContent.craftingToolWalkthroughPages

        assertEquals(listOf("Assombrir le ciel", "Agence spatiale"), pages.map { it.title })
        assertEquals(
            listOf(
                "2 exemplaires Ville",
                "2 exemplaires Périurbain",
                "3 exemplaires Campagne",
                "6 exemplaires Montagne",
            ),
            pages.first().costs.map { it.cost },
        )
        assertTrue(
            pages.last().costs.any { cost ->
                cost.label == "Standard -> Tamponnée" &&
                    cost.cost == "10 exemplaires standard"
            },
        )
    }
}
