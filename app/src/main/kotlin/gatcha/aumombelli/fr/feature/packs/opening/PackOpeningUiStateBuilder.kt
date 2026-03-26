package fr.aumombelli.gatcha.feature.packs.opening

import fr.aumombelli.gatcha.data.CatalogGateway
import fr.aumombelli.gatcha.model.DisplayCard
import fr.aumombelli.gatcha.model.DrawPackResponse
import fr.aumombelli.gatcha.model.toDisplayCard
import fr.aumombelli.gatcha.model.toDisplayVariant
import fr.aumombelli.gatcha.ui.motion.summarizePackOpening

internal suspend fun buildPackOpeningUiState(
    catalogRepository: CatalogGateway,
    packResult: DrawPackResponse,
): PackOpeningUiState {
    val cardsById = catalogRepository.loadCards().associateBy { it.id }
    val extensionName = catalogRepository.loadExtensions()
        .firstOrNull { it.id == packResult.extensionId }
        ?.name
        ?: packResult.extensionId

    val displayCards = packResult.cards.map { card ->
        val definition = checkNotNull(cardsById[card.cardId]) {
            "Unknown card '${card.cardId}' for the current catalog."
        }
        definition.toDisplayCard(
            extensionName = extensionName,
            activeVariant = card.variant.toDisplayVariant(),
        )
    }

    val summary = summarizePackOpening(displayCards)

    return PackOpeningUiState(
        packResult = packResult,
        displayCards = displayCards,
        highestBurstRarity = summary?.highestRarityLabel,
        hasHolographicBurst = summary?.hasHolographicCard == true,
    )
}

internal fun packOpeningExtensionLabel(
    displayCards: List<DisplayCard>,
    packResult: DrawPackResponse,
): String = displayCards.firstOrNull()?.extensionName ?: packResult.extensionId
