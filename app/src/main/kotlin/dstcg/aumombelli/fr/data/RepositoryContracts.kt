package fr.aumombelli.dstcg.data

import fr.aumombelli.dstcg.model.CardDefinition
import fr.aumombelli.dstcg.model.DrawPackResponse
import fr.aumombelli.dstcg.model.EquipmentCardDefinition
import fr.aumombelli.dstcg.model.EquipmentSettingsDefinition
import fr.aumombelli.dstcg.model.EquipmentState
import fr.aumombelli.dstcg.model.ExtensionDefinition
import fr.aumombelli.dstcg.model.GameBalanceDefinition
import fr.aumombelli.dstcg.model.OwnedCollection
import fr.aumombelli.dstcg.model.PackCard
import fr.aumombelli.dstcg.model.StandaloneProgress
import fr.aumombelli.dstcg.model.VariantProfile
import kotlinx.coroutines.flow.StateFlow

interface CatalogGateway {
    suspend fun loadExtensions(): List<ExtensionDefinition>
    suspend fun loadCards(): List<CardDefinition>
    suspend fun loadVariantProfiles(): List<VariantProfile>
    suspend fun loadGameBalance(): GameBalanceDefinition
    suspend fun loadEquipmentCards(): List<EquipmentCardDefinition>
    suspend fun loadEquipmentSettings(): EquipmentSettingsDefinition
}

interface ProgressGateway {
    suspend fun loadProgress(): ProgressLoadResult
    suspend fun saveProgress(progress: StandaloneProgress)
    suspend fun resetProgress()
}

interface CollectionGateway {
    suspend fun loadCollection(): OwnedCollection
    suspend fun saveCollection(collection: OwnedCollection)
    fun mergeCards(collection: OwnedCollection, cards: List<PackCard>): OwnedCollection
}

interface EquipmentGateway {
    suspend fun loadEquipmentState(): EquipmentState
    suspend fun activateEquipment(equipmentCardId: String): EquipmentState
}

interface PackGateway {
    fun currentPackResult(): StateFlow<DrawPackResponse?>
    fun clearCurrentPackResult()
    suspend fun openPack(extensionId: String): DrawPackResponse
}
