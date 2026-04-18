package fr.aumombelli.dstcg.feature.packs.opening

import fr.aumombelli.dstcg.data.CatalogGateway
import fr.aumombelli.dstcg.model.AstronomyPackRevealSlot
import fr.aumombelli.dstcg.model.DisplayCard
import fr.aumombelli.dstcg.model.DrawPackResponse
import fr.aumombelli.dstcg.model.EquipmentPackRevealSlot
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

    val revealItems = packResult.revealSlots.map { slot ->
        when (slot) {
            is AstronomyPackRevealSlot -> {
                val definition = checkNotNull(cardsById[slot.card.cardId]) {
                    "Carte inconnue '${slot.card.cardId}' dans le catalogue courant."
                }
                AstroPackRevealUiItem(
                    displayCard = definition.toDisplayCard(
                        extensionName = extensionName,
                        activeVariant = slot.card.variant.toDisplayVariant(),
                    ),
                    showFirstEncounterIndicator = slot.isFirstEncounter,
                )
            }

            is EquipmentPackRevealSlot -> {
                EquipmentPackRevealUiItem(definition = slot.definition)
            }
        }
    }
    val displayCards = revealItems
        .filterIsInstance<AstroPackRevealUiItem>()
        .map { it.displayCard }

    val summary = summarizePackOpening(displayCards)

    return PackOpeningUiState(
        packResult = packResult,
        revealItems = revealItems,
        displayCards = displayCards,
        highestBurstRarity = summary?.highestRarityLabel,
        hasHolographicBurst = summary?.hasHolographicCard == true,
    )
}

internal fun packOpeningExtensionLabel(
    displayCards: List<DisplayCard>,
    packResult: DrawPackResponse,
): String = displayCards.firstOrNull()?.extensionName ?: packResult.extensionId
