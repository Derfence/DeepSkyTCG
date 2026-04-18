package fr.aumombelli.dstcg

import fr.aumombelli.dstcg.feature.badges.buildBadgeBookSections
import fr.aumombelli.dstcg.feature.badges.buildNewlyUnlockedBadges
import fr.aumombelli.dstcg.model.ExtensionDefinition
import fr.aumombelli.dstcg.model.OwnedCollection
import fr.aumombelli.dstcg.model.OwnedVariantCount
import fr.aumombelli.dstcg.model.StandaloneProgress
import fr.aumombelli.dstcg.testsupport.fixtures.testCardDefinition
import fr.aumombelli.dstcg.testsupport.fixtures.testVariantProfiles
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
    fun `stamped city card unlocks stamped badge but not holo stamped badge`() {
        val section = buildBadgeBookSections(
            extensions = listOf(ExtensionDefinition("astro", "Astro", "cover")),
            cards = listOf(testCardDefinition("AST-001", extensionId = "astro")),
            variantProfiles = testVariantProfiles(),
            progress = badgeProgress(
                collection = ownedCollectionWithVariants(
                    "AST-001",
                    OwnedVariantCount("city", "stamped", 1),
                ),
            ),
        ).first { it.extensionId == "astro" }

        val badgesById = section.badges.associateBy { it.id }

        assertTrue(checkNotNull(badgesById["astro::finish::stamped"]).isUnlocked)
        assertFalse(checkNotNull(badgesById["astro::finish::holographic-stamped"]).isUnlocked)
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
                        "AST-001" to fr.aumombelli.dstcg.model.OwnedCardEntry(
                            totalOwned = 1,
                            variants = listOf(OwnedVariantCount("holographic", "stamped", 1)),
                        ),
                        "AST-002" to fr.aumombelli.dstcg.model.OwnedCardEntry(
                            totalOwned = 1,
                            variants = listOf(OwnedVariantCount("holographic", "standard", 1)),
                        ),
                    ),
                ),
            ),
        ).first { it.extensionId == "astro" }

        val stampedBadge = section.badges.first { it.id == "astro::finish::stamped" }
        val holographicStampedBadge = section.badges.first {
            it.id == "astro::finish::holographic-stamped"
        }

        assertEquals("1 / 2 cartes valides", stampedBadge.progress.label)
        assertFalse(stampedBadge.isUnlocked)
        assertFalse(holographicStampedBadge.isUnlocked)
        assertEquals(section.badges.count { it.isUnlocked }, section.unlockedCount)
    }

    @Test
    fun `perfect collection badge unlocks only when all ten variants are owned`() {
        val section = buildBadgeBookSections(
            extensions = listOf(ExtensionDefinition("astro", "Astro", "cover")),
            cards = listOf(testCardDefinition("AST-001", extensionId = "astro")),
            variantProfiles = testVariantProfiles(),
            progress = badgeProgress(
                collection = ownedCollectionWithVariants(
                    "AST-001",
                    OwnedVariantCount("city", "standard", 1),
                    OwnedVariantCount("city", "stamped", 1),
                    OwnedVariantCount("suburban", "standard", 1),
                    OwnedVariantCount("suburban", "stamped", 1),
                    OwnedVariantCount("rural", "standard", 1),
                    OwnedVariantCount("rural", "stamped", 1),
                    OwnedVariantCount("mountain", "standard", 1),
                    OwnedVariantCount("mountain", "stamped", 1),
                    OwnedVariantCount("holographic", "standard", 1),
                    OwnedVariantCount("holographic", "stamped", 1),
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
                    OwnedVariantCount("holographic", "stamped", 1),
                ),
            ),
        )

        assertEquals(
            listOf(
                "astro::finish::holographic-stamped",
                "astro::finish::stamped",
                "astro::sky::holographic",
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
