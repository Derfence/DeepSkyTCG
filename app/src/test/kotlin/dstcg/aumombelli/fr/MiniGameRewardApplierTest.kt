package fr.aumombelli.dstcg

import fr.aumombelli.dstcg.data.DeterministicWeatherCalendar
import fr.aumombelli.dstcg.data.MiniGameRewardApplier
import fr.aumombelli.dstcg.data.MiniGameRewardGrantResult
import fr.aumombelli.dstcg.data.WeatherPolicy
import fr.aumombelli.dstcg.data.WeatherState
import fr.aumombelli.dstcg.data.buildPackChargeUiStatus
import fr.aumombelli.dstcg.model.MiniGameDailyState
import fr.aumombelli.dstcg.model.MiniGameId
import fr.aumombelli.dstcg.model.MiniGameReward
import fr.aumombelli.dstcg.model.MiniGamesProgress
import fr.aumombelli.dstcg.model.OwnedCollection
import fr.aumombelli.dstcg.model.PackRechargeState
import fr.aumombelli.dstcg.model.StandaloneProgress
import java.time.Duration
import java.time.Instant
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MiniGameRewardApplierTest {
    private val applier = MiniGameRewardApplier()
    private val now = Instant.parse("2026-05-10T12:00:00Z")
    private val drawCooldown = Duration.ofHours(6)
    private val clearWeather = constantWeather(WeatherState.Clear)
    private val pureWeather = constantWeather(WeatherState.Pure)

    @Test
    fun `reward advances partial recharge by its wall clock duration in clear weather`() {
        val result = applier.grantReward(
            progress = StandaloneProgress(
                collection = OwnedCollection(),
                rechargeState = PackRechargeState(
                    availableDrawCount = 0,
                    accumulatedChargeUnits = 0L,
                    lastChargeEvaluationAt = now.toString(),
                ),
            ),
            miniGameId = MiniGameId.Memory,
            todayUtc = "2026-05-10",
            reward = MiniGameReward.fromMinutes(60L),
            now = now,
            drawCooldown = drawCooldown,
            maxStoredDraws = 10,
            weatherPolicy = clearWeather,
        )

        require(result is MiniGameRewardGrantResult.Granted)
        assertEquals(0, result.rechargeState.availableDrawCount)
        assertEquals(18_000L, result.rechargeState.accumulatedChargeUnits)
        assertEquals(now.toString(), result.rechargeState.lastChargeEvaluationAt)
    }

    @Test
    fun `reward keeps the same wall clock reduction under pure weather`() {
        val rechargeState = PackRechargeState(
            availableDrawCount = 0,
            accumulatedChargeUnits = 0L,
            lastChargeEvaluationAt = now.toString(),
        )
        val initialChargeStatus = buildPackChargeUiStatus(
            rechargeState = rechargeState,
            now = now,
            drawCooldown = drawCooldown,
            maxStoredDraws = 10,
            weatherPolicy = pureWeather,
        )

        val result = applier.grantReward(
            progress = StandaloneProgress(
                collection = OwnedCollection(),
                rechargeState = rechargeState,
            ),
            miniGameId = MiniGameId.Memory,
            todayUtc = "2026-05-10",
            reward = MiniGameReward.fromMinutes(60L),
            now = now,
            drawCooldown = drawCooldown,
            maxStoredDraws = 10,
            weatherPolicy = pureWeather,
        )

        require(result is MiniGameRewardGrantResult.Granted)
        val rewardedChargeStatus = buildPackChargeUiStatus(
            rechargeState = result.rechargeState,
            now = now,
            drawCooldown = drawCooldown,
            maxStoredDraws = 10,
            weatherPolicy = pureWeather,
        )

        assertEquals(now.plus(Duration.ofHours(3)).toString(), initialChargeStatus.nextChargeAt)
        assertEquals(now.plus(Duration.ofHours(2)).toString(), rewardedChargeStatus.nextChargeAt)
    }

    @Test
    fun `reward overflow becomes progress toward the next pack`() {
        val result = applier.grantReward(
            progress = StandaloneProgress(
                collection = OwnedCollection(),
                rechargeState = PackRechargeState(
                    availableDrawCount = 0,
                    accumulatedChargeUnits = 99_000L,
                    lastChargeEvaluationAt = now.toString(),
                ),
            ),
            miniGameId = MiniGameId.Memory,
            todayUtc = "2026-05-10",
            reward = MiniGameReward.fromMinutes(60L),
            now = now,
            drawCooldown = drawCooldown,
            maxStoredDraws = 10,
            weatherPolicy = clearWeather,
        )

        require(result is MiniGameRewardGrantResult.Granted)
        assertEquals(1, result.rechargeState.availableDrawCount)
        assertEquals(9_000L, result.rechargeState.accumulatedChargeUnits)
    }

    @Test
    fun `reward applies second precision to recharge`() {
        val result = applier.grantReward(
            progress = StandaloneProgress(
                collection = OwnedCollection(),
                rechargeState = PackRechargeState(
                    availableDrawCount = 0,
                    accumulatedChargeUnits = 0L,
                    lastChargeEvaluationAt = now.toString(),
                ),
            ),
            miniGameId = MiniGameId.Quiz,
            todayUtc = "2026-05-10",
            reward = MiniGameReward.fromSeconds(450L),
            now = now,
            drawCooldown = drawCooldown,
            maxStoredDraws = 10,
            weatherPolicy = clearWeather,
        )

        require(result is MiniGameRewardGrantResult.Granted)
        assertEquals(2_250L, result.rechargeState.accumulatedChargeUnits)
        assertEquals(MiniGameReward.fromSeconds(450L), result.dailyState.reward)
    }

    @Test
    fun `reward caps pack stock at maximum`() {
        val result = applier.grantReward(
            progress = StandaloneProgress(
                collection = OwnedCollection(),
                rechargeState = PackRechargeState(
                    availableDrawCount = 9,
                    accumulatedChargeUnits = 100_000L,
                    lastChargeEvaluationAt = now.toString(),
                ),
            ),
            miniGameId = MiniGameId.Observatory,
            todayUtc = "2026-05-10",
            reward = MiniGameReward.fromMinutes(60L),
            now = now,
            drawCooldown = drawCooldown,
            maxStoredDraws = 10,
            weatherPolicy = clearWeather,
        )

        require(result is MiniGameRewardGrantResult.Granted)
        assertEquals(10, result.rechargeState.availableDrawCount)
        assertEquals(0L, result.rechargeState.accumulatedChargeUnits)
        assertEquals(null, result.rechargeState.lastChargeEvaluationAt)
    }

    @Test
    fun `reward can only be granted once per day and game`() {
        val first = applier.grantReward(
            progress = StandaloneProgress(
                collection = OwnedCollection(),
                rechargeState = PackRechargeState(lastChargeEvaluationAt = now.toString()),
            ),
            miniGameId = MiniGameId.Quiz,
            todayUtc = "2026-05-10",
            reward = MiniGameReward.fromMinutes(15L),
            now = now,
            drawCooldown = drawCooldown,
            maxStoredDraws = 10,
            weatherPolicy = DeterministicWeatherCalendar,
        )
        require(first is MiniGameRewardGrantResult.Granted)

        val second = applier.grantReward(
            progress = StandaloneProgress(
                collection = OwnedCollection(),
                rechargeState = first.rechargeState,
            miniGamesProgress = first.miniGamesProgress,
            ),
            miniGameId = MiniGameId.Quiz,
            todayUtc = "2026-05-10",
            reward = MiniGameReward.fromMinutes(15L),
            now = now,
            drawCooldown = drawCooldown,
            maxStoredDraws = 10,
            weatherPolicy = DeterministicWeatherCalendar,
        )

        assertTrue(second is MiniGameRewardGrantResult.AlreadyGranted)
    }

    @Test
    fun `reward can be granted after attempt was consumed without reward`() {
        val result = applier.grantReward(
            progress = StandaloneProgress(
                collection = OwnedCollection(),
                rechargeState = PackRechargeState(lastChargeEvaluationAt = now.toString()),
                miniGamesProgress = MiniGamesProgress(
                    dailyStates = mapOf(
                        MiniGameId.Memory to MiniGameDailyState(
                            dateUtc = "2026-05-10",
                            hasPlayed = true,
                            reward = null,
                        ),
                    ),
                ),
            ),
            miniGameId = MiniGameId.Memory,
            todayUtc = "2026-05-10",
            reward = MiniGameReward.fromMinutes(30L),
            now = now,
            drawCooldown = drawCooldown,
            maxStoredDraws = 10,
            weatherPolicy = DeterministicWeatherCalendar,
        )

        require(result is MiniGameRewardGrantResult.Granted)
        assertEquals(MiniGameReward.fromMinutes(30L), result.dailyState.reward)
        assertTrue(result.dailyState.hasPlayed)
    }

    @Test
    fun `reward deserializes legacy minute field and writes seconds field`() {
        val json = Json { ignoreUnknownKeys = true }

        val legacyReward = json.decodeFromString(
            MiniGameReward.serializer(),
            """{"reductionMinutes":15}""",
        )
        val encoded = json.encodeToString(MiniGameReward.fromSeconds(450L))

        assertEquals(MiniGameReward.fromMinutes(15L), legacyReward)
        assertTrue(encoded.contains("reductionSeconds"))
        assertTrue(!encoded.contains("reductionMinutes"))
    }

    private fun constantWeather(weatherState: WeatherState): WeatherPolicy = object : WeatherPolicy {
        override fun weatherAt(instant: Instant): WeatherState = weatherState
    }
}
