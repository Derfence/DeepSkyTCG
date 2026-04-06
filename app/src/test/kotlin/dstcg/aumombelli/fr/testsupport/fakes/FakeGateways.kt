package fr.aumombelli.dstcg.testsupport.fakes

import fr.aumombelli.dstcg.data.CatalogGateway
import fr.aumombelli.dstcg.data.CollectionGateway
import fr.aumombelli.dstcg.data.EquipmentGateway
import fr.aumombelli.dstcg.data.PackGateway
import fr.aumombelli.dstcg.data.ProgressLoadResult
import fr.aumombelli.dstcg.data.ProgressGateway
import fr.aumombelli.dstcg.model.CardDefinition
import fr.aumombelli.dstcg.model.DrawPackResponse
import fr.aumombelli.dstcg.model.EquipmentCardDefinition
import fr.aumombelli.dstcg.model.EquipmentSettingsDefinition
import fr.aumombelli.dstcg.model.EquipmentState
import fr.aumombelli.dstcg.model.ExtensionDefinition
import fr.aumombelli.dstcg.model.GameBalanceDefinition
import fr.aumombelli.dstcg.model.OwnedCollection
import fr.aumombelli.dstcg.model.PackCard
import fr.aumombelli.dstcg.model.PackRechargeState
import fr.aumombelli.dstcg.model.StandaloneProgress
import fr.aumombelli.dstcg.model.VariantProfile
import fr.aumombelli.dstcg.model.mergePackCards
import fr.aumombelli.dstcg.testsupport.fixtures.testGameBalanceDefinition
import fr.aumombelli.dstcg.testsupport.fixtures.testVariantProfiles
import java.time.Instant
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class FakeProgressGateway : ProgressGateway {
    var progress = StandaloneProgress(
        collection = OwnedCollection(),
        rechargeState = PackRechargeState(),
    )
    var loadFailure: Throwable? = null
    var compromisedMessage: String? = null
    var recoveryNotice: String? = null
    var trustedNow: Instant = Instant.parse("2026-03-24T12:00:00Z")
    val savedProgress = mutableListOf<StandaloneProgress>()
    var loadCallCount = AtomicInteger(0)
    var resetCallCount = AtomicInteger(0)

    override suspend fun loadProgress(): ProgressLoadResult {
        loadCallCount.incrementAndGet()
        loadFailure?.let { throw it }
        compromisedMessage?.let { return ProgressLoadResult.Compromised(it) }
        recoveryNotice?.let {
            return ProgressLoadResult.Recovered(
                progress = progress,
                trustedNow = trustedNow,
                noticeMessage = it,
            )
        }
        return ProgressLoadResult.Ok(
            progress = progress,
            trustedNow = trustedNow,
        )
    }

    override suspend fun saveProgress(progress: StandaloneProgress) {
        savedProgress += progress
        this.progress = progress
    }

    override suspend fun resetProgress() {
        resetCallCount.incrementAndGet()
        compromisedMessage = null
        recoveryNotice = null
        progress = StandaloneProgress(
            collection = OwnedCollection(),
            rechargeState = PackRechargeState(),
        )
    }
}

class FakeCollectionGateway : CollectionGateway {
    var collection = OwnedCollection()
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
    var extensions: List<ExtensionDefinition> = emptyList()
    var cards: List<CardDefinition> = emptyList()
    var variantProfiles: List<VariantProfile> = testVariantProfiles()
    var gameBalance: GameBalanceDefinition = testGameBalanceDefinition()
    var equipmentCards: List<EquipmentCardDefinition> = emptyList()
    var equipmentSettings: EquipmentSettingsDefinition = EquipmentSettingsDefinition(
        commonReplacementChancePercent = 0.0,
    )
    var extensionsFailure: Throwable? = null
    var cardsFailure: Throwable? = null
    var variantProfilesFailure: Throwable? = null
    var gameBalanceFailure: Throwable? = null
    var equipmentCardsFailure: Throwable? = null
    var equipmentSettingsFailure: Throwable? = null

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

    override suspend fun loadGameBalance(): GameBalanceDefinition {
        gameBalanceFailure?.let { throw it }
        return gameBalance
    }

    override suspend fun loadEquipmentCards(): List<EquipmentCardDefinition> {
        equipmentCardsFailure?.let { throw it }
        return equipmentCards
    }

    override suspend fun loadEquipmentSettings(): EquipmentSettingsDefinition {
        equipmentSettingsFailure?.let { throw it }
        return equipmentSettings
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

class FakeEquipmentGateway : EquipmentGateway {
    var equipmentState: EquipmentState = EquipmentState()
    var loadFailure: Throwable? = null
    var activateFailure: Throwable? = null
    val activateCalls = mutableListOf<String>()

    override suspend fun loadEquipmentState(): EquipmentState {
        loadFailure?.let { throw it }
        return equipmentState
    }

    override suspend fun activateEquipment(equipmentCardId: String): EquipmentState {
        activateCalls += equipmentCardId
        activateFailure?.let { throw it }
        return equipmentState
    }
}
