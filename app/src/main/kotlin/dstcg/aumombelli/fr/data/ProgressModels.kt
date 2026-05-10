package fr.aumombelli.dstcg.data

import fr.aumombelli.dstcg.model.OwnedCollection
import fr.aumombelli.dstcg.model.EquipmentType
import fr.aumombelli.dstcg.model.ActiveEquipmentEffect
import fr.aumombelli.dstcg.model.EquipmentBadgeProgress
import fr.aumombelli.dstcg.model.OwnedEquipmentInventory
import fr.aumombelli.dstcg.model.NewPlayerOnboardingStep
import fr.aumombelli.dstcg.model.PackRechargeState
import fr.aumombelli.dstcg.model.StandaloneProgress
import fr.aumombelli.dstcg.model.HomeMenuNoveltyState
import fr.aumombelli.dstcg.model.LibraryCardNoveltyState
import fr.aumombelli.dstcg.model.TradeLedgerState
import java.time.Instant
import kotlinx.serialization.Serializable

@Serializable
data class ProgressSnapshot(
    val installId: String,
    val schemaVersion: Int = CURRENT_SCHEMA_VERSION,
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
    val miniGamesMenuUnlocked: Boolean = false,
    val lastTrustedWallClockUtc: String,
    val lastTrustedElapsedRealtimeMs: Long = 0L,
    val lastObservedBootMarker: String,
    val tamperFlag: Boolean = false,
) {
    fun toProgress(): StandaloneProgress = StandaloneProgress(
        collection = collection,
        rechargeState = rechargeState,
        openedPackCount = openedPackCount,
        hasOpenedEpicBoostedPack = hasOpenedEpicBoostedPack,
        newPlayerOnboardingStep = newPlayerOnboardingStep,
        equipmentInventory = equipmentInventory,
        activeEquipmentByType = activeEquipmentByType,
        lastActivatedCardIdByType = lastActivatedCardIdByType,
        equipmentBadgeProgress = equipmentBadgeProgress,
        homeMenuNoveltyState = homeMenuNoveltyState,
        libraryCardNoveltyState = libraryCardNoveltyState,
        tradeLedgerState = tradeLedgerState,
        miniGamesMenuUnlocked = miniGamesMenuUnlocked,
    )

    companion object {
        const val CURRENT_SCHEMA_VERSION: Int = 10
        const val ONBOARDING_STATE_SCHEMA_VERSION: Int = 4
    }
}

data class LoadedProgress(
    val progress: StandaloneProgress,
    val trustedNow: Instant,
)

sealed interface ProgressLoadResult {
    data class Ok(
        val progress: StandaloneProgress,
        val trustedNow: Instant,
    ) : ProgressLoadResult

    data class Recovered(
        val progress: StandaloneProgress,
        val trustedNow: Instant,
        val noticeMessage: String,
    ) : ProgressLoadResult

    data class Compromised(
        val message: String,
    ) : ProgressLoadResult
}

class CompromisedProgressException(
    message: String,
) : IllegalStateException(message)

fun ProgressLoadResult.requireUsableProgress(): LoadedProgress = when (this) {
    is ProgressLoadResult.Ok -> LoadedProgress(progress = progress, trustedNow = trustedNow)
    is ProgressLoadResult.Recovered -> LoadedProgress(progress = progress, trustedNow = trustedNow)
    is ProgressLoadResult.Compromised -> throw CompromisedProgressException(message)
}
