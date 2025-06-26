package org.wikipedia.tests.editing

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Test
import org.junit.runner.RunWith
import org.wikipedia.Constants
import org.wikipedia.FakeData
import org.wikipedia.base.BaseTest
import org.wikipedia.base.DataInjector
import org.wikipedia.page.PageActivity
import org.wikipedia.page.PageActivity.Companion.ACTION_LOAD_IN_CURRENT_TAB
import org.wikipedia.page.PageActivity.Companion.EXTRA_HISTORYENTRY
import org.wikipedia.robots.DialogRobot
import org.wikipedia.robots.feature.EditorRobot
import org.wikipedia.robots.feature.PageActionItemRobot
import org.wikipedia.robots.feature.PageRobot

@LargeTest
@RunWith(AndroidJUnit4::class)
class ArticleEditingTest : BaseTest<PageActivity>(
 activityClass = PageActivity::class.java,
 dataInjector = DataInjector(
        intentBuilder = {
            action = ACTION_LOAD_IN_CURRENT_TAB
            putExtra(EXTRA_HISTORYENTRY, FakeData.testSiteHistoryEntry)
            putExtra(Constants.ARG_TITLE, FakeData.testSiteHistoryEntry.title)
        }
    )
) {
    private val editorRobot = EditorRobot()
    private val dialogRobot = DialogRobot()
    private val pageRobot = PageRobot(context)
    private val pageActionItemRobot = PageActionItemRobot()
    val boldText = "Bold text"
    val italicText = "Italic text"
    val underlineText = "Underline text"
    val strikeThroughText = "Strikethrough text"
    val superScript = "2"
    val subScript = "10"
    val fullText = "What is Espresso Test?\n\n" +
            "$boldText\n\n" +
            "$italicText\n\n" +
            "$underlineText\n\n" +
            "$strikeThroughText\n\n" +
            "X${superScript}\n\n" +
            "X$subScript\n\n"
    @Test
    fun runTest() {
        pageRobot
            .clickOverFlowMenuToolbar()
        pageActionItemRobot
            .clickEditArticles()
        dialogRobot
            .click("Got it")
        editorRobot
            .replaceTextInEditWindow("")
            .typeInEditWindow(fullText)
            .selectSpecificText(boldText)
            .clickTextFormatButton()
            .applyBoldFormat()
            .selectSpecificText(italicText)
            .applyItalicFormat()
            .selectSpecificText(underlineText)
            .applyUnderlineFormat()
            .selectSpecificText(strikeThroughText)
            .applyStrikeThroughFormat()
            .selectSpecificText(superScript)
            .applySuperScriptFormat()
            .selectSpecificText(subScript)
            .applySuperScriptFormat()
            .closeKeyboard()
            .showPreview()
    }
}
