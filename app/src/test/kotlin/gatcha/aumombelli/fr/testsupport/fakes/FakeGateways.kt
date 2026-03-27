package fr.aumombelli.gatcha.testsupport.fakes

import fr.aumombelli.gatcha.data.CatalogGateway
import fr.aumombelli.gatcha.data.CollectionGateway
import fr.aumombelli.gatcha.data.PackGateway
import fr.aumombelli.gatcha.data.ProgressGateway
import fr.aumombelli.gatcha.model.CatalogMetadata
import fr.aumombelli.gatcha.model.CardDefinition
import fr.aumombelli.gatcha.model.DrawPackResponse
import fr.aumombelli.gatcha.model.ExtensionDefinition
import fr.aumombelli.gatcha.model.OwnedCollection
import fr.aumombelli.gatcha.model.PackCard
import fr.aumombelli.gatcha.model.StandaloneProgress
import fr.aumombelli.gatcha.model.VariantProfile
import fr.aumombelli.gatcha.model.mergePackCards
import fr.aumombelli.gatcha.testsupport.fixtures.testVariantProfiles
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class FakeProgressGateway : ProgressGateway {
    var progress = StandaloneProgress(
        collection = OwnedCollection(version = 5),
        availableDrawCount = 10,
        nextChargeAt = null,
    )
    var loadFailure: Throwable? = null
    val savedProgress = mutableListOf<StandaloneProgress>()
    var loadCallCount = AtomicInteger(0)

    override suspend fun loadProgress(): StandaloneProgress {
        loadCallCount.incrementAndGet()
        loadFailure?.let { throw it }
        return progress
    }

    override suspend fun saveProgress(progress: StandaloneProgress) {
        savedProgress += progress
        this.progress = progress
    }
}

class FakeCollectionGateway : CollectionGateway {
    var collection = OwnedCollection(version = 5)
    var loadCollectionFailure: Throwable? = null
    val savedCollections = mutableListOf<OwnedCollection>()
    var loadCollectionCallCount = AtomicInteger(0)

    override suspend fun loadCollection(): OwnedCollection {
        loadCollectionCallCount.incrementAndGet()
        loadCollectionFailure?.let { throw it }
        return collection
    }

    override suspend fun saveCollection(collection: OwnedCollection) {
        savedCollections += collection
        this.collection = collection
    }

    override fun mergeCards(collection: OwnedCollection, cards: List<PackCard>): OwnedCollection =
        collection.mergePackCards(cards)
}

class FakeCatalogGateway : CatalogGateway {
    var metadata = CatalogMetadata(catalogVersion = 5)
    var extensions: List<ExtensionDefinition> = emptyList()
    var cards: List<CardDefinition> = emptyList()
    var variantProfiles: List<VariantProfile> = testVariantProfiles()
    var metadataFailure: Throwable? = null
    var extensionsFailure: Throwable? = null
    var cardsFailure: Throwable? = null
    var variantProfilesFailure: Throwable? = null

    override suspend fun loadMetadata(): CatalogMetadata {
        metadataFailure?.let { throw it }
        return metadata
    }

    override suspend fun loadExtensions(): List<ExtensionDefinition> {
        extensionsFailure?.let { throw it }
        return extensions
    }

    override suspend fun loadCards(): List<CardDefinition> {
        cardsFailure?.let { throw it }
        return cards
    }

    override suspend fun loadVariantProfiles(): List<VariantProfile> {
        variantProfilesFailure?.let { throw it }
        return variantProfiles
    }
}

class FakePackGateway : PackGateway {
    private val packFlow = MutableStateFlow<DrawPackResponse?>(null)
    var openPackResponse: DrawPackResponse? = null
    var openPackFailure: Throwable? = null
    var onOpenPack: ((String) -> Unit)? = null
    val openPackCalls = mutableListOf<String>()

    override fun currentPackResult(): StateFlow<DrawPackResponse?> = packFlow

    override fun clearCurrentPackResult() {
        packFlow.value = null
    }

    override suspend fun openPack(extensionId: String): DrawPackResponse {
        openPackCalls += extensionId
        openPackFailure?.let { throw it }
        onOpenPack?.invoke(extensionId)
        return checkNotNull(openPackResponse) { "openPackResponse must be configured in FakePackGateway." }
            .also { packFlow.value = it }
    }
}
