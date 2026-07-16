package fr.aumombelli.dstcg.data

import fr.aumombelli.dstcg.model.CardDefinition
import fr.aumombelli.dstcg.model.EquipmentCardDefinition
import fr.aumombelli.dstcg.model.StandaloneProgress
import fr.aumombelli.dstcg.model.VariantProfile

class ProgressRestoreValidationException(message: String) : IllegalArgumentException(message)

internal class ProgressRestoreValidator(
    private val catalogRepository: CatalogGateway,
) {
    suspend fun validate(progress: StandaloneProgress) {
        val cardsById = catalogRepository.loadCards().associateBy(CardDefinition::id)
        val profilesById = catalogRepository.loadVariantProfiles().associateBy(VariantProfile::id)
        val extensionIds = catalogRepository.loadExtensions().mapTo(mutableSetOf()) { it.id }
        val equipmentById = catalogRepository.loadEquipmentCards().associateBy(EquipmentCardDefinition::id)

        progress.collection.cards.forEach { (cardId, entry) ->
            val card = cardsById[cardId]
                ?: invalidReference("carte", cardId)
            if (card.extensionId !in extensionIds) {
                invalidReference("extension", card.extensionId)
            }
            val profile = profilesById[card.variantProfileId]
                ?: invalidReference("profil de variante", card.variantProfileId)
            entry.variants.forEach { variant ->
                if (profile.skyQualities.none { it.code == variant.skyQuality }) {
                    invalidReference("qualité de ciel", variant.skyQuality)
                }
                if (profile.finishes.none { it.code == variant.finish }) {
                    invalidReference("finition", variant.finish)
                }
            }
        }

        progress.equipmentInventory.cards.keys.forEach { cardId ->
            if (cardId !in equipmentById) invalidReference("équipement", cardId)
        }
        progress.activeEquipmentByType.forEach { (type, effect) ->
            val equipment = equipmentById[effect.equipmentCardId]
                ?: invalidReference("équipement actif", effect.equipmentCardId)
            if (equipment.type != type || effect.equipmentType != type) {
                throw ProgressRestoreValidationException(
                    "Le type de l'équipement actif ${effect.equipmentCardId} est incohérent.",
                )
            }
        }
        progress.lastActivatedCardIdByType.forEach { (type, cardId) ->
            val equipment = equipmentById[cardId]
                ?: invalidReference("dernier équipement activé", cardId)
            if (equipment.type != type) {
                throw ProgressRestoreValidationException(
                    "Le type du dernier équipement activé $cardId est incohérent.",
                )
            }
        }

        progress.miniGamesProgress.dailyStates.values
            .flatMap { it.resolvedCards }
            .forEach { resolved ->
                validateMiniGameCard(
                    label = "carte globale de mini-jeu",
                    cardId = resolved.globalCard.cardId,
                    extensionId = resolved.globalCard.extensionId,
                    cardsById = cardsById,
                    extensionIds = extensionIds,
                )
                validateMiniGameCard(
                    label = "carte possédée de mini-jeu",
                    cardId = resolved.ownedVariant.cardId,
                    extensionId = resolved.ownedVariant.extensionId,
                    cardsById = cardsById,
                    extensionIds = extensionIds,
                )
                val ownedCard = cardsById.getValue(resolved.ownedVariant.cardId)
                val profile = profilesById[ownedCard.variantProfileId]
                    ?: invalidReference("profil de variante", ownedCard.variantProfileId)
                if (profile.skyQualities.none { it.code == resolved.ownedVariant.skyQuality }) {
                    invalidReference("qualité de ciel", resolved.ownedVariant.skyQuality)
                }
                if (profile.finishes.none { it.code == resolved.ownedVariant.finish }) {
                    invalidReference("finition", resolved.ownedVariant.finish)
                }
            }
    }

    private fun validateMiniGameCard(
        label: String,
        cardId: String,
        extensionId: String,
        cardsById: Map<String, CardDefinition>,
        extensionIds: Set<String>,
    ) {
        val card = cardsById[cardId] ?: invalidReference(label, cardId)
        if (extensionId !in extensionIds || card.extensionId != extensionId) {
            invalidReference("extension de mini-jeu", extensionId)
        }
    }

    private fun invalidReference(label: String, value: String): Nothing =
        throw ProgressRestoreValidationException(
            "La sauvegarde contient une référence inconnue ($label : $value).",
        )
}
