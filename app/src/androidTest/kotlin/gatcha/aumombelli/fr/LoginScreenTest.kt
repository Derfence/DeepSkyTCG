package fr.aumombelli.gatcha

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test

class LoginScreenTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun login_screen_is_shown_on_launch() {
        composeRule.onNodeWithText("Gatcha").assertIsDisplayed()
        composeRule.onNodeWithText("Login").assertIsDisplayed()
    }
}
