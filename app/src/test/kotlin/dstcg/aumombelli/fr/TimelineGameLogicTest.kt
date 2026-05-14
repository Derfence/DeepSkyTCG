package fr.aumombelli.dstcg

import fr.aumombelli.dstcg.feature.minigames.TimelineCriterion
import fr.aumombelli.dstcg.feature.minigames.TimelineGameBuildResult
import fr.aumombelli.dstcg.feature.minigames.buildTimelineGame
import fr.aumombelli.dstcg.feature.minigames.calculateTimelineReward
import fr.aumombelli.dstcg.feature.minigames.eligibleTimelineCardIds
import fr.aumombelli.dstcg.feature.minigames.selectPlayableTimelineCriterion
import fr.aumombelli.dstcg.model.AbsoluteMagnitudeMeasurement
import fr.aumombelli.dstcg.model.AngularMeasurement
import fr.aumombelli.dstcg.model.CardDefinition
import fr.aumombelli.dstcg.model.DeepSkyDetails
import fr.aumombelli.dstcg.model.ExtensionDefinition
import fr.aumombelli.dstcg.model.LightYearMeasurement
import fr.aumombelli.dstcg.model.MiniGameCardResolutionSource
import fr.aumombelli.dstcg.model.MiniGameGlobalCardRef
import fr.aumombelli.dstcg.model.MiniGameOwnedVariantRef
import fr.aumombelli.dstcg.model.MiniGameResolvedCardRef
import fr.aumombelli.dstcg.model.MiniGameReward
import fr.aumombelli.dstcg.model.SolarSystemDetails
import fr.aumombelli.dstcg.model.VisualSize
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TimelineGameLogicTest {
    private val extension = ExtensionDefinition(
        id = "alpha",
        name = "Alpha",
        coverImageRef = "cover",
    )

    @Test
    fun `distance criterion sorts nearest to farthest`() {
        val cards = listOf(
            deepSkyCard("ALP-003", distance = 300.0),
            deepSkyCard("ALP-001", distance = 100.0),
            deepSkyCard("ALP-002", distance = 200.0),
        )

        val game = buildReadyGame(
            criterion = TimelineCriterion.StellarDistance,
            cards = cards,
            resolvedCards = cards.map { resolved(it.id) },
        )

        assertEquals(
            listOf("ALP-001", "ALP-002", "ALP-003"),
            game.correctOrder.map { it.id },
        )
    }

    @Test
    fun `criterion eligibility excludes incompatible cards`() {
        val deepSky = deepSkyCard("ALP-001")
        val solar = solarSystemCard("SOL-001")

        val deepSkySizeIds = eligibleTimelineCardIds(
            criterion = TimelineCriterion.DeepSkyRealSize,
            cards = listOf(deepSky, solar),
        )
        val solarDiameterIds = eligibleTimelineCardIds(
            criterion = TimelineCriterion.SolarSystemDiameter,
            cards = listOf(deepSky, solar),
        )

        assertEquals(setOf("ALP-001"), deepSkySizeIds)
        assertEquals(setOf("SOL-001"), solarDiameterIds)
    }

    @Test
    fun `playable criterion falls back to sky position when primary criteria cannot use two owned cards`() {
        val deepSky = deepSkyCard("ALP-001")
        val solarWithoutDiameter = solarSystemCard("SOL-001", diameter = null)

        val criterion = selectPlayableTimelineCriterion(
            dateUtc = "2026-05-10",
            cards = listOf(deepSky, solarWithoutDiameter),
            ownedCardIds = setOf(deepSky.id, solarWithoutDiameter.id),
        )

        assertEquals(TimelineCriterion.SkyPosition, criterion)
    }

    @Test
    fun `timeline prefers five cards but accepts smaller playable hands`() {
        val sixCards = (1..6).map { index ->
            deepSkyCard("ALP-${index.toString().padStart(3, '0')}", distance = index.toDouble())
        }
        val threeCards = sixCards.take(3)

        val fiveCardGame = buildReadyGame(
            criterion = TimelineCriterion.StellarDistance,
            cards = sixCards,
            resolvedCards = sixCards.map { resolved(it.id) },
        )
        val threeCardGame = buildReadyGame(
            criterion = TimelineCriterion.StellarDistance,
            cards = threeCards,
            resolvedCards = threeCards.map { resolved(it.id) },
        )

        assertEquals(5, fiveCardGame.cards.size)
        assertEquals(3, threeCardGame.cards.size)
    }

    @Test
    fun `timeline is unavailable below two compatible cards`() {
        val card = deepSkyCard("ALP-001")

        val result = buildTimelineGame(
            criterion = TimelineCriterion.StellarDistance,
            dateUtc = "2026-05-10",
            resolvedCards = listOf(resolved(card.id)),
            cards = listOf(card),
            extensions = listOf(extension),
            variantProfiles = testVariantProfiles(),
        )

        assertTrue(result is TimelineGameBuildResult.Unavailable)
    }

    @Test
    fun `hand order is deterministic even when inputs are reordered`() {
        val cards = (1..5).map { index ->
            deepSkyCard("ALP-${index.toString().padStart(3, '0')}", distance = index.toDouble())
        }

        val first = buildReadyGame(
            criterion = TimelineCriterion.StellarDistance,
            cards = cards,
            resolvedCards = cards.map { resolved(it.id) },
        )
        val second = buildReadyGame(
            criterion = TimelineCriterion.StellarDistance,
            cards = cards.reversed(),
            resolvedCards = cards.reversed().map { resolved(it.id) },
        )

        assertEquals(first.cards.map { it.id }, second.cards.map { it.id })
        assertEquals(first.correctOrder.map { it.id }, second.correctOrder.map { it.id })
    }

    @Test
    fun `timeline reward keeps half base plus proportional score`() {
        val reward = calculateTimelineReward(
            correctCount = 1,
            totalCount = 5,
        )

        assertEquals(MiniGameReward.fromSeconds(2_160L), reward)
    }

    private fun buildReadyGame(
        criterion: TimelineCriterion,
        cards: List<CardDefinition>,
        resolvedCards: List<MiniGameResolvedCardRef>,
    ) = when (
        val result = buildTimelineGame(
            criterion = criterion,
            dateUtc = "2026-05-10",
            resolvedCards = resolvedCards,
            cards = cards,
            extensions = listOf(extension),
            variantProfiles = testVariantProfiles(),
        )
    ) {
        is TimelineGameBuildResult.Ready -> result.game
        is TimelineGameBuildResult.Unavailable -> error(result.message)
    }

    private fun resolved(cardId: String): MiniGameResolvedCardRef = MiniGameResolvedCardRef(
        globalCard = MiniGameGlobalCardRef(
            cardId = cardId,
            extensionId = extension.id,
        ),
        ownedVariant = MiniGameOwnedVariantRef(
            cardId = cardId,
            extensionId = extension.id,
            skyQuality = "city",
            finish = "standard",
        ),
        source = MiniGameCardResolutionSource.GlobalCard,
    )

    private fun deepSkyCard(
        id: String,
        distance: Double = 100.0,
        realSize: Double = 10.0,
        visualWidth: Double = 1.0,
        visualHeight: Double = 1.0,
        absoluteMagnitude: Double = -1.0,
    ): CardDefinition {
        val base = testCardDefinition(
            id = id,
            extensionId = extension.id,
            name = id,
        )
        return base.copy(
            astronomy = base.astronomy.copy(
                objectFamily = "deep_sky",
                details = DeepSkyDetails(
                    distance = LightYearMeasurement(distance, "$distance années-lumière"),
                    realSize = LightYearMeasurement(realSize, "$realSize années-lumière"),
                    visualSize = visualSize(visualWidth, visualHeight),
                    absoluteMagnitude = AbsoluteMagnitudeMeasurement(absoluteMagnitude, absoluteMagnitude.toString()),
                ),
            ),
        )
    }

    private fun solarSystemCard(
        id: String,
        diameter: Double? = 12_742.0,
    ): CardDefinition {
        val base = testCardDefinition(
            id = id,
            extensionId = extension.id,
            name = id,
        )
        return base.copy(
            astronomy = base.astronomy.copy(
                objectFamily = "solar_system",
                details = SolarSystemDetails(
                    realSize = diameter?.let { LightYearMeasurement(it, "$it km") },
                ),
            ),
        )
    }

    private fun visualSize(
        width: Double,
        height: Double,
    ): VisualSize = VisualSize(
        fullMoonWidth = width,
        fullMoonHeight = height,
        angularWidth = AngularMeasurement(0, 30, 0, "0°30′00″"),
        angularHeight = AngularMeasurement(0, 30, 0, "0°30′00″"),
        label = "$width × $height",
    )
}
