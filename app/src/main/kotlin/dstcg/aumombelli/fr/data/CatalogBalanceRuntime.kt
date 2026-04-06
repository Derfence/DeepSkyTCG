package fr.aumombelli.dstcg.data

import fr.aumombelli.dstcg.model.CardDefinition
import fr.aumombelli.dstcg.model.GameBalanceDefinition
import fr.aumombelli.dstcg.model.VariantProfile
import fr.aumombelli.dstcg.model.WeightedCode
import fr.aumombelli.dstcg.model.raritySortPriority
import java.time.Duration
import kotlin.math.abs
import kotlin.math.roundToInt

internal data class CatalogDrawConfig(
    val cardsPerDraw: Int,
    val drawCooldown: Duration,
)

internal data class RuntimeVariantWeights(
    val skyQualityWeights: List<WeightedCode>,
    val finishWeights: List<WeightedCode>,
)

internal data class WeightedCardDefinition(
    val card: CardDefinition,
    val weight: Int,
)

internal data class ExtensionDrawPlan(
    val rarityWeights: List<WeightedCode>,
    val cardsByRarity: Map<String, List<WeightedCardDefinition>>,
    val rarityProbabilities: Map<String, Double>,
    val cardProbabilities: Map<String, Double>,
)

internal data class RuntimeCatalogBalance(
    val drawConfig: CatalogDrawConfig,
    val variantWeightsByProfileId: Map<String, RuntimeVariantWeights>,
    val extensionPlansById: Map<String, ExtensionDrawPlan>,
)

internal class CatalogBalanceRuntimeCalculator {
    fun resolve(
        cards: List<CardDefinition>,
        variantProfiles: List<VariantProfile>,
        gameBalance: GameBalanceDefinition,
    ): RuntimeCatalogBalance {
        val validatedBalance = gameBalance.validated()
        return RuntimeCatalogBalance(
            drawConfig = CatalogDrawConfig(
                cardsPerDraw = validatedBalance.cardsPerDraw,
                drawCooldown = validatedBalance.drawCooldownDuration(),
            ),
            variantWeightsByProfileId = variantProfiles.associate { profile ->
                profile.id to RuntimeVariantWeights(
                    skyQualityWeights = weightedCodesFromProbabilities(
                        probabilities = validatedBalance.skyQualityProbabilities(),
                        orderedCodes = profile.skyQualities.map { it.code },
                    ),
                    finishWeights = weightedCodesFromProbabilities(
                        probabilities = validatedBalance.finishProbabilities(),
                        orderedCodes = profile.finishes.map { it.code },
                    ),
                )
            },
            extensionPlansById = buildExtensionPlans(
                cards = cards,
                gameBalance = validatedBalance,
            ),
        )
    }

    private fun buildExtensionPlans(
        cards: List<CardDefinition>,
        gameBalance: GameBalanceDefinition,
    ): Map<String, ExtensionDrawPlan> {
        val baseRarityProbabilities = gameBalance.rarityProbabilities()
        return cards.groupBy { it.extensionId }.mapValues { (_, extensionCards) ->
            val cardsByRarity = extensionCards.groupBy { card ->
                require(baseRarityProbabilities.containsKey(card.rarityLabel)) {
                    "Unsupported rarity '${card.rarityLabel}'."
                }
                require(card.cardRarityMultiplier > 0.0) {
                    "cardRarityMultiplier must be strictly positive for '${card.id}'."
                }
                card.rarityLabel
            }
            val presentRarities = cardsByRarity.keys.sortedBy(::raritySortPriority)
            val rarityProbabilitySum = presentRarities.sumOf { baseRarityProbabilities.getValue(it) }
            val extensionRarityProbabilities = presentRarities.associateWith { rarity ->
                baseRarityProbabilities.getValue(rarity) / rarityProbabilitySum
            }
            val rarityWeights = weightedCodesFromProbabilities(
                probabilities = extensionRarityProbabilities,
                orderedCodes = presentRarities,
            )
            val weightedCardsByRarity = presentRarities.associateWith { rarity ->
                val rarityCards = cardsByRarity.getValue(rarity)
                val totalMultiplier = rarityCards.sumOf { it.cardRarityMultiplier }
                val conditionalProbabilities = rarityCards.associate { card ->
                    card.id to (card.cardRarityMultiplier / totalMultiplier)
                }
                val cardWeights = weightedCodesFromProbabilities(
                    probabilities = conditionalProbabilities,
                    orderedCodes = rarityCards.map { it.id },
                )
                rarityCards.zip(cardWeights).map { (card, weight) ->
                    WeightedCardDefinition(card = card, weight = weight.weight)
                }
            }
            val cardProbabilities = presentRarities.flatMap { rarity ->
                val rarityProbability = extensionRarityProbabilities.getValue(rarity)
                val rarityCards = cardsByRarity.getValue(rarity)
                val totalMultiplier = rarityCards.sumOf { it.cardRarityMultiplier }
                rarityCards.map { card ->
                    card.id to (rarityProbability * (card.cardRarityMultiplier / totalMultiplier))
                }
            }.toMap()

            ExtensionDrawPlan(
                rarityWeights = rarityWeights,
                cardsByRarity = weightedCardsByRarity,
                rarityProbabilities = extensionRarityProbabilities,
                cardProbabilities = cardProbabilities,
            )
        }
    }

