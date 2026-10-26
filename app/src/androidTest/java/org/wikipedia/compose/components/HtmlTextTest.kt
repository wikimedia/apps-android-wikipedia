package org.wikipedia.compose.components

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.theme.Theme

@RunWith(AndroidJUnit4::class)
class HtmlTextTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun linkClickGoesToTheMostRecentListener() {
        val clickedBy = mutableListOf<String>()
        var listenerName by mutableStateOf("first")
        composeTestRule.setContent {
            BaseTheme(currentTheme = Theme.LIGHT) {
                val name = listenerName
                HtmlText(
                    text = "<a href=\"https://example.org\">link</a>",
                    linkInteractionListener = { clickedBy.add(name) }
                )
            }
        }

        listenerName = "second"
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("link").performClick()

        assertEquals(listOf("second"), clickedBy)
    }

    @Test
    fun textIsReparsedWhenItChanges() {
        var html by mutableStateOf("<b>before</b>")
        composeTestRule.setContent {
            BaseTheme(currentTheme = Theme.LIGHT) {
                HtmlText(text = html)
            }
        }
        composeTestRule.onNodeWithText("before").assertIsDisplayed()

        html = "<b>after</b>"

        composeTestRule.onNodeWithText("after").assertIsDisplayed()
    }
}
