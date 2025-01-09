package org.wikipedia.tests.settings

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Test
import org.junit.runner.RunWith
import org.wikipedia.base.BaseTest
import org.wikipedia.main.MainActivity
import org.wikipedia.robots.DialogRobot
import org.wikipedia.robots.SystemRobot
import org.wikipedia.robots.feature.PageRobot
import org.wikipedia.robots.feature.SearchRobot
import org.wikipedia.robots.feature.SettingsRobot
import org.wikipedia.robots.navigation.BottomNavRobot

@LargeTest
@RunWith(AndroidJUnit4::class)
class CollapseTablesTest : BaseTest<MainActivity>(
 activityClass = MainActivity::class.java
) {

    private val bottomNavRobot = BottomNavRobot()
    private val settingsRobot = SettingsRobot()
    private val searchRobot = SearchRobot()
    private val pageRobot = PageRobot()
    private val systemRobot = SystemRobot()
    private val dialogRobot = DialogRobot()

    @Test
    fun runTest() {
        systemRobot
            .clickOnSystemDialogWithText("Allow")
        bottomNavRobot
            .navigateToSearchPage()
        searchRobot
            .tapSearchView()
            .typeTextInView("apple")
            .clickOnItemFromSearchList(0)
        pageRobot
            .scrollToCollapsingTables()
            .assertCollapsingTableIsVisible(isVisible = false)
            .pressBack()
            .pressBack()
            .pressBack()
        bottomNavRobot
            .navigateToMoreMenu()
            .goToSettings()
        settingsRobot
            .toggleCollapseTables()
            .pressBack()
        searchRobot
            .tapSearchView()
            .typeTextInView("apple")
            .clickOnItemFromSearchList(0)
        dialogRobot
            .dismissBigEnglishDialog()
            .dismissContributionDialog()
        pageRobot
            .scrollToCollapsingTables()
            .assertCollapsingTableIsVisible(isVisible = true)
    }
}
