package org.wikipedia.robots

import android.os.Build
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.withId
import com.google.android.apps.common.testing.accessibility.framework.utils.contrast.Color
import org.wikipedia.R
import org.wikipedia.TestUtil
import org.wikipedia.base.BaseRobot
import org.wikipedia.base.TestConfig

class AppThemeRobot : BaseRobot() {
    fun toggleTheme() = apply {
        clickOnDisplayedView(R.id.page_theme)
        delay(TestConfig.DELAY_MEDIUM)
    }

    fun switchOffMatchSystemTheme() = apply {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            checkViewDoesNotExist(R.id.theme_chooser_match_system_theme_switch)
        } else {
            scrollToViewAndClick(R.id.theme_chooser_match_system_theme_switch)
        }
        delay(TestConfig.DELAY_SHORT)
    }

    fun selectBlackTheme() = apply {
        scrollToViewAndClick(R.id.button_theme_black)
        delay(TestConfig.DELAY_MEDIUM)
    }

    fun verifyBackgroundIsBlack() = apply {
        onView(withId(R.id.page_actions_tab_layout)).check(matches(TestUtil.hasBackgroundColor(Color.BLACK)))
    }

    fun goBackToLightTheme() = apply {
        clickOnViewWithId(R.id.page_theme)
        delay(TestConfig.DELAY_SHORT)
        scrollToViewAndClick(R.id.button_theme_light)
        delay(TestConfig.DELAY_MEDIUM)
    }

    fun clickThemeIconOnEditPage() = apply {
        clickOnDisplayedView(R.id.menu_edit_theme)
        delay(TestConfig.DELAY_MEDIUM)
    }

    fun increaseTextSize() = apply {
        scrollToViewAndClick(R.id.buttonIncreaseTextSize)
        delay(TestConfig.DELAY_MEDIUM)
    }

    fun decreaseTextSize() = apply {
        scrollToViewAndClick(R.id.buttonDecreaseTextSize)
        delay(TestConfig.DELAY_MEDIUM)
    }

    fun applySerif() = apply {
        scrollToViewAndClick(R.id.button_font_family_serif)
        delay(TestConfig.DELAY_MEDIUM)
    }

    fun applySansSerif() = apply {
        scrollToViewAndClick(R.id.button_font_family_sans_serif)
        delay(TestConfig.DELAY_MEDIUM)
    }

    fun toggleReadingFocusMode() = apply {
        scrollToViewAndClick(R.id.theme_chooser_reading_focus_mode_switch)
        delay(TestConfig.DELAY_MEDIUM)
    }

    fun applySepiaTheme() = apply {
        scrollToViewAndClick(R.id.button_theme_sepia)
        delay(TestConfig.DELAY_MEDIUM)
    }

    fun applyLightTheme() = apply {
        scrollToViewAndClick(R.id.button_theme_light)
        delay(TestConfig.DELAY_MEDIUM)
    }

    fun applyDarkTheme() = apply {
        scrollToViewAndClick(R.id.button_theme_dark)
        delay(TestConfig.DELAY_MEDIUM)
    }

    fun applyBlackTheme() = apply {
        scrollToViewAndClick(R.id.button_theme_black)
        delay(TestConfig.DELAY_MEDIUM)
    }

    fun toggleMatchSystemTheme() = apply {
        scrollToViewAndClick(R.id.theme_chooser_match_system_theme_switch)
        delay(TestConfig.DELAY_MEDIUM)
    }

    fun backToHomeScreen() = apply {
        pressBack()
        pressBack()
    }

    fun pressBack() = apply {
        goBack()
        delay(TestConfig.DELAY_SHORT)
    }
}
