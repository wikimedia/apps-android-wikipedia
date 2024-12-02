package org.wikipedia.tests.settings

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Test
import org.junit.runner.RunWith
import org.wikipedia.base.BaseTest
import org.wikipedia.robots.feature.SettingsRobot
import org.wikipedia.settings.SettingsActivity

@LargeTest
@RunWith(AndroidJUnit4::class)
class AboutSettingsTest : BaseTest<SettingsActivity>(
 activityClass = SettingsActivity::class.java
) {
    private val settingsRobot = SettingsRobot()

    @Test
    fun runTest() {
        settingsRobot
            .clickAboutWikipediaAppOptionItem()
            .activateDeveloperMode()
            .pressBack()
            .clickDeveloperMode()
            .assertWeAreInDeveloperSettings()
    }
}
