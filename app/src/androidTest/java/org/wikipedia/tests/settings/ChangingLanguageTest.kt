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
import org.wikipedia.theme.Theme

@LargeTest
@RunWith(AndroidJUnit4::class)
class ChangingLanguageTest : BaseTest<MainActivity>(
 activityClass = MainActivity::class.java
) {
    private val HEBREW = "Hebrew"
    private val JAPANESE = "Japanese"
    private val LANG_CODE_JAPANESE = "ja"
    private val LANG_CODE_HEBREW = "he"
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
            .assertAddLanguageTextColor(theme = Theme.DARK)
            .addNewLanguage()
            .scrollToLanguageAndClick(HEBREW)
        systemRobot
            .disableDarkMode(context)
        languageListRobot
            .addNewLanguage()
            .openSearchLanguage()
        searchRobot
            .typeTextInView(JAPANESE)
        languageListRobot
            .assertJapaneseLanguageTextColor(theme = Theme.LIGHT)
            .scrollToLanguageAndClick(JAPANESE)
            .pressBack()
            .pressBack()
        bottomNavRobot
            .navigateToSearchPage()
        setDeviceOrientation(isLandscape = true)
        searchRobot
            .tapSearchView()
            .checkLanguageAvailability(LANG_CODE_JAPANESE)
            .checkLanguageAvailability(LANG_CODE_HEBREW)
            .clickLanguage(LANG_CODE_HEBREW)
            .typeTextInView(searchTerm)
            .checkSearchListItemHasRTLDirection()
        setDeviceOrientation(isLandscape = false)
    }
}
