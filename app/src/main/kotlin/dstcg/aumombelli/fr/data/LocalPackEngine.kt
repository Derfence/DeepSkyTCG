package fr.aumombelli.dstcg.data

import android.content.Context
import fr.aumombelli.dstcg.model.AstronomyPackRevealSlot
import fr.aumombelli.dstcg.model.CardVariant
import fr.aumombelli.dstcg.model.DrawPackResponse
import fr.aumombelli.dstcg.model.EquipmentCardDefinition
import fr.aumombelli.dstcg.model.EquipmentPackRevealSlot
import fr.aumombelli.dstcg.model.EquipmentSettingsDefinition
import fr.aumombelli.dstcg.model.NewPlayerOnboardingStep
import fr.aumombelli.dstcg.model.PackCard
import fr.aumombelli.dstcg.model.PackRechargeState
import fr.aumombelli.dstcg.model.StandaloneProgress
import fr.aumombelli.dstcg.model.WeightedCode
import fr.aumombelli.dstcg.model.raritySortPriority
import fr.aumombelli.dstcg.model.requireFinishDefinition
import fr.aumombelli.dstcg.model.requireSkyQualityDefinition
import fr.aumombelli.dstcg.model.sortedRevealSlotsForPackReveal
import java.time.Clock
import java.time.Duration
import java.time.Instant
import kotlin.math.abs

class PackCooldownException(
    val retryAt: String,
) : Exception("Un nouveau pack n'est pas encore disponible.")

/**
 * Central gameplay settings for the fully offline standalone client.
 *
 * Weather and recharge both depend on the trusted time source so the behavior
 * stays deterministic across devices for the same trusted UTC date.
 */
data class StandaloneGameSettings(
    val maxStoredDraws: Int = DEFAULT_MAX_STORED_DRAWS,
    val weatherPolicy: WeatherPolicy = DeterministicWeatherCalendar,
    val timeSource: TrustedTimeSource = ClockTrustedTimeSource(Clock.systemUTC()),
    val entropySource: EntropySource = RandomEntropySource(kotlin.random.Random.Default),
) {
    companion object {
        fun offlineDefault(context: Context): StandaloneGameSettings = StandaloneGameSettings(
            timeSource = AndroidTrustedTimeSource(context),
            entropySource = SecureEntropySource(),
        )
    }
}

