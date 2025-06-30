package org.wikipedia.robots.feature

import BaseRobot
import android.content.Context
import android.util.Log
import android.view.KeyEvent
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.NoMatchingViewException
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.pressKey
import androidx.test.espresso.matcher.ViewMatchers.withId
import org.wikipedia.R
import org.wikipedia.base.TestConfig

class EditorRobot : BaseRobot() {
    val currentWikiText
        get() = input.getCurrentText(R.id.edit_section_text)

    fun replaceTextInEditWindow(text: String) = apply {
        input.replaceTextInView(R.id.edit_section_text, text)
        delay(TestConfig.DELAY_MEDIUM)
    }

    fun clickEditWindow() = apply {
        click.onViewWithId(R.id.edit_section_text)
        delay(TestConfig.DELAY_SHORT)
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

    fun applyUnderlineFormat() = apply {
        click.onViewWithId(R.id.wikitext_button_underline)
        delay(TestConfig.DELAY_SHORT)
    }

    fun applyStrikeThroughFormat() = apply {
        click.onViewWithId(R.id.wikitext_button_strikethrough)
        delay(TestConfig.DELAY_SHORT)
    }

    fun applySuperScriptFormat() = apply {
        click.onViewWithId(R.id.wikitext_button_sup)
        delay(TestConfig.DELAY_SHORT)
    }

    fun applySubScriptFormat() = apply {
        click.onViewWithId(R.id.wikitext_button_sub)
        delay(TestConfig.DELAY_SHORT)
    }

    fun applyLargeTextFormat() = apply {
        click.onViewWithId(R.id.wikitext_button_text_large)
        delay(TestConfig.DELAY_SHORT)
    }

    fun applySmallTextFormat() = apply {
        click.onViewWithId(R.id.wikitext_button_text_small)
        delay(TestConfig.DELAY_SHORT)
    }

    fun applyCodeFormat() = apply {
        click.onViewWithId(R.id.wikitext_button_code)
        delay(TestConfig.DELAY_SHORT)
    }

    fun clickHeadingFormats() = apply {
        click.onViewWithId(R.id.wikitext_button_heading)
        delay(TestConfig.DELAY_SHORT)
    }

    fun applyH2() = apply {
        click.onViewWithId(R.id.wikitext_button_h2)
        delay(TestConfig.DELAY_SHORT)
    }

    fun applyH3() = apply {
        click.onViewWithId(R.id.wikitext_button_h3)
        delay(TestConfig.DELAY_SHORT)
    }

    fun applyH4() = apply {
        click.onViewWithId(R.id.wikitext_button_h4)
        delay(TestConfig.DELAY_SHORT)
    }

    fun applyH5() = apply {
        click.onViewWithId(R.id.wikitext_button_h5)
        delay(TestConfig.DELAY_SHORT)
    }

    fun closeTextFormatting() = apply {
        click.onViewWithId(R.id.close_button)
        delay(TestConfig.DELAY_SHORT)
    }

    fun closeHeadlinesFormatting() = apply {
        click.onViewWithId(R.id.closeButton)
        delay(TestConfig.DELAY_SHORT)
    }

    fun closeKeyboard() = apply {
        input.closeKeyboard(R.id.edit_section_text)
        delay(TestConfig.DELAY_SHORT)
    }

    fun closeEditNotice() = apply {
        try {
            click.onViewWithId(R.id.editNoticeCloseButton)
        } catch (e: NoMatchingViewException) {
            Log.e("EditorRobot", "${e.message}")
        } catch (e: Exception) {
            Log.e("EditorRobot", "Unexpected Error: ${e.message}")
        }
    }

    fun clickUndoButton() = apply {
        click.onViewWithId(R.id.wikitext_button_undo)
        delay(TestConfig.DELAY_SHORT)
    }

    fun clickRedoButton() = apply {
        click.onViewWithId(R.id.wikitext_button_redo)
        delay(TestConfig.DELAY_SHORT)
    }

    fun clickInsertMediaButton() = apply {
        click.onViewWithId(R.id.wikitext_button_insert_media)
        delay(TestConfig.DELAY_MEDIUM)
    }

    fun clickInsertLinkButton() = apply {
        click.onViewWithId(R.id.wikitext_button_link)
        delay(TestConfig.DELAY_SHORT)
    }

    fun insertImageFrom(position: Int) = apply {
        list.clickOnItemInList(R.id.recyclerView, position)
        delay(TestConfig.DELAY_SHORT)
    }

    fun clickNext() = apply {
        click.onViewWithText("Next")
        delay(TestConfig.DELAY_LARGE)
    }

    fun clickPublish() = apply {
        click.onViewWithText("Publish")
        delay(TestConfig.DELAY_LARGE)
    }

    fun clickInsert() = apply {
        click.onViewWithText("Insert")
        delay(TestConfig.DELAY_LARGE)
    }

    fun checkMinorEdit() = apply {
        click.onViewWithId(R.id.minorEditCheckBox)
        delay(TestConfig.DELAY_SHORT)
    }

    fun selectSpecificText(targetText: String) = apply {
        val currentText = input.getCurrentText(R.id.edit_section_text)
        val position = findTextPosition(currentText, targetText)
        if (position != null) {
            input.selectText(R.id.edit_section_text, position.first, position.second)
        }
    }

    fun scrollToEndOfTextFormatting() = apply {
        swipe.left(R.id.wiki_text_keyboard_formatting_horizontal_scroll_view)
        delay(TestConfig.DELAY_SHORT)
    }

    fun verifyEditPublished(context: Context) = apply {
        delay(TestConfig.DELAY_LARGE)
        verify.messageOfSnackbar(context.getString(R.string.edit_saved_successfully))
    }

    private fun findTextPosition(fullText: String, targetText: String): Pair<Int, Int>? {
        val startIndex = fullText.indexOf(targetText)
        return if (startIndex != -1) {
            val endIndex = startIndex + targetText.length
            Pair(startIndex, endIndex)
        } else {
            null
        }
    }

    fun pressBack() = apply {
        goBack()
        delay(TestConfig.DELAY_SHORT)
    }
}
