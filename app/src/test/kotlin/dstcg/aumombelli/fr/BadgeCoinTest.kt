package fr.aumombelli.dstcg

import androidx.compose.ui.unit.dp
import fr.aumombelli.dstcg.feature.badges.BadgeItem
import fr.aumombelli.dstcg.feature.badges.BadgeCenterMarkKind
import fr.aumombelli.dstcg.feature.badges.BadgeProgress
import fr.aumombelli.dstcg.feature.badges.BadgeRequirementType
import fr.aumombelli.dstcg.feature.badges.badgeCoinLogoScale
import fr.aumombelli.dstcg.feature.badges.badgeCoinLogoSize
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BadgeCoinTest {
    @Test
    fun general_badge_logo_is_larger_than_regular_badge_logo_for_same_coin_size() {
        val coinSize = 92.dp

        val generalLogoSize = badgeCoinLogoSize(
            badge = sampleBadge(requirementType = BadgeRequirementType.FirstPackOpened),
            coinSize = coinSize,
        )
        val equipmentLogoSize = badgeCoinLogoSize(
            badge = sampleBadge(requirementType = BadgeRequirementType.EquipmentActivations100),
            coinSize = coinSize,
        )
        val regularLogoSize = badgeCoinLogoSize(
            badge = sampleBadge(requirementType = BadgeRequirementType.Stamped),
            coinSize = coinSize,
        )

        assertTrue(generalLogoSize > regularLogoSize)
        assertTrue(equipmentLogoSize > regularLogoSize)
        assertEquals(0.72f, badgeCoinLogoScale(sampleBadge(BadgeRequirementType.FirstPackOpened)), 0.0001f)
        assertEquals(0.64f, badgeCoinLogoScale(sampleBadge(BadgeRequirementType.EquipmentActivations100)), 0.0001f)
        assertEquals(0.60f, badgeCoinLogoScale(sampleBadge(BadgeRequirementType.Stamped)), 0.0001f)
    }

    @Test
    fun general_badge_logo_keeps_same_ratio_when_coin_size_changes() {
        val badge = sampleBadge(requirementType = BadgeRequirementType.FirstPackOpened)
        val compactRatio = badgeCoinLogoSize(badge = badge, coinSize = 92.dp).value / 92f
        val expandedRatio = badgeCoinLogoSize(badge = badge, coinSize = 180.dp).value / 180f

        assertEquals(compactRatio, expandedRatio, 0.0001f)
        assertEquals(0.72f, expandedRatio, 0.0001f)
    }

    private fun sampleBadge(requirementType: BadgeRequirementType): BadgeItem = BadgeItem(
        id = "general::pack::first-opened",
        extensionId = "general",
        extensionName = "Generaux",
        title = "Premier pack",
        description = "Description",
        requirementType = requirementType,
        progress = BadgeProgress(matchedCards = 1, totalCards = 1),
        centerMarkKind = when (requirementType) {
            BadgeRequirementType.FirstPackOpened -> BadgeCenterMarkKind.GeneralLogo
            BadgeRequirementType.EquipmentAllCardsActivatedOnce,
            BadgeRequirementType.EquipmentThreeTypesActiveSimultaneously,
            BadgeRequirementType.EquipmentThreeLevelThreeTypesActiveSimultaneously,
            BadgeRequirementType.EquipmentAffectedPacks100,
            BadgeRequirementType.EquipmentActivations100,
            -> BadgeCenterMarkKind.EquipmentMountGlyph
            else -> BadgeCenterMarkKind.ExtensionLogo
        },
    )
}
