package org.wikipedia.robots

import org.wikipedia.R
import org.wikipedia.base.BaseRobot
import org.wikipedia.base.TestConfig

class OnboardingRobot: BaseRobot() {
    fun completeOnboarding() = apply {
        repeat(3) {
            clickOnDisplayedView(R.id.fragment_onboarding_forward_button)
        }
        delay(TestConfig.DELAY_SHORT)
        clickOnDisplayedView(R.id.fragment_onboarding_done_button)
        delay(TestConfig.DELAY_MEDIUM)
    }

    fun dismissFeedCustomization() = apply {
        clicksOnDisplayedViewWithText(R.id.view_announcement_action_negative, "Got it")
        delay(TestConfig.DELAY_SHORT)
    }
}