    private fun weightedCodesFromProbabilities(
        probabilities: Map<String, Double>,
        orderedCodes: List<String>,
    ): List<WeightedCode> {
        require(orderedCodes.isNotEmpty()) { "A weighted draw needs at least one entry." }
        val weights = orderedCodes.map { code ->
            val probability = probabilities[code]
            require(probability != null && probability > 0.0) {
                "Probability must be strictly positive for '$code'."
            }
            maxOf(1, (probability * WEIGHT_SCALE).roundToInt())
        }
        val reducedWeights = reduceWeights(weights)
        return orderedCodes.zip(reducedWeights).map { (code, weight) ->
            WeightedCode(code = code, weight = weight)
        }
    }

    private fun reduceWeights(weights: List<Int>): List<Int> {
        val divisor = weights.fold(0, ::greatestCommonDivisor).coerceAtLeast(1)
        return weights.map { weight -> weight / divisor }
    }

    private fun greatestCommonDivisor(left: Int, right: Int): Int {
        var a = abs(left)
        var b = abs(right)
        while (b != 0) {
            val remainder = a % b
            a = b
            b = remainder
        }
        return a
    }

    private companion object {
        private const val WEIGHT_SCALE = 1_000_000.0
    }
}

internal fun GameBalanceDefinition.drawCooldownDuration(): Duration {
    val totalMillis = (drawCooldownHours * 3_600_000.0).roundToInt().toLong()
    require(totalMillis > 0L) { "drawCooldownHours must stay strictly positive." }
    return Duration.ofMillis(totalMillis)
}

internal fun GameBalanceDefinition.validated(): GameBalanceDefinition {
    require(cardsPerDraw > 0) { "cardsPerDraw must be strictly positive." }
    require(drawCooldownHours > 0.0) { "drawCooldownHours must be strictly positive." }
    require(percentUncommonPerDay > 0.0) { "percentUncommonPerDay must be strictly positive." }
    require(percentRarePerDay > 0.0) { "percentRarePerDay must be strictly positive." }
    require(percentEpicPerDay > 0.0) { "percentEpicPerDay must be strictly positive." }
    require(suburbanMeanPerDay > 0.0) { "suburbanMeanPerDay must be strictly positive." }
    require(ruralMeanPerDay > 0.0) { "ruralMeanPerDay must be strictly positive." }
    require(mountainMeanPerDay > 0.0) { "mountainMeanPerDay must be strictly positive." }
    require(percentHoloMeanPerDay > 0.0) { "percentHoloMeanPerDay must be strictly positive." }
    require(commonRarityProbability() > 0.0) { "Derived common probability must stay strictly positive." }
    require(cityProbability() > 0.0) { "Derived city probability must stay strictly positive." }
    require(standardProbability() > 0.0) { "Derived standard probability must stay strictly positive." }
    return this
}

internal fun GameBalanceDefinition.cardsPerDay(): Double = (cardsPerDraw * 24.0) / drawCooldownHours

internal fun GameBalanceDefinition.commonRarityProbability(): Double =
    1.0 - percentUncommonPerDay / 100.0 - percentRarePerDay / 100.0 - percentEpicPerDay / 100.0

internal fun GameBalanceDefinition.cityProbability(): Double =
    1.0 - (suburbanMeanPerDay + ruralMeanPerDay + mountainMeanPerDay) / cardsPerDay()

internal fun GameBalanceDefinition.standardProbability(): Double = 1.0 - percentHoloMeanPerDay / 100.0

internal fun GameBalanceDefinition.rarityProbabilities(): Map<String, Double> = mapOf(
    "Common" to commonRarityProbability(),
    "Uncommon" to percentUncommonPerDay / 100.0,
    "Rare" to percentRarePerDay / 100.0,
    "Epic" to percentEpicPerDay / 100.0,
)

internal fun GameBalanceDefinition.skyQualityProbabilities(): Map<String, Double> = mapOf(
    "city" to cityProbability(),
    "suburban" to suburbanMeanPerDay / cardsPerDay(),
    "rural" to ruralMeanPerDay / cardsPerDay(),
    "mountain" to mountainMeanPerDay / cardsPerDay(),
)

internal fun GameBalanceDefinition.finishProbabilities(): Map<String, Double> = mapOf(
    "standard" to standardProbability(),
    "holographic" to percentHoloMeanPerDay / 100.0,
)
