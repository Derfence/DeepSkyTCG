package fr.aumombelli.dstcg.data

import fr.aumombelli.dstcg.model.EquipmentCardDefinition
import fr.aumombelli.dstcg.model.EquipmentSettingsDefinition
import fr.aumombelli.dstcg.model.NewPlayerOnboardingStep
import fr.aumombelli.dstcg.model.StandaloneProgress
import fr.aumombelli.dstcg.model.raritySortPriority

internal data class PlannedRevealSlot(
    val slotIndex: Int,
    val rarityLabel: String,
)

internal data class ForcedEquipmentReward(
    val slotIndex: Int,
    val definition: EquipmentCardDefinition,
)

internal data class AstronomyVariantDrawPolicy(
    val allowHolographic: Boolean,
    val allowStamped: Boolean,
    val maxHolographicCardsPerPack: Int,
)

internal sealed interface EquipmentDrawPolicy {
    data object Disabled : EquipmentDrawPolicy

    data class Standard(
        val replacementChancePercent: Double,
    ) : EquipmentDrawPolicy

    data class ForceSingleLevelOne(
        val candidates: List<EquipmentCardDefinition>,
    ) : EquipmentDrawPolicy
}

internal fun resolveEquipmentDrawPolicy(
    progress: StandaloneProgress,
    equipmentCards: List<EquipmentCardDefinition>,
    equipmentSettings: EquipmentSettingsDefinition,
): EquipmentDrawPolicy = when {
    isFirstOnboardingPack(progress) ->
        EquipmentDrawPolicy.Disabled

    isSecondOnboardingPack(progress) ->
        EquipmentDrawPolicy.ForceSingleLevelOne(
            candidates = equipmentCards.filter { it.level == 1 && it.dropWeight > 0 },
        )

    else -> EquipmentDrawPolicy.Standard(
        replacementChancePercent = equipmentSettings.commonReplacementChancePercent,
    )
}

internal fun resolveAstronomyRarityCap(progress: StandaloneProgress): String? = when {
    isFirstOnboardingPack(progress) ->
        "Common"

    isSecondOnboardingPack(progress) ->
        "Uncommon"

    else -> null
}

internal fun resolveAstronomyVariantDrawPolicy(progress: StandaloneProgress): AstronomyVariantDrawPolicy {
    val protectsOpeningVariants = isSpecialOnboardingPack(progress)
    return AstronomyVariantDrawPolicy(
        allowHolographic = !protectsOpeningVariants,
        allowStamped = !protectsOpeningVariants,
        maxHolographicCardsPerPack = if (protectsOpeningVariants) 0 else 1,
    )
}

internal fun resolveForcedEquipmentReward(
    plannedRevealSlots: List<PlannedRevealSlot>,
    drawPolicy: EquipmentDrawPolicy,
    randomizer: PackDrawRandomizer,
): ForcedEquipmentReward? {
    val policy = drawPolicy as? EquipmentDrawPolicy.ForceSingleLevelOne ?: return null
    require(policy.candidates.isNotEmpty()) {
        "Le second pack d'onboarding requiert au moins une carte d'equipement de niveau 1 avec un poids positif."
    }
    val slotIndex = plannedRevealSlots
        .firstOrNull { it.rarityLabel == "Common" }
        ?.slotIndex
        ?: checkNotNull(
            plannedRevealSlots.minWithOrNull(
                compareBy<PlannedRevealSlot> { raritySortPriority(it.rarityLabel) }
                    .thenBy { it.slotIndex },
            ),
        ) {
            "Un pack d'onboarding doit contenir au moins un slot a remplacer."
        }.slotIndex
    return ForcedEquipmentReward(
        slotIndex = slotIndex,
        definition = randomizer.pickWeighted(policy.candidates) { it.dropWeight },
    )
}

private val FIRST_PACK_ONBOARDING_STEPS = setOf(
    NewPlayerOnboardingStep.ShowWelcomeIntro,
    NewPlayerOnboardingStep.OpenFirstPackMenu,
    NewPlayerOnboardingStep.SelectFirstExtension,
    NewPlayerOnboardingStep.SelectFirstBooster,
)

internal fun isSpecialOnboardingPack(progress: StandaloneProgress): Boolean =
    isFirstOnboardingPack(progress) || isSecondOnboardingPack(progress)

private fun isFirstOnboardingPack(progress: StandaloneProgress): Boolean =
    progress.newPlayerOnboardingPackCount == 0 &&
        progress.newPlayerOnboardingStep in FIRST_PACK_ONBOARDING_STEPS

private fun isSecondOnboardingPack(progress: StandaloneProgress): Boolean =
    progress.newPlayerOnboardingPackCount == 1 &&
        progress.newPlayerOnboardingStep == NewPlayerOnboardingStep.OpenSecondPackMenu
