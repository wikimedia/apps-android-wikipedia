package org.wikipedia.tests.settings

import android.graphics.Typeface
import android.widget.TextView
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import junit.framework.TestCase.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.wikipedia.R
import org.wikipedia.base.BaseTest
import org.wikipedia.robots.AppThemeRobot
import org.wikipedia.robots.SystemRobot
import org.wikipedia.theme.ThemeFittingRoomActivity
import org.wikipedia.theme.ThemeFittingRoomFragment

@LargeTest
@RunWith(AndroidJUnit4::class)
class FontChangeTest : BaseTest<ThemeFittingRoomActivity>(
 activityClass = ThemeFittingRoomActivity::class.java
) {
    private val appThemeRobot = AppThemeRobot()
    private val systemRobot = SystemRobot()

    @Test
    fun runTest() {
        systemRobot
            .clickOnSystemDialogWithText("Allow")
        appThemeRobot
            .applySerif()

        activityScenarioRule.scenario.onActivity { newActivity ->
            val fragment = newActivity.supportFragmentManager
                .findFragmentById(R.id.fragment_container) as ThemeFittingRoomFragment
            val textView = fragment.requireView().findViewById<TextView>(R.id.theme_test_text)
            assertTrue("Font should be Serif", textView.typeface == Typeface.SERIF)
        }

        appThemeRobot
            .applySansSerif()

        activityScenarioRule.scenario.onActivity { newActivity ->
            activity = newActivity
            val fragment = newActivity.supportFragmentManager
                .findFragmentById(R.id.fragment_container) as ThemeFittingRoomFragment
            val textView = fragment.requireView().findViewById<TextView>(R.id.theme_test_text)
            assertTrue("Font should be Serif", textView.typeface == Typeface.SANS_SERIF)
        }
    }
}
