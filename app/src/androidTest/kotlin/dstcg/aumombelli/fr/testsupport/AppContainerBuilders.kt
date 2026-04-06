package fr.aumombelli.dstcg.testsupport

import android.content.Context
import androidx.datastore.core.DataStoreFactory
import fr.aumombelli.dstcg.AppContainer
import fr.aumombelli.dstcg.data.AesGcmProgressCipher
import fr.aumombelli.dstcg.data.AndroidTrustedTimeSource
import fr.aumombelli.dstcg.data.CatalogGateway
import fr.aumombelli.dstcg.data.CollectionGateway
import fr.aumombelli.dstcg.data.CollectionRepository
import fr.aumombelli.dstcg.data.EncryptedProgressEnvelopeSerializer
import fr.aumombelli.dstcg.data.EquipmentRepository
import fr.aumombelli.dstcg.data.GameCatalogRepository
import fr.aumombelli.dstcg.data.LocalPackEngine
import fr.aumombelli.dstcg.data.PackGateway
import fr.aumombelli.dstcg.data.PackRepository
import fr.aumombelli.dstcg.data.ProgressLoadResult
import fr.aumombelli.dstcg.data.ProgressGateway
import fr.aumombelli.dstcg.data.ProgressRepository
import fr.aumombelli.dstcg.data.RandomEntropySource
import fr.aumombelli.dstcg.data.StandaloneGameSettings
import fr.aumombelli.dstcg.model.CardDefinition
import fr.aumombelli.dstcg.model.CardFinishDefinition
import fr.aumombelli.dstcg.model.DrawPackResponse
import fr.aumombelli.dstcg.model.EquipmentCardDefinition
import fr.aumombelli.dstcg.model.EquipmentSettingsDefinition
import fr.aumombelli.dstcg.model.ExtensionDefinition
import fr.aumombelli.dstcg.model.GameBalanceDefinition
import fr.aumombelli.dstcg.model.NewPlayerOnboardingStep
import fr.aumombelli.dstcg.model.OwnedCollection
import fr.aumombelli.dstcg.model.OwnedEquipmentCardEntry
import fr.aumombelli.dstcg.model.OwnedEquipmentInventory
import fr.aumombelli.dstcg.model.OwnedVariantCount
import fr.aumombelli.dstcg.model.PackRechargeState
import fr.aumombelli.dstcg.model.PackCard
import fr.aumombelli.dstcg.model.SkyQualityDefinition
import fr.aumombelli.dstcg.model.StandaloneProgress
import fr.aumombelli.dstcg.model.VariantProfile
import fr.aumombelli.dstcg.model.mergePackCards
import fr.aumombelli.dstcg.testCardDefinition
import fr.aumombelli.dstcg.testGameBalanceDefinition
import fr.aumombelli.dstcg.testPackCard
import java.io.File
import java.time.Instant
import kotlin.random.Random
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

internal fun offlineMainActivityTestAppContainer(
    context: Context,
    dataStoreFileName: String,
    randomSeed: Int = 12345,
): AppContainer {
    val appContext = context.applicationContext
    val secureFile = File(appContext.cacheDir, dataStoreFileName.replace(".preferences_pb", ".secure.json"))
    secureFile.delete()
    val catalogRepository = GameCatalogRepository(appContext)
    val secureDataStore = DataStoreFactory.create(
        serializer = EncryptedProgressEnvelopeSerializer,
        scope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
        produceFile = { secureFile },
    )
    val gameSettings = StandaloneGameSettings(
        timeSource = AndroidTrustedTimeSource(appContext),
        entropySource = RandomEntropySource(Random(randomSeed)),
    )
    val progressRepository = ProgressRepository(
        secureDataStore = secureDataStore,
        catalogRepository = catalogRepository,
        settings = gameSettings,
        progressCipher = AesGcmProgressCipher(keyProvider = ::newTestSecretKey),
    )
    val collectionRepository = CollectionRepository(progressRepository)
    val equipmentRepository = EquipmentRepository(
        progressRepository = progressRepository,
        catalogRepository = catalogRepository,
    )
    val packRepository = PackRepository(
        progressRepository = progressRepository,
        collectionRepository = collectionRepository,
        localPackEngine = LocalPackEngine(
            catalogRepository = catalogRepository,
            settings = gameSettings,
        ),
    )

    return AppContainer(
        progressRepository = progressRepository,
        catalogRepository = catalogRepository,
        collectionRepository = collectionRepository,
        equipmentRepository = equipmentRepository,
        packRepository = packRepository,
        gameSettings = gameSettings,
    )
}

