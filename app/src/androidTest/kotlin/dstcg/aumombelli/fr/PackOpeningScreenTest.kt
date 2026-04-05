package fr.aumombelli.dstcg

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.dp
import fr.aumombelli.dstcg.feature.packs.opening.EquipmentPackRevealUiItem
import fr.aumombelli.dstcg.model.CardDefinition
import fr.aumombelli.dstcg.model.DrawPackResponse
import fr.aumombelli.dstcg.model.EquipmentPackRevealSlot
import fr.aumombelli.dstcg.model.EquipmentType
import fr.aumombelli.dstcg.model.PackCard
import fr.aumombelli.dstcg.model.toDisplayCard
import fr.aumombelli.dstcg.model.toDisplayVariant
import fr.aumombelli.dstcg.testsupport.androidTestRechargeStateWithNextChargeAt
import fr.aumombelli.dstcg.ui.component.TRADING_CARD_WIDTH_OVER_HEIGHT
import fr.aumombelli.dstcg.ui.screen.PackOpeningScreen
import fr.aumombelli.dstcg.ui.viewmodel.PackOpeningUiState
import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class PackOpeningScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun pack_opening_reveals_cards_supports_swipe_and_fullscreen() {
        val firstCard = testCardDefinition("ALP-001", name = "Nebuleuse d'Orion")
        val secondCard = testCardDefinition("ALP-002", name = "Galaxie d'Andromede")
        val packResult = DrawPackResponse.fromCards(
            extensionId = "astronomes-en-herbe",
            drawnAt = "2026-03-23T12:00:00Z",
            rechargeState = androidTestRechargeStateWithNextChargeAt(
                availableDrawCount = 9,
                nextChargeAt = "2026-03-24T18:00:00Z",
            ),
            cards = listOf(
                testPackCard("ALP-001", "Nebuleuse d'Orion", "Common", "spark_fox"),
                testPackCard(
                    "ALP-002",
                    "Galaxie d'Andromede",
                    "Rare",
                    "steam_golem",
                    skyQuality = "rural",
                    skyQualityLabel = "Campagne",
                ),
            ),
        )

        var doneCallCount = 0
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            PackOpeningScreen(
                state = PackOpeningUiState(
                    packResult = packResult,
                    displayCards = listOf(
                        firstCard.toDisplayCard(
                            extensionName = "Astronomes en herbe",
                            activeVariant = packResult.cards[0].variant.toDisplayVariant(),
                        ),
                        secondCard.toDisplayCard(
                            extensionName = "Astronomes en herbe",
                            activeVariant = packResult.cards[1].variant.toDisplayVariant(),
                        ),
                    ),
                    highestBurstRarity = "Rare",
                    hasHolographicBurst = false,
                ),
                onDone = { doneCallCount += 1 },
            )
        }

        composeRule.mainClock.advanceTimeBy(1_200)
        composeRule.runOnIdle { }
        composeRule.onNodeWithTag("pack-opening-booster").assertIsDisplayed()
        composeRule.assertApproxCardRatio("pack-opening-booster")
        composeRule.onAllNodesWithText("Booster").assertCountEquals(0)

        composeRule.mainClock.advanceTimeBy(4_500)
        composeRule.runOnIdle { }

        composeRule.onNodeWithTag("pack-opening-burst").assertIsDisplayed()
        composeRule.onNodeWithTag("pack-opening-progress").assertIsDisplayed()
        composeRule.onAllNodesWithTag("pack-opening-card-id").assertCountEquals(0)
        assertEquals("ALP-001", composeRule.readCurrentPackOpeningCardId())
        composeRule.onAllNodesWithTag("pack-opening-arrow-left").assertCountEquals(0)
        composeRule.onAllNodesWithTag("pack-opening-arrow-right").assertCountEquals(1)
        composeRule.onAllNodesWithTag("pack-opening-card-name").assertCountEquals(0)
        composeRule.assertApproxCardRatio("pack-opening-current-card-surface")
        composeRule.mainClock.autoAdvance = true
        composeRule.firstNodeWithTag("pack-opening-current-card-surface").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag("astro-card-fullscreen-close")
                .fetchSemanticsNodes(atLeastOneRootRequired = false)
                .isNotEmpty()
        }
        composeRule.onNodeWithTag("astro-card-fullscreen-close").assertIsDisplayed()
        composeRule.onNodeWithTag("astro-card-fullscreen-close").performClick()
        composeRule.runOnIdle { }
        assertEquals("ALP-001", composeRule.readCurrentPackOpeningCardId())

        composeRule.firstNodeWithTag("pack-opening-current-card-surface").performTouchInput { swipeLeft() }
        composeRule.waitUntil(timeoutMillis = 5_000) {
            runCatching {
                composeRule.readCurrentPackOpeningCardId() == "ALP-002"
            }.getOrDefault(false)
        }
        assertEquals("ALP-002", composeRule.readCurrentPackOpeningCardId())
        composeRule.onAllNodesWithTag("pack-opening-arrow-left").assertCountEquals(1)
        composeRule.onAllNodesWithTag("pack-opening-arrow-right").assertCountEquals(0)

        composeRule.mainClock.autoAdvance = false
        composeRule.firstNodeWithTag("pack-opening-current-card-surface").performTouchInput { swipeUp() }
        composeRule.mainClock.advanceTimeBy(1_000)
        composeRule.runOnIdle { }
        composeRule.mainClock.autoAdvance = true
        assertEquals(1, doneCallCount)
    }

    @Test
    fun pack_opening_holographic_burst_adds_falling_stars() {
        val holoCard = testCardDefinition("ALP-777", rarityLabel = "Epic")
        val packResult = DrawPackResponse.fromCards(
            extensionId = "astronomes-en-herbe",
            drawnAt = "2026-03-23T12:00:00Z",
            rechargeState = androidTestRechargeStateWithNextChargeAt(
                availableDrawCount = 9,
                nextChargeAt = "2026-03-24T18:00:00Z",
            ),
            cards = listOf(
                testPackCard(
                    "ALP-777",
                    "Comete",
                    "Epic",
                    "comet",
                    finish = "holographic",
                    finishLabel = "Holographique",
                    isHolographic = true,
                ),
            ),
        )

        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            PackOpeningScreen(
                state = PackOpeningUiState(
                    packResult = packResult,
                    displayCards = listOf(
                        holoCard.toDisplayCard(
                            extensionName = "Astronomes en herbe",
                            activeVariant = packResult.cards[0].variant.toDisplayVariant(),
                        ),
                    ),
                    highestBurstRarity = "Epic",
                    hasHolographicBurst = true,
                ),
                onDone = {},
            )
        }

        composeRule.advanceToRevealedCards()

        composeRule.onNodeWithTag("pack-opening-burst").assertIsDisplayed()
        composeRule.onNodeWithTag("pack-opening-burst-rain").assertIsDisplayed()
    }

    @Test
    fun pack_opening_renders_equipment_reward_cards() {
        val definition = testEquipmentCardDefinition(
            id = "mount-advanced",
            type = EquipmentType.Mount,
            displayName = "Monture Niveau 2",
            level = 2,
            packsAffected = 4,
            bonusValue = 18.0,
            description = "Augmente nettement la chance de promotion de rarete.",
        )
        val packResult = DrawPackResponse(
            extensionId = "astronomes-en-herbe",
            drawnAt = "2026-03-23T12:00:00Z",
            rechargeState = androidTestRechargeStateWithNextChargeAt(
                availableDrawCount = 9,
                nextChargeAt = "2026-03-24T18:00:00Z",
            ),
            revealSlots = listOf(
                EquipmentPackRevealSlot(
                    slotIndex = 0,
                    definition = definition,
                ),
            ),
        )

        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            PackOpeningScreen(
                state = PackOpeningUiState(
                    packResult = packResult,
                    revealItems = listOf(EquipmentPackRevealUiItem(definition)),
                ),
                onDone = {},
            )
        }

        composeRule.advanceToRevealedCards()

        composeRule.onNodeWithTag("pack-opening-current-card-surface").assertIsDisplayed()
        assertEquals(definition.id, composeRule.readCurrentPackOpeningCardId())
        composeRule.onNodeWithText("Monture").assertIsDisplayed()
        composeRule.onNodeWithText("Monture Niveau 2").assertIsDisplayed()
        composeRule.onNodeWithText("Actif pendant 4 packs").assertIsDisplayed()
    }

    @Test
    fun pack_opening_unlocks_swipe_hint_for_current_card_after_first_last_card_visit() {
        val firstCard = testCardDefinition("ALP-001", name = "Nebuleuse d'Orion")
        val secondCard = testCardDefinition("ALP-002", name = "Galaxie d'Andromede")
        val thirdCard = testCardDefinition("ALP-003", name = "Amas globulaire")
        val packResult = DrawPackResponse.fromCards(
            extensionId = "astronomes-en-herbe",
            drawnAt = "2026-03-23T12:00:00Z",
            rechargeState = androidTestRechargeStateWithNextChargeAt(
                availableDrawCount = 7,
                nextChargeAt = "2026-03-24T18:00:00Z",
            ),
            cards = listOf(
                testPackCard("ALP-001", "Nebuleuse d'Orion", "Common", "spark_fox"),
                testPackCard("ALP-002", "Galaxie d'Andromede", "Rare", "steam_golem"),
                testPackCard("ALP-003", "Amas globulaire", "Uncommon", "cluster"),
            ),
        )

        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            PackOpeningScreen(
                state = PackOpeningUiState(
                    packResult = packResult,
                    displayCards = listOf(
                        firstCard.toDisplayCard(
                            extensionName = "Astronomes en herbe",
                            activeVariant = packResult.cards[0].variant.toDisplayVariant(),
                        ),
                        secondCard.toDisplayCard(
                            extensionName = "Astronomes en herbe",
                            activeVariant = packResult.cards[1].variant.toDisplayVariant(),
                        ),
                        thirdCard.toDisplayCard(
                            extensionName = "Astronomes en herbe",
                            activeVariant = packResult.cards[2].variant.toDisplayVariant(),
                        ),
                    ),
                    highestBurstRarity = "Rare",
                    hasHolographicBurst = false,
                ),
                onDone = {},
            )
        }

        composeRule.advanceToRevealedCards()
        composeRule.mainClock.autoAdvance = true

        composeRule.firstNodeWithTag("pack-opening-current-card-surface").performTouchInput { swipeLeft() }
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.safeReadCurrentPackOpeningCardId() == "ALP-002"
        }
        composeRule.onAllNodesWithTag("pack-opening-arrow-left").assertCountEquals(1)
        composeRule.onAllNodesWithTag("pack-opening-arrow-right").assertCountEquals(1)
        composeRule.onAllNodesWithTag("pack-opening-last-card-nudge").assertCountEquals(0)

        composeRule.firstNodeWithTag("pack-opening-current-card-surface").performTouchInput { swipeLeft() }
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.safeReadCurrentPackOpeningCardId() == "ALP-003"
        }
        composeRule.onAllNodesWithTag("pack-opening-last-card-nudge").assertCountEquals(0)

        composeRule.firstNodeWithTag("pack-opening-current-card-surface").performTouchInput { swipeRight() }
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.safeReadCurrentPackOpeningCardId() == "ALP-002"
        }

        composeRule.mainClock.autoAdvance = false
        composeRule.mainClock.advanceTimeBy(2_400)
        composeRule.runOnIdle { }
        composeRule.onAllNodesWithTag("pack-opening-last-card-nudge").assertCountEquals(1)
        composeRule.onNodeWithTag("pack-opening-swipe-hint-label").assertIsDisplayed()
    }

    @Test
    fun pack_opening_reveals_unlocked_hint_after_fullscreen_closes() {
        val firstCard = testCardDefinition("ALP-001", name = "Nebuleuse d'Orion")
        val secondCard = testCardDefinition("ALP-002", name = "Galaxie d'Andromede")
        val packResult = DrawPackResponse.fromCards(
            extensionId = "astronomes-en-herbe",
            drawnAt = "2026-03-23T12:00:00Z",
            rechargeState = androidTestRechargeStateWithNextChargeAt(
                availableDrawCount = 7,
                nextChargeAt = "2026-03-24T18:00:00Z",
            ),
            cards = listOf(
                testPackCard("ALP-001", "Nebuleuse d'Orion", "Common", "spark_fox"),
                testPackCard("ALP-002", "Galaxie d'Andromede", "Rare", "steam_golem"),
            ),
        )

        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            PackOpeningScreen(
                state = PackOpeningUiState(
                    packResult = packResult,
                    displayCards = listOf(
                        firstCard.toDisplayCard(
                            extensionName = "Astronomes en herbe",
                            activeVariant = packResult.cards[0].variant.toDisplayVariant(),
                        ),
                        secondCard.toDisplayCard(
                            extensionName = "Astronomes en herbe",
                            activeVariant = packResult.cards[1].variant.toDisplayVariant(),
                        ),
                    ),
                    highestBurstRarity = "Rare",
                    hasHolographicBurst = false,
                ),
                onDone = {},
            )
        }

        composeRule.advanceToRevealedCards()
        composeRule.mainClock.autoAdvance = true
        composeRule.firstNodeWithTag("pack-opening-current-card-surface").performTouchInput { swipeLeft() }
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.safeReadCurrentPackOpeningCardId() == "ALP-002"
        }
        composeRule.firstNodeWithTag("pack-opening-current-card-surface").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("astro-card-fullscreen-close").assertIsDisplayed()

        composeRule.mainClock.autoAdvance = false
        composeRule.mainClock.advanceTimeBy(2_400)
        composeRule.runOnIdle { }
        composeRule.onAllNodesWithTag("pack-opening-last-card-nudge").assertCountEquals(0)

        composeRule.mainClock.autoAdvance = true
        composeRule.onNodeWithTag("astro-card-fullscreen-close").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag("pack-opening-last-card-nudge")
                .fetchSemanticsNodes(atLeastOneRootRequired = false)
                .size == 1
        }
        composeRule.onNodeWithTag("pack-opening-swipe-hint-label").assertIsDisplayed()
    }

    @Test
    fun pack_opening_resets_swipe_hint_for_each_pack() {
        val currentState = mutableStateOf(
            buildPackOpeningState(
                drawnAt = "2026-03-23T12:00:00Z",
                cards = listOf(
                    testPackCard("ALP-001", "Nebuleuse d'Orion", "Common", "spark_fox"),
                    testPackCard("ALP-002", "Galaxie d'Andromede", "Rare", "steam_golem"),
                ),
                definitions = listOf(
                    testCardDefinition("ALP-001", name = "Nebuleuse d'Orion"),
                    testCardDefinition("ALP-002", name = "Galaxie d'Andromede"),
                ),
            ),
        )
        val secondPack = buildPackOpeningState(
            drawnAt = "2026-03-23T12:05:00Z",
            cards = listOf(
                testPackCard("BET-001", "Ciel profond", "Common", "cluster"),
                testPackCard("BET-002", "Aurore", "Rare", "aurora"),
            ),
            definitions = listOf(
                testCardDefinition("BET-001", name = "Ciel profond"),
                testCardDefinition("BET-002", name = "Aurore"),
            ),
        )

        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            PackOpeningScreen(
                state = currentState.value,
                onDone = {},
            )
        }

        composeRule.advanceToRevealedCards()
        composeRule.mainClock.autoAdvance = true
        composeRule.firstNodeWithTag("pack-opening-current-card-surface").performTouchInput { swipeLeft() }
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.safeReadCurrentPackOpeningCardId() == "ALP-002"
        }

        composeRule.mainClock.autoAdvance = false
        composeRule.mainClock.advanceTimeBy(2_400)
        composeRule.runOnIdle { }
        composeRule.onAllNodesWithTag("pack-opening-last-card-nudge").assertCountEquals(1)

        composeRule.runOnIdle {
            currentState.value = secondPack
        }
        composeRule.advanceToRevealedCards()
        assertEquals("BET-001", composeRule.readCurrentPackOpeningCardId())
        composeRule.onAllNodesWithTag("pack-opening-last-card-nudge").assertCountEquals(0)
    }

    @Test
    fun pack_opening_swipe_up_dismiss_keeps_dragged_position_before_exit_animation_progresses() {
        val firstCard = testCardDefinition("ALP-001", name = "Nebuleuse d'Orion")
        val secondCard = testCardDefinition("ALP-002", name = "Galaxie d'Andromede")
        val packResult = DrawPackResponse.fromCards(
            extensionId = "astronomes-en-herbe",
            drawnAt = "2026-03-23T12:00:00Z",
            rechargeState = androidTestRechargeStateWithNextChargeAt(
                availableDrawCount = 9,
                nextChargeAt = "2026-03-24T18:00:00Z",
            ),
            cards = listOf(
                testPackCard("ALP-001", "Nebuleuse d'Orion", "Common", "spark_fox"),
                testPackCard("ALP-002", "Galaxie d'Andromede", "Rare", "steam_golem"),
            ),
        )

        var doneCallCount = 0
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            PackOpeningScreen(
                state = PackOpeningUiState(
                    packResult = packResult,
                    displayCards = listOf(
                        firstCard.toDisplayCard(
                            extensionName = "Astronomes en herbe",
                            activeVariant = packResult.cards[0].variant.toDisplayVariant(),
                        ),
                        secondCard.toDisplayCard(
                            extensionName = "Astronomes en herbe",
                            activeVariant = packResult.cards[1].variant.toDisplayVariant(),
                        ),
                    ),
                    highestBurstRarity = "Rare",
                    hasHolographicBurst = false,
                ),
                onDone = { doneCallCount += 1 },
            )
        }

        composeRule.advanceToRevealedCards()
        val restingBounds = composeRule.currentCardBounds()

        composeRule.performDismissSwipeOnCurrentCard()
        composeRule.runOnIdle { }
        val dismissingBounds = composeRule.currentCardBounds()

        assertTrue(
            "Expected the card to remain above its resting position while dismiss starts.",
            dismissingBounds.top < restingBounds.top - 20f,
        )
        assertEquals(0, doneCallCount)

        composeRule.mainClock.advanceTimeBy(500)
        composeRule.runOnIdle { }
        assertEquals(1, doneCallCount)
    }

    @Test
    fun pack_opening_background_art_leaves_visible_sky_quality_frame() {
        val firstCard = testCardDefinition("ALP-001", name = "Nebuleuse d'Orion")
        val packResult = DrawPackResponse.fromCards(
            extensionId = "astronomes-en-herbe",
            drawnAt = "2026-03-23T12:00:00Z",
            rechargeState = androidTestRechargeStateWithNextChargeAt(
                availableDrawCount = 9,
                nextChargeAt = "2026-03-24T18:00:00Z",
            ),
            cards = listOf(
                testPackCard("ALP-001", "Nebuleuse d'Orion", "Common", "spark_fox"),
            ),
        )

        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            PackOpeningScreen(
                state = PackOpeningUiState(
                    packResult = packResult,
                    displayCards = listOf(
                        firstCard.toDisplayCard(
                            extensionName = "Astronomes en herbe",
                            activeVariant = packResult.cards[0].variant.toDisplayVariant(),
                        ),
                    ),
                    highestBurstRarity = "Common",
                    hasHolographicBurst = false,
                ),
                onDone = {},
            )
        }

        composeRule.advanceToRevealedCards()

        val surfaceBounds = composeRule.currentCardBounds()
        val artBounds = composeRule
            .firstNodeWithTag("astro-card-background-art", useUnmergedTree = true)
            .fetchSemanticsNode().boundsInRoot

        assertTrue("Expected pack reveal art width to leave a visible border", artBounds.width < surfaceBounds.width - 1f)
        assertTrue("Expected pack reveal art height to leave a visible border", artBounds.height < surfaceBounds.height - 1f)
    }

    @Test
    fun pack_opening_progress_repositions_to_avoid_overlapping_card_on_compact_height() {
        val firstCard = testCardDefinition("ALP-001", name = "Nebuleuse d'Orion")
        val packResult = DrawPackResponse.fromCards(
            extensionId = "astronomes-en-herbe",
            drawnAt = "2026-03-23T12:00:00Z",
            rechargeState = androidTestRechargeStateWithNextChargeAt(
                availableDrawCount = 9,
                nextChargeAt = "2026-03-24T18:00:00Z",
            ),
            cards = listOf(
                testPackCard("ALP-001", "Nebuleuse d'Orion", "Common", "spark_fox"),
            ),
        )

        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            Box(modifier = Modifier.size(width = 320.dp, height = 420.dp)) {
                PackOpeningScreen(
                    state = PackOpeningUiState(
                        packResult = packResult,
                        displayCards = listOf(
                            firstCard.toDisplayCard(
                                extensionName = "Astronomes en herbe",
                                activeVariant = packResult.cards[0].variant.toDisplayVariant(),
                            ),
                        ),
                        highestBurstRarity = "Common",
                        hasHolographicBurst = false,
                    ),
                    onDone = {},
                )
            }
        }

        composeRule.advanceToRevealedCards()

        val progressBounds = composeRule
            .onNodeWithTag("pack-opening-progress", useUnmergedTree = true)
            .fetchSemanticsNode().boundsInRoot
        val cardBounds = composeRule.currentCardBounds()

        assertTrue(
            "Expected progress indicator to stay outside the revealed card bounds. Progress=$progressBounds Card=$cardBounds",
            progressBounds.bottom <= cardBounds.top || progressBounds.top >= cardBounds.bottom,
        )
    }

    private fun androidx.compose.ui.test.junit4.ComposeContentTestRule.assertApproxCardRatio(
        tag: String,
        tolerance: Float = 0.03f,
    ) {
        val bounds = firstNodeWithTag(tag, useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val actualRatio = bounds.width / bounds.height
        assertTrue(
            "Expected $tag width/height ratio near $TRADING_CARD_WIDTH_OVER_HEIGHT but was $actualRatio",
            abs(actualRatio - TRADING_CARD_WIDTH_OVER_HEIGHT) <= tolerance,
        )
    }

    private fun androidx.compose.ui.test.junit4.ComposeContentTestRule.firstNodeWithTag(
        tag: String,
        useUnmergedTree: Boolean = false,
    ) = onAllNodesWithTag(tag, useUnmergedTree = useUnmergedTree)[0]

    private fun androidx.compose.ui.test.junit4.ComposeContentTestRule.readCurrentPackOpeningCardId(): String =
        safeReadCurrentPackOpeningCardId() ?: error("No current pack opening card id was found")

    private fun androidx.compose.ui.test.junit4.ComposeContentTestRule.safeReadCurrentPackOpeningCardId(): String? {
        val nodes = onAllNodesWithTag("pack-opening-current-card-id", useUnmergedTree = true)
            .fetchSemanticsNodes(atLeastOneRootRequired = false)
        val node = nodes.firstOrNull() ?: return null
        if (!node.config.contains(SemanticsProperties.Text)) return null
        val textValues = node.config[SemanticsProperties.Text]
        return textValues.joinToString(separator = "") { annotated -> annotated.toString() }
    }

    private fun androidx.compose.ui.test.junit4.ComposeContentTestRule.advanceToRevealedCards() {
        mainClock.autoAdvance = false
        mainClock.advanceTimeBy(5_700)
        runOnIdle { }
    }

    private fun androidx.compose.ui.test.junit4.ComposeContentTestRule.currentCardBounds() =
        firstNodeWithTag("pack-opening-current-card-surface", useUnmergedTree = true)
            .fetchSemanticsNode().boundsInRoot

    private fun androidx.compose.ui.test.junit4.ComposeContentTestRule.performDismissSwipeOnCurrentCard() {
        firstNodeWithTag("pack-opening-current-card-surface").performTouchInput {
            down(center)
            advanceEventTime(16L)
            moveBy(Offset(0f, -180f))
            advanceEventTime(16L)
            moveBy(Offset(0f, -180f))
            advanceEventTime(16L)
            up()
        }
    }

    private fun buildPackOpeningState(
        drawnAt: String,
        cards: List<PackCard>,
        definitions: List<CardDefinition>,
        highestBurstRarity: String = "Rare",
    ): PackOpeningUiState {
        val packResult = DrawPackResponse.fromCards(
            extensionId = "astronomes-en-herbe",
            drawnAt = drawnAt,
            rechargeState = androidTestRechargeStateWithNextChargeAt(
                availableDrawCount = 9,
                nextChargeAt = "2026-03-24T18:00:00Z",
            ),
            cards = cards,
        )

        return PackOpeningUiState(
            packResult = packResult,
            displayCards = definitions.mapIndexed { index, definition ->
                definition.toDisplayCard(
                    extensionName = "Astronomes en herbe",
                    activeVariant = packResult.cards[index].variant.toDisplayVariant(),
                )
            },
            highestBurstRarity = highestBurstRarity,
            hasHolographicBurst = false,
        )
    }
}
