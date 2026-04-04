package fr.aumombelli.dstcg.data

import fr.aumombelli.dstcg.model.OwnedCollection
import fr.aumombelli.dstcg.model.PackCard
import fr.aumombelli.dstcg.model.mergePackCards

class CollectionRepository(
    private val progressRepository: ProgressGateway,
) : CollectionGateway {
    override suspend fun loadCollection(): OwnedCollection =
        progressRepository.loadProgress().requireUsableProgress().progress.collection

    override suspend fun saveCollection(collection: OwnedCollection) {
        val progress = progressRepository.loadProgress().requireUsableProgress().progress
        progressRepository.saveProgress(
            progress.copy(
                collection = collection,
            ),
        )
    }

    override fun mergeCards(collection: OwnedCollection, cards: List<PackCard>): OwnedCollection {
        return collection.mergePackCards(cards)
    }
}
