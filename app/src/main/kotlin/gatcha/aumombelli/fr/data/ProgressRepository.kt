package fr.aumombelli.gatcha.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.dataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import fr.aumombelli.gatcha.model.CardDefinition
import fr.aumombelli.gatcha.model.OwnedCardEntry
import fr.aumombelli.gatcha.model.OwnedCollection
import fr.aumombelli.gatcha.model.OwnedVariantCount
import fr.aumombelli.gatcha.model.StandaloneProgress
import fr.aumombelli.gatcha.model.VariantProfile
import fr.aumombelli.gatcha.model.normalized
import java.time.Duration
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

internal val Context.legacyStandaloneProgressDataStore by preferencesDataStore(name = "gatcha_standalone_progress")
internal val Context.secureStandaloneProgressDataStore: DataStore<EncryptedProgressEnvelope> by dataStore(
    fileName = "gatcha_standalone_secure_progress.json",
    serializer = EncryptedProgressEnvelopeSerializer,
)

class ProgressRepository(
    private val secureDataStore: DataStore<EncryptedProgressEnvelope>,
    private val legacyDataStore: DataStore<Preferences>,
    private val catalogRepository: CatalogGateway,
    private val settings: StandaloneGameSettings = StandaloneGameSettings(),
    private val progressCipher: ProgressCipher,
    private val installIdFactory: () -> String = { UUID.randomUUID().toString() },
) : ProgressGateway {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    override suspend fun loadProgress(): ProgressLoadResult = loadProgressRecord().result

    override suspend fun saveProgress(progress: StandaloneProgress) {
        val currentRecord = loadProgressRecord()
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
        ).withNormalizedPackCharge(
            now = effectiveNow,
            drawCooldown = settings.drawCooldown,
            maxStoredDraws = settings.maxStoredDraws,
        )

        val snapshot = ProgressSnapshot(
            installId = baseSnapshot?.installId ?: installIdFactory(),
            schemaVersion = ProgressSnapshot.CURRENT_SCHEMA_VERSION,
            collection = normalizedProgress.collection,
            availableDrawCount = normalizedProgress.availableDrawCount,
            nextChargeAt = normalizedProgress.nextChargeAt,
            openedPackCount = normalizedProgress.openedPackCount.coerceAtLeast(0),
            lastTrustedWallClockUtc = effectiveNow.toString(),
            lastTrustedElapsedRealtimeMs = timeEvidence.elapsedRealtimeMs,
            lastObservedBootMarker = timeEvidence.bootSessionId,
            tamperFlag = false,
        )

        writeSnapshot(snapshot)
        clearLegacyProgress()
    }

    override suspend fun resetProgress() {
        val timeEvidence = settings.timeSource.now()
        val snapshot = ProgressSnapshot(
            installId = installIdFactory(),
            collection = emptyCollection(),
            availableDrawCount = settings.maxStoredDraws,
            nextChargeAt = null,
            openedPackCount = 0,
            lastTrustedWallClockUtc = timeEvidence.wallClockUtc.toString(),
            lastTrustedElapsedRealtimeMs = timeEvidence.elapsedRealtimeMs,
            lastObservedBootMarker = timeEvidence.bootSessionId,
            tamperFlag = false,
        )
        writeSnapshot(snapshot)
        clearLegacyProgress()
    }

    private suspend fun loadProgressRecord(): ProgressRecord {
        val secureEnvelope = runCatching { secureDataStore.data.first() }
            .getOrElse { return ProgressRecord(result = compromisedResult(), snapshot = null, trustedNow = null) }

        if (!secureEnvelope.isEmpty()) {
            val snapshot = decryptSnapshot(secureEnvelope)
                ?: return ProgressRecord(result = compromisedResult(), snapshot = null, trustedNow = null)
            return normalizeSnapshot(
                snapshot = snapshot,
                fromLegacy = false,
                forcePersist = false,
            )
        }

        val legacyPreferences = legacyDataStore.data.first()
        val legacySnapshot = runCatching { migrateLegacyPreferences(legacyPreferences) }
            .getOrElse { return ProgressRecord(result = compromisedResult(), snapshot = null, trustedNow = null) }

        return if (legacySnapshot != null) {
            normalizeSnapshot(
                snapshot = legacySnapshot,
                fromLegacy = true,
                forcePersist = true,
            )
        } else {
            val timeEvidence = settings.timeSource.now()
            normalizeSnapshot(
                snapshot = ProgressSnapshot(
                    installId = installIdFactory(),
                    collection = emptyCollection(),
                    availableDrawCount = settings.maxStoredDraws,
                    nextChargeAt = null,
                    openedPackCount = 0,
                    lastTrustedWallClockUtc = timeEvidence.wallClockUtc.toString(),
                    lastTrustedElapsedRealtimeMs = timeEvidence.elapsedRealtimeMs,
                    lastObservedBootMarker = timeEvidence.bootSessionId,
                    tamperFlag = false,
                ),
                fromLegacy = false,
                forcePersist = true,
            )
        }
    }

    private suspend fun normalizeSnapshot(
        snapshot: ProgressSnapshot,
        fromLegacy: Boolean,
        forcePersist: Boolean,
    ): ProgressRecord {
        val trustedTime = resolveTrustedTime(snapshot)
        val sanitizedCollection = sanitizeCollection(snapshot.collection)
        val normalizedProgress = StandaloneProgress(
            collection = sanitizedCollection,
            availableDrawCount = snapshot.availableDrawCount,
            nextChargeAt = snapshot.nextChargeAt,
            openedPackCount = snapshot.openedPackCount.coerceAtLeast(0),
        ).withNormalizedPackCharge(
            now = trustedTime.trustedNow,
            drawCooldown = settings.drawCooldown,
            maxStoredDraws = settings.maxStoredDraws,
        )

        val normalizedSnapshot = snapshot.copy(
            schemaVersion = ProgressSnapshot.CURRENT_SCHEMA_VERSION,
            collection = normalizedProgress.collection,
            availableDrawCount = normalizedProgress.availableDrawCount,
            nextChargeAt = normalizedProgress.nextChargeAt,
            openedPackCount = normalizedProgress.openedPackCount.coerceAtLeast(0),
            lastTrustedWallClockUtc = trustedTime.trustedNow.toString(),
            lastTrustedElapsedRealtimeMs = trustedTime.timeEvidence.elapsedRealtimeMs,
            lastObservedBootMarker = trustedTime.timeEvidence.bootSessionId,
            tamperFlag = trustedTime.tamperDetected,
        )

        val wasRecovered = fromLegacy ||
            snapshot.schemaVersion != normalizedSnapshot.schemaVersion ||
            snapshot.collection != normalizedSnapshot.collection ||
            snapshot.availableDrawCount != normalizedSnapshot.availableDrawCount ||
            snapshot.nextChargeAt != normalizedSnapshot.nextChargeAt ||
            snapshot.openedPackCount != normalizedSnapshot.openedPackCount ||
            snapshot.tamperFlag ||
            trustedTime.tamperDetected

        val needsRewrite = forcePersist ||
            snapshot != normalizedSnapshot
        if (needsRewrite) {
            writeSnapshot(normalizedSnapshot)
            clearLegacyProgress()
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

    private suspend fun migrateLegacyPreferences(preferences: Preferences): ProgressSnapshot? {
        val storedCollectionJson = preferences[LegacyKeys.collectionJson]
        val availableDrawCount = preferences[LegacyKeys.availableDrawCount] ?: settings.maxStoredDraws
        val nextChargeAt = preferences[LegacyKeys.nextChargeAt] ?: preferences[LegacyKeys.legacyNextDrawAt]
        val openedPackCount = preferences[LegacyKeys.openedPackCount] ?: 0
        if (
            storedCollectionJson == null &&
            preferences[LegacyKeys.availableDrawCount] == null &&
            nextChargeAt == null &&
            preferences[LegacyKeys.openedPackCount] == null
        ) {
            return null
        }

        val collection = storedCollectionJson?.let(::decodeCollection)
            ?: emptyCollection()
        val timeEvidence = settings.timeSource.now()
        return ProgressSnapshot(
            installId = installIdFactory(),
            collection = collection,
            availableDrawCount = availableDrawCount,
            nextChargeAt = nextChargeAt,
            openedPackCount = openedPackCount,
            lastTrustedWallClockUtc = timeEvidence.wallClockUtc.toString(),
            lastTrustedElapsedRealtimeMs = timeEvidence.elapsedRealtimeMs,
            lastObservedBootMarker = timeEvidence.bootSessionId,
            tamperFlag = false,
        )
    }

    private fun decodeCollection(collectionJson: String): OwnedCollection = try {
        json.decodeFromString(OwnedCollection.serializer(), collectionJson)
    } catch (exception: SerializationException) {
        throw IllegalStateException("Saved progression could not be read.", exception)
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

    private suspend fun clearLegacyProgress() {
        legacyDataStore.edit { preferences ->
            preferences.remove(LegacyKeys.collectionJson)
            preferences.remove(LegacyKeys.availableDrawCount)
            preferences.remove(LegacyKeys.nextChargeAt)
            preferences.remove(LegacyKeys.openedPackCount)
            preferences.remove(LegacyKeys.legacyNextDrawAt)
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

    private object LegacyKeys {
        val collectionJson = stringPreferencesKey("collection_json")
        val availableDrawCount = intPreferencesKey("available_draw_count")
        val nextChargeAt = stringPreferencesKey("next_charge_at")
        val openedPackCount = intPreferencesKey("opened_pack_count")
        val legacyNextDrawAt = stringPreferencesKey("next_draw_at")
    }

    companion object {
        private val CLOCK_TOLERANCE: Duration = Duration.ofMinutes(2)
        private const val COMPROMISED_PROGRESS_MESSAGE =
            "La progression locale semble corrompue. Réinitialise-la pour continuer."
        private const val RECOVERED_PROGRESS_MESSAGE =
            "La progression locale a été sécurisée et certaines données ont été normalisées."

        fun fromContext(
            context: Context,
            catalogRepository: CatalogGateway,
            settings: StandaloneGameSettings = StandaloneGameSettings(),
            progressCipher: ProgressCipher,
        ): ProgressRepository = ProgressRepository(
            secureDataStore = context.secureStandaloneProgressDataStore,
            legacyDataStore = context.legacyStandaloneProgressDataStore,
            catalogRepository = catalogRepository,
            settings = settings,
            progressCipher = progressCipher,
        )
    }
}
