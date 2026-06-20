package fr.aumombelli.dstcg.data

import fr.aumombelli.dstcg.model.CardDefinition
import fr.aumombelli.dstcg.model.DisplayCard
import fr.aumombelli.dstcg.model.ExtensionDefinition
import fr.aumombelli.dstcg.model.OwnedCollection
import fr.aumombelli.dstcg.model.TradeCardCandidate
import fr.aumombelli.dstcg.model.TradeCardRef
import fr.aumombelli.dstcg.model.TradeValidationResult
import fr.aumombelli.dstcg.model.VariantProfile
import fr.aumombelli.dstcg.model.applyTrade
import fr.aumombelli.dstcg.model.canTradeAway
import fr.aumombelli.dstcg.model.raritySortPriority
import fr.aumombelli.dstcg.model.toDisplayCard
import fr.aumombelli.dstcg.model.toDisplayVariant
import fr.aumombelli.dstcg.model.tradeCountFor
import fr.aumombelli.dstcg.model.validateTradePair
import java.security.MessageDigest

class TradeRepository(
    private val catalogRepository: CatalogGateway,
    private val progressRepository: ProgressGateway,
) : TradeGateway {
    override suspend fun loadTradeCandidates(): List<TradeCardCandidate> {
        val extensionsById = catalogRepository.loadExtensions().associateBy(ExtensionDefinition::id)
        val cardsById = catalogRepository.loadCards().associateBy(CardDefinition::id)
        val variantProfilesById = catalogRepository.loadVariantProfiles().associateBy(VariantProfile::id)
        val progress = progressRepository.loadProgress().requireUsableProgress().progress

        return buildTradeCandidates(
            collection = progress.collection,
            cardsById = cardsById,
            extensionsById = extensionsById,
            variantProfilesById = variantProfilesById,
        )
    }

    override suspend fun loadTradeCard(ref: TradeCardRef): DisplayCard? {
        val extensionsById = catalogRepository.loadExtensions().associateBy(ExtensionDefinition::id)
        val cardsById = catalogRepository.loadCards().associateBy(CardDefinition::id)
        val variantProfilesById = catalogRepository.loadVariantProfiles().associateBy(VariantProfile::id)
        val progress = progressRepository.loadProgress().requireUsableProgress().progress
        val card = cardsById[ref.cardId] ?: return null
        val extension = extensionsById[card.extensionId] ?: return null
        val variantProfile = variantProfilesById[card.variantProfileId] ?: return null
        val variant = runCatching {
            variantProfile.toDisplayVariant(
                skyQuality = ref.skyQuality,
                finish = ref.finish,
                count = progress.collection.tradeCountFor(ref),
            )
        }.getOrNull() ?: return null
        return card.toDisplayCard(
            extensionName = extension.name,
            activeVariant = variant,
            availableVariants = listOf(variant),
        )
    }

    override suspend fun catalogFingerprint(): String {
        val cards = catalogRepository.loadCards()
        val variantProfiles = catalogRepository.loadVariantProfiles()
        val payload = buildString {
            cards.sortedBy(CardDefinition::id).forEach { card ->
                append(card.id)
                append(':')
                append(card.rarityLabel)
                append(':')
                append(card.variantProfileId)
                append('|')
            }
            append('#')
            variantProfiles.sortedBy(VariantProfile::id).forEach { profile ->
                append(profile.id)
                append(':')
                append(profile.skyQualities.joinToString(",") { it.code })
                append(':')
                append(profile.finishes.joinToString(",") { it.code })
                append('|')
            }
        }
        return MessageDigest.getInstance("SHA-256")
            .digest(payload.encodeToByteArray())
            .joinToString(separator = "") { byte -> "%02x".format(byte) }
            .take(16)
    }

    override suspend fun validateTrade(
        localOutgoing: TradeCardRef,
        remoteOutgoing: TradeCardRef,
    ): TradeValidationResult {
        val cardsById = catalogRepository.loadCards().associateBy(CardDefinition::id)
        val variantProfilesById = catalogRepository.loadVariantProfiles().associateBy(VariantProfile::id)
        val progress = progressRepository.loadProgress().requireUsableProgress().progress
        return validateTradePair(
            localCollection = progress.collection,
            localOutgoing = localOutgoing,
            remoteOutgoing = remoteOutgoing,
            cardsById = cardsById,
            variantProfilesById = variantProfilesById,
        )
    }

    override suspend fun prepareTrade(
        tradeId: String,
        outgoing: TradeCardRef,
        incoming: TradeCardRef,
    ): TradeValidationResult {
        val cardsById = catalogRepository.loadCards().associateBy(CardDefinition::id)
        val variantProfilesById = catalogRepository.loadVariantProfiles().associateBy(VariantProfile::id)
        var result: TradeValidationResult = TradeValidationResult.Invalid("Échange non préparé.")

        progressRepository.updateProgress { progress ->
            when {
                progress.tradeLedgerState.hasCompleted(tradeId) -> {
                    result = TradeValidationResult.Valid
                    progress
                }

                progress.tradeLedgerState.pendingTrade == null ||
                    progress.tradeLedgerState.pendingTrade.tradeId == tradeId -> {
                    val validation = validateTradePair(
                        localCollection = progress.collection,
                        localOutgoing = outgoing,
                        remoteOutgoing = incoming,
                        cardsById = cardsById,
                        variantProfilesById = variantProfilesById,
                    )
                    if (validation is TradeValidationResult.Invalid) {
                        result = validation
                        progress
                    } else {
                        result = TradeValidationResult.Valid
                        progress.copy(
                            tradeLedgerState = progress.tradeLedgerState.beginPending(
                                tradeId = tradeId,
                                outgoing = outgoing,
                                incoming = incoming,
                            ),
                        )
                    }
                }

                else -> {
                    result = TradeValidationResult.Invalid("Un autre échange est déjà en attente.")
                    progress
                }
            }
        }

        return result
    }

    override suspend fun clearPreparedTrade(tradeId: String) {
        progressRepository.updateProgress { progress ->
            progress.copy(
                tradeLedgerState = progress.tradeLedgerState.clearPending(tradeId),
            )
        }
    }

    override suspend fun applyTrade(
        tradeId: String,
        outgoing: TradeCardRef,
        incoming: TradeCardRef,
    ): TradeValidationResult {
        val cardsById = catalogRepository.loadCards().associateBy(CardDefinition::id)
        val variantProfilesById = catalogRepository.loadVariantProfiles().associateBy(VariantProfile::id)
        var result: TradeValidationResult = TradeValidationResult.Invalid("Échange non appliqué.")

        progressRepository.updateProgress { progress ->
            if (progress.tradeLedgerState.hasCompleted(tradeId)) {
                result = TradeValidationResult.Valid
                progress
            } else {
                val validation = validateTradePair(
                    localCollection = progress.collection,
                    localOutgoing = outgoing,
                    remoteOutgoing = incoming,
                    cardsById = cardsById,
                    variantProfilesById = variantProfilesById,
                )
                if (validation is TradeValidationResult.Invalid) {
                    result = validation
                    progress
                } else {
                    result = TradeValidationResult.Valid
                    progress.copy(
                        collection = progress.collection.applyTrade(
                            outgoing = outgoing,
                            incoming = incoming,
                        ),
                        tradeLedgerState = progress.tradeLedgerState.markCompleted(tradeId),
                    )
                }
            }
        }

        return result
    }
}

private fun buildTradeCandidates(
    collection: OwnedCollection,
    cardsById: Map<String, CardDefinition>,
    extensionsById: Map<String, ExtensionDefinition>,
    variantProfilesById: Map<String, VariantProfile>,
): List<TradeCardCandidate> =
    collection.cards.flatMap { (cardId, entry) ->
        val card = cardsById[cardId] ?: return@flatMap emptyList()
        val extension = extensionsById[card.extensionId] ?: return@flatMap emptyList()
        val variantProfile = variantProfilesById[card.variantProfileId] ?: return@flatMap emptyList()
        entry.variants
            .filter { it.canTradeAway() }
            .map { variantCount ->
                TradeCardCandidate(
                    card = card,
                    extensionName = extension.name,
                    variant = variantProfile.toDisplayVariant(
                        skyQuality = variantCount.skyQuality,
                        finish = variantCount.finish,
                        count = variantCount.count,
                    ),
                )
            }
    }.sortedWith(
        compareBy<TradeCardCandidate> { raritySortPriority(it.card.rarityLabel) }
            .thenBy { it.card.name }
            .thenBy { it.variant.skyQualityLabel }
            .thenBy { it.variant.finishLabel },
    )
