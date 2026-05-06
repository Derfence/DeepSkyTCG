package fr.aumombelli.dstcg.model

import kotlinx.serialization.Serializable

@Serializable
data class OwnedCollection(
    val cards: Map<String, OwnedCardEntry> = emptyMap(),
)

@Serializable
data class OwnedCardEntry(
    val totalOwned: Int = 0,
    val variants: List<OwnedVariantCount> = emptyList(),
)

@Serializable
data class OwnedVariantCount(
    val skyQuality: String,
    val finish: String,
    val count: Int,
)

data class StandaloneProgress(
    val collection: OwnedCollection,
    val rechargeState: PackRechargeState = PackRechargeState(),
    val openedPackCount: Int = 0,
    val hasOpenedEpicBoostedPack: Boolean = false,
    val newPlayerOnboardingStep: NewPlayerOnboardingStep = NewPlayerOnboardingStep.ShowWelcomeIntro,
    val equipmentInventory: OwnedEquipmentInventory = OwnedEquipmentInventory(),
    val activeEquipmentByType: Map<EquipmentType, ActiveEquipmentEffect> = emptyMap(),
    val lastActivatedCardIdByType: Map<EquipmentType, String> = emptyMap(),
    val equipmentBadgeProgress: EquipmentBadgeProgress = EquipmentBadgeProgress(),
    val homeMenuNoveltyState: HomeMenuNoveltyState = HomeMenuNoveltyState(),
    val libraryCardNoveltyState: LibraryCardNoveltyState = LibraryCardNoveltyState(),
    val tradeLedgerState: TradeLedgerState = TradeLedgerState(),
)
