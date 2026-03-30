package fr.aumombelli.gatcha.data

import android.content.Context
import fr.aumombelli.gatcha.model.CardVariant
import fr.aumombelli.gatcha.model.DrawPackResponse
import fr.aumombelli.gatcha.model.PackCard
import fr.aumombelli.gatcha.model.WeightedCode
import fr.aumombelli.gatcha.model.requireFinishDefinition
import fr.aumombelli.gatcha.model.requireSkyQualityDefinition
import java.time.Clock
import java.time.Duration
import java.time.Instant

class PackCooldownException(
    val retryAt: String,
) : Exception("A new pack is not available yet.")

data class StandaloneGameSettings(
    val cardsPerPack: Int = 5,
    val drawCooldown: Duration = DEFAULT_DRAW_COOLDOWN,
    val maxStoredDraws: Int = DEFAULT_MAX_STORED_DRAWS,
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
    suspend fun drawPack(
        extensionId: String,
        availableDrawCount: Int,
        nextChargeAt: String?,
        now: Instant,
    ): DrawPackResponse {
        val normalizedChargeState = normalizePackChargeState(
            availableDrawCount = availableDrawCount,
            nextChargeAt = nextChargeAt,
            now = now,
            drawCooldown = settings.drawCooldown,
            maxStoredDraws = settings.maxStoredDraws,
        )
        if (normalizedChargeState.availableDrawCount == 0) {
            val retryAt = checkNotNull(normalizedChargeState.nextChargeAt) {
                "A locked pack draw must expose a next charge time."
            }
            throw PackCooldownException(retryAt.toString())
        }
        val updatedChargeState = consumePackCharge(
            normalizedState = normalizedChargeState,
            now = now,
            drawCooldown = settings.drawCooldown,
            maxStoredDraws = settings.maxStoredDraws,
        )

        val cards = catalogRepository.loadCards().filter { it.extensionId == extensionId }
        if (cards.isEmpty()) {
            throw IllegalStateException("No cards were found for this extension.")
        }
        val variantProfilesById = catalogRepository.loadVariantProfiles().associateBy { it.id }
        val drawnAt = now.toString()
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
            availableDrawCount = updatedChargeState.availableDrawCount,
            nextChargeAt = updatedChargeState.nextChargeAt?.toString(),
            cards = drawnCards,
        )
    }

    private fun <T> pickWeighted(entries: List<T>, weightOf: (T) -> Int): T {
        val totalWeight = entries.sumOf(weightOf)
        require(totalWeight > 0) { "Weights must be strictly positive." }
        var cursor = settings.entropySource.nextInt(totalWeight)
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
