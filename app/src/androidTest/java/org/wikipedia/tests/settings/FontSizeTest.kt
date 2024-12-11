package org.wikipedia.tests.settings

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
class FontSizeTest : BaseTest<ThemeFittingRoomActivity>(
 activityClass = ThemeFittingRoomActivity::class.java
) {
    private val appThemeRobot = AppThemeRobot()
    private val systemRobot = SystemRobot()

    @Test
    fun runTest() {
        systemRobot
            .clickOnSystemDialogWithText("Allow")
        val fragment = activity.supportFragmentManager
            .findFragmentById(R.id.fragment_container) as ThemeFittingRoomFragment
        val textView = fragment.requireView().findViewById<TextView>(R.id.theme_test_text)
        var currentSize = 0f
        var newSize = 0f
        repeat(2) {
            appThemeRobot
                .increaseTextSize()
            newSize = textView.textSize
            assertTrue("new size $newSize should be greater than current size $currentSize", newSize > currentSize)
            currentSize = textView.textSize
        }

        repeat(2) {
            appThemeRobot
                .decreaseTextSize()
            newSize = textView.textSize
            assertTrue("new size $newSize should be less than current size $currentSize", newSize < currentSize)
            currentSize = textView.textSize
        }
    }
}
