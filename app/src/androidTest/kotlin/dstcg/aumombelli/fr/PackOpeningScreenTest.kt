package fr.aumombelli.dstcg

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.dp
import fr.aumombelli.dstcg.feature.packs.opening.EquipmentPackRevealUiItem
import fr.aumombelli.dstcg.feature.packs.opening.PackOpeningRevealSlotProbe
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

private const val PACK_OPENING_INITIAL_BOOSTER_ASSERTION_MS = 300L
private const val PACK_OPENING_REVEAL_SETTLE_MS = 4_100L

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

        composeRule.mainClock.advanceTimeBy(PACK_OPENING_INITIAL_BOOSTER_ASSERTION_MS)
        composeRule.runOnIdle { }
        composeRule.onNodeWithTag("pack-opening-booster").assertIsDisplayed()
        composeRule.assertApproxCardRatio("pack-opening-booster")
        val boosterBoundsBeforeReveal = composeRule.boosterBounds()
        composeRule.onAllNodesWithText("Booster").assertCountEquals(0)

        composeRule.mainClock.advanceTimeBy(
            PACK_OPENING_REVEAL_SETTLE_MS - PACK_OPENING_INITIAL_BOOSTER_ASSERTION_MS,
        )
        composeRule.runOnIdle { }

        composeRule.onNodeWithTag("pack-opening-burst").assertIsDisplayed()
        composeRule.onNodeWithTag("pack-opening-progress").assertIsDisplayed()
        composeRule.onAllNodesWithTag("pack-opening-card-id").assertCountEquals(0)
        assertEquals("ALP-001", composeRule.readCurrentPackOpeningCardId())
        composeRule.onAllNodesWithTag("pack-opening-arrow-left").assertCountEquals(0)
        composeRule.onAllNodesWithTag("pack-opening-arrow-right").assertCountEquals(1)
        composeRule.onAllNodesWithTag("pack-opening-card-name").assertCountEquals(0)
        composeRule.onAllNodesWithTag("astro-card-holo-foil", useUnmergedTree = true).assertCountEquals(0)
        composeRule.onAllNodesWithTag("astro-card-holo-glare", useUnmergedTree = true).assertCountEquals(0)
        composeRule.onAllNodesWithTag("astro-card-holo-sparkles", useUnmergedTree = true).assertCountEquals(0)
        composeRule.assertApproxCardRatio("pack-opening-current-card-surface")
        composeRule.assertBoundsClose(
            expected = boosterBoundsBeforeReveal,
            actual = composeRule.currentCardBounds(),
            tolerancePx = 2.5f,
        )
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
    fun reveal_slot_probe_matches_pack_opening_booster_slot() {
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
        var showOpeningScreen by mutableStateOf(false)
        var probeBounds by mutableStateOf<Rect?>(null)

        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            Box(modifier = Modifier.size(width = 411.dp, height = 731.dp)) {
                if (!showOpeningScreen) {
                    PackOpeningRevealSlotProbe(
                        extensionLabel = "Astronomes en herbe",
                        totalItems = 1,
                        onBoundsChanged = { bounds ->
                            probeBounds = bounds?.let {
                                Rect(
                                    left = it.leftPx,
                                    top = it.topPx,
                                    right = it.leftPx + it.widthPx,
                                    bottom = it.topPx + it.heightPx,
                                )
                            }
                        },
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
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
        }

        composeRule.waitForIdle()
        val measuredProbeBounds = requireNotNull(probeBounds)

        composeRule.runOnIdle { showOpeningScreen = true }
        composeRule.mainClock.advanceTimeBy(300)
        composeRule.waitForIdle()

        composeRule.assertBoundsClose(
            expected = measuredProbeBounds,
            actual = composeRule.boosterBounds(),
            tolerancePx = 3f,
        )
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
                    skyQuality = "holographic",
                    skyQualityLabel = "Holographique",
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
        composeRule.onNodeWithTag("pack-opening-burst-orbit").assertIsDisplayed()
    }

    @Test
    fun pack_opening_swiping_onto_holographic_card_triggers_arrival_animation() {
        val firstCard = testCardDefinition("ALP-001", name = "Nebuleuse d'Orion")
        val holoCard = testCardDefinition("ALP-777", name = "Comete", rarityLabel = "Epic")
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
                    "ALP-777",
                    "Comete",
                    "Epic",
                    "comet",
                    skyQuality = "holographic",
                    skyQualityLabel = "Holographique",
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
                        firstCard.toDisplayCard(
                            extensionName = "Astronomes en herbe",
                            activeVariant = packResult.cards[0].variant.toDisplayVariant(),
                        ),
                        holoCard.toDisplayCard(
                            extensionName = "Astronomes en herbe",
                            activeVariant = packResult.cards[1].variant.toDisplayVariant(),
                        ),
                    ),
                    highestBurstRarity = "Epic",
                    hasHolographicBurst = true,
                ),
                onDone = {},
            )
        }

        composeRule.advanceToRevealedCards()
        composeRule.onAllNodesWithTag("pack-opening-holo-arrival", useUnmergedTree = true).assertCountEquals(0)
        composeRule.onAllNodesWithTag("pack-opening-holo-celebration", useUnmergedTree = true).assertCountEquals(0)

        composeRule.firstNodeWithTag("pack-opening-current-card-surface").performTouchInput { swipeLeft() }
        composeRule.mainClock.advanceTimeBy(700)
        composeRule.runOnIdle { }
        composeRule.onNodeWithTag("pack-opening-holo-arrival", useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithTag("pack-opening-holo-celebration", useUnmergedTree = true).assertIsDisplayed()
        assertEquals("ALP-777", composeRule.readCurrentPackOpeningCardId())
    }

    @Test
    fun pack_opening_holographic_arrival_animation_only_plays_once_per_card() {
        val firstCard = testCardDefinition("ALP-001", name = "Nebuleuse d'Orion")
        val holoCard = testCardDefinition("ALP-777", name = "Comete", rarityLabel = "Epic")
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
                    "ALP-777",
                    "Comete",
                    "Epic",
                    "comet",
                    skyQuality = "holographic",
                    skyQualityLabel = "Holographique",
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
                        firstCard.toDisplayCard(
                            extensionName = "Astronomes en herbe",
                            activeVariant = packResult.cards[0].variant.toDisplayVariant(),
                        ),
                        holoCard.toDisplayCard(
                            extensionName = "Astronomes en herbe",
                            activeVariant = packResult.cards[1].variant.toDisplayVariant(),
                        ),
                    ),
                    highestBurstRarity = "Epic",
                    hasHolographicBurst = true,
                ),
                onDone = {},
            )
        }

        composeRule.advanceToRevealedCards()

        composeRule.firstNodeWithTag("pack-opening-current-card-surface").performTouchInput { swipeLeft() }
        composeRule.mainClock.advanceTimeBy(700)
        composeRule.runOnIdle { }
        assertEquals("ALP-777", composeRule.readCurrentPackOpeningCardId())
        composeRule.onNodeWithTag("pack-opening-holo-arrival", useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithTag("pack-opening-holo-celebration", useUnmergedTree = true).assertIsDisplayed()

        composeRule.mainClock.advanceTimeBy(1_800)
        composeRule.runOnIdle { }
        composeRule.onAllNodesWithTag("pack-opening-holo-arrival", useUnmergedTree = true).assertCountEquals(0)
        composeRule.onAllNodesWithTag("pack-opening-holo-celebration", useUnmergedTree = true).assertCountEquals(0)

        composeRule.firstNodeWithTag("pack-opening-current-card-surface").performTouchInput { swipeRight() }
        composeRule.mainClock.advanceTimeBy(700)
        composeRule.runOnIdle { }
        assertEquals("ALP-001", composeRule.readCurrentPackOpeningCardId())

        composeRule.firstNodeWithTag("pack-opening-current-card-surface").performTouchInput { swipeLeft() }
        composeRule.mainClock.advanceTimeBy(700)
        composeRule.runOnIdle { }
        assertEquals("ALP-777", composeRule.readCurrentPackOpeningCardId())
        composeRule.onAllNodesWithTag("pack-opening-holo-arrival", useUnmergedTree = true).assertCountEquals(0)
        composeRule.onAllNodesWithTag("pack-opening-holo-celebration", useUnmergedTree = true).assertCountEquals(0)
    }

    @Test
    fun pack_opening_holographic_cards_render_foil_layers_in_pack_and_fullscreen() {
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
                    skyQuality = "holographic",
                    skyQualityLabel = "Holographique",
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

        composeRule.onNodeWithTag("astro-card-holo-foil", useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithTag("astro-card-holo-glare", useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithTag("astro-card-holo-sparkles", useUnmergedTree = true).assertIsDisplayed()

        composeRule.mainClock.autoAdvance = true
        composeRule.firstNodeWithTag("pack-opening-current-card-surface").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag("astro-card-holo-foil", useUnmergedTree = true)
                .fetchSemanticsNodes(atLeastOneRootRequired = false)
                .size >= 2
        }
        composeRule.onNodeWithTag("astro-card-fullscreen-close").assertIsDisplayed()
        composeRule.onAllNodesWithTag("astro-card-holo-glare", useUnmergedTree = true)
            .assertCountEquals(2)
    }

    @Test
    fun pack_opening_renders_equipment_reward_cards() {
        val definition = testEquipmentCardDefinition(
            id = "mount-advanced",
            type = EquipmentType.Mount,
            displayName = "Monture Niveau 2",
            level = 2,
            imageRef = "equipment_mount_2",
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
        composeRule.onAllNodesWithTag(
            "pack-opening-equipment-art-mount-advanced",
            useUnmergedTree = true,
        ).assertCountEquals(1)
        composeRule.onAllNodesWithTag(
            "pack-opening-equipment-icon-mount-advanced",
            useUnmergedTree = true,
        ).assertCountEquals(1)
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
        composeRule.assertCurrentCardCentered()

        composeRule.mainClock.autoAdvance = false
        composeRule.mainClock.advanceTimeBy(2_600)
        composeRule.runOnIdle { }
        composeRule.onAllNodesWithTag("pack-opening-last-card-nudge").assertCountEquals(1)
        composeRule.onAllNodesWithTag("pack-opening-swipe-hint-label").assertCountEquals(0)
    }

    @Test
    fun pack_opening_reveals_unlocked_nudge_after_fullscreen_closes() {
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
        composeRule.onAllNodesWithTag("pack-opening-swipe-hint-label").assertCountEquals(0)
    }

    @Test
    fun pack_opening_swipe_hint_animation_keeps_pager_snapped_to_a_page() {
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

        composeRule.firstNodeWithTag("pack-opening-current-card-surface").performTouchInput { swipeLeft() }
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.safeReadCurrentPackOpeningCardId() == "ALP-003"
        }

        composeRule.firstNodeWithTag("pack-opening-current-card-surface").performTouchInput { swipeRight() }
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.safeReadCurrentPackOpeningCardId() == "ALP-002"
        }

        composeRule.mainClock.autoAdvance = false
        composeRule.mainClock.advanceTimeBy(2_600)
        composeRule.runOnIdle { }
        composeRule.onAllNodesWithTag("pack-opening-last-card-nudge").assertCountEquals(1)

        composeRule.mainClock.autoAdvance = true
        composeRule.onNodeWithTag("pack-opening-pager").performTouchInput { swipeRight() }
        if (!composeRule.waitUntilCardId("ALP-001", timeoutMillis = 1_500)) {
            composeRule.onNodeWithTag("pack-opening-pager").performTouchInput { swipeRight() }
        }
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.safeReadCurrentPackOpeningCardId() == "ALP-001"
        }
        composeRule.assertCurrentCardCentered()
    }

    @Test
    fun pack_opening_onboarding_hint_appears_after_first_nudge_then_stays_visible_during_navigation() {
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
                showPersistentDismissHint = true,
                onDone = {},
            )
        }

        composeRule.advanceToRevealedCards()
        composeRule.onAllNodesWithTag("pack-opening-swipe-hint-label").assertCountEquals(0)

        composeRule.mainClock.autoAdvance = true
        composeRule.firstNodeWithTag("pack-opening-current-card-surface").performTouchInput { swipeLeft() }
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.safeReadCurrentPackOpeningCardId() == "ALP-002"
        }

        composeRule.mainClock.autoAdvance = false
        composeRule.mainClock.advanceTimeBy(2_600)
        composeRule.runOnIdle { }
        composeRule.onNodeWithTag("pack-opening-swipe-hint-label").assertIsDisplayed()

        composeRule.mainClock.autoAdvance = true
        composeRule.firstNodeWithTag("pack-opening-current-card-surface").performTouchInput { swipeRight() }
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.safeReadCurrentPackOpeningCardId() == "ALP-001"
        }
        composeRule.onNodeWithTag("pack-opening-swipe-hint-label").assertIsDisplayed()
    }

    @Test
    fun pack_opening_swipe_hint_nudges_current_card_upward_once_unlocked() {
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

        composeRule.mainClock.autoAdvance = false
        val restingBounds = composeRule.currentCardBounds()
        composeRule.mainClock.advanceTimeBy(2_600)
        composeRule.runOnIdle { }
        val nudgedBounds = composeRule.currentCardBounds()

        assertTrue(
            "Expected onboarding nudge to lift the current card upward. Rest=$restingBounds Nudged=$nudgedBounds",
            nudgedBounds.top < restingBounds.top - 3f,
        )
        composeRule.onAllNodesWithTag("pack-opening-swipe-hint-label").assertCountEquals(0)
    }

    @Test
    fun pack_opening_swipe_hint_label_does_not_push_card_down_when_it_appears() {
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
                showPersistentDismissHint = true,
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
        val beforeHintBounds = composeRule.currentCardBounds()
        composeRule.mainClock.advanceTimeBy(2_600)
        composeRule.runOnIdle { }
        val afterHintBounds = composeRule.currentCardBounds()

        composeRule.onNodeWithTag("pack-opening-swipe-hint-label").assertIsDisplayed()
        assertTrue(
            "Expected the onboarding hint label to avoid pushing the card downward. Before=$beforeHintBounds After=$afterHintBounds",
            afterHintBounds.top <= beforeHintBounds.top + 2f,
        )
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

    @Test
    fun pack_opening_compact_phone_keeps_revealed_card_large_enough() {
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

        val rootBounds = composeRule.onRoot(useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val cardBounds = composeRule.currentCardBounds()

        assertTrue(
            "Expected the revealed card to keep a strong visual presence on compact phones. Root=$rootBounds Card=$cardBounds",
            (cardBounds.height / rootBounds.height) >= 0.53f,
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
        mainClock.advanceTimeBy(PACK_OPENING_REVEAL_SETTLE_MS)
        runOnIdle { }
    }

    private fun androidx.compose.ui.test.junit4.ComposeContentTestRule.currentCardBounds() =
        firstNodeWithTag("pack-opening-current-card-surface", useUnmergedTree = true)
            .fetchSemanticsNode().boundsInRoot

    private fun androidx.compose.ui.test.junit4.ComposeContentTestRule.boosterBounds() =
        onNodeWithTag("pack-opening-booster", useUnmergedTree = true)
            .fetchSemanticsNode().boundsInRoot

    private fun androidx.compose.ui.test.junit4.ComposeContentTestRule.assertBoundsClose(
        expected: androidx.compose.ui.geometry.Rect,
        actual: androidx.compose.ui.geometry.Rect,
        tolerancePx: Float,
    ) {
        assertTrue(
            "Expected bounds to stay aligned. Expected=$expected Actual=$actual",
            abs(expected.left - actual.left) <= tolerancePx &&
                abs(expected.top - actual.top) <= tolerancePx &&
                abs(expected.width - actual.width) <= tolerancePx &&
                abs(expected.height - actual.height) <= tolerancePx,
        )
    }

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

    private fun androidx.compose.ui.test.junit4.ComposeContentTestRule.assertCurrentCardCentered(
        tolerancePx: Float = 24f,
    ) {
        val rootBounds = onRoot(useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val cardBounds = currentCardBounds()
        assertTrue(
            "Expected current card to be centered. Root=$rootBounds Card=$cardBounds",
            abs(cardBounds.center.x - rootBounds.center.x) <= tolerancePx,
        )
    }

    private fun androidx.compose.ui.test.junit4.ComposeContentTestRule.waitUntilCardId(
        cardId: String,
        timeoutMillis: Long,
    ): Boolean = runCatching {
        waitUntil(timeoutMillis) {
            safeReadCurrentPackOpeningCardId() == cardId
        }
        true
    }.getOrDefault(false)

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
