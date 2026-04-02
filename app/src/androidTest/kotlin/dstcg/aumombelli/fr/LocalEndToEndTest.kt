package fr.aumombelli.dstcg

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeUp
import fr.aumombelli.dstcg.testsupport.offlineMainActivityTestAppContainer
import org.junit.After
import org.junit.Rule
import org.junit.Test

class LocalEndToEndTest {
    init {
        MainActivity.appContainerFactory = { context ->
            offlineMainActivityTestAppContainer(
                context = context,
                dataStoreFileName = "local-e2e.preferences_pb",
                randomSeed = 12345,
            )
        }
    }

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun local_end_to_end_flow() {
        startAndReachMainMenu()

        val firstDrawnCardId = openPackAndCaptureVisibleCardId()

        verifyLibraryContainsDrawnCard(firstDrawnCardId)
        verifyRechargeStatusIsVisible()
    }

    @After
    fun tearDown() {
        MainActivity.appContainerFactory = null
    }

    private fun startAndReachMainMenu() {
        composeRule.waitUntilTagEnabled("start-begin", timeoutMillis = 20_000)
        composeRule.onNodeWithTag("start-begin").performClick()
        composeRule.waitUntilTagEnabled("menu-open-pack", timeoutMillis = 20_000)
        composeRule.onNodeWithTag("menu-open-pack").assertIsDisplayed()
    }

    private fun openPackAndCaptureVisibleCardId(): String {
        composeRule.waitUntilTagEnabled("menu-open-pack", timeoutMillis = 10_000)
        composeRule.onNodeWithTag("menu-open-pack").performClick()
        composeRule.waitUntilTagEnabled("pack-extension-enter-${LocalE2eConfig.extensionId}", timeoutMillis = 15_000)
        composeRule.onNodeWithTag("pack-extension-enter-${LocalE2eConfig.extensionId}").performClick()
        composeRule.waitUntilTagEnabled("pack-booster-0", timeoutMillis = 10_000)
        composeRule.onNodeWithTag("pack-booster-0").performClick()

        composeRule.waitUntilTagExists("pack-opening-title", timeoutMillis = 20_000)
        composeRule.waitUntilTagExists("pack-opening-current-card-id", timeoutMillis = 10_000)

        val firstDrawnCardId = composeRule.readText("pack-opening-current-card-id")
        composeRule.firstNodeWithTag("pack-opening-current-card-surface").performClick()
        composeRule.waitUntilTagExists("astro-card-fullscreen-close", timeoutMillis = 10_000)
        composeRule.onNodeWithTag("astro-card-fullscreen-close").performClick()
        composeRule.waitUntilTagExists("pack-opening-current-card-surface", timeoutMillis = 10_000)

        repeat(4) {
            composeRule.firstNodeWithTag("pack-opening-current-card-surface").performTouchInput { swipeLeft() }
            composeRule.waitForIdle()
        }

        composeRule.firstNodeWithTag("pack-opening-current-card-surface").performTouchInput { swipeUp() }
        composeRule.waitForPackReturnToMenu()
        return firstDrawnCardId
    }

    private fun verifyLibraryContainsDrawnCard(cardId: String) {
        composeRule.waitUntilTagEnabled("menu-library", timeoutMillis = 10_000)
        composeRule.onNodeWithTag("menu-library").performClick()
        composeRule.waitUntilTagExists("library-section-${LocalE2eConfig.extensionId}", timeoutMillis = 10_000)

        composeRule.onNodeWithTag("library-grid").performScrollToNode(hasTestTag("library-card-$cardId"))
        composeRule.onNodeWithTag("library-card-$cardId").assertIsDisplayed()
        composeRule.onNodeWithTag("library-owned-$cardId").assertTextContains("En collection :", substring = true)
        composeRule.onNodeWithTag("library-card-$cardId").performClick()
        composeRule.waitUntilTagExists("library-card-preview", timeoutMillis = 10_000)
        composeRule.onNodeWithTag("library-card-preview-surface").performClick()
        composeRule.waitUntilTagExists("astro-card-fullscreen-close", timeoutMillis = 10_000)
        composeRule.onNodeWithTag("astro-card-fullscreen-close").performClick()

        composeRule.waitUntilTagGone("astro-card-fullscreen-close", timeoutMillis = 10_000)
        composeRule.pressAndroidBack()
        composeRule.waitUntilTagEnabled("menu-open-pack", timeoutMillis = 10_000)
    }

