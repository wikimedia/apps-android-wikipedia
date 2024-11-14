package org.wikipedia.test.loggedoutuser

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Test
import org.junit.runner.RunWith
import org.wikipedia.Constants
import org.wikipedia.FakeData
import org.wikipedia.base.BaseTest
import org.wikipedia.page.PageActivity
import org.wikipedia.page.PageActivity.Companion.ACTION_LOAD_IN_CURRENT_TAB
import org.wikipedia.page.PageActivity.Companion.EXTRA_HISTORYENTRY
import org.wikipedia.robots.ThemeRobot
import org.wikipedia.robots.feature.EditorRobot
import org.wikipedia.robots.feature.PageRobot

@LargeTest
@RunWith(AndroidJUnit4::class)
class EditArticleTest : BaseTest<PageActivity>(
    PageActivity::class.java,
    {
        action = ACTION_LOAD_IN_CURRENT_TAB
        putExtra(EXTRA_HISTORYENTRY, FakeData.historyEntry)
        putExtra(Constants.ARG_TITLE, FakeData.historyEntry.title)
    }
) {
    private val editorRobot = EditorRobot()
    private val themeRobot = ThemeRobot()
    private val pageRobot = PageRobot()

    @Test
    fun startEditTest() {
        pageRobot
            .clickEditPencilAtTopOfArticle()
        editorRobot
            .clickEditIntroductionMenuItem()
            .dismissDialogIfShown()
        themeRobot
            .clickThemeIconOnEditPage()
            .increaseTextSize()
            .increaseTextSize()
            .pressBack()
            .clickThemeIconOnEditPage()
            .decreaseTextSize()
            .decreaseTextSize()
            .pressBack()
        editorRobot
            .typeInEditWindow()
            .tapNext()
            .tapNext()
            .clickDefaultEditSummaryChoices()
            .navigateUp()
            .navigateUp()
            .navigateUp()
            .remainInEditWorkflow()
            .pressBack()
            .leaveEditWorkflow()
        pageRobot
            .launchTabsScreen()
    }
}
