package fr.aumombelli.gatcha.data

import fr.aumombelli.gatcha.model.OwnedCollection

class CollectionMigrationService(
    private val catalogGateway: CatalogGateway,
) {
    suspend fun emptyCollection(): OwnedCollection =
        OwnedCollection(version = currentCatalogVersion())

    suspend fun migrateToCurrentVersion(collection: OwnedCollection): OwnedCollection =
        migrate(collection, currentCatalogVersion())

    suspend fun currentCatalogVersion(): Int = catalogGateway.loadMetadata().catalogVersion

    private fun migrate(collection: OwnedCollection, targetVersion: Int): OwnedCollection {
        if (collection.version > targetVersion) {
            throw IllegalStateException(
                "Collection version ${collection.version} is newer than supported version $targetVersion.",
            )
        }

        var current = collection.copy(cards = collection.cards.toSortedMap())
        while (current.version < targetVersion) {
            current = migrateStep(current)
        }
        return current
    }

    private fun migrateStep(collection: OwnedCollection): OwnedCollection =
        when (collection.version) {
            1 -> collection.copy(version = 2, cards = collection.cards.toSortedMap())
            else -> throw IllegalStateException(
                "No migration path exists for collection version ${collection.version}.",
            )
        }
}
