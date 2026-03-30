package fr.aumombelli.gatcha.data

import fr.aumombelli.gatcha.model.CardDefinition
import fr.aumombelli.gatcha.model.DrawPackResponse
import fr.aumombelli.gatcha.model.ExtensionDefinition
import fr.aumombelli.gatcha.model.OwnedCollection
import fr.aumombelli.gatcha.model.PackCard
import fr.aumombelli.gatcha.model.StandaloneProgress
import fr.aumombelli.gatcha.model.VariantProfile
import kotlinx.coroutines.flow.StateFlow

interface CatalogGateway {
    suspend fun loadExtensions(): List<ExtensionDefinition>
    suspend fun loadCards(): List<CardDefinition>
    suspend fun loadVariantProfiles(): List<VariantProfile>
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

interface PackGateway {
    fun currentPackResult(): StateFlow<DrawPackResponse?>
    fun clearCurrentPackResult()
    suspend fun openPack(extensionId: String): DrawPackResponse
}
