package fr.aumombelli.dstcg

import fr.aumombelli.dstcg.feature.minigames.QuizGame
import fr.aumombelli.dstcg.feature.minigames.QuizGameBuildResult
import fr.aumombelli.dstcg.feature.minigames.QuizQuestionKind
import fr.aumombelli.dstcg.feature.minigames.buildQuizGame
import fr.aumombelli.dstcg.feature.minigames.calculateQuizReward
import fr.aumombelli.dstcg.model.AstronomyDetails
import fr.aumombelli.dstcg.model.CardDefinition
import fr.aumombelli.dstcg.model.ConstellationDetails
import fr.aumombelli.dstcg.model.DeepSkyDetails
import fr.aumombelli.dstcg.model.ExtensionDefinition
import fr.aumombelli.dstcg.model.MiniGameCardResolutionSource
import fr.aumombelli.dstcg.model.MiniGameDifficulty
import fr.aumombelli.dstcg.model.MiniGameGlobalCardRef
import fr.aumombelli.dstcg.model.MiniGameOwnedVariantRef
import fr.aumombelli.dstcg.model.MiniGameResolvedCardRef
import fr.aumombelli.dstcg.model.MiniGameReward
import fr.aumombelli.dstcg.model.SkyEventDetails
import fr.aumombelli.dstcg.model.SolarSystemDetails
import fr.aumombelli.dstcg.model.StarDetails
import java.io.File
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QuizGameLogicTest {
    private val astronomyDetailsSerializersModule = SerializersModule {
        polymorphic(AstronomyDetails::class) {
            subclass(DeepSkyDetails::class)
            subclass(StarDetails::class)
            subclass(ConstellationDetails::class)
            subclass(SkyEventDetails::class)
            subclass(SolarSystemDetails::class)
        }
    }

    private val json = Json {
        ignoreUnknownKeys = true
        classDiscriminator = "detailType"
        serializersModule = astronomyDetailsSerializersModule
    }

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
    fun `synthetic distractors keep quiz playable when catalog cannot help`() {
        val cards = listOf(quizCards().first())

        val game = buildReadyQuiz(
            difficulty = MiniGameDifficulty.Explorer,
            cards = cards,
        )

        assertEquals(4, game.questions.size)
        assertTrue(game.questions.all { it.answers.size == 4 })
        assertTrue(game.questions.all { it.answers.toSet().size == 4 })
    }

    @Test
    fun `current catalog cards generate every quiz difficulty`() {
        val cards = catalogCards()

        cards.forEach { card ->
            MiniGameDifficulty.entries.forEach { difficulty ->
                val game = buildReadyQuiz(
                    difficulty = difficulty,
                    cards = cards,
                    resolvedCardId = card.id,
                    resolvedExtensionId = card.extensionId,
                )

                assertEquals(card.id, game.targetCard.definition.id)
                assertEquals(difficulty.level, game.questions.size)
                assertTrue(game.questions.all { it.answers.size == 4 })
                assertTrue(game.questions.all { it.answers.toSet().size == 4 })
                assertTrue(game.questions.all { it.correctAnswer in it.answers })
            }
        }
    }

    @Test
    fun `solar system cards with variable placement do not ask fixed constellation`() {
        val cards = catalogCards()
        val variableSolarCards = cards.filter { card ->
            card.astronomy.objectFamily == "solar_system" &&
                card.astronomy.constellation.equals("Variable", ignoreCase = true)
        }

        assertTrue(variableSolarCards.isNotEmpty())
        variableSolarCards.forEach { card ->
            val game = buildReadyQuiz(
                difficulty = MiniGameDifficulty.Explorer,
                cards = cards,
                resolvedCardId = card.id,
                resolvedExtensionId = card.extensionId,
            )

            assertFalse(game.questions.any { it.kind == QuizQuestionKind.Constellation })
        }
    }

    @Test
    fun `constellation cards do not ask their own constellation`() {
        val cards = catalogCards()
        val constellationCards = cards.filter { card ->
            card.astronomy.objectFamily == "constellation"
        }

        assertTrue(constellationCards.isNotEmpty())
        constellationCards.forEach { card ->
            val game = buildReadyQuiz(
                difficulty = MiniGameDifficulty.Explorer,
                cards = cards,
                resolvedCardId = card.id,
                resolvedExtensionId = card.extensionId,
            )

            assertFalse(game.questions.any { it.kind == QuizQuestionKind.Constellation })
        }
    }

    @Test
    fun `quiz never exposes exact coordinates`() {
        val game = buildReadyQuiz(
            difficulty = MiniGameDifficulty.Explorer,
            cards = catalogCards(),
            resolvedCardId = "aeh-m42-orion-nebula",
            resolvedExtensionId = "astronomes-en-herbe",
        )

        val visibleText = game.questions.flatMap { question ->
            listOf(question.prompt, question.correctAnswer, question.explanation) + question.answers
        }
        assertTrue(visibleText.none { it.contains("AD ") })
        assertTrue(visibleText.none { it.contains("Dec ") })
        assertTrue(visibleText.none { it.contains("°") })
        assertTrue(visibleText.none { it.contains("′") })
        assertTrue(visibleText.none { it.contains("″") })
    }

    @Test
    fun `quiz questions carry short explanations`() {
        val game = buildReadyQuiz(
            difficulty = MiniGameDifficulty.Explorer,
            cards = quizCards(),
        )

        assertTrue(game.questions.all { it.explanation.isNotBlank() })
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
        resolvedExtensionId: String = extension.id,
    ): QuizGame = when (
        val result = buildQuizGame(
            difficulty = difficulty,
            dateUtc = "2026-05-10",
            resolvedCards = listOf(resolved(resolvedCardId, resolvedExtensionId)),
            cards = cards,
            extensions = listOf(extension),
            variantProfiles = testVariantProfiles(),
        )
    ) {
        is QuizGameBuildResult.Ready -> result.game
        is QuizGameBuildResult.Unavailable -> error(result.message)
    }

    private fun resolved(
        cardId: String,
        extensionId: String = extension.id,
    ): MiniGameResolvedCardRef = MiniGameResolvedCardRef(
        globalCard = MiniGameGlobalCardRef(
            cardId = cardId,
            extensionId = extensionId,
        ),
        ownedVariant = MiniGameOwnedVariantRef(
            cardId = cardId,
            extensionId = extensionId,
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

    private fun catalogCards(): List<CardDefinition> {
        val file = listOf(
            File("app/src/main/assets/catalog/cards.json"),
            File("src/main/assets/catalog/cards.json"),
        ).first(File::exists)
        return json.decodeFromString<List<CardDefinition>>(file.readText())
    }
}
