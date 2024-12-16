package org.wikipedia.tests.settings

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import junit.framework.TestCase.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.wikipedia.WikipediaApp
import org.wikipedia.base.BaseTest
import org.wikipedia.robots.AppThemeRobot
import org.wikipedia.robots.SystemRobot
import org.wikipedia.theme.Theme
import org.wikipedia.theme.ThemeFittingRoomActivity

@LargeTest
@RunWith(AndroidJUnit4::class)
class AppThemeTest : BaseTest<ThemeFittingRoomActivity>(
 activityClass = ThemeFittingRoomActivity::class.java
) {

    private val appThemeRobot = AppThemeRobot()
    private val systemRobot = SystemRobot()

    @Test
    fun runTest() {
        systemRobot
            .disableDarkMode(context)

        testLightMode()
        systemRobot
            .enableDarkMode(context)
        testDarkMode()
        appThemeRobot
            .toggleMatchSystemTheme()
        testLightMode()
        testDarkMode()
    }

    private fun testDarkMode() {
        var currentTheme = getThemeAttribute()

        appThemeRobot
            .applyDarkTheme()
        var newTheme = getThemeAttribute()
        assertNotEquals("Theme should change to Black", currentTheme, newTheme)
        assertEquals("Theme should be Black", newTheme.resourceId, Theme.DARK.resourceId)
        currentTheme = newTheme

        appThemeRobot
            .applyBlackTheme()
        newTheme = getThemeAttribute()
        assertNotEquals("Theme should change to Dark", currentTheme, newTheme)
        assertEquals("Theme should be Dark", newTheme.resourceId, Theme.BLACK.resourceId)
    }

    private fun testLightMode() {
        var currentTheme = getThemeAttribute()
        appThemeRobot
            .applySepiaTheme()
        var newTheme = getThemeAttribute()
        assertNotEquals("Theme should change to Sepia", currentTheme, newTheme)
        assertEquals("Theme should be Sepia", newTheme.resourceId, Theme.SEPIA.resourceId)
        currentTheme = newTheme

        appThemeRobot
            .applyLightTheme()
        newTheme = getThemeAttribute()
        assertNotEquals("Theme should change to Light", currentTheme, newTheme)
        assertEquals("Theme should be Light", newTheme.resourceId, Theme.LIGHT.resourceId)
    }

    private fun getThemeAttribute(): Theme {
        var currentTheme = Theme.LIGHT
        activityScenarioRule.scenario.onActivity { activity ->
            currentTheme = WikipediaApp.instance.currentTheme
        }
        return currentTheme
    }
}
