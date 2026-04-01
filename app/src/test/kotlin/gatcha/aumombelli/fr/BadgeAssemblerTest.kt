package fr.aumombelli.gatcha

import fr.aumombelli.gatcha.feature.badges.buildBadgeBookSections
import fr.aumombelli.gatcha.feature.badges.buildNewlyUnlockedBadges
import fr.aumombelli.gatcha.model.ExtensionDefinition
import fr.aumombelli.gatcha.model.OwnedCollection
import fr.aumombelli.gatcha.model.OwnedVariantCount
import fr.aumombelli.gatcha.model.StandaloneProgress
import fr.aumombelli.gatcha.testsupport.fixtures.testCardDefinition
import fr.aumombelli.gatcha.testsupport.fixtures.testVariantProfiles
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BadgeAssemblerTest {
    @Test
    fun `rural card unlocks all lower sky quality badges`() {
        val section = buildBadgeBookSections(
            extensions = listOf(ExtensionDefinition("astro", "Astro", "cover")),
            cards = listOf(testCardDefinition("AST-001", extensionId = "astro")),
            variantProfiles = testVariantProfiles(),
            progress = badgeProgress(
                collection = ownedCollectionWithVariants(
                    "AST-001",
                    OwnedVariantCount("rural", "standard", 1),
                ),
            ),
        ).first { it.extensionId == "astro" }

        val badgesById = section.badges.associateBy { it.id }

        assertTrue(checkNotNull(badgesById["astro::sky::city"]).isUnlocked)
        assertTrue(checkNotNull(badgesById["astro::sky::suburban"]).isUnlocked)
        assertTrue(checkNotNull(badgesById["astro::sky::rural"]).isUnlocked)
        assertFalse(checkNotNull(badgesById["astro::sky::mountain"]).isUnlocked)
    }

    @Test
    fun `holographic city card unlocks holographic badge but not mountain holographic`() {
        val section = buildBadgeBookSections(
            extensions = listOf(ExtensionDefinition("astro", "Astro", "cover")),
            cards = listOf(testCardDefinition("AST-001", extensionId = "astro")),
            variantProfiles = testVariantProfiles(),
            progress = badgeProgress(
                collection = ownedCollectionWithVariants(
                    "AST-001",
                    OwnedVariantCount("city", "holographic", 1),
                ),
            ),
        ).first { it.extensionId == "astro" }

        val badgesById = section.badges.associateBy { it.id }

        assertTrue(checkNotNull(badgesById["astro::finish::holographic"]).isUnlocked)
        assertFalse(checkNotNull(badgesById["astro::finish::mountain-holographic"]).isUnlocked)
    }

    @Test
    fun `section counts unlocked badges only when every card matches the requirement`() {
        val section = buildBadgeBookSections(
            extensions = listOf(ExtensionDefinition("astro", "Astro", "cover")),
            cards = listOf(
                testCardDefinition("AST-001", extensionId = "astro"),
                testCardDefinition("AST-002", extensionId = "astro"),
            ),
            variantProfiles = testVariantProfiles(),
            progress = badgeProgress(
                collection = OwnedCollection(
                    cards = sortedMapOf(
                        "AST-001" to fr.aumombelli.gatcha.model.OwnedCardEntry(
                            totalOwned = 1,
                            variants = listOf(OwnedVariantCount("mountain", "holographic", 1)),
                        ),
                        "AST-002" to fr.aumombelli.gatcha.model.OwnedCardEntry(
                            totalOwned = 1,
                            variants = listOf(OwnedVariantCount("mountain", "standard", 1)),
                        ),
                    ),
                ),
            ),
        ).first { it.extensionId == "astro" }

        val holographicBadge = section.badges.first { it.id == "astro::finish::holographic" }
        val mountainHolographicBadge = section.badges.first {
            it.id == "astro::finish::mountain-holographic"
        }

        assertEquals("1 / 2 cartes valides", holographicBadge.progress.label)
        assertFalse(holographicBadge.isUnlocked)
        assertFalse(mountainHolographicBadge.isUnlocked)
        assertEquals(section.badges.count { it.isUnlocked }, section.unlockedCount)
    }

    @Test
    fun `perfect collection badge unlocks only when all eight variants are owned`() {
        val section = buildBadgeBookSections(
            extensions = listOf(ExtensionDefinition("astro", "Astro", "cover")),
            cards = listOf(testCardDefinition("AST-001", extensionId = "astro")),
            variantProfiles = testVariantProfiles(),
            progress = badgeProgress(
                collection = ownedCollectionWithVariants(
                    "AST-001",
                    OwnedVariantCount("city", "standard", 1),
                    OwnedVariantCount("city", "holographic", 1),
                    OwnedVariantCount("suburban", "standard", 1),
                    OwnedVariantCount("suburban", "holographic", 1),
                    OwnedVariantCount("rural", "standard", 1),
                    OwnedVariantCount("rural", "holographic", 1),
                    OwnedVariantCount("mountain", "standard", 1),
                    OwnedVariantCount("mountain", "holographic", 1),
                ),
            ),
        ).first { it.extensionId == "astro" }

        val perfectBadge = section.badges.first { it.id == "astro::collection::perfect" }

        assertTrue(perfectBadge.isUnlocked)
        assertEquals("1 / 1 cartes valides", perfectBadge.progress.label)
    }

    @Test
    fun `newly unlocked badges are diffed and sorted for celebration`() {
        val newlyUnlockedBadges = buildNewlyUnlockedBadges(
            extensions = listOf(ExtensionDefinition("astro", "Astro", "cover")),
            cards = listOf(testCardDefinition("AST-001", extensionId = "astro")),
            variantProfiles = testVariantProfiles(),
            beforeProgress = badgeProgress(OwnedCollection()),
            afterProgress = badgeProgress(
                collection = ownedCollectionWithVariants(
                    "AST-001",
                    OwnedVariantCount("mountain", "holographic", 1),
                ),
            ),
        )

        assertEquals(
            listOf(
                "astro::finish::mountain-holographic",
                "astro::finish::holographic",
                "astro::sky::mountain",
                "astro::sky::rural",
                "astro::sky::suburban",
                "astro::sky::city",
            ),
            newlyUnlockedBadges.map { it.id },
        )
    }

    @Test
    fun `general section contains first pack opened badge`() {
        val sections = buildBadgeBookSections(
            extensions = listOf(ExtensionDefinition("astro", "Astro", "cover")),
            cards = listOf(testCardDefinition("AST-001", extensionId = "astro")),
            variantProfiles = testVariantProfiles(),
            progress = badgeProgress(
                collection = OwnedCollection(),
                openedPackCount = 1,
            ),
        )

        val generalSection = sections.first()
        val firstPackBadge = generalSection.badges.single()

        assertEquals("general", generalSection.extensionId)
        assertEquals("Général", generalSection.extensionName)
        assertTrue(firstPackBadge.isUnlocked)
        assertEquals("1 / 1 pack ouvert", firstPackBadge.progress.label)
    }

    private fun badgeProgress(
        collection: OwnedCollection,
        openedPackCount: Int = 0,
    ): StandaloneProgress = StandaloneProgress(
        collection = collection,
        openedPackCount = openedPackCount,
    )
}
