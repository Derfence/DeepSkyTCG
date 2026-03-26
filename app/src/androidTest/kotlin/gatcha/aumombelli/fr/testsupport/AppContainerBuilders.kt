package fr.aumombelli.gatcha.testsupport

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import fr.aumombelli.gatcha.AppContainer
import fr.aumombelli.gatcha.data.CatalogGateway
import fr.aumombelli.gatcha.data.CollectionGateway
import fr.aumombelli.gatcha.data.CollectionMigrationService
import fr.aumombelli.gatcha.data.CollectionRepository
import fr.aumombelli.gatcha.data.GameCatalogRepository
import fr.aumombelli.gatcha.data.LocalPackEngine
import fr.aumombelli.gatcha.data.PackGateway
import fr.aumombelli.gatcha.data.PackRepository
import fr.aumombelli.gatcha.data.ProgressGateway
import fr.aumombelli.gatcha.data.ProgressRepository
import fr.aumombelli.gatcha.data.StandaloneGameSettings
import fr.aumombelli.gatcha.model.CatalogMetadata
import fr.aumombelli.gatcha.model.CardDefinition
import fr.aumombelli.gatcha.model.CardFinishDefinition
import fr.aumombelli.gatcha.model.DrawPackResponse
import fr.aumombelli.gatcha.model.ExtensionDefinition
import fr.aumombelli.gatcha.model.OwnedCollection
import fr.aumombelli.gatcha.model.OwnedVariantCount
import fr.aumombelli.gatcha.model.PackCard
import fr.aumombelli.gatcha.model.SkyQualityDefinition
import fr.aumombelli.gatcha.model.StandaloneProgress
import fr.aumombelli.gatcha.model.VariantProfile
import fr.aumombelli.gatcha.model.WeightedCode
import fr.aumombelli.gatcha.model.mergePackCards
import fr.aumombelli.gatcha.testCardDefinition
import fr.aumombelli.gatcha.testPackCard
import java.io.File
import java.time.Clock
import kotlin.random.Random
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

internal fun offlineMainActivityTestAppContainer(
    context: Context,
    dataStoreFileName: String,
    randomSeed: Int = 12345,
): AppContainer {
    val appContext = context.applicationContext
    val file = File(appContext.cacheDir, dataStoreFileName)
    file.delete()
    val catalogRepository = GameCatalogRepository(appContext)
    val migrationService = CollectionMigrationService(catalogRepository)
    val dataStore = PreferenceDataStoreFactory.create(
        scope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
        produceFile = { file },
    )
    val progressRepository = ProgressRepository(
        dataStore = dataStore,
        collectionMigrationService = migrationService,
    )
    val collectionRepository = CollectionRepository(progressRepository)
    val packRepository = PackRepository(
        progressRepository = progressRepository,
        collectionRepository = collectionRepository,
        localPackEngine = LocalPackEngine(
            catalogRepository = catalogRepository,
            settings = StandaloneGameSettings(
                clock = Clock.systemUTC(),
                random = Random(randomSeed),
            ),
        ),
    )

    return AppContainer(
        progressRepository = progressRepository,
        catalogRepository = catalogRepository,
        collectionRepository = collectionRepository,
        packRepository = packRepository,
    )
}

internal fun backNavigationTestAppContainer(): AppContainer {
    val extension = ExtensionDefinition(
        id = "astronomes-en-herbe",
        name = "Astronomes en herbe",
        coverImageRef = "cover",
    )
    val cardDefinition = testCardDefinition("ALP-001")
    val progressRepository = MutableProgressGateway(
        initialProgress = StandaloneProgress(
            collection = OwnedCollection(
                version = 5,
                cards = mapOf(
                    "ALP-001" to fr.aumombelli.gatcha.model.OwnedCardEntry(
                        totalOwned = 1,
                        variants = listOf(
                            OwnedVariantCount(
                                skyQuality = "city",
                                finish = "standard",
                                count = 1,
                            ),
                        ),
                    ),
                ),
            ),
            nextDrawAt = null,
        ),
    )
    val collectionRepository = NavigationCollectionGateway(progressRepository)
    val packResponse = DrawPackResponse(
        extensionId = extension.id,
        drawnAt = "2026-03-23T12:00:00Z",
        nextDrawAt = "2026-03-24T00:00:00Z",
        cards = listOf(
            testPackCard(
                cardId = "ALP-001",
                name = "Nebuleuse d'Orion",
                rarityLabel = "Common",
                imageRef = "m42_orion_nebula",
            ),
        ),
    )

    return AppContainer(
        progressRepository = progressRepository,
        catalogRepository = NavigationCatalogGateway(
            extensions = listOf(extension),
            cards = listOf(cardDefinition),
            variantProfiles = listOf(navigationVariantProfile()),
        ),
        collectionRepository = collectionRepository,
        packRepository = NavigationPackGateway(progressRepository, packResponse),
    )
}

private class MutableProgressGateway(
    initialProgress: StandaloneProgress,
) : ProgressGateway {
    private var progress = initialProgress

    override suspend fun loadProgress(): StandaloneProgress = progress

    override suspend fun saveProgress(progress: StandaloneProgress) {
        this.progress = progress
    }
}

private fun navigationVariantProfile(): VariantProfile = VariantProfile(
    id = "observation-default",
    skyQualities = listOf(
        SkyQualityDefinition(code = "city", label = "Ville"),
    ),
    finishes = listOf(
        CardFinishDefinition(code = "standard", label = "Standard"),
    ),
    skyQualityWeights = listOf(WeightedCode(code = "city", weight = 1)),
    finishWeights = listOf(WeightedCode(code = "standard", weight = 1)),
)

private class NavigationCatalogGateway(
    private val extensions: List<ExtensionDefinition>,
    private val cards: List<CardDefinition>,
    private val variantProfiles: List<VariantProfile>,
) : CatalogGateway {
    override suspend fun loadMetadata(): CatalogMetadata = CatalogMetadata(catalogVersion = 5)
    override suspend fun loadExtensions(): List<ExtensionDefinition> = extensions
    override suspend fun loadCards(): List<CardDefinition> = cards
    override suspend fun loadVariantProfiles(): List<VariantProfile> = variantProfiles
}

private class NavigationCollectionGateway(
    private val progressRepository: MutableProgressGateway,
) : CollectionGateway {
    override suspend fun loadCollection(): OwnedCollection = progressRepository.loadProgress().collection

    override suspend fun saveCollection(collection: OwnedCollection) {
        val progress = progressRepository.loadProgress()
        progressRepository.saveProgress(progress.copy(collection = collection))
    }

    override fun mergeCards(collection: OwnedCollection, cards: List<PackCard>): OwnedCollection =
        collection.mergePackCards(cards)
}

private class NavigationPackGateway(
    private val progressRepository: MutableProgressGateway,
    private val openPackResponse: DrawPackResponse,
) : PackGateway {
    private val packFlow = MutableStateFlow<DrawPackResponse?>(null)

    override fun currentPackResult(): StateFlow<DrawPackResponse?> = packFlow

    override fun clearCurrentPackResult() {
        packFlow.value = null
    }

    override suspend fun openPack(extensionId: String): DrawPackResponse {
        val progress = progressRepository.loadProgress()
        val mergedCollection = progress.collection.mergePackCards(openPackResponse.cards)
        progressRepository.saveProgress(
            progress.copy(
                collection = mergedCollection,
                nextDrawAt = openPackResponse.nextDrawAt,
            ),
        )
        packFlow.value = openPackResponse
        return openPackResponse
    }
}