class LocalPackEngine(
    private val catalogRepository: CatalogGateway,
    private val settings: StandaloneGameSettings,
) {
    private val runtimeCalculator = CatalogBalanceRuntimeCalculator()

    suspend fun drawPack(
        extensionId: String,
        progress: StandaloneProgress,
        now: Instant,
    ): DrawPackResponse {
        val cards = catalogRepository.loadCards()
        val variantProfiles = catalogRepository.loadVariantProfiles()
        val equipmentCards = catalogRepository.loadEquipmentCards()
        val equipmentSettings = catalogRepository.loadEquipmentSettings()
        val runtimeCatalog = runtimeCalculator.resolve(
            cards = cards,
            variantProfiles = variantProfiles,
            gameBalance = catalogRepository.loadGameBalance(),
        )
        val activeBonus = resolveActiveEquipmentBonus(
            activeEquipmentByType = progress.activeEquipmentByType,
            equipmentCards = equipmentCards,
        )
        val drawConfig = runtimeCatalog.drawConfig
        val normalizedChargeState = normalizePackRechargeState(
            rechargeState = progress.rechargeState,
            now = now,
            drawCooldown = drawConfig.drawCooldown,
            maxStoredDraws = settings.maxStoredDraws,
            weatherPolicy = settings.weatherPolicy,
            rechargeMultiplier = activeBonus.rechargeMultiplier,
        )
        if (normalizedChargeState.availableDrawCount == 0) {
            val retryAt = checkNotNull(
                normalizedChargeState.derivedNextChargeAt(
                    now = now,
                    drawCooldown = drawConfig.drawCooldown,
                    maxStoredDraws = settings.maxStoredDraws,
                    weatherPolicy = settings.weatherPolicy,
                    rechargeMultiplier = activeBonus.rechargeMultiplier,
                ),
            ) {
                "Un tirage verrouille doit exposer une heure de recharge suivante."
            }
            throw PackCooldownException(retryAt.toString())
        }
        val updatedChargeState = consumePackCharge(
            normalizedState = normalizedChargeState,
            now = now,
            drawCooldown = drawConfig.drawCooldown,
            maxStoredDraws = settings.maxStoredDraws,
        )

        val extensionPlan = runtimeCatalog.extensionPlansById[extensionId]
        if (extensionPlan == null) {
            throw IllegalStateException("Aucune carte n'a ete trouvee pour cette extension.")
        }
        val variantProfilesById = variantProfiles.associateBy { it.id }
        val equipmentDrawPolicy = resolveEquipmentDrawPolicy(
            progress = progress,
            equipmentCards = equipmentCards,
            equipmentSettings = equipmentSettings,
        )
        val astronomyRarityCap = resolveAstronomyRarityCap(progress)
        val drawnAt = now.toString()
        val plannedRevealSlots = List(drawConfig.cardsPerDraw) { slotIndex ->
            val baseRarity = pickWeighted(extensionPlan.rarityWeights) { it.weight }.code
            PlannedRevealSlot(
                slotIndex = slotIndex,
                rarityLabel = clampAstronomyRarity(
                    rarityLabel = maybePromoteRarity(
                        baseRarity = baseRarity,
                        availableRarities = extensionPlan.cardsByRarity.keys,
                        rarityBoostPercent = activeBonus.rarityBoostPercent,
                    ),
                    availableRarities = extensionPlan.cardsByRarity.keys,
                    maxAllowedRarityLabel = astronomyRarityCap,
                ),
            )
        }
        val forcedEquipmentReward = resolveForcedEquipmentReward(
            plannedRevealSlots = plannedRevealSlots,
            drawPolicy = equipmentDrawPolicy,
        )
        val usedAstronomyCardIds = mutableSetOf<String>()
        val usedEquipmentDefinitionIds = mutableSetOf<String>()
        val revealSlots = plannedRevealSlots.map { plannedSlot ->
            if (forcedEquipmentReward?.slotIndex == plannedSlot.slotIndex) {
                usedEquipmentDefinitionIds += forcedEquipmentReward.definition.id
                EquipmentPackRevealSlot(
                    slotIndex = plannedSlot.slotIndex,
                    definition = forcedEquipmentReward.definition,
                )
            } else {
                val equipmentReward = when (equipmentDrawPolicy) {
                    is EquipmentDrawPolicy.Standard -> maybeDrawEquipmentReward(
                        rarityLabel = plannedSlot.rarityLabel,
                        equipmentCards = equipmentCards,
                        replacementChancePercent = equipmentDrawPolicy.replacementChancePercent,
                        excludedEquipmentDefinitionIds = usedEquipmentDefinitionIds,
                    )

                    EquipmentDrawPolicy.Disabled,
                    is EquipmentDrawPolicy.ForceSingleLevelOne,
                    -> null
                }
                if (equipmentReward != null) {
                    usedEquipmentDefinitionIds += equipmentReward.id
                    EquipmentPackRevealSlot(
                        slotIndex = plannedSlot.slotIndex,
                        definition = equipmentReward,
                    )
                } else {
                    drawAstronomyRevealSlot(
                        plannedSlot = plannedSlot,
                        extensionPlan = extensionPlan,
                        variantProfilesById = variantProfilesById,
                        runtimeCatalog = runtimeCatalog,
                        activeBonus = activeBonus,
                        excludedCardIds = usedAstronomyCardIds,
                        maxAllowedRarityLabel = astronomyRarityCap,
                    )
                }
            }
        }.sortedRevealSlotsForPackReveal()

        return DrawPackResponse(
            extensionId = extensionId,
            drawnAt = drawnAt,
            rechargeState = updatedChargeState,
            revealSlots = revealSlots,
        )
    }

    private fun drawAstronomyRevealSlot(
        plannedSlot: PlannedRevealSlot,
        extensionPlan: ExtensionDrawPlan,
        variantProfilesById: Map<String, fr.aumombelli.dstcg.model.VariantProfile>,
        runtimeCatalog: RuntimeCatalogBalance,
        activeBonus: ActiveEquipmentBonus,
        excludedCardIds: MutableSet<String>,
        maxAllowedRarityLabel: String?,
    ): AstronomyPackRevealSlot {
        val definition = pickUniqueAstronomyCardDefinition(
            plannedSlot = plannedSlot,
            extensionPlan = extensionPlan,
            excludedCardIds = excludedCardIds,
            maxAllowedRarityLabel = maxAllowedRarityLabel,
        )
        excludedCardIds += definition.id
        val variantProfile = checkNotNull(variantProfilesById[definition.variantProfileId]) {
            "Profil de variante inconnu '${definition.variantProfileId}' pour la carte '${definition.id}'."
        }
        val runtimeVariantWeights = checkNotNull(runtimeCatalog.variantWeightsByProfileId[definition.variantProfileId]) {
            "Poids de variante introuvables pour '${definition.variantProfileId}'."
        }
        val skyQuality = pickSkyQualityWithEquipmentBonus(
            variantProfile = variantProfile,
            runtimeVariantWeights = runtimeVariantWeights,
            holographicQualityPercent = activeBonus.holographicQualityPercent,
        )
        val finish = variantProfile.requireFinishDefinition(
            pickWeightedCode(runtimeVariantWeights.finishWeights),
        )
        return AstronomyPackRevealSlot(
            slotIndex = plannedSlot.slotIndex,
            card = PackCard(
                cardId = definition.id,
                name = definition.name,
                rarityLabel = definition.rarityLabel,
                imageRef = definition.imageRef,
                variant = CardVariant(
                    skyQuality = skyQuality.code,
                    skyQualityLabel = skyQuality.label,
                    finish = finish.code,
                    finishLabel = finish.label,
                    isHolographic = skyQuality.isHolographic,
                    isStamped = finish.isStamped,
                ),
            ),
        )
    }

    private fun resolveEquipmentDrawPolicy(
        progress: StandaloneProgress,
        equipmentCards: List<EquipmentCardDefinition>,
        equipmentSettings: EquipmentSettingsDefinition,
    ): EquipmentDrawPolicy = when {
        progress.openedPackCount == 0 &&
            progress.newPlayerOnboardingStep in FIRST_PACK_ONBOARDING_STEPS ->
            EquipmentDrawPolicy.Disabled

        progress.openedPackCount == 1 &&
            progress.newPlayerOnboardingStep == NewPlayerOnboardingStep.OpenSecondPackMenu ->
            EquipmentDrawPolicy.ForceSingleLevelOne(
                candidates = equipmentCards.filter { it.level == 1 && it.dropWeight > 0 },
            )

        else -> EquipmentDrawPolicy.Standard(
            replacementChancePercent = equipmentSettings.commonReplacementChancePercent,
        )
    }

    private fun resolveAstronomyRarityCap(progress: StandaloneProgress): String? = when {
        progress.openedPackCount == 0 &&
            progress.newPlayerOnboardingStep in FIRST_PACK_ONBOARDING_STEPS ->
            "Common"

        progress.openedPackCount == 1 &&
            progress.newPlayerOnboardingStep == NewPlayerOnboardingStep.OpenSecondPackMenu ->
            "Uncommon"

        else -> null
    }

    private fun resolveForcedEquipmentReward(
        plannedRevealSlots: List<PlannedRevealSlot>,
        drawPolicy: EquipmentDrawPolicy,
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
            definition = pickWeighted(policy.candidates) { it.dropWeight },
        )
    }

    private fun pickUniqueAstronomyCardDefinition(
        plannedSlot: PlannedRevealSlot,
        extensionPlan: ExtensionDrawPlan,
        excludedCardIds: Set<String>,
        maxAllowedRarityLabel: String?,
    ) = pickWeighted(
        findUniqueAstronomyCandidatePool(
            plannedSlot = plannedSlot,
            extensionPlan = extensionPlan,
            excludedCardIds = excludedCardIds,
            maxAllowedRarityLabel = maxAllowedRarityLabel,
        ),
    ) { it.weight }.card

    private fun findUniqueAstronomyCandidatePool(
        plannedSlot: PlannedRevealSlot,
        extensionPlan: ExtensionDrawPlan,
        excludedCardIds: Set<String>,
        maxAllowedRarityLabel: String?,
    ): List<WeightedCardDefinition> {
        val targetRarityPool = checkNotNull(extensionPlan.cardsByRarity[plannedSlot.rarityLabel]) {
            "Aucune carte n'a ete configuree pour la rarete '${plannedSlot.rarityLabel}'."
        }.filterNot { candidate -> candidate.card.id in excludedCardIds }
        if (targetRarityPool.isNotEmpty()) {
            return targetRarityPool
        }

        val targetRarityPriority = raritySortPriority(plannedSlot.rarityLabel)
        val fallbackPool = extensionPlan.cardsByRarity.keys
            .asSequence()
            .filterNot { rarity -> rarity == plannedSlot.rarityLabel }
            .filter { rarity ->
                maxAllowedRarityLabel == null ||
                    raritySortPriority(rarity) <= raritySortPriority(maxAllowedRarityLabel)
            }
            .sortedWith(
                compareBy<String>(
                    { rarity -> abs(raritySortPriority(rarity) - targetRarityPriority) },
                    { rarity -> -extensionPlan.rarityProbabilities.getValue(rarity) },
                    ::raritySortPriority,
                ),
            )
            .mapNotNull { rarity ->
                extensionPlan.cardsByRarity
                    .getValue(rarity)
                    .filterNot { candidate -> candidate.card.id in excludedCardIds }
                    .takeIf { candidates -> candidates.isNotEmpty() }
            }
            .firstOrNull()

        return requireNotNull(fallbackPool) {
            "Le tirage requiert plus de cartes uniques que l'extension n'en propose."
        }
    }

    private fun <T> pickWeighted(entries: List<T>, weightOf: (T) -> Int): T {
        val totalWeight = entries.sumOf(weightOf)
        require(totalWeight > 0) { "Les poids doivent etre strictement positifs." }
        var cursor = settings.entropySource.nextInt(totalWeight)
        for (entry in entries) {
            cursor -= weightOf(entry)
            if (cursor < 0) {
                return entry
            }
        }
        return entries.last()
    }

    private fun pickWeightedCode(options: List<WeightedCode>): String =
        pickWeighted(options) { it.weight }.code

    private fun maybePromoteRarity(
        baseRarity: String,
        availableRarities: Set<String>,
        rarityBoostPercent: Double,
    ): String {
        if (rarityBoostPercent <= 0.0 || baseRarity == "Epic") {
            return baseRarity
        }
        if (!rollPercent(rarityBoostPercent)) {
            return baseRarity
        }
        return nextHigherConfiguredRarity(
            currentRarity = baseRarity,
            availableRarities = availableRarities,
        )
    }

    private fun clampAstronomyRarity(
        rarityLabel: String,
        availableRarities: Set<String>,
        maxAllowedRarityLabel: String?,
    ): String {
        if (maxAllowedRarityLabel == null) {
            return rarityLabel
        }
        if (raritySortPriority(rarityLabel) <= raritySortPriority(maxAllowedRarityLabel)) {
            return rarityLabel
        }
        return availableRarities
            .filter { rarity -> raritySortPriority(rarity) <= raritySortPriority(maxAllowedRarityLabel) }
            .maxByOrNull(::raritySortPriority)
            ?: availableRarities.minByOrNull(::raritySortPriority)
            ?: rarityLabel
    }

    private fun maybeDrawEquipmentReward(
        rarityLabel: String,
        equipmentCards: List<EquipmentCardDefinition>,
        replacementChancePercent: Double,
        excludedEquipmentDefinitionIds: Set<String>,
    ): EquipmentCardDefinition? {
        if (rarityLabel != "Common") {
            return null
        }
        val weightedEquipmentCards = equipmentCards.filter { card ->
            card.dropWeight > 0 && card.id !in excludedEquipmentDefinitionIds
        }
        if (weightedEquipmentCards.isEmpty()) {
            return null
        }
        if (!rollPercent(replacementChancePercent)) {
            return null
        }
        return pickWeighted(weightedEquipmentCards) { it.dropWeight }
    }

    private fun pickSkyQualityWithEquipmentBonus(
        variantProfile: fr.aumombelli.dstcg.model.VariantProfile,
        runtimeVariantWeights: RuntimeVariantWeights,
        holographicQualityPercent: Double,
    ): fr.aumombelli.dstcg.model.SkyQualityDefinition {
        if (holographicQualityPercent <= 0.0) {
            return variantProfile.requireSkyQualityDefinition(
                pickWeightedCode(runtimeVariantWeights.skyQualityWeights),
            )
        }

        val skyWeightsByCode = runtimeVariantWeights.skyQualityWeights.associateBy { it.code }
        val holographicEntries = variantProfile.skyQualities
            .filter { it.isHolographic }
            .mapNotNull { skyQuality -> skyWeightsByCode[skyQuality.code] }
        val nonHolographicEntries = variantProfile.skyQualities
            .filterNot { it.isHolographic }
            .mapNotNull { skyQuality -> skyWeightsByCode[skyQuality.code] }

        if (holographicEntries.isEmpty() || nonHolographicEntries.isEmpty()) {
            return variantProfile.requireSkyQualityDefinition(
                pickWeightedCode(runtimeVariantWeights.skyQualityWeights),
            )
        }

        val totalWeight = runtimeVariantWeights.skyQualityWeights.sumOf { it.weight }.coerceAtLeast(1)
        val baseHolographicProbability = holographicEntries.sumOf { it.weight }.toDouble() / totalWeight.toDouble()
        val finalHolographicProbability = (baseHolographicProbability + holographicQualityPercent / 100.0)
            .coerceIn(0.0, 1.0)
        val selectedPool = if (rollProbability(finalHolographicProbability)) {
            holographicEntries
        } else {
            nonHolographicEntries
        }
        return variantProfile.requireSkyQualityDefinition(
            pickWeighted(selectedPool) { it.weight }.code,
        )
    }

    private fun rollPercent(percent: Double): Boolean = rollProbability(
        probability = (percent / 100.0).coerceIn(0.0, 1.0),
    )

    private fun rollProbability(probability: Double): Boolean {
        if (probability <= 0.0) return false
        if (probability >= 1.0) return true
        val scale = 1_000_000
        val threshold = (probability * scale).toInt().coerceIn(1, scale)
        return settings.entropySource.nextInt(scale) < threshold
    }

    private data class PlannedRevealSlot(
        val slotIndex: Int,
        val rarityLabel: String,
    )

    private data class ForcedEquipmentReward(
        val slotIndex: Int,
        val definition: EquipmentCardDefinition,
    )

    private sealed interface EquipmentDrawPolicy {
        data object Disabled : EquipmentDrawPolicy

        data class Standard(
            val replacementChancePercent: Double,
        ) : EquipmentDrawPolicy

        data class ForceSingleLevelOne(
            val candidates: List<EquipmentCardDefinition>,
        ) : EquipmentDrawPolicy
    }

    private companion object {
        val FIRST_PACK_ONBOARDING_STEPS = setOf(
            NewPlayerOnboardingStep.OpenFirstPackMenu,
            NewPlayerOnboardingStep.SelectFirstExtension,
            NewPlayerOnboardingStep.SelectFirstBooster,
        )
    }
}
