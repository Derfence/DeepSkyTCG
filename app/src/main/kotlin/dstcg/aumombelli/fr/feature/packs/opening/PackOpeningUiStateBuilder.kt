package fr.aumombelli.dstcg.feature.packs.opening

import fr.aumombelli.dstcg.data.CatalogGateway
import fr.aumombelli.dstcg.model.DisplayCard
import fr.aumombelli.dstcg.model.DrawPackResponse
import fr.aumombelli.dstcg.model.toDisplayCard
import fr.aumombelli.dstcg.model.toDisplayVariant
import fr.aumombelli.dstcg.ui.motion.summarizePackOpening

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
            "Carte inconnue '${card.cardId}' dans le catalogue courant."
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
