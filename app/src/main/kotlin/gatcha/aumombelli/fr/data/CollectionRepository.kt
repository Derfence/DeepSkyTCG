package fr.aumombelli.gatcha.data

import fr.aumombelli.gatcha.model.OwnedCollection
import fr.aumombelli.gatcha.model.PackCard
import fr.aumombelli.gatcha.model.mergePackCards

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
