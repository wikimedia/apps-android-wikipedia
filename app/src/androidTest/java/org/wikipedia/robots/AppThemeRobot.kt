package org.wikipedia.robots

import BaseRobot
import android.os.Build
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.withId
import com.google.android.apps.common.testing.accessibility.framework.utils.contrast.Color
import org.wikipedia.R
import org.wikipedia.TestUtil
import org.wikipedia.base.TestConfig

class AppThemeRobot : BaseRobot() {
    fun toggleTheme() = apply {
        click.onDisplayedView(R.id.page_theme)
        delay(TestConfig.DELAY_MEDIUM)
    }

    fun switchOffMatchSystemTheme() = apply {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            verify.viewWithIdIsNotVisible(R.id.theme_chooser_match_system_theme_switch)
        } else {
            scroll.toViewAndClick(R.id.theme_chooser_match_system_theme_switch)
        }
        delay(TestConfig.DELAY_SHORT)
    }

    fun selectBlackTheme() = apply {
        scroll.toViewAndClick(R.id.button_theme_black)
        delay(TestConfig.DELAY_MEDIUM)
    }

    fun verifyBackgroundIsBlack() = apply {
        onView(withId(R.id.page_actions_tab_layout)).check(matches(TestUtil.hasBackgroundColor(Color.BLACK)))
    }

    fun goBackToLightTheme() = apply {
        click.onViewWithId(R.id.page_theme)
        delay(TestConfig.DELAY_SHORT)
        scroll.toViewAndClick(R.id.button_theme_light)
        delay(TestConfig.DELAY_MEDIUM)
    }

    fun clickThemeIconOnEditPage() = apply {
        click.onDisplayedView(R.id.menu_edit_theme)
        delay(TestConfig.DELAY_MEDIUM)
    }

    fun increaseTextSize() = apply {
        scroll.toViewAndClick(R.id.buttonIncreaseTextSize)
        delay(TestConfig.DELAY_MEDIUM)
    }

    fun decreaseTextSize() = apply {
        scroll.toViewAndClick(R.id.buttonDecreaseTextSize)
        delay(TestConfig.DELAY_MEDIUM)
    }

    fun applySerif() = apply {
        scroll.toViewAndClick(R.id.button_font_family_serif)
        delay(TestConfig.DELAY_MEDIUM)
    }

    fun applySansSerif() = apply {
        scroll.toViewAndClick(R.id.button_font_family_sans_serif)
        delay(TestConfig.DELAY_MEDIUM)
    }

    fun toggleReadingFocusMode() = apply {
        scroll.toViewAndClick(R.id.theme_chooser_reading_focus_mode_switch)
        delay(TestConfig.DELAY_MEDIUM)
    }

    fun applySepiaTheme() = apply {
        scroll.toViewAndClick(R.id.button_theme_sepia)
        delay(TestConfig.DELAY_MEDIUM)
    }

    fun applyLightTheme() = apply {
        scroll.toViewAndClick(R.id.button_theme_light)
        delay(TestConfig.DELAY_MEDIUM)
    }

    fun applyDarkTheme() = apply {
        scroll.toViewAndClick(R.id.button_theme_dark)
        delay(TestConfig.DELAY_MEDIUM)
    }

    fun applyBlackTheme() = apply {
        scroll.toViewAndClick(R.id.button_theme_black)
        delay(TestConfig.DELAY_MEDIUM)
    }

    fun toggleMatchSystemTheme() = apply {
        scroll.toViewAndClick(R.id.theme_chooser_match_system_theme_switch)
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
