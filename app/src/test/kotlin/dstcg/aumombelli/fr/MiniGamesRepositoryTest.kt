package fr.aumombelli.dstcg

import fr.aumombelli.dstcg.data.MiniGameAttemptConsumeResult
import fr.aumombelli.dstcg.data.MiniGameRewardGrantResult
import fr.aumombelli.dstcg.data.MiniGamesRepository
import fr.aumombelli.dstcg.model.MiniGameDailyState
import fr.aumombelli.dstcg.model.MiniGameDifficulty
import fr.aumombelli.dstcg.model.MiniGameId
import fr.aumombelli.dstcg.model.MiniGameReward
import fr.aumombelli.dstcg.model.MiniGamesProgress
import fr.aumombelli.dstcg.model.OwnedCardEntry
import fr.aumombelli.dstcg.model.OwnedCollection
import fr.aumombelli.dstcg.model.OwnedVariantCount
import fr.aumombelli.dstcg.model.PackRechargeState
import fr.aumombelli.dstcg.model.StandaloneProgress
import fr.aumombelli.dstcg.model.dailyStateFor
import fr.aumombelli.dstcg.model.unlockedDifficultyFor
import java.time.Instant
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MiniGamesRepositoryTest {
    private val now = Instant.parse("2026-05-10T12:00:00Z")

    @Test
    fun `resolved cards are persisted and reused for the same day`() = runTest {
        val fixture = newFixture(
            progress = StandaloneProgress(
                collection = ownedCollection("ALP-002", "BET-001"),
                rechargeState = PackRechargeState(),
            ),
        )

        val first = fixture.repository.prepareResolvedCardsForToday(
            miniGameId = MiniGameId.Quiz,
            slotCount = 1,
        )
        fixture.progressGateway.progress = fixture.progressGateway.progress.copy(
            collection = ownedCollection("BET-001"),
        )

        val second = fixture.repository.prepareResolvedCardsForToday(
            miniGameId = MiniGameId.Quiz,
            slotCount = 1,
        )

        assertEquals(first, second)
        assertEquals(
            first,
            fixture.progressGateway.progress.miniGamesProgress
                .dailyStateFor(MiniGameId.Quiz, "2026-05-10")
                .resolvedCards,
        )
    }

    @Test
    fun `daily state resets by utc date when read`() = runTest {
        val fixture = newFixture(
            progress = StandaloneProgress(
                collection = ownedCollection("ALP-001"),
                rechargeState = PackRechargeState(),
                miniGamesProgress = MiniGamesProgress(
                    dailyStates = mapOf(
                        MiniGameId.Memory to MiniGameDailyState(
                            dateUtc = "2026-05-09",
                            hasPlayed = true,
                            reward = MiniGameReward.fromMinutes(15L),
                        ),
                    ),
                ),
            ),
        )

        val state = fixture.repository.loadMiniGamesState()
        val todayState = state.progress.dailyStateFor(MiniGameId.Memory, state.todayUtc)

        assertEquals("2026-05-10", state.todayUtc)
        assertFalse(todayState.hasPlayed)
        assertEquals(null, todayState.reward)
    }

    @Test
    fun `reward is saved once per game and day`() = runTest {
        val fixture = newFixture(
            progress = StandaloneProgress(
                collection = ownedCollection("ALP-001"),
                rechargeState = PackRechargeState(
                    availableDrawCount = 0,
                    accumulatedChargeUnits = 0L,
                    lastChargeEvaluationAt = now.toString(),
                ),
            ),
        )

        val first = fixture.repository.grantRewardForToday(
            miniGameId = MiniGameId.Memory,
            reward = MiniGameReward.fromMinutes(15L),
        )
        val second = fixture.repository.grantRewardForToday(
            miniGameId = MiniGameId.Memory,
            reward = MiniGameReward.fromMinutes(15L),
        )

        assertTrue(first is MiniGameRewardGrantResult.Granted)
        assertTrue(second is MiniGameRewardGrantResult.AlreadyGranted)
        val dailyState = fixture.progressGateway.progress.miniGamesProgress
            .dailyStateFor(MiniGameId.Memory, "2026-05-10")
        assertTrue(dailyState.hasPlayed)
        assertEquals(MiniGameReward.fromMinutes(15L), dailyState.reward)
    }

    @Test
    fun `attempt is consumed once without granting reward`() = runTest {
        val fixture = newFixture(
            progress = StandaloneProgress(
                collection = ownedCollection("ALP-001"),
                rechargeState = PackRechargeState(),
            ),
        )

        val first = fixture.repository.consumeAttemptForToday(MiniGameId.Memory)
        val second = fixture.repository.consumeAttemptForToday(MiniGameId.Memory)

        assertTrue(first is MiniGameAttemptConsumeResult.Consumed)
        assertTrue(second is MiniGameAttemptConsumeResult.AlreadyConsumed)
        val dailyState = fixture.progressGateway.progress.miniGamesProgress
            .dailyStateFor(MiniGameId.Memory, "2026-05-10")
        assertTrue(dailyState.hasPlayed)
        assertEquals(null, dailyState.reward)
    }

    @Test
    fun `reward can be granted after attempt consumption`() = runTest {
        val fixture = newFixture(
            progress = StandaloneProgress(
                collection = ownedCollection("ALP-001"),
                rechargeState = PackRechargeState(
                    availableDrawCount = 0,
                    accumulatedChargeUnits = 0L,
                    lastChargeEvaluationAt = now.toString(),
                ),
            ),
        )

        fixture.repository.consumeAttemptForToday(MiniGameId.Memory)
        val reward = fixture.repository.grantRewardForToday(
            miniGameId = MiniGameId.Memory,
            reward = MiniGameReward.fromMinutes(30L),
        )

        assertTrue(reward is MiniGameRewardGrantResult.Granted)
        val dailyState = fixture.progressGateway.progress.miniGamesProgress
            .dailyStateFor(MiniGameId.Memory, "2026-05-10")
        assertTrue(dailyState.hasPlayed)
        assertEquals(MiniGameReward.fromMinutes(30L), dailyState.reward)
    }

    @Test
    fun `eligible card filter keeps fallback resolution compatible`() = runTest {
        val fixture = newFixture(
            progress = StandaloneProgress(
                collection = ownedCollection("ALP-001", "BET-001"),
                rechargeState = PackRechargeState(),
            ),
        )

        val resolvedCards = fixture.repository.prepareResolvedCardsForToday(
            miniGameId = MiniGameId.Timeline,
            slotCount = 2,
            eligibleCardIds = setOf("ALP-001"),
            distinctOwnedCards = true,
        )

        assertEquals(listOf("ALP-001"), resolvedCards.map { it.ownedVariant.cardId })
    }

    @Test
    fun `unlock difficulty only moves forward`() = runTest {
        val fixture = newFixture(
            progress = StandaloneProgress(
                collection = ownedCollection("ALP-001"),
                rechargeState = PackRechargeState(),
                miniGamesProgress = MiniGamesProgress(
                    unlockedDifficulties = mapOf(MiniGameId.Memory to MiniGameDifficulty.Scientist),
                ),
            ),
        )

        fixture.repository.unlockDifficulty(MiniGameId.Memory, MiniGameDifficulty.Observer)
        fixture.repository.unlockDifficulty(MiniGameId.Memory, MiniGameDifficulty.Explorer)

        assertEquals(
            MiniGameDifficulty.Explorer,
            fixture.progressGateway.progress.miniGamesProgress.unlockedDifficultyFor(MiniGameId.Memory),
        )
    }

    private fun newFixture(
        progress: StandaloneProgress,
    ): Fixture {
        val progressGateway = FakeProgressGateway().apply {
            trustedNow = now
            this.progress = progress
        }
        val catalogGateway = FakeCatalogGateway().apply {
            cards = listOf(
                testCardDefinition(id = "ALP-001", extensionId = "alpha"),
                testCardDefinition(id = "ALP-002", extensionId = "alpha"),
                testCardDefinition(id = "BET-001", extensionId = "beta"),
            )
        }
        return Fixture(
            progressGateway = progressGateway,
            repository = MiniGamesRepository(
                progressRepository = progressGateway,
                catalogRepository = catalogGateway,
                settings = testGameSettings(now = now),
            ),
        )
    }

    private data class Fixture(
        val progressGateway: FakeProgressGateway,
        val repository: MiniGamesRepository,
    )

    private fun ownedCollection(vararg cardIds: String): OwnedCollection = OwnedCollection(
        cards = cardIds.associateWith {
            OwnedCardEntry(
                totalOwned = 1,
                variants = listOf(
                    OwnedVariantCount(
                        skyQuality = "city",
                        finish = "standard",
                        count = 1,
                    ),
                ),
            )
        },
    )
}
