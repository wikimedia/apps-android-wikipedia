package org.wikipedia.robots.feature

import android.content.res.Resources
import android.view.View
import androidx.test.espresso.matcher.BoundedMatcher
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.wikipedia.R
import org.wikipedia.base.BaseRobot
import org.wikipedia.base.TestConfig

class OnboardingRobot : BaseRobot() {
    fun moveAllTheWayToEndUsingTapButton() = apply {
        repeat(3) {
            clickOnDisplayedView(R.id.fragment_onboarding_forward_button)
        }
        delay(TestConfig.DELAY_SHORT)
    }

    fun checkWelcomeScreenViewsForVisibility() = apply {
        checkViewExists(R.id.imageViewCentered)
        checkViewExists(R.id.primaryTextView)
        checkViewExists(R.id.secondaryTextView)
        checkViewExists(R.id.addLanguageButton)
        checkViewExists(R.id.fragment_onboarding_skip_button)
        checkViewExists(R.id.fragment_onboarding_forward_button)
        delay(TestConfig.DELAY_SHORT)
    }

    fun swipeAllTheWayToEnd() = apply {
        repeat(3) {
            swipeLeft(R.id.fragment_pager)
        }
        delay(TestConfig.DELAY_SHORT)
    }

    fun swipeBackToWelcomeScreen() = apply {
        repeat(3) {
            swipeRight(R.id.fragment_pager)
        }
        delay(TestConfig.DELAY_SHORT)
    }

    fun skipWelcomeScreen() = apply {
        clickOnDisplayedView(R.id.fragment_onboarding_skip_button)
        delay(TestConfig.DELAY_SHORT)
    }

    fun verifyAppLanguageMatchesDeviceLanguage() = apply {
        verifyWithMatcher(viewId = R.id.primaryTextView, matcher = matchesDeviceLanguage())
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
