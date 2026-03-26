package fr.aumombelli.gatcha.data

import fr.aumombelli.gatcha.model.CardVariant
import fr.aumombelli.gatcha.model.DrawPackResponse
import fr.aumombelli.gatcha.model.PackCard
import fr.aumombelli.gatcha.model.WeightedCode
import fr.aumombelli.gatcha.model.requireFinishDefinition
import fr.aumombelli.gatcha.model.requireSkyQualityDefinition
import java.time.Clock
import java.time.Duration
import java.time.Instant
import kotlin.random.Random

class PackCooldownException(
    val retryAt: String,
) : Exception("A new pack is not available yet.")

data class StandaloneGameSettings(
    val cardsPerPack: Int = 5,
    val drawCooldown: Duration = Duration.ofHours(12),
    val clock: Clock = Clock.systemUTC(),
    val random: Random = Random.Default,
)

class LocalPackEngine(
    private val catalogRepository: CatalogGateway,
    private val settings: StandaloneGameSettings,
) {
    suspend fun drawPack(extensionId: String, nextDrawAt: String?): DrawPackResponse {
        val now = settings.clock.instant()
        val retryAt = nextDrawAt
            ?.let { runCatching { Instant.parse(it) }.getOrNull() }
            ?.takeIf { now.isBefore(it) }
        if (retryAt != null) {
            throw PackCooldownException(retryAt.toString())
        }

        val cards = catalogRepository.loadCards().filter { it.extensionId == extensionId }
        if (cards.isEmpty()) {
            throw IllegalStateException("No cards were found for this extension.")
        }
        val variantProfilesById = catalogRepository.loadVariantProfiles().associateBy { it.id }
        val drawnAt = now.toString()
        val nextDrawAtValue = now.plus(settings.drawCooldown).toString()
        val drawnCards = List(settings.cardsPerPack) {
            val definition = pickWeighted(cards)
            val variantProfile = checkNotNull(variantProfilesById[definition.variantProfileId]) {
                "Unknown variant profile '${definition.variantProfileId}' for card '${definition.id}'."
            }
            val skyQuality = variantProfile.requireSkyQualityDefinition(
                pickWeightedCode(variantProfile.skyQualityWeights),
            )
            val finish = variantProfile.requireFinishDefinition(
                pickWeightedCode(variantProfile.finishWeights),
            )
            PackCard(
                cardId = definition.id,
                name = definition.name,
                rarityLabel = definition.rarityLabel,
                imageRef = definition.imageRef,
                variant = CardVariant(
                    skyQuality = skyQuality.code,
                    skyQualityLabel = skyQuality.label,
                    finish = finish.code,
                    finishLabel = finish.label,
                    isHolographic = finish.isHolographic,
                ),
            )
        }

        return DrawPackResponse(
            extensionId = extensionId,
            drawnAt = drawnAt,
            nextDrawAt = nextDrawAtValue,
            cards = drawnCards,
        )
    }

    private fun <T> pickWeighted(entries: List<T>, weightOf: (T) -> Int): T {
        val totalWeight = entries.sumOf(weightOf)
        require(totalWeight > 0) { "Weights must be strictly positive." }
        var cursor = settings.random.nextInt(totalWeight)
        for (entry in entries) {
            cursor -= weightOf(entry)
            if (cursor < 0) {
                return entry
            }
        }
        return entries.last()
    }

    private fun pickWeighted(cards: List<fr.aumombelli.gatcha.model.CardDefinition>) =
        pickWeighted(cards) { it.drawWeight }

    private fun pickWeightedCode(options: List<WeightedCode>): String =
        pickWeighted(options) { it.weight }.code
}
