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
            .typeInEditWindow("What is Espresso Test?\n")
            .typeInEditWindow("\nThis is a bold text.")
            .selectLastTypedText("This is a bold text.")
            .clickTextFormatButton()
            .applyBoldFormat()
            .typeInEditWindow("\nThis is an Italic text.")
            .selectLastTypedText("This is an Italic text.")
            .applyItalicFormat()
    }
}