internal fun backNavigationTestAppContainer(): AppContainer {
    return navigationTestAppContainer(
        initialCollection = OwnedCollection(
            cards = mapOf(
                "ALP-001" to fr.aumombelli.dstcg.model.OwnedCardEntry(
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
        unlockEquipmentMenu = true,
    )
}

internal fun badgeCelebrationBackNavigationTestAppContainer(): AppContainer {
    return navigationTestAppContainer(
        initialCollection = OwnedCollection(),
    )
}

private fun navigationTestAppContainer(
    initialCollection: OwnedCollection,
    unlockEquipmentMenu: Boolean = false,
): AppContainer {
    val extension = ExtensionDefinition(
        id = "astronomes-en-herbe",
        name = "Astronomes en herbe",
        coverImageRef = "cover",
    )
    val cardDefinition = testCardDefinition("ALP-001")
    val progressRepository = MutableProgressGateway(
        initialProgress = StandaloneProgress(
            collection = initialCollection,
            rechargeState = PackRechargeState(),
            newPlayerOnboardingStep = if (initialCollection.cards.isEmpty()) {
                NewPlayerOnboardingStep.OpenFirstPackMenu
            } else {
                NewPlayerOnboardingStep.Completed
            },
            equipmentInventory = if (unlockEquipmentMenu) {
                OwnedEquipmentInventory(
                    cards = mapOf(
                        "starter-equipment" to OwnedEquipmentCardEntry(countOwned = 1),
                    ),
                )
            } else {
                OwnedEquipmentInventory()
            },
        ),
    )
    val collectionRepository = NavigationCollectionGateway(progressRepository)
    val catalogRepository = NavigationCatalogGateway(
        extensions = listOf(extension),
        cards = listOf(cardDefinition),
        variantProfiles = listOf(navigationVariantProfile()),
    )
    val packResponse = DrawPackResponse.fromCards(
        extensionId = extension.id,
        drawnAt = "2026-03-23T12:00:00Z",
        rechargeState = androidTestRechargeStateWithNextChargeAt(
            availableDrawCount = 9,
            nextChargeAt = "2026-03-24T18:00:00Z",
        ),
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
        catalogRepository = catalogRepository,
        collectionRepository = collectionRepository,
        equipmentRepository = EquipmentRepository(
            progressRepository = progressRepository,
            catalogRepository = catalogRepository,
        ),
        packRepository = NavigationPackGateway(progressRepository, packResponse),
        gameSettings = StandaloneGameSettings(
            entropySource = RandomEntropySource(Random(12345)),
        ),
    )
}

private class MutableProgressGateway(
    initialProgress: StandaloneProgress,
) : ProgressGateway {
    private var progress = initialProgress
    private val trustedNow = Instant.parse("2026-03-24T12:00:00Z")

    override suspend fun loadProgress(): ProgressLoadResult = ProgressLoadResult.Ok(
        progress = progress,
        trustedNow = trustedNow,
    )

    override suspend fun saveProgress(progress: StandaloneProgress) {
        this.progress = progress
    }

    override suspend fun resetProgress() {
        progress = StandaloneProgress(
            collection = OwnedCollection(),
            rechargeState = PackRechargeState(),
        )
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
)

private class NavigationCatalogGateway(
    private val extensions: List<ExtensionDefinition>,
    private val cards: List<CardDefinition>,
    private val variantProfiles: List<VariantProfile>,
    private val gameBalance: GameBalanceDefinition = testGameBalanceDefinition(),
) : CatalogGateway {
    override suspend fun loadExtensions(): List<ExtensionDefinition> = extensions
    override suspend fun loadCards(): List<CardDefinition> = cards
    override suspend fun loadVariantProfiles(): List<VariantProfile> = variantProfiles
    override suspend fun loadGameBalance(): GameBalanceDefinition = gameBalance
    override suspend fun loadEquipmentCards(): List<EquipmentCardDefinition> = emptyList()
    override suspend fun loadEquipmentSettings(): EquipmentSettingsDefinition = EquipmentSettingsDefinition(
        commonReplacementChancePercent = 0.0,
    )
}

private class NavigationCollectionGateway(
    private val progressRepository: MutableProgressGateway,
) : CollectionGateway {
    override suspend fun loadCollection(): OwnedCollection =
        (progressRepository.loadProgress() as ProgressLoadResult.Ok).progress.collection

    override suspend fun saveCollection(collection: OwnedCollection) {
        val progress = (progressRepository.loadProgress() as ProgressLoadResult.Ok).progress
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
        val progress = (progressRepository.loadProgress() as ProgressLoadResult.Ok).progress
        val mergedCollection = progress.collection.mergePackCards(openPackResponse.cards)
        progressRepository.saveProgress(
            progress.copy(
                collection = mergedCollection,
                rechargeState = openPackResponse.rechargeState,
                openedPackCount = progress.openedPackCount + 1,
            ),
        )
        packFlow.value = openPackResponse
        return openPackResponse
    }
}

private val instrumentationTestSecretKey: SecretKey by lazy {
    KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()
}

private fun newTestSecretKey(): SecretKey = instrumentationTestSecretKey