    private fun verifyRechargeStatusIsVisible() {
        composeRule.waitUntilTagEnabled("menu-open-pack", timeoutMillis = 10_000)
        composeRule.onNodeWithTag("menu-open-pack").performClick()
        composeRule.waitUntilTagExists("pack-extension-enter-${LocalE2eConfig.extensionId}", timeoutMillis = 15_000)
        composeRule.onNodeWithTag("pack-status-count").assertTextContains("9/10")
        composeRule.onNodeWithTag("pack-status-remaining").assertTextContains("Prochaine charge dans", substring = true)
        composeRule.onNodeWithTag("pack-extension-enter-${LocalE2eConfig.extensionId}").assertIsEnabled()
        composeRule.pressAndroidBack()
        composeRule.waitUntilTagEnabled("menu-library", timeoutMillis = 10_000)
    }

    private fun androidx.compose.ui.test.junit4.AndroidComposeTestRule<*, *>.waitUntilTagExists(
        tag: String,
        timeoutMillis: Long = 5_000,
    ) {
        waitUntil(timeoutMillis) {
            onAllNodesWithTag(tag, useUnmergedTree = true)
                .fetchSemanticsNodes(atLeastOneRootRequired = false)
                .isNotEmpty()
        }
    }

    private fun androidx.compose.ui.test.junit4.AndroidComposeTestRule<*, *>.waitUntilTagGone(
        tag: String,
        timeoutMillis: Long = 5_000,
    ) {
        waitUntil(timeoutMillis) {
            onAllNodesWithTag(tag, useUnmergedTree = true)
                .fetchSemanticsNodes(atLeastOneRootRequired = false)
                .isEmpty()
        }
    }

    private fun androidx.compose.ui.test.junit4.AndroidComposeTestRule<*, *>.waitUntilTagDisplayed(
        tag: String,
        timeoutMillis: Long = 5_000,
    ) {
        waitUntil(timeoutMillis) {
            isTagDisplayed(tag)
        }
    }

    private fun androidx.compose.ui.test.junit4.AndroidComposeTestRule<*, *>.waitUntilTagEnabled(
        tag: String,
        timeoutMillis: Long = 5_000,
    ) {
        waitUntil(timeoutMillis) {
            isTagEnabled(tag)
        }
    }

    private fun androidx.compose.ui.test.junit4.AndroidComposeTestRule<*, *>.waitForPackReturnToMenu() {
        waitUntil(timeoutMillis = 12_000) {
            isTagPresent("badge-unlock-celebration") || isTagDisplayed("menu-open-pack")
        }
        if (isTagPresent("badge-unlock-celebration")) {
            waitUntilTagGone("badge-unlock-celebration", timeoutMillis = 5_000)
        }
        waitUntilTagEnabled("menu-open-pack", timeoutMillis = 10_000)
    }

    private fun androidx.compose.ui.test.junit4.AndroidComposeTestRule<*, *>.isTagDisplayed(tag: String): Boolean =
        runCatching {
            onNodeWithTag(tag).assertIsDisplayed()
            true
        }.getOrDefault(false)

    private fun androidx.compose.ui.test.junit4.AndroidComposeTestRule<*, *>.isTagPresent(tag: String): Boolean =
        onAllNodesWithTag(tag, useUnmergedTree = true)
            .fetchSemanticsNodes(atLeastOneRootRequired = false)
            .isNotEmpty()

    private fun androidx.compose.ui.test.junit4.AndroidComposeTestRule<*, *>.isTagEnabled(tag: String): Boolean =
        runCatching {
            onNodeWithTag(tag).assertIsEnabled()
            true
        }.getOrDefault(false)

    private fun androidx.compose.ui.test.junit4.AndroidComposeTestRule<*, *>.readText(tag: String): String =
        safeReadText(tag) ?: error("No text was found for tag $tag")

    private fun androidx.compose.ui.test.junit4.AndroidComposeTestRule<*, *>.safeReadText(tag: String): String? {
        val nodes = onAllNodesWithTag(tag, useUnmergedTree = true)
            .fetchSemanticsNodes(atLeastOneRootRequired = false)
        val node = nodes.firstOrNull() ?: return null
        if (!node.config.contains(SemanticsProperties.Text)) return null
        val textValues = node.config[SemanticsProperties.Text]
        return textValues.joinToString(separator = "") { annotated -> annotated.toString() }
    }

    private fun androidx.compose.ui.test.junit4.AndroidComposeTestRule<*, *>.firstNodeWithTag(
        tag: String,
        useUnmergedTree: Boolean = false,
    ) = onAllNodesWithTag(tag, useUnmergedTree = useUnmergedTree)[0]

    private fun androidx.compose.ui.test.junit4.AndroidComposeTestRule<*, *>.pressAndroidBack() {
        activity.runOnUiThread {
            activity.onBackPressedDispatcher.onBackPressed()
        }
    }
}
