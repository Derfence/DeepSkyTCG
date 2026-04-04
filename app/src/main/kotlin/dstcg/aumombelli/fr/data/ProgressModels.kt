package fr.aumombelli.dstcg.data

import fr.aumombelli.dstcg.model.OwnedCollection
import fr.aumombelli.dstcg.model.NewPlayerOnboardingStep
import fr.aumombelli.dstcg.model.PackRechargeState
import fr.aumombelli.dstcg.model.StandaloneProgress
import java.time.Instant
import kotlinx.serialization.Serializable

@Serializable
data class ProgressSnapshot(
    val installId: String,
    val schemaVersion: Int = CURRENT_SCHEMA_VERSION,
    val collection: OwnedCollection,
    val rechargeState: PackRechargeState = PackRechargeState(),
    val openedPackCount: Int = 0,
    val newPlayerOnboardingStep: NewPlayerOnboardingStep = NewPlayerOnboardingStep.OpenFirstPackMenu,
    val lastTrustedWallClockUtc: String,
    val lastTrustedElapsedRealtimeMs: Long = 0L,
    val lastObservedBootMarker: String,
    val tamperFlag: Boolean = false,
) {
    fun toProgress(): StandaloneProgress = StandaloneProgress(
        collection = collection,
        rechargeState = rechargeState,
        openedPackCount = openedPackCount,
        newPlayerOnboardingStep = newPlayerOnboardingStep,
    )

    companion object {
        const val CURRENT_SCHEMA_VERSION: Int = 3
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
