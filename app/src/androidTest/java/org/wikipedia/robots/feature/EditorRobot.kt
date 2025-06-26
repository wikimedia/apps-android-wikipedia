package org.wikipedia.robots.feature

import BaseRobot
import android.view.KeyEvent
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.pressKey
import androidx.test.espresso.matcher.ViewMatchers.withId
import org.wikipedia.R
import org.wikipedia.base.TestConfig

class EditorRobot : BaseRobot() {
    fun replaceTextInEditWindow(text: String) = apply {
        input.replaceTextInView(R.id.edit_section_text, text)
        delay(TestConfig.DELAY_MEDIUM)
    }

    fun typeInEditWindow(text: String) = apply {
        input.typeInEditText(R.id.edit_section_text, text)
        delay(TestConfig.DELAY_SHORT)
    }

    fun setCursorToEnd() = apply {
        // Using standard Espresso to move cursor to end
        onView(withId(R.id.edit_section_text))
            .perform(click())
            .perform(pressKey(KeyEvent.KEYCODE_MOVE_END))
        delay(TestConfig.DELAY_SHORT)
    }

    fun tapNext() = apply {
        // can be flaky
        click.onDisplayedView(R.id.edit_actionbar_button_text)
        delay(TestConfig.DELAY_MEDIUM)
    }

    fun clickDefaultEditSummaryChoices() = apply {
        scroll.toTextAndClick("Fixed typo")
        delay(TestConfig.DELAY_MEDIUM)
    }

    fun navigateUp() = apply {
        click.onDisplayedViewWithContentDescription("Navigate up")
        delay(TestConfig.DELAY_SHORT)
    }

    fun remainInEditWorkflow() = apply {
        click.onDisplayedViewWithText(android.R.id.button2, "No")
        delay(TestConfig.DELAY_SHORT)
    }

    fun leaveEditWorkflow() = apply {
        click.onDisplayedViewWithText(android.R.id.button1, "Yes")
        delay(TestConfig.DELAY_SHORT)
    }

    fun clickTextFormatButton() = apply {
        click.onViewWithId(R.id.wikitext_button_text_format)
    }

    fun applyBoldFormat() = apply {
        click.onViewWithId(R.id.wikitext_button_bold)
    }

    fun applyItalicFormat() = apply {
        click.onViewWithId(R.id.wikitext_button_italic)
        delay(TestConfig.DELAY_SHORT)
    }

    fun selectText(start: Int, end: Int) = apply {
        input.selectText(R.id.edit_section_text, start, end)
    }

    fun selectLastTypedText(text: String) = apply {
        input.selectLastTypedText(R.id.edit_section_text, text)
    }

    fun pressBack() = apply {
        goBack()
        delay(TestConfig.DELAY_SHORT)
    }
}
