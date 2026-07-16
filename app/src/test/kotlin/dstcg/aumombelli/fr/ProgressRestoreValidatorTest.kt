package fr.aumombelli.dstcg

import fr.aumombelli.dstcg.data.ProgressRestoreValidationException
import fr.aumombelli.dstcg.data.ProgressRestoreValidator
import fr.aumombelli.dstcg.model.ExtensionDefinition
import fr.aumombelli.dstcg.model.MiniGameDailyState
import fr.aumombelli.dstcg.model.MiniGameGlobalCardRef
import fr.aumombelli.dstcg.model.MiniGameId
import fr.aumombelli.dstcg.model.MiniGameOwnedVariantRef
import fr.aumombelli.dstcg.model.MiniGameResolvedCardRef
import fr.aumombelli.dstcg.model.MiniGameCardResolutionSource
import fr.aumombelli.dstcg.model.MiniGamesProgress
import fr.aumombelli.dstcg.model.OwnedCardEntry
import fr.aumombelli.dstcg.model.OwnedCollection
import fr.aumombelli.dstcg.model.OwnedVariantCount
import fr.aumombelli.dstcg.model.StandaloneProgress
import kotlinx.coroutines.test.runTest
import org.junit.Test

class ProgressRestoreValidatorTest {
    @Test(expected = ProgressRestoreValidationException::class)
    fun `unknown variant is rejected`() = runTest {
        val validator = validator()

        validator.validate(
            StandaloneProgress(
                collection = OwnedCollection(
                    mapOf(
                        "ALP-001" to OwnedCardEntry(
                            totalOwned = 1,
                            variants = listOf(OwnedVariantCount("unknown", "standard", 1)),
                        ),
                    ),
                ),
            ),
        )
    }

    @Test(expected = ProgressRestoreValidationException::class)
    fun `unknown mini game card is rejected`() = runTest {
        val validator = validator()
        val resolvedCard = MiniGameResolvedCardRef(
            globalCard = MiniGameGlobalCardRef("UNKNOWN", "astronomes-en-herbe"),
            ownedVariant = MiniGameOwnedVariantRef(
                cardId = "ALP-001",
                extensionId = "astronomes-en-herbe",
                skyQuality = "city",
                finish = "standard",
            ),
            source = MiniGameCardResolutionSource.GlobalCard,
        )

        validator.validate(
            StandaloneProgress(
                collection = OwnedCollection(),
                miniGamesProgress = MiniGamesProgress(
                    dailyStates = mapOf(
                        MiniGameId.Memory to MiniGameDailyState(resolvedCards = listOf(resolvedCard)),
                    ),
                ),
            ),
        )
    }

    private fun validator(): ProgressRestoreValidator = ProgressRestoreValidator(
        FakeCatalogGateway().apply {
            extensions = listOf(
                ExtensionDefinition("astronomes-en-herbe", "Astronomes en herbe", "cover"),
            )
            cards = listOf(testCardDefinition("ALP-001"))
        },
    )
}
