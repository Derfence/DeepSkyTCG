package fr.aumombelli.gatcha

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeUp
import org.junit.Rule
import org.junit.Test

class LocalEndToEndTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun local_end_to_end_flow() {
        createAccountAndReachMainMenu()

        val firstDrawnCardId = openPackAndCaptureVisibleCardId()

        verifyLibraryContainsDrawnCard(firstDrawnCardId)
        verifyCooldownIsVisible()
        verifyInvalidLoginShowsError()
    }

    private fun createAccountAndReachMainMenu() {
        composeRule.waitUntilTagDisplayed("login-toggle-mode", timeoutMillis = 20_000)
        composeRule.onNodeWithTag("login-toggle-mode").performClick()
        composeRule.waitUntilTagDisplayed("login-email")

        composeRule.replaceText("login-username", LocalE2eConfig.username)
        composeRule.replaceText("login-email", LocalE2eConfig.email)
        composeRule.replaceText("login-password", LocalE2eConfig.password)
        composeRule.onNodeWithTag("login-submit").performClick()

        composeRule.waitUntil(timeoutMillis = 20_000) {
            composeRule.isTagEnabled("menu-open-pack") || composeRule.isTagDisplayed("login-error")
        }
        if (composeRule.isTagDisplayed("login-error")) {
            composeRule.onNodeWithTag("login-error")
                .assertTextContains("already exists", substring = true)
            composeRule.onNodeWithTag("login-toggle-mode").performClick()
            composeRule.waitUntilTagGone("login-email", timeoutMillis = 10_000)
            composeRule.waitUntilTagDisplayed("login-submit", timeoutMillis = 10_000)
            composeRule.replaceText("login-username", LocalE2eConfig.username)
            composeRule.replaceText("login-password", LocalE2eConfig.password)
            composeRule.onNodeWithTag("login-submit").performClick()
        }

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
        composeRule.waitUntilTagExists("pack-opening-card-id", timeoutMillis = 10_000)

        val firstDrawnCardId = composeRule.readText("pack-opening-card-id")
        composeRule.firstNodeWithTag("pack-opening-card-surface").performClick()
        composeRule.waitUntilTagExists("astro-card-fullscreen-close", timeoutMillis = 10_000)
        composeRule.onNodeWithTag("astro-card-fullscreen-close").performClick()
        composeRule.waitUntilTagExists("pack-opening-card-surface", timeoutMillis = 10_000)

        repeat(4) {
            composeRule.firstNodeWithTag("pack-opening-card-surface").performTouchInput { swipeLeft() }
            composeRule.waitForIdle()
        }

        composeRule.firstNodeWithTag("pack-opening-card-surface").performTouchInput { swipeUp() }
        composeRule.waitUntilTagEnabled("menu-open-pack", timeoutMillis = 10_000)
        return firstDrawnCardId
    }

    private fun verifyLibraryContainsDrawnCard(cardId: String) {
        composeRule.waitUntilTagEnabled("menu-library", timeoutMillis = 10_000)
        composeRule.onNodeWithTag("menu-library").performClick()
        composeRule.waitUntilTagExists("library-section-${LocalE2eConfig.extensionId}", timeoutMillis = 10_000)

        composeRule.onNodeWithTag("library-grid").performScrollToNode(hasTestTag("library-card-$cardId"))
        composeRule.onNodeWithTag("library-card-$cardId").assertIsDisplayed()
        composeRule.onNodeWithTag("library-owned-$cardId").assertTextContains("Owned:", substring = true)
        composeRule.onNodeWithTag("library-card-$cardId").performClick()
        composeRule.waitUntilTagExists("library-card-preview", timeoutMillis = 10_000)
        composeRule.onNodeWithTag("library-card-preview-surface").performClick()
        composeRule.waitUntilTagExists("astro-card-fullscreen-close", timeoutMillis = 10_000)
        composeRule.onNodeWithTag("astro-card-fullscreen-close").performClick()

        composeRule.waitUntilTagGone("astro-card-fullscreen-close", timeoutMillis = 10_000)
        composeRule.pressAndroidBack()
        composeRule.waitUntilTagEnabled("menu-open-pack", timeoutMillis = 10_000)
    }

    private fun verifyCooldownIsVisible() {
        composeRule.waitUntilTagEnabled("menu-open-pack", timeoutMillis = 10_000)
        composeRule.onNodeWithTag("menu-open-pack").performClick()
        composeRule.waitUntilTagExists("pack-extension-enter-${LocalE2eConfig.extensionId}", timeoutMillis = 15_000)
        composeRule.onNodeWithTag("pack-status").assertTextContains("Prochain tirage disponible", substring = true)
        composeRule.onNodeWithTag("pack-extension-enter-${LocalE2eConfig.extensionId}").assertIsNotEnabled()
        composeRule.pressAndroidBack()
        composeRule.waitUntilTagEnabled("menu-logout", timeoutMillis = 10_000)
    }

    private fun verifyInvalidLoginShowsError() {
        composeRule.waitUntilTagEnabled("menu-logout", timeoutMillis = 10_000)
        composeRule.onNodeWithTag("menu-logout").performClick()
        composeRule.waitUntilTagDisplayed("login-submit", timeoutMillis = 20_000)

        composeRule.replaceText("login-username", LocalE2eConfig.username)
        composeRule.replaceText("login-password", "${LocalE2eConfig.password}-wrong")
        composeRule.onNodeWithTag("login-submit").performClick()

        composeRule.waitUntilTagExists("login-error", timeoutMillis = 10_000)
        composeRule.onNodeWithTag("login-error").assertTextContains("Invalid credentials", substring = true)
    }

    private fun androidx.compose.ui.test.junit4.AndroidComposeTestRule<*, *>.replaceText(
        tag: String,
        value: String,
    ) {
        val interaction = onNodeWithTag(tag)
        interaction.performTextClearance()
        interaction.performTextInput(value)
    }

    private fun androidx.compose.ui.test.junit4.AndroidComposeTestRule<*, *>.waitUntilTagExists(
        tag: String,
        timeoutMillis: Long = 5_000,
    ) {
        waitUntil(timeoutMillis) {
            onAllNodesWithTag(tag).fetchSemanticsNodes(atLeastOneRootRequired = false).isNotEmpty()
        }
    }

    private fun androidx.compose.ui.test.junit4.AndroidComposeTestRule<*, *>.waitUntilTagGone(
        tag: String,
        timeoutMillis: Long = 5_000,
    ) {
        waitUntil(timeoutMillis) {
            onAllNodesWithTag(tag).fetchSemanticsNodes(atLeastOneRootRequired = false).isEmpty()
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

    private fun androidx.compose.ui.test.junit4.AndroidComposeTestRule<*, *>.isTagDisplayed(tag: String): Boolean =
        runCatching {
            onNodeWithTag(tag).assertIsDisplayed()
            true
        }.getOrDefault(false)

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
