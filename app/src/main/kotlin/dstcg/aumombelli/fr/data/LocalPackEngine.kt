package fr.aumombelli.dstcg.data

import android.content.Context
import fr.aumombelli.dstcg.model.CardVariant
import fr.aumombelli.dstcg.model.DrawPackResponse
import fr.aumombelli.dstcg.model.PackCard
import fr.aumombelli.dstcg.model.PackRechargeState
import fr.aumombelli.dstcg.model.WeightedCode
import fr.aumombelli.dstcg.model.requireFinishDefinition
import fr.aumombelli.dstcg.model.requireSkyQualityDefinition
import java.time.Clock
import java.time.Duration
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

    suspend fun drawPack(
        extensionId: String,
        rechargeState: PackRechargeState,
        now: Instant,
    ): DrawPackResponse {
        val cards = catalogRepository.loadCards()
        val variantProfiles = catalogRepository.loadVariantProfiles()
        val runtimeCatalog = runtimeCalculator.resolve(
            cards = cards,
            variantProfiles = variantProfiles,
            gameBalance = catalogRepository.loadGameBalance(),
        )
        val drawConfig = runtimeCatalog.drawConfig
        val normalizedChargeState = normalizePackRechargeState(
            rechargeState = rechargeState,
            now = now,
            drawCooldown = drawConfig.drawCooldown,
            maxStoredDraws = settings.maxStoredDraws,
            weatherPolicy = settings.weatherPolicy,
        )
        if (normalizedChargeState.availableDrawCount == 0) {
            val retryAt = checkNotNull(
                normalizedChargeState.derivedNextChargeAt(
                    now = now,
                    drawCooldown = drawConfig.drawCooldown,
                    maxStoredDraws = settings.maxStoredDraws,
                    weatherPolicy = settings.weatherPolicy,
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
        val drawnAt = now.toString()
        val drawnCards = List(drawConfig.cardsPerDraw) {
            val drawnRarity = pickWeighted(extensionPlan.rarityWeights) { it.weight }.code
            val weightedCards = checkNotNull(extensionPlan.cardsByRarity[drawnRarity]) {
                "Aucune carte n'a ete configuree pour la rarete '$drawnRarity' dans '$extensionId'."
            }
            val definition = pickWeighted(weightedCards) { it.weight }.card
            val variantProfile = checkNotNull(variantProfilesById[definition.variantProfileId]) {
                "Profil de variante inconnu '${definition.variantProfileId}' pour la carte '${definition.id}'."
            }
            val runtimeVariantWeights = checkNotNull(runtimeCatalog.variantWeightsByProfileId[definition.variantProfileId]) {
                "Poids de variante introuvables pour '${definition.variantProfileId}'."
            }
            val skyQuality = variantProfile.requireSkyQualityDefinition(
                pickWeightedCode(runtimeVariantWeights.skyQualityWeights),
            )
            val finish = variantProfile.requireFinishDefinition(
                pickWeightedCode(runtimeVariantWeights.finishWeights),
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
            rechargeState = updatedChargeState,
            cards = drawnCards,
        )
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
}
