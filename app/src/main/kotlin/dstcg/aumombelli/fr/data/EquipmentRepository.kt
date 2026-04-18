package fr.aumombelli.dstcg.data

import fr.aumombelli.dstcg.model.ActiveEquipmentEffect
import fr.aumombelli.dstcg.model.EquipmentState
import fr.aumombelli.dstcg.model.consumeForActivation
import fr.aumombelli.dstcg.model.or
import fr.aumombelli.dstcg.model.recordEquipmentActivationSnapshot
import fr.aumombelli.dstcg.model.toEquipmentState

class EquipmentRepository(
    private val progressRepository: ProgressGateway,
    private val catalogRepository: CatalogGateway,
    private val homeMenuNoveltyEvaluator: HomeMenuNoveltyEvaluator,
) : EquipmentGateway {
    override suspend fun loadEquipmentState(): EquipmentState =
        progressRepository.loadProgress().requireUsableProgress().progress.toEquipmentState()

    override suspend fun activateEquipment(equipmentCardId: String): EquipmentState {
        val loadedProgress = progressRepository.loadProgress().requireUsableProgress()
        val progress = loadedProgress.progress
        val equipmentCards = catalogRepository.loadEquipmentCards()
        val definition = equipmentCards.firstOrNull { it.id == equipmentCardId }
            ?: throw IllegalStateException("Carte d'equipement inconnue '$equipmentCardId'.")

        if (progress.activeEquipmentByType.containsKey(definition.type)) {
            throw IllegalStateException(
                "Un ${definition.type.displayName.lowercase()} est deja actif.",
            )
        }

        val updatedInventory = progress.equipmentInventory.consumeForActivation(equipmentCardId)
        val updatedProgress = progress.copy(
            equipmentInventory = updatedInventory,
            activeEquipmentByType = progress.activeEquipmentByType + (
                definition.type to ActiveEquipmentEffect(
                    equipmentCardId = definition.id,
                    equipmentType = definition.type,
                    packsRemaining = definition.packsAffected,
                )
            ),
            lastActivatedCardIdByType = progress.lastActivatedCardIdByType + (definition.type to definition.id),
        )
            .recordEquipmentActivationSnapshot(equipmentCards)

        val noveltyState = progress.homeMenuNoveltyState.or(
            other = fr.aumombelli.dstcg.model.HomeMenuNoveltyState(
                badgeBook = homeMenuNoveltyEvaluator.hasNewBadgeUnlock(
                    beforeProgress = progress,
                    afterProgress = updatedProgress,
                ),
            ),
        )
        val finalProgress = updatedProgress.copy(homeMenuNoveltyState = noveltyState)

        progressRepository.saveProgress(finalProgress)
        return finalProgress.toEquipmentState()
    }
}
