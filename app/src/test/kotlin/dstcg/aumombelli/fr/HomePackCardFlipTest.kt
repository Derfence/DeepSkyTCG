package fr.aumombelli.dstcg

import fr.aumombelli.dstcg.feature.home.isHomeCardMiniGamesFaceVisible
import fr.aumombelli.dstcg.feature.home.nextHomeCardFlipStepForHorizontalDrag
import fr.aumombelli.dstcg.feature.home.shouldShowHomeMiniGamesDiscoveryHint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomePackCardFlipTest {
    @Test
    fun horizontal_swipes_rotate_in_both_directions_from_either_face() {
        assertEquals(-1, nextHomeCardFlipStepForHorizontalDrag(currentStep = 0, dragX = -80f, thresholdPx = 52f))
        assertEquals(1, nextHomeCardFlipStepForHorizontalDrag(currentStep = 0, dragX = 80f, thresholdPx = 52f))
        assertEquals(0, nextHomeCardFlipStepForHorizontalDrag(currentStep = 1, dragX = -80f, thresholdPx = 52f))
        assertEquals(2, nextHomeCardFlipStepForHorizontalDrag(currentStep = 1, dragX = 80f, thresholdPx = 52f))
        assertEquals(-2, nextHomeCardFlipStepForHorizontalDrag(currentStep = -1, dragX = -80f, thresholdPx = 52f))
        assertEquals(0, nextHomeCardFlipStepForHorizontalDrag(currentStep = -1, dragX = 80f, thresholdPx = 52f))
    }

    @Test
    fun short_horizontal_drags_do_not_rotate_card() {
        assertEquals(0, nextHomeCardFlipStepForHorizontalDrag(currentStep = 0, dragX = 32f, thresholdPx = 52f))
        assertEquals(1, nextHomeCardFlipStepForHorizontalDrag(currentStep = 1, dragX = -32f, thresholdPx = 52f))
    }

    @Test
    fun mini_games_face_is_visible_for_positive_and_negative_half_turns() {
        assertFalse(isHomeCardMiniGamesFaceVisible(0f))
        assertTrue(isHomeCardMiniGamesFaceVisible(180f))
        assertTrue(isHomeCardMiniGamesFaceVisible(-180f))
        assertFalse(isHomeCardMiniGamesFaceVisible(360f))
        assertFalse(isHomeCardMiniGamesFaceVisible(-360f))
    }

    @Test
    fun mini_games_discovery_hint_is_visible_when_onboarding_targets_front_face() {
        assertTrue(
            shouldShowHomeMiniGamesDiscoveryHint(
                miniGamesEnabled = true,
                discoveryStepActive = true,
                showingMiniGamesBack = false,
            ),
        )
    }

    @Test
    fun mini_games_discovery_hint_is_hidden_when_back_face_is_visible() {
        assertFalse(
            shouldShowHomeMiniGamesDiscoveryHint(
                miniGamesEnabled = true,
                discoveryStepActive = true,
                showingMiniGamesBack = true,
            ),
        )
    }

    @Test
    fun mini_games_discovery_hint_is_hidden_outside_discovery_or_before_unlock() {
        assertFalse(
            shouldShowHomeMiniGamesDiscoveryHint(
                miniGamesEnabled = false,
                discoveryStepActive = true,
                showingMiniGamesBack = false,
            ),
        )
        assertFalse(
            shouldShowHomeMiniGamesDiscoveryHint(
                miniGamesEnabled = true,
                discoveryStepActive = false,
                showingMiniGamesBack = false,
            ),
        )
    }
}
