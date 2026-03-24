package fr.aumombelli.gatcha.data

import fr.aumombelli.gatcha.model.OwnedCollection
import fr.aumombelli.gatcha.model.normalized

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

        var current = collection.normalized()
        while (current.version < targetVersion) {
            current = migrateStep(current)
        }
        return current.normalized()
    }

    private fun migrateStep(collection: OwnedCollection): OwnedCollection =
        when (collection.version) {
            1 -> collection.copy(version = 2).normalized()
            2 -> collection.copy(version = 3).normalized()
            3 -> collection.copy(version = 4).normalized()
            else -> throw IllegalStateException(
                "No migration path exists for collection version ${collection.version}.",
            )
        }
}
