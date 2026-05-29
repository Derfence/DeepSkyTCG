package fr.aumombelli.dstcg

import fr.aumombelli.dstcg.model.CraftingCardRef
import fr.aumombelli.dstcg.model.CraftingMode
import fr.aumombelli.dstcg.model.CraftingRecipeValidation
import fr.aumombelli.dstcg.model.OwnedVariantCount
import fr.aumombelli.dstcg.model.applyCraftingRecipe
import fr.aumombelli.dstcg.model.craftingCountFor
import fr.aumombelli.dstcg.model.validateCraftingRecipe
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CraftingOperationsTest {
    private val card = testCardDefinition("ALP-001")
    private val variantProfilesById = testVariantProfiles().associateBy { it.id }
    private val cardsById = mapOf(card.id to card)
    private val upgradeCosts = mapOf(
        "city" to 2,
        "suburban" to 2,
        "rural" to 3,
        "mountain" to 6,
    )

    @Test
    fun `darken sky consumes exact source variant and creates next standard sky`() {
        val collection = ownedCollectionWithVariants(
            card.id,
            OwnedVariantCount("city", "standard", 2),
        )

        val recipe = validateRecipe(
            collection = collection,
            mode = CraftingMode.DarkenSky,
            source = CraftingCardRef(card.id, "city", "standard"),
        )
        val updated = collection.applyCraftingRecipe(recipe)

        assertEquals(0, updated.craftingCountFor(CraftingCardRef(card.id, "city", "standard")))
        assertEquals(1, updated.craftingCountFor(CraftingCardRef(card.id, "suburban", "standard")))
    }

    @Test
    fun `darken sky loses stamped finish when stamped copies are consumed`() {
        val collection = ownedCollectionWithVariants(
            card.id,
            OwnedVariantCount("city", "stamped", 2),
        )

        val recipe = validateRecipe(
            collection = collection,
            mode = CraftingMode.DarkenSky,
            source = CraftingCardRef(card.id, "city", "stamped"),
        )
        val updated = collection.applyCraftingRecipe(recipe)

        assertEquals(0, updated.craftingCountFor(CraftingCardRef(card.id, "city", "stamped")))
        assertEquals(1, updated.craftingCountFor(CraftingCardRef(card.id, "suburban", "standard")))
        assertEquals(0, updated.craftingCountFor(CraftingCardRef(card.id, "suburban", "stamped")))
    }

    @Test
    fun `darken sky rejects insufficient copies`() {
        val result = validateCraftingRecipe(
            mode = CraftingMode.DarkenSky,
            collection = ownedCollectionWithVariants(
                card.id,
                OwnedVariantCount("rural", "standard", 2),
            ),
            source = CraftingCardRef(card.id, "rural", "standard"),
            cardsById = cardsById,
            variantProfilesById = variantProfilesById,
            skyUpgradeCosts = upgradeCosts,
        )

        assertTrue(result is CraftingRecipeValidation.Invalid)
    }

    @Test
    fun `darken sky rejects max quality`() {
        val result = validateCraftingRecipe(
            mode = CraftingMode.DarkenSky,
            collection = ownedCollectionWithVariants(
                card.id,
                OwnedVariantCount("holographic", "standard", 10),
            ),
            source = CraftingCardRef(card.id, "holographic", "standard"),
            cardsById = cardsById,
            variantProfilesById = variantProfilesById,
            skyUpgradeCosts = upgradeCosts,
        )

        assertTrue(result is CraftingRecipeValidation.Invalid)
    }

    @Test
    fun `space agency consumes ten standard copies and creates stamped variant`() {
        val collection = ownedCollectionWithVariants(
            card.id,
            OwnedVariantCount("city", "standard", 10),
        )

        val recipe = validateRecipe(
            collection = collection,
            mode = CraftingMode.SpaceAgency,
            source = CraftingCardRef(card.id, "city", "standard"),
        )
        val updated = collection.applyCraftingRecipe(recipe)

        assertEquals(0, updated.craftingCountFor(CraftingCardRef(card.id, "city", "standard")))
        assertEquals(1, updated.craftingCountFor(CraftingCardRef(card.id, "city", "stamped")))
    }

    @Test
    fun `space agency increments existing stamped variant`() {
        val collection = ownedCollectionWithVariants(
            card.id,
            OwnedVariantCount("city", "standard", 10),
            OwnedVariantCount("city", "stamped", 2),
        )

        val recipe = validateRecipe(
            collection = collection,
            mode = CraftingMode.SpaceAgency,
            source = CraftingCardRef(card.id, "city", "standard"),
        )
        val updated = collection.applyCraftingRecipe(recipe)

        assertEquals(3, updated.craftingCountFor(CraftingCardRef(card.id, "city", "stamped")))
    }

    private fun validateRecipe(
        collection: fr.aumombelli.dstcg.model.OwnedCollection,
        mode: CraftingMode,
        source: CraftingCardRef,
    ) = when (
        val result = validateCraftingRecipe(
            mode = mode,
            collection = collection,
            source = source,
            cardsById = cardsById,
            variantProfilesById = variantProfilesById,
            skyUpgradeCosts = upgradeCosts,
        )
    ) {
        is CraftingRecipeValidation.Valid -> result.recipe
        is CraftingRecipeValidation.Invalid -> error(result.message)
    }
}
