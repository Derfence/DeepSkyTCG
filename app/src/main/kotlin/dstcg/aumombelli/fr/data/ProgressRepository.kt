package fr.aumombelli.dstcg.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.dataStore
import fr.aumombelli.dstcg.model.CardDefinition
import fr.aumombelli.dstcg.model.NewPlayerOnboardingStep
import fr.aumombelli.dstcg.model.OwnedCardEntry
import fr.aumombelli.dstcg.model.OwnedCollection
import fr.aumombelli.dstcg.model.OwnedVariantCount
import fr.aumombelli.dstcg.model.PackRechargeState
import fr.aumombelli.dstcg.model.StandaloneProgress
import fr.aumombelli.dstcg.model.VariantProfile
import fr.aumombelli.dstcg.model.normalized
import fr.aumombelli.dstcg.model.normalizedForProgress
import java.time.Duration
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json

internal val Context.secureStandaloneProgressDataStore: DataStore<EncryptedProgressEnvelope> by dataStore(
    fileName = "dstcg_standalone_secure_progress.json",
    serializer = EncryptedProgressEnvelopeSerializer,
)

class ProgressRepository(
    private val secureDataStore: DataStore<EncryptedProgressEnvelope>,
    private val catalogRepository: CatalogGateway,
    private val settings: StandaloneGameSettings = StandaloneGameSettings(),
    private val progressCipher: ProgressCipher,
    private val installIdFactory: () -> String = { UUID.randomUUID().toString() },
) : ProgressGateway {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    override suspend fun loadProgress(): ProgressLoadResult = loadProgressRecord().result

    override suspend fun saveProgress(progress: StandaloneProgress) {
        val currentRecord = loadProgressRecord()
        val drawCooldown = currentDrawCooldown()
        val baseSnapshot = when (val result = currentRecord.result) {
            is ProgressLoadResult.Compromised -> throw CompromisedProgressException(result.message)
            is ProgressLoadResult.Ok,
            is ProgressLoadResult.Recovered,
            -> currentRecord.snapshot
        }

        val timeEvidence = settings.timeSource.now()
        val normalizedCollection = sanitizeCollection(progress.collection)
        val effectiveNow = currentRecord.trustedNow ?: timeEvidence.wallClockUtc
        val normalizedProgress = progress.copy(
            collection = normalizedCollection,
            openedPackCount = progress.openedPackCount.coerceAtLeast(0),
            newPlayerOnboardingStep = progress.newPlayerOnboardingStep,
        ).withNormalizedPackCharge(
            now = effectiveNow,
            drawCooldown = drawCooldown,
            maxStoredDraws = settings.maxStoredDraws,
            weatherPolicy = settings.weatherPolicy,
        )

        val snapshot = ProgressSnapshot(
            installId = baseSnapshot?.installId ?: installIdFactory(),
            schemaVersion = ProgressSnapshot.CURRENT_SCHEMA_VERSION,
            collection = normalizedProgress.collection,
            rechargeState = normalizedProgress.rechargeState,
            openedPackCount = normalizedProgress.openedPackCount.coerceAtLeast(0),
            newPlayerOnboardingStep = normalizedProgress.newPlayerOnboardingStep,
            lastTrustedWallClockUtc = effectiveNow.toString(),
            lastTrustedElapsedRealtimeMs = timeEvidence.elapsedRealtimeMs,
            lastObservedBootMarker = timeEvidence.bootSessionId,
            tamperFlag = false,
        )

        writeSnapshot(snapshot)
    }

    override suspend fun resetProgress() {
        val timeEvidence = settings.timeSource.now()
        val snapshot = ProgressSnapshot(
            installId = installIdFactory(),
            collection = emptyCollection(),
            rechargeState = PackRechargeState(availableDrawCount = settings.maxStoredDraws),
            openedPackCount = 0,
            newPlayerOnboardingStep = NewPlayerOnboardingStep.OpenFirstPackMenu,
            lastTrustedWallClockUtc = timeEvidence.wallClockUtc.toString(),
            lastTrustedElapsedRealtimeMs = timeEvidence.elapsedRealtimeMs,
            lastObservedBootMarker = timeEvidence.bootSessionId,
            tamperFlag = false,
        )
        writeSnapshot(snapshot)
    }

    private suspend fun loadProgressRecord(): ProgressRecord {
        val drawCooldown = currentDrawCooldown()
        val secureEnvelope = runCatching { secureDataStore.data.first() }
            .getOrElse { return ProgressRecord(result = compromisedResult(), snapshot = null, trustedNow = null) }

        if (!secureEnvelope.isEmpty()) {
            val snapshot = decryptSnapshot(secureEnvelope)
                ?: return ProgressRecord(result = compromisedResult(), snapshot = null, trustedNow = null)
            return normalizeSnapshot(
                snapshot = snapshot,
                drawCooldown = drawCooldown,
                forcePersist = false,
            )
        }

        val timeEvidence = settings.timeSource.now()
        return normalizeSnapshot(
            snapshot = ProgressSnapshot(
                installId = installIdFactory(),
                collection = emptyCollection(),
                rechargeState = PackRechargeState(availableDrawCount = settings.maxStoredDraws),
                openedPackCount = 0,
                newPlayerOnboardingStep = NewPlayerOnboardingStep.OpenFirstPackMenu,
                lastTrustedWallClockUtc = timeEvidence.wallClockUtc.toString(),
                lastTrustedElapsedRealtimeMs = timeEvidence.elapsedRealtimeMs,
                lastObservedBootMarker = timeEvidence.bootSessionId,
                tamperFlag = false,
            ),
            drawCooldown = drawCooldown,
            forcePersist = true,
        )
    }

    private suspend fun normalizeSnapshot(
        snapshot: ProgressSnapshot,
        drawCooldown: Duration,
        forcePersist: Boolean,
    ): ProgressRecord {
        val trustedTime = resolveTrustedTime(snapshot)
        val sanitizedCollection = sanitizeCollection(snapshot.collection)
        val normalizedOnboardingStep = snapshot.newPlayerOnboardingStep.normalizedForProgress(
            openedPackCount = snapshot.openedPackCount.coerceAtLeast(0),
            collection = sanitizedCollection,
            isLegacySnapshot = snapshot.schemaVersion < ProgressSnapshot.CURRENT_SCHEMA_VERSION,
        )
        val normalizedProgress = StandaloneProgress(
            collection = sanitizedCollection,
            rechargeState = snapshot.rechargeState,
            openedPackCount = snapshot.openedPackCount.coerceAtLeast(0),
            newPlayerOnboardingStep = normalizedOnboardingStep,
        ).withNormalizedPackCharge(
            now = trustedTime.trustedNow,
            drawCooldown = drawCooldown,
            maxStoredDraws = settings.maxStoredDraws,
            weatherPolicy = settings.weatherPolicy,
        )

        val normalizedSnapshot = snapshot.copy(
            schemaVersion = ProgressSnapshot.CURRENT_SCHEMA_VERSION,
            collection = normalizedProgress.collection,
            rechargeState = normalizedProgress.rechargeState,
            openedPackCount = normalizedProgress.openedPackCount.coerceAtLeast(0),
            newPlayerOnboardingStep = normalizedProgress.newPlayerOnboardingStep,
            lastTrustedWallClockUtc = trustedTime.trustedNow.toString(),
            lastTrustedElapsedRealtimeMs = trustedTime.timeEvidence.elapsedRealtimeMs,
            lastObservedBootMarker = trustedTime.timeEvidence.bootSessionId,
            tamperFlag = trustedTime.tamperDetected,
        )

        val wasRecovered = snapshot.schemaVersion != normalizedSnapshot.schemaVersion ||
            snapshot.collection != normalizedSnapshot.collection ||
            snapshot.rechargeState != normalizedSnapshot.rechargeState ||
            snapshot.openedPackCount != normalizedSnapshot.openedPackCount ||
            snapshot.newPlayerOnboardingStep != normalizedSnapshot.newPlayerOnboardingStep ||
            snapshot.tamperFlag ||
            trustedTime.tamperDetected

        if (forcePersist || snapshot != normalizedSnapshot) {
            writeSnapshot(normalizedSnapshot)
        }

        val result: ProgressLoadResult = if (wasRecovered) {
            ProgressLoadResult.Recovered(
                progress = normalizedProgress,
                trustedNow = trustedTime.trustedNow,
                noticeMessage = RECOVERED_PROGRESS_MESSAGE,
            )
        } else {
            ProgressLoadResult.Ok(
                progress = normalizedProgress,
                trustedNow = trustedTime.trustedNow,
            )
        }

        return ProgressRecord(
            result = result,
            snapshot = normalizedSnapshot,
            trustedNow = trustedTime.trustedNow,
        )
    }

    private fun decryptSnapshot(envelope: EncryptedProgressEnvelope): ProgressSnapshot? = try {
        val plaintext = progressCipher.decrypt(envelope.toPayload())
        json.decodeFromString(ProgressSnapshot.serializer(), plaintext.decodeToString())
    } catch (_: Exception) {
        null
    }

    private fun emptyCollection(): OwnedCollection = OwnedCollection()

    private suspend fun writeSnapshot(snapshot: ProgressSnapshot) {
        val payload = progressCipher.encrypt(
            json.encodeToString(ProgressSnapshot.serializer(), snapshot).encodeToByteArray(),
        )
        secureDataStore.updateData {
            EncryptedProgressEnvelope.fromPayload(payload)
        }
    }

    private suspend fun sanitizeCollection(collection: OwnedCollection): OwnedCollection {
        val cardsById = catalogRepository.loadCards().associateBy(CardDefinition::id)
        val variantProfilesById = catalogRepository.loadVariantProfiles().associateBy(VariantProfile::id)
        val sanitizedCards = collection.cards.mapNotNull { (cardId, entry) ->
            val card = cardsById[cardId] ?: return@mapNotNull null
            val profile = variantProfilesById[card.variantProfileId] ?: return@mapNotNull null
            val sanitizedEntry = sanitizeOwnedEntry(entry, profile)
            if (sanitizedEntry.variants.isEmpty()) {
                null
            } else {
                cardId to sanitizedEntry
            }
        }.toMap().toSortedMap()

        return collection.copy(cards = sanitizedCards).normalized()
    }

    private fun sanitizeOwnedEntry(
        entry: OwnedCardEntry,
        profile: VariantProfile,
    ): OwnedCardEntry {
        val variants = entry.variants.mapNotNull { variant ->
            val isKnownSkyQuality = profile.skyQualities.any { it.code == variant.skyQuality }
            val isKnownFinish = profile.finishes.any { it.code == variant.finish }
            if (!isKnownSkyQuality || !isKnownFinish || variant.count <= 0) {
                null
            } else {
                OwnedVariantCount(
                    skyQuality = variant.skyQuality,
                    finish = variant.finish,
                    count = variant.count,
                )
            }
        }

        return OwnedCardEntry(
            totalOwned = variants.sumOf { it.count },
            variants = variants,
        ).normalized()
    }

    private fun resolveTrustedTime(snapshot: ProgressSnapshot): TrustedTimeResolution {
        val timeEvidence = settings.timeSource.now()
        val storedWallClock = runCatching { Instant.parse(snapshot.lastTrustedWallClockUtc) }
            .getOrElse { timeEvidence.wallClockUtc }
        val sameBoot = snapshot.lastObservedBootMarker == timeEvidence.bootSessionId
        var tamperDetected = snapshot.tamperFlag

        val trustedNow = if (sameBoot) {
            val elapsedDeltaMs = timeEvidence.elapsedRealtimeMs - snapshot.lastTrustedElapsedRealtimeMs
            if (elapsedDeltaMs < 0L) {
                tamperDetected = true
                storedWallClock
            } else {
                val monotonicWallClock = storedWallClock.plusMillis(elapsedDeltaMs)
                when {
                    timeEvidence.wallClockUtc.isBefore(storedWallClock.minus(CLOCK_TOLERANCE)) -> {
                        tamperDetected = true
                        storedWallClock
                    }

                    timeEvidence.wallClockUtc.isAfter(monotonicWallClock.plus(CLOCK_TOLERANCE)) -> {
                        tamperDetected = true
                        monotonicWallClock
                    }

                    else -> monotonicWallClock
                }
            }
        } else {
            if (timeEvidence.wallClockUtc.isBefore(storedWallClock.minus(CLOCK_TOLERANCE))) {
                tamperDetected = true
                storedWallClock
            } else {
                timeEvidence.wallClockUtc
            }
        }

        return TrustedTimeResolution(
            trustedNow = trustedNow,
            timeEvidence = timeEvidence,
            tamperDetected = tamperDetected,
        )
    }

    private fun compromisedResult(): ProgressLoadResult.Compromised = ProgressLoadResult.Compromised(
        message = COMPROMISED_PROGRESS_MESSAGE,
    )

    private suspend fun currentDrawCooldown(): Duration =
        catalogRepository.loadGameBalance().validated().drawCooldownDuration()

    private data class ProgressRecord(
        val result: ProgressLoadResult,
        val snapshot: ProgressSnapshot?,
        val trustedNow: Instant?,
    )

    private data class TrustedTimeResolution(
        val trustedNow: Instant,
        val timeEvidence: TrustedTimeEvidence,
        val tamperDetected: Boolean,
    )

    companion object {
        private val CLOCK_TOLERANCE: Duration = Duration.ofMinutes(2)
        private const val COMPROMISED_PROGRESS_MESSAGE =
            "La progression locale semble corrompue. Reinitialise-la pour continuer."
        private const val RECOVERED_PROGRESS_MESSAGE =
            "La progression locale a ete securisee et certaines donnees ont ete normalisees."

        fun fromContext(
            context: Context,
            catalogRepository: CatalogGateway,
            settings: StandaloneGameSettings = StandaloneGameSettings(),
            progressCipher: ProgressCipher,
        ): ProgressRepository = ProgressRepository(
            secureDataStore = context.secureStandaloneProgressDataStore,
            catalogRepository = catalogRepository,
            settings = settings,
            progressCipher = progressCipher,
        )
    }
}
