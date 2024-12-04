package org.wikipedia.tests.settings

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Test
import org.junit.runner.RunWith
import org.wikipedia.base.BaseTest
import org.wikipedia.main.MainActivity
import org.wikipedia.robots.SystemRobot
import org.wikipedia.robots.feature.SearchRobot
import org.wikipedia.robots.feature.SettingsRobot
import org.wikipedia.robots.navigation.BottomNavRobot
import org.wikipedia.robots.screen.LanguageListRobot

@LargeTest
@RunWith(AndroidJUnit4::class)
class ChangingLanguageTest : BaseTest<MainActivity>(
 activityClass = MainActivity::class.java
) {
    private val HEBREW = "Hebrew"
    private val JAPANESE = "Japanese"
    private val searchTerm = "apple"
    private val bottomNavRobot = BottomNavRobot()
    private val settingsRobot = SettingsRobot()
    private val languageListRobot = LanguageListRobot()
    private val searchRobot = SearchRobot()
    private val systemRobot = SystemRobot()

    @Test
    fun runTest() {
        systemRobot
            .clickOnSystemDialogWithText("Allow")
        bottomNavRobot
            .navigateToMoreMenu()
            .goToSettings()
        systemRobot
            .enableDarkMode(context = context)
        settingsRobot
            .clickLanguages()
        languageListRobot
            .addNewLanguage()
            .scrollToLanguageAndClick(HEBREW)
            .addNewLanguage()
            .openSearchLanguage()
        searchRobot
            .typeTextInView(JAPANESE)
        languageListRobot
            .scrollToLanguageAndClick(JAPANESE)
            .pressBack()
            .pressBack()
        bottomNavRobot
            .navigateToSearchPage()
        setDeviceOrientation(isLandscape = true)
        searchRobot
            .tapSearchView()
            .checkLanguageAvailability(JAPANESE)
            .checkLanguageAvailability(HEBREW)
            .clickLanguage(HEBREW)
            .typeTextInView(searchTerm)
        setDeviceOrientation(isLandscape = false)
    }
}
