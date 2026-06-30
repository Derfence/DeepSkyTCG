package fr.aumombelli.dstcg

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import fr.aumombelli.dstcg.audio.AmbientTrack
import fr.aumombelli.dstcg.audio.AudioController
import fr.aumombelli.dstcg.audio.AudioPlaybackOptions
import fr.aumombelli.dstcg.audio.AudioSettings
import fr.aumombelli.dstcg.audio.SoundCue
import fr.aumombelli.dstcg.model.NewPlayerOnboardingStep
import fr.aumombelli.dstcg.testsupport.badgeCelebrationBackNavigationTestAppContainer
import fr.aumombelli.dstcg.testsupport.backNavigationTestAppContainer
import fr.aumombelli.dstcg.testsupport.miniGamesMenuTestAppContainer
import fr.aumombelli.dstcg.ui.theme.DstcgTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class DstcgAppBackNavigationTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun android_back_from_home_reset_confirmation_closes_dialog_before_finishing_activity() {
        setAppContent(backNavigationTestAppContainer())
        startAndReachHome()

        composeRule.onNodeWithTag("home-settings").performClick()
        advanceBy(1)
        composeRule.onNodeWithTag("home-settings-reset").performClick()
        advanceBy(1)
        composeRule.onNodeWithTag("home-reset-confirmation").assertIsDisplayed()

        pressAndroidBack()
        advanceBy(1)
        composeRule.onAllNodesWithTag("home-reset-confirmation").assertCountEquals(0)
        composeRule.onNodeWithTag("home-open-pack").assertIsDisplayed()
        assertTrue(!composeRule.activity.isFinishing)
    }

    @Test
    fun android_back_from_home_about_sheet_closes_sheet_before_finishing_activity() {
        setAppContent(backNavigationTestAppContainer())
        startAndReachHome()

        composeRule.onNodeWithTag("home-settings").performClick()
        advanceBy(1)
        composeRule.onNodeWithTag("home-settings-about").performClick()
        advanceUntilTagDisplayed("home-about-sheet", timeoutMillis = 5_000)
        composeRule.onNodeWithTag("home-about-sheet").assertIsDisplayed()

        pressAndroidBack()
        advanceUntilTagGone("home-about-sheet", timeoutMillis = 5_000)
        composeRule.onAllNodesWithTag("home-about-sheet").assertCountEquals(0)
        composeRule.onNodeWithTag("home-open-pack").assertIsDisplayed()
        assertTrue(!composeRule.activity.isFinishing)
    }

    @Test
    fun android_back_from_library_returns_to_home() {
        setAppContent(backNavigationTestAppContainer())
        startAndReachHome()

        composeRule.onNodeWithTag("home-library").performClick()
        advanceBy(2_700)
        composeRule.onNodeWithTag("library-grid").assertIsDisplayed()

        pressAndroidBack()
        advanceBy(2_100)
        composeRule.onNodeWithTag("home-open-pack").assertIsDisplayed()
    }

    @Test
    fun visible_back_from_library_returns_to_home() {
        setAppContent(backNavigationTestAppContainer())
        startAndReachHome()

        composeRule.onNodeWithTag("home-library").performClick()
        advanceBy(2_700)
        composeRule.onNodeWithTag("library-grid").assertIsDisplayed()
        composeRule.onNodeWithTag("library-back").performClick()

        advanceUntilTagDisplayed("home-open-pack", timeoutMillis = 10_000)
    }

    @Test
    fun visible_back_from_library_follows_title_during_opening_and_closing() {
        setAppContent(backNavigationTestAppContainer())
        startAndReachHome()

        composeRule.onNodeWithTag("home-library").performClick()
        advanceBy(1_700)
        composeRule.onNodeWithTag("library-grid").assertIsDisplayed()
        composeRule.onNodeWithTag("library-back").assertIsDisplayed()

        advanceUntilTagGone("app-transition-book", timeoutMillis = 10_000)
        composeRule.onNodeWithTag("library-back").performClick()
        advanceBy(120)
        composeRule.onNodeWithTag("library-back").assertIsDisplayed()

        advanceUntilTagDisplayed("home-open-pack", timeoutMillis = 10_000)
    }

    @Test
    fun home_menu_click_plays_navigation_sound_without_replacing_animation_sound() {
        val audioController = RecordingAudioController()
        setAppContent(backNavigationTestAppContainer(audioController = audioController))
        startAndReachHome()
        audioController.clearPlayedCues()

        composeRule.onNodeWithTag("home-library").performClick()
        composeRule.waitForIdle()

        assertEquals(SoundCue.UiNavigate, audioController.playedCues.firstOrNull())

        advanceUntilTagDisplayed("library-grid", timeoutMillis = 10_000)
        assertTrue(audioController.playedCues.contains(SoundCue.LibraryOpen))
    }

    @Test
    fun visible_back_from_menu_plays_navigation_sound_without_replacing_animation_sound() {
        val audioController = RecordingAudioController()
        setAppContent(backNavigationTestAppContainer(audioController = audioController))
        startAndReachHome()

        composeRule.onNodeWithTag("home-library").performClick()
        advanceUntilTagDisplayed("library-grid", timeoutMillis = 10_000)
        advanceUntilTagGone("app-transition-book", timeoutMillis = 10_000)
        audioController.clearPlayedCues()

        composeRule.onNodeWithTag("library-back").performClick()
        composeRule.waitForIdle()

        assertEquals(SoundCue.UiNavigate, audioController.playedCues.firstOrNull())

        advanceBy(1_200)
        assertTrue(audioController.playedCues.contains(SoundCue.LibraryClose))
    }

    @Test
    fun android_back_from_equipment_returns_to_home() {
        setAppContent(backNavigationTestAppContainer())
        startAndReachHome()

        composeRule.onNodeWithTag("home-equipment").performClick()
        advanceUntilTagDisplayed("equipment-screen", timeoutMillis = 10_000)
        advanceUntilTagGone("app-transition-equipment", timeoutMillis = 10_000)
        composeRule.onNodeWithTag("equipment-screen").assertIsDisplayed()
        composeRule.onNodeWithTag("equipment-section-observatory").assertIsDisplayed()

        pressAndroidBack()
        advanceUntilTagDisplayed("home-open-pack", timeoutMillis = 10_000)
    }

    @Test
    fun visible_back_from_equipment_returns_to_home() {
        setAppContent(backNavigationTestAppContainer())
        startAndReachHome()

        composeRule.onNodeWithTag("home-equipment").performClick()
        advanceUntilTagDisplayed("equipment-screen", timeoutMillis = 10_000)
        advanceUntilTagGone("app-transition-equipment", timeoutMillis = 10_000)
        composeRule.onNodeWithTag("equipment-back").performClick()

        advanceUntilTagDisplayed("home-open-pack", timeoutMillis = 10_000)
    }

    @Test
    fun android_back_from_badge_book_closes_detail_then_returns_to_home() {
        val targetBadgeTag = "badge-coin-general::pack::first-opened"

        setAppContent(backNavigationTestAppContainer())
        startAndReachHome()

        composeRule.onNodeWithTag("home-badges").performClick()
        advanceUntilTagDisplayed("badge-book-scroll", timeoutMillis = 10_000)
        advanceUntilTagGone("app-transition-chest", timeoutMillis = 10_000)
        advanceUntilTagDisplayed(targetBadgeTag, timeoutMillis = 10_000)

        composeRule.onNodeWithTag(targetBadgeTag).performClick()
        advanceUntilTagDisplayed("badge-detail", timeoutMillis = 10_000)

        pressAndroidBack()
        advanceUntilTagGone("badge-detail", timeoutMillis = 10_000)
        composeRule.onNodeWithTag("badge-book-scroll").assertIsDisplayed()

        pressAndroidBack()
        advanceUntilTagDisplayed("home-open-pack", timeoutMillis = 10_000)
    }

    @Test
    fun visible_back_from_badge_book_returns_to_home() {
        setAppContent(backNavigationTestAppContainer())
        startAndReachHome()

        composeRule.onNodeWithTag("home-badges").performClick()
        advanceBy(2_800)
        composeRule.onNodeWithTag("badge-book-scroll").assertIsDisplayed()
        composeRule.onNodeWithTag("badge-book-back").performClick()

        advanceUntilTagDisplayed("home-open-pack", timeoutMillis = 10_000)
    }

    @Test
    fun mini_games_menu_opens_from_home_card_and_visible_back_returns_home() {
        setAppContent(miniGamesMenuTestAppContainer())
        startAndReachHome()

        composeRule.onNodeWithTag("home-card-flip").performClick()
        advanceUntilTagDisplayed("home-mini-games-open-menu", timeoutMillis = 5_000)
        composeRule.onNodeWithTag("home-mini-games-open-menu").performClick()
        advanceUntilTagDisplayed("mini-games-menu-screen", timeoutMillis = 10_000)
        advanceUntilTagEnabled("mini-games-menu-back", timeoutMillis = 10_000)

        composeRule.onNodeWithTag("mini-games-quiz").assertIsDisplayed().assertIsEnabled()
        composeRule.onNodeWithTag("mini-games-memory").assertIsDisplayed().assertIsEnabled()
        composeRule.onNodeWithTag("mini-games-timeline").assertIsDisplayed().assertIsEnabled()
        composeRule.onNodeWithTag("mini-games-observatory").assertIsDisplayed().assertIsEnabled()

        composeRule.onNodeWithTag("mini-games-menu-back").performClick()
        advanceUntilTagDisplayed("home-open-pack", timeoutMillis = 10_000)
    }

    @Test
    fun android_back_from_mini_games_menu_returns_home() {
        setAppContent(miniGamesMenuTestAppContainer())
        startAndReachHome()

        composeRule.onNodeWithTag("home-card-flip").performClick()
        advanceUntilTagDisplayed("home-mini-games-open-menu", timeoutMillis = 5_000)
        composeRule.onNodeWithTag("home-mini-games-open-menu").performClick()
        advanceUntilTagDisplayed("mini-games-menu-screen", timeoutMillis = 10_000)
        advanceUntilTagEnabled("mini-games-menu-back", timeoutMillis = 10_000)

        pressAndroidBack()
        advanceUntilTagDisplayed("home-open-pack", timeoutMillis = 10_000)
    }

    @Test
    fun mini_games_discovery_conclusion_waits_for_return_home() {
        setAppContent(
            miniGamesMenuTestAppContainer(
                initialOnboardingStep = NewPlayerOnboardingStep.DiscoverMiniGames,
            ),
        )
        startAndReachHome()

        composeRule.onNodeWithTag("home-card-flip").performClick()
        advanceUntilTagDisplayed("home-mini-games-open-menu", timeoutMillis = 5_000)
        composeRule.onNodeWithTag("home-mini-games-card").performClick()
        advanceUntilTagDisplayed("mini-games-menu-screen", timeoutMillis = 10_000)
        advanceUntilTagEnabled("mini-games-menu-back", timeoutMillis = 10_000)
        composeRule.onAllNodesWithTag("new-player-modal-conclusion").assertCountEquals(0)

        pressAndroidBack()
        advanceUntilTagDisplayed("new-player-modal-conclusion", timeoutMillis = 10_000)
    }

    @Test
    fun android_back_from_pack_selection_returns_to_extension_list_then_home() {
        setAppContent(backNavigationTestAppContainer())
        startAndReachHome()

        composeRule.onNodeWithTag("home-open-pack").performClick()
        advanceUntilTagEnabled("pack-extension-enter-astronomes-en-herbe", timeoutMillis = 10_000)
        composeRule.onNodeWithTag("pack-extension-enter-astronomes-en-herbe").performClick()
        advanceUntilTagEnabled("pack-booster-0", timeoutMillis = 10_000)
        composeRule.onNodeWithTag("pack-booster-0").assertIsDisplayed()

        pressAndroidBack()
        advanceUntilTagGone("pack-booster-0", timeoutMillis = 10_000)
        advanceUntilTagDisplayed("pack-extension-enter-astronomes-en-herbe", timeoutMillis = 10_000)
        composeRule.onNodeWithTag("pack-extension-enter-astronomes-en-herbe").assertIsDisplayed()

        pressAndroidBack()
        advanceUntilTagDisplayed("home-open-pack", timeoutMillis = 10_000)
        composeRule.onNodeWithTag("home-open-pack").assertIsDisplayed()
    }

    @Test
    fun selecting_pack_extension_plays_navigation_sound() {
        val audioController = RecordingAudioController()
        setAppContent(backNavigationTestAppContainer(audioController = audioController))
        startAndReachHome()

        composeRule.onNodeWithTag("home-open-pack").performClick()
        advanceUntilTagEnabled("pack-extension-enter-astronomes-en-herbe", timeoutMillis = 10_000)
        audioController.clearPlayedCues()

        composeRule.onNodeWithTag("pack-extension-enter-astronomes-en-herbe").performClick()
        composeRule.waitForIdle()

        assertEquals(listOf(SoundCue.UiNavigate), audioController.playedCues)
    }

    @Test
    fun selecting_pack_booster_plays_navigation_sound() {
        val audioController = RecordingAudioController()
        setAppContent(backNavigationTestAppContainer(audioController = audioController))
        startAndReachHome()

        composeRule.onNodeWithTag("home-open-pack").performClick()
        advanceUntilTagEnabled("pack-extension-enter-astronomes-en-herbe", timeoutMillis = 10_000)
        composeRule.onNodeWithTag("pack-extension-enter-astronomes-en-herbe").performClick()
        advanceUntilTagEnabled("pack-booster-0", timeoutMillis = 10_000)
        audioController.clearPlayedCues()

        composeRule.onNodeWithTag("pack-booster-0").performClick()
        composeRule.waitForIdle()

        assertEquals(listOf(SoundCue.UiNavigate), audioController.playedCues)
    }

    @Test
    fun visible_back_from_pack_selection_extension_list_returns_home() {
        setAppContent(backNavigationTestAppContainer())
        startAndReachHome()

        composeRule.onNodeWithTag("home-open-pack").performClick()
        advanceUntilTagEnabled("pack-extension-enter-astronomes-en-herbe", timeoutMillis = 10_000)
        composeRule.onNodeWithTag("pack-extension-enter-astronomes-en-herbe").performClick()
        advanceUntilTagEnabled("pack-booster-0", timeoutMillis = 10_000)
        composeRule.onNodeWithTag("pack-booster-0").assertIsDisplayed()

        pressAndroidBack()
        advanceUntilTagGone("pack-booster-0", timeoutMillis = 10_000)
        advanceUntilTagDisplayed("pack-extension-enter-astronomes-en-herbe", timeoutMillis = 10_000)
        advanceUntilTagGone("pack-extension-stage", timeoutMillis = 10_000)
        composeRule.onNodeWithTag("pack-extension-enter-astronomes-en-herbe").assertIsDisplayed()

        composeRule.onNodeWithTag("pack-back").performClick()
        advanceUntilTagDisplayed("home-open-pack", timeoutMillis = 10_000)
    }

    @Test
    fun android_back_from_pack_opening_returns_to_pack_selection() {
        setAppContent(backNavigationTestAppContainer())
        startAndReachHome()

        composeRule.onNodeWithTag("home-open-pack").performClick()
        advanceUntilTagEnabled("pack-extension-enter-astronomes-en-herbe", timeoutMillis = 10_000)
        composeRule.onNodeWithTag("pack-extension-enter-astronomes-en-herbe").performClick()
        advanceUntilTagEnabled("pack-booster-0", timeoutMillis = 10_000)
        composeRule.onNodeWithTag("pack-booster-0").performClick()
        advanceUntilTagDisplayed("pack-opening-current-card-surface", timeoutMillis = 15_000)
        composeRule.onNodeWithTag("pack-opening-current-card-surface").assertIsDisplayed()

        pressAndroidBack()
        advanceBy(500)
        composeRule.onNodeWithTag("pack-opening-current-card-surface").assertIsDisplayed()

        advancePackOpeningToFifthCard()
        pressAndroidBack()
        advanceUntilTagEnabled("pack-extension-enter-astronomes-en-herbe", timeoutMillis = 10_000)
        composeRule.onNodeWithTag("pack-extension-enter-astronomes-en-herbe").assertIsDisplayed()
    }

    @Test
    fun visible_close_from_pack_opening_returns_to_pack_selection() {
        setAppContent(backNavigationTestAppContainer())
        startAndReachHome()

        composeRule.onNodeWithTag("home-open-pack").performClick()
        advanceUntilTagEnabled("pack-extension-enter-astronomes-en-herbe", timeoutMillis = 10_000)
        composeRule.onNodeWithTag("pack-extension-enter-astronomes-en-herbe").performClick()
        advanceUntilTagEnabled("pack-booster-0", timeoutMillis = 10_000)
        composeRule.onNodeWithTag("pack-booster-0").performClick()
        advanceUntilTagDisplayed("pack-opening-current-card-surface", timeoutMillis = 15_000)

        composeRule.onNodeWithTag("pack-opening-close").performClick()
        advanceUntilTagEnabled("pack-extension-enter-astronomes-en-herbe", timeoutMillis = 10_000)
    }

    @Test
    fun pack_selection_background_crossfades_into_pack_opening() {
        setAppContent(backNavigationTestAppContainer())
        startAndReachHome()

        composeRule.onNodeWithTag("home-open-pack").performClick()
        advanceUntilTagEnabled("pack-extension-enter-astronomes-en-herbe", timeoutMillis = 10_000)
        composeRule.onNodeWithTag("pack-extension-enter-astronomes-en-herbe").performClick()
        advanceUntilTagEnabled("pack-booster-0", timeoutMillis = 10_000)
        composeRule.onNodeWithTag("pack-booster-0").performClick()
        advanceUntilTagDisplayed("pack-opening-booster", timeoutMillis = 10_000)

        composeRule.onNodeWithTag("pack-screen-root").assertIsDisplayed()

        advanceBy(700)
        composeRule.onAllNodesWithTag("pack-screen-root").assertCountEquals(0)
    }

    @Test
    fun pack_opening_does_not_keep_pack_selection_titles_mounted_during_transition() {
        setAppContent(backNavigationTestAppContainer())
        startAndReachHome()

        composeRule.onNodeWithTag("home-open-pack").performClick()
        advanceUntilTagEnabled("pack-extension-enter-astronomes-en-herbe", timeoutMillis = 10_000)
        composeRule.onNodeWithTag("pack-extension-enter-astronomes-en-herbe").performClick()
        advanceUntilTagEnabled("pack-booster-0", timeoutMillis = 10_000)
        composeRule.onNodeWithTag("pack-booster-0").performClick()
        advanceUntilTagDisplayed("pack-opening-booster", timeoutMillis = 10_000)

        composeRule.onNodeWithTag("pack-title").assertIsNotDisplayed()
        composeRule.onNodeWithTag("pack-extension-title").assertIsNotDisplayed()

        advanceBy(700)
        composeRule.onAllNodesWithTag("pack-title").assertCountEquals(0)
        composeRule.onAllNodesWithTag("pack-extension-title").assertCountEquals(0)
    }

    @Test
    fun pack_opening_back_returns_to_extension_list_before_any_new_selection() {
        setAppContent(backNavigationTestAppContainer())
        startAndReachHome()

        composeRule.onNodeWithTag("home-open-pack").performClick()
        advanceUntilTagEnabled("pack-extension-enter-astronomes-en-herbe", timeoutMillis = 10_000)
        composeRule.onNodeWithTag("pack-extension-enter-astronomes-en-herbe").performClick()
        advanceUntilTagEnabled("pack-booster-0", timeoutMillis = 10_000)
        composeRule.onNodeWithTag("pack-booster-0").performClick()
        advanceUntilTagDisplayed("pack-opening-current-card-surface", timeoutMillis = 15_000)

        advancePackOpeningToFifthCard()
        pressAndroidBack()
        advanceUntilTagEnabled("pack-extension-enter-astronomes-en-herbe", timeoutMillis = 10_000)
        composeRule.onNodeWithTag("pack-extension-enter-astronomes-en-herbe").assertIsDisplayed()
        composeRule.onAllNodesWithTag("pack-booster-0").assertCountEquals(0)
    }

    @Test
    fun android_back_from_pack_opening_with_new_badge_defers_celebration_until_library_is_seen() {
        setAppContent(badgeCelebrationBackNavigationTestAppContainer())
        startAndReachHome()

        composeRule.onNodeWithTag("home-open-pack").performClick()
        advanceUntilTagEnabled("pack-extension-enter-astronomes-en-herbe", timeoutMillis = 10_000)
        composeRule.onNodeWithTag("pack-extension-enter-astronomes-en-herbe").performClick()
        advanceUntilTagEnabled("pack-booster-0", timeoutMillis = 10_000)
        composeRule.onNodeWithTag("pack-booster-0").performClick()
        advanceUntilTagDisplayed("pack-opening-current-card-surface", timeoutMillis = 15_000)
        composeRule.onNodeWithTag("pack-opening-current-card-surface").assertIsDisplayed()

        advancePackOpeningToFifthCard()
        pressAndroidBack()
        advanceUntilTagDisplayed("new-player-coachmark-HomeLibrary", timeoutMillis = 10_000)
        composeRule.onNodeWithTag("new-player-coachmark-HomeLibrary").assertIsDisplayed()
        composeRule.onAllNodesWithTag("badge-unlock-celebration").assertCountEquals(0)

        composeRule.onNodeWithTag("home-library").performClick()
        advanceUntilTagDisplayed("new-player-modal-library-variants", timeoutMillis = 10_000)
        completeLibraryVariantWalkthrough()
        composeRule.onNodeWithTag("library-grid").assertIsDisplayed()
        pressAndroidBack()
        advanceBy(2_400)
        composeRule.onNodeWithTag("badge-unlock-celebration").assertIsDisplayed()
        composeRule.onNodeWithTag("badge-unlock-celebration-coin-astronomes-en-herbe::sky::city").assertIsDisplayed()

        advanceBy(1_800)
        composeRule.onAllNodesWithTag("badge-unlock-celebration").assertCountEquals(0)
        composeRule.onNodeWithTag("home-open-pack").assertIsEnabled()
        composeRule.onNodeWithTag("home-open-pack").assertIsDisplayed()
    }

    @Test
    fun android_back_from_home_finishes_activity() {
        setAppContent(backNavigationTestAppContainer())
        startAndReachHome()

        val activity = composeRule.activity
        pressAndroidBack(activity)
        composeRule.waitUntil(timeoutMillis = 5_000) { activity.isFinishing || activity.isDestroyed }
        assertTrue(activity.isFinishing || activity.isDestroyed)
    }

    private fun setAppContent(appContainer: AppContainer) {
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            DstcgTheme {
                DstcgApp(appContainer = appContainer)
            }
        }
        advanceBy(3_000)
    }

    private fun startAndReachHome() {
        composeRule.onNodeWithTag("home-open-pack").assertIsDisplayed()
    }

    private fun completeLibraryVariantWalkthrough() {
        repeat(3) {
            composeRule.onNodeWithTag("new-player-modal-next").performClick()
            advanceBy(500)
        }
        advanceUntilTagDisplayed("new-player-modal-page-3", timeoutMillis = 5_000)
        composeRule.onNodeWithTag("new-player-modal-finish").performClick()
        advanceUntilTagGone("new-player-modal-library-variants", timeoutMillis = 10_000)
    }

    private fun advanceBy(durationMillis: Long) {
        composeRule.mainClock.advanceTimeBy(durationMillis)
        composeRule.waitForIdle()
    }

    private fun pressAndroidBack(activity: ComponentActivity = composeRule.activity) {
        activity.runOnUiThread {
            activity.onBackPressedDispatcher.onBackPressed()
        }
        composeRule.waitForIdle()
    }

    private fun advancePackOpeningToFifthCard() {
        repeat(4) { pageOffset ->
            composeRule.onNodeWithTag("pack-opening-current-card-surface").performTouchInput { swipeLeft() }
            val expectedCardId = "ALP-${(pageOffset + 2).toString().padStart(3, '0')}"
            advanceUntil(timeoutMillis = 5_000) {
                runCatching {
                    composeRule.onNodeWithTag("pack-opening-current-card-id", useUnmergedTree = true)
                        .assertTextContains(expectedCardId)
                    true
                }.getOrDefault(false)
            }
        }
    }

    private fun advanceUntilTagDisplayed(tag: String, timeoutMillis: Long = 5_000) {
        advanceUntil(timeoutMillis) {
            runCatching {
                composeRule.onNodeWithTag(tag).assertIsDisplayed()
                true
            }.getOrDefault(false)
        }
    }

    private fun advanceUntilTagEnabled(tag: String, timeoutMillis: Long = 5_000) {
        advanceUntil(timeoutMillis) {
            runCatching {
                composeRule.onNodeWithTag(tag).assertIsEnabled()
                true
            }.getOrDefault(false)
        }
    }

    private fun advanceUntilTagGone(tag: String, timeoutMillis: Long = 5_000) {
        advanceUntil(timeoutMillis) {
            composeRule.onAllNodesWithTag(tag, useUnmergedTree = true)
                .fetchSemanticsNodes(atLeastOneRootRequired = false)
                .isEmpty()
        }
    }

    private fun advanceUntil(timeoutMillis: Long, condition: () -> Boolean) {
        val deadline = timeoutMillis.coerceAtLeast(0L)
        var elapsed = 0L
        while (elapsed <= deadline) {
            if (condition()) {
                return
            }
            advanceBy(100)
            elapsed += 100
        }
        throw androidx.compose.ui.test.ComposeTimeoutException(
            "Condition still not satisfied after ${timeoutMillis} ms",
        )
    }

    private class RecordingAudioController : AudioController {
        private val mutableSettings = MutableStateFlow(AudioSettings())
        private val playedCueStorage = mutableListOf<SoundCue>()

        override val settings: StateFlow<AudioSettings> = mutableSettings.asStateFlow()

        val playedCues: List<SoundCue>
            get() = synchronized(playedCueStorage) { playedCueStorage.toList() }

        override fun play(cue: SoundCue, options: AudioPlaybackOptions) {
            synchronized(playedCueStorage) {
                playedCueStorage += cue
            }
        }

        override fun setAmbient(track: AmbientTrack?) = Unit

        override suspend fun setEnabled(enabled: Boolean) {
            mutableSettings.value = mutableSettings.value.copy(enabled = enabled)
        }

        override fun onAppForegrounded() = Unit

        override fun onAppBackgrounded() = Unit

        override fun release() = Unit

        fun clearPlayedCues() {
            synchronized(playedCueStorage) {
                playedCueStorage.clear()
            }
        }
    }
}
