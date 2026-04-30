package fr.aumombelli.dstcg.data

import android.content.Context
import fr.aumombelli.dstcg.model.DrawPackResponse
import fr.aumombelli.dstcg.model.EquipmentPackRevealSlot
import fr.aumombelli.dstcg.model.StandaloneProgress
import fr.aumombelli.dstcg.model.sortedRevealSlotsForPackReveal
import java.time.Clock
import java.time.Instant

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
    private val drawRandomizer = PackDrawRandomizer(settings.entropySource)

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
        val astronomyVariantDrawPolicy = resolveAstronomyVariantDrawPolicy(progress)
        val drawnAt = now.toString()
        val plannedRevealSlots = List(drawConfig.cardsPerDraw) { slotIndex ->
            val baseRarity = drawRandomizer.pickWeighted(extensionPlan.rarityWeights) { it.weight }.code
            PlannedRevealSlot(
                slotIndex = slotIndex,
                rarityLabel = drawRandomizer.clampAstronomyRarity(
                    rarityLabel = drawRandomizer.maybePromoteRarity(
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
            randomizer = drawRandomizer,
        )
        val usedAstronomyCardIds = mutableSetOf<String>()
        val usedEquipmentDefinitionIds = mutableSetOf<String>()
        var drawnHolographicCardCount = 0
        val revealSlots = plannedRevealSlots.map { plannedSlot ->
            if (forcedEquipmentReward?.slotIndex == plannedSlot.slotIndex) {
                usedEquipmentDefinitionIds += forcedEquipmentReward.definition.id
                EquipmentPackRevealSlot(
                    slotIndex = plannedSlot.slotIndex,
                    definition = forcedEquipmentReward.definition,
                )
            } else {
                val equipmentReward = when (equipmentDrawPolicy) {
                    is EquipmentDrawPolicy.Standard -> drawRandomizer.maybeDrawEquipmentReward(
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
                    drawRandomizer.drawAstronomyRevealSlot(
                        plannedSlot = plannedSlot,
                        extensionPlan = extensionPlan,
                        variantProfilesById = variantProfilesById,
                        runtimeCatalog = runtimeCatalog,
                        activeBonus = activeBonus,
                        excludedCardIds = usedAstronomyCardIds,
                        maxAllowedRarityLabel = astronomyRarityCap,
                        variantDrawPolicy = astronomyVariantDrawPolicy,
                        holographicCardsAlreadyDrawn = drawnHolographicCardCount,
                    ).also { astronomyRevealSlot ->
                        if (astronomyRevealSlot.card.variant.isHolographic) {
                            drawnHolographicCardCount += 1
                        }
                    }
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
}
