package fr.aumombelli.dstcg

import fr.aumombelli.dstcg.feature.minigames.QuizGame
import fr.aumombelli.dstcg.feature.minigames.QuizGameBuildResult
import fr.aumombelli.dstcg.feature.minigames.QuizQuestionKind
import fr.aumombelli.dstcg.feature.minigames.buildQuizGame
import fr.aumombelli.dstcg.feature.minigames.calculateQuizReward
import fr.aumombelli.dstcg.model.CardDefinition
import fr.aumombelli.dstcg.model.ExtensionDefinition
import fr.aumombelli.dstcg.model.MiniGameCardResolutionSource
import fr.aumombelli.dstcg.model.MiniGameDifficulty
import fr.aumombelli.dstcg.model.MiniGameGlobalCardRef
import fr.aumombelli.dstcg.model.MiniGameOwnedVariantRef
import fr.aumombelli.dstcg.model.MiniGameResolvedCardRef
import fr.aumombelli.dstcg.model.MiniGameReward
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QuizGameLogicTest {
    private val extension = ExtensionDefinition(
        id = "alpha",
        name = "Alpha",
        coverImageRef = "cover",
    )

    @Test
    fun `quiz uses the resolved card as target`() {
        val cards = quizCards()
        val game = buildReadyQuiz(
            cards = cards,
            resolvedCardId = "ALP-003",
        )

        assertEquals("ALP-003", game.targetCard.definition.id)
    }

    @Test
    fun `quiz questions and answers are deterministic`() {
        val first = buildReadyQuiz(cards = quizCards())
        val second = buildReadyQuiz(cards = quizCards().reversed())

        assertEquals(first.questions, second.questions)
    }

    @Test
    fun `each quiz question has four distinct answers`() {
        val game = buildReadyQuiz(
            difficulty = MiniGameDifficulty.Explorer,
            cards = quizCards(),
        )

        assertEquals(4, game.questions.size)
        assertTrue(game.questions.all { it.answers.size == 4 })
        assertTrue(game.questions.all { it.answers.toSet().size == 4 })
        assertTrue(game.questions.all { it.correctAnswer in it.answers })
    }

    @Test
    fun `quiz skips a question kind without enough distractors`() {
        val cards = quizCards().map { card ->
            card.copy(
                astronomy = card.astronomy.copy(objectTypeLabel = "Même type"),
            )
        }

        val game = buildReadyQuiz(
            difficulty = MiniGameDifficulty.Explorer,
            cards = cards,
        )

        assertFalse(game.questions.any { it.kind == QuizQuestionKind.ObjectType })
        assertEquals(4, game.questions.size)
    }

    @Test
    fun `quiz is unavailable when not enough factual questions can be built`() {
        val cards = quizCards().take(3)
        val result = buildQuizGame(
            difficulty = MiniGameDifficulty.Apprentice,
            dateUtc = "2026-05-10",
            resolvedCards = listOf(resolved("ALP-001")),
            cards = cards,
            extensions = listOf(extension),
            variantProfiles = testVariantProfiles(),
        )

        assertTrue(result is QuizGameBuildResult.Unavailable)
    }

    @Test
    fun `quiz reward keeps half base plus proportional score`() {
        val reward = calculateQuizReward(
            difficulty = MiniGameDifficulty.Explorer,
            correctCount = 1,
            questionCount = 4,
        )

        assertEquals(MiniGameReward.fromSeconds(2_250L), reward)
    }

    private fun buildReadyQuiz(
        difficulty: MiniGameDifficulty = MiniGameDifficulty.Explorer,
        cards: List<CardDefinition>,
        resolvedCardId: String = "ALP-001",
    ): QuizGame = when (
        val result = buildQuizGame(
            difficulty = difficulty,
            dateUtc = "2026-05-10",
            resolvedCards = listOf(resolved(resolvedCardId)),
            cards = cards,
            extensions = listOf(extension),
            variantProfiles = testVariantProfiles(),
        )
    ) {
        is QuizGameBuildResult.Ready -> result.game
        is QuizGameBuildResult.Unavailable -> error(result.message)
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

    private fun quizCards(): List<CardDefinition> {
        val objectTypes = listOf(
            "Nébuleuse",
            "Galaxie",
            "Étoile",
            "Constellation",
            "Amas ouvert",
            "Planète",
        )
        val constellations = listOf("Orion", "Cygne", "Lyre", "Aigle", "Andromède", "Taureau")
        val seasons = listOf("Hiver", "Été", "Printemps", "Automne", "Toute l'année", "Fin d'été")
        val catalogs = listOf("Messier", "Bayer", "NGC", "IAU", "Caldwell", "Système solaire")
        return objectTypes.indices.map { index ->
            val id = "ALP-${(index + 1).toString().padStart(3, '0')}"
            val base = testCardDefinition(
                id = id,
                extensionId = extension.id,
                name = "Carte ${index + 1}",
            )
            base.copy(
                astronomy = base.astronomy.copy(
                    objectTypeLabel = objectTypes[index],
                    constellation = constellations[index],
                    mainSeason = seasons[index],
                    primaryCatalogName = catalogs[index],
                    catalogNumber = "REF-${index + 1}",
                ),
            )
        }
    }
}
