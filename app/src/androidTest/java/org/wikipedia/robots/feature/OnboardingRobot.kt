package org.wikipedia.robots.feature

import BaseRobot
import android.content.res.Resources
import android.view.View
import androidx.test.espresso.matcher.BoundedMatcher
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.wikipedia.R
import org.wikipedia.base.TestConfig
import org.wikipedia.base.TestThemeColorType
import org.wikipedia.base.TestWikipediaColors
import org.wikipedia.theme.Theme

class OnboardingRobot : BaseRobot() {

    fun checkPrimaryTextViewColor(theme: Theme) = apply {
        val color = TestWikipediaColors.getGetColor(theme = theme, colorType = TestThemeColorType.PRIMARY)
        verify.textViewColor(textViewId = R.id.primaryTextView, colorResId = color)
        delay(TestConfig.DELAY_MEDIUM)
    }

    fun checkSecondaryTextViewColor(theme: Theme) = apply {
        val color = TestWikipediaColors.getGetColor(theme = theme, colorType = TestThemeColorType.SECONDARY)
        verify.textViewColor(textViewId = R.id.secondaryTextView, colorResId = color)
        delay(TestConfig.DELAY_MEDIUM)
    }

    fun moveAllTheWayToEndUsingTapButton() = apply {
        repeat(3) {
            click.onDisplayedView(R.id.fragment_onboarding_forward_button)
        }
        delay(TestConfig.DELAY_SHORT)
    }

    fun checkWelcomeScreenViewsForVisibility() = apply {
        verify.viewExists(R.id.imageViewCentered)
        verify.viewExists(R.id.primaryTextView)
        verify.viewExists(R.id.secondaryTextView)
        verify.viewExists(R.id.addLanguageButton)
        verify.viewExists(R.id.fragment_onboarding_skip_button)
        verify.viewExists(R.id.fragment_onboarding_forward_button)
        delay(TestConfig.DELAY_SHORT)
    }

    fun swipeAllTheWayToEnd() = apply {
        repeat(3) {
            swipe.left(R.id.fragment_pager)
        }
        delay(TestConfig.DELAY_SHORT)
    }

    fun swipeBackToWelcomeScreen() = apply {
        repeat(3) {
            swipe.right(R.id.fragment_pager)
        }
        delay(TestConfig.DELAY_SHORT)
    }

    fun skipWelcomeScreen() = apply {
        click.onDisplayedView(R.id.fragment_onboarding_skip_button)
        delay(TestConfig.DELAY_SHORT)
    }

    fun verifyAppLanguageMatchesDeviceLanguage() = apply {
        verify.withMatcher(viewId = R.id.primaryTextView, matcher = matchesDeviceLanguage())
    }

    private fun matchesDeviceLanguage(): Matcher<View> {
        return object : BoundedMatcher<View, View> (View::class.java) {
            override fun describeTo(description: Description?) {
                description?.appendText("with locale matching device locale")
            }

            override fun matchesSafely(item: View?): Boolean {
                val deviceLocale = Resources.getSystem().configuration.locales[0]
                val appLocale = item?.resources?.configuration?.locales?.get(0)
                return appLocale == deviceLocale
            }
        }
    }
}
