package fr.aumombelli.dstcg

import fr.aumombelli.dstcg.feature.minigames.ObservatoryDifficultySpec
import fr.aumombelli.dstcg.feature.minigames.ObservatoryGameBuildResult
import fr.aumombelli.dstcg.feature.minigames.buildObservatoryGame
import fr.aumombelli.dstcg.feature.minigames.isObservatorySettingReady
import fr.aumombelli.dstcg.model.ExtensionDefinition
import fr.aumombelli.dstcg.model.MiniGameCardResolutionSource
import fr.aumombelli.dstcg.model.MiniGameDifficulty
import fr.aumombelli.dstcg.model.MiniGameGlobalCardRef
import fr.aumombelli.dstcg.model.MiniGameOwnedVariantRef
import fr.aumombelli.dstcg.model.MiniGameResolvedCardRef
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ObservatoryGameLogicTest {
    private val extension = ExtensionDefinition(
        id = "alpha",
        name = "Alpha",
        coverImageRef = "cover",
    )

    @Test
    fun `difficulty controls target count and tolerance`() {
        assertEquals(1, ObservatoryDifficultySpec.forDifficulty(MiniGameDifficulty.Apprentice).targetCount)
        assertEquals(2, ObservatoryDifficultySpec.forDifficulty(MiniGameDifficulty.Observer).targetCount)
        assertEquals(3, ObservatoryDifficultySpec.forDifficulty(MiniGameDifficulty.Scientist).targetCount)
        assertEquals(4, ObservatoryDifficultySpec.forDifficulty(MiniGameDifficulty.Explorer).targetCount)

        assertEquals(0.06f, ObservatoryDifficultySpec.forDifficulty(MiniGameDifficulty.Apprentice).tolerance)
        assertEquals(0.045f, ObservatoryDifficultySpec.forDifficulty(MiniGameDifficulty.Observer).tolerance)
        assertEquals(0.03f, ObservatoryDifficultySpec.forDifficulty(MiniGameDifficulty.Scientist).tolerance)
        assertEquals(0.02f, ObservatoryDifficultySpec.forDifficulty(MiniGameDifficulty.Explorer).tolerance)
        assertEquals("±4,5%", ObservatoryDifficultySpec.forDifficulty(MiniGameDifficulty.Observer).precisionLabel)
    }

    @Test
    fun `explorer game uses four distinct targets`() {
        val result = buildReadyGame(MiniGameDifficulty.Explorer)

        assertEquals(4, result.targets.size)
        assertEquals(4, result.targets.map { it.id }.toSet().size)
    }

    @Test
    fun `settings are stable for the same day`() {
        val first = buildReadyGame(MiniGameDifficulty.Scientist)
        val second = buildReadyGame(MiniGameDifficulty.Scientist)

        assertEquals(first.targets.map { it.azimuth }, second.targets.map { it.azimuth })
        assertEquals(first.targets.map { it.altitude }, second.targets.map { it.altitude })
        assertEquals(first.targets.map { it.focus }, second.targets.map { it.focus })
    }

    @Test
    fun `setting readiness follows tolerance`() {
        assertTrue(isObservatorySettingReady(value = 0.50f, target = 0.55f, tolerance = 0.06f))
        assertFalse(isObservatorySettingReady(value = 0.50f, target = 0.57f, tolerance = 0.06f))
    }

    @Test
    fun `game is unavailable when distinct cards are missing`() {
        val cards = listOf(testCardDefinition(id = "ALP-001", extensionId = extension.id))
        val result = buildObservatoryGame(
            difficulty = MiniGameDifficulty.Observer,
            dateUtc = "2026-05-10",
            resolvedCards = listOf(
                resolved("ALP-001"),
                resolved("ALP-001"),
            ),
            cards = cards,
            extensions = listOf(extension),
            variantProfiles = testVariantProfiles(),
        )

        assertTrue(result is ObservatoryGameBuildResult.Unavailable)
    }

    private fun buildReadyGame(difficulty: MiniGameDifficulty) = when (
        val result = buildObservatoryGame(
            difficulty = difficulty,
            dateUtc = "2026-05-10",
            resolvedCards = (1..4).map { index ->
                resolved("ALP-${index.toString().padStart(3, '0')}")
            },
            cards = (1..4).map { index ->
                testCardDefinition(
                    id = "ALP-${index.toString().padStart(3, '0')}",
                    extensionId = extension.id,
                    name = "Carte $index",
                )
            },
            extensions = listOf(extension),
            variantProfiles = testVariantProfiles(),
        )
    ) {
        is ObservatoryGameBuildResult.Ready -> result.game
        is ObservatoryGameBuildResult.Unavailable -> error(result.message)
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
}
