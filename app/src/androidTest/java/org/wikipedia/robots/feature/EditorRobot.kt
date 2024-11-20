package org.wikipedia.robots.feature

import org.wikipedia.R
import org.wikipedia.base.BaseRobot
import org.wikipedia.base.TestConfig

class EditorRobot : BaseRobot() {
    fun clickEditIntroductionMenuItem() = apply {
        clicksOnDisplayedViewWithText(viewId = R.id.title, text = "Edit introduction")
        delay(TestConfig.DELAY_LARGE)
    }

    fun dismissDialogIfShown() = apply {
        performIfDialogShown(dialogText = "Got it", action = {
            clickOnViewWithText("Got it")
        })
    }

    fun typeInEditWindow() = apply {
        typeTextInView(R.id.edit_section_text, "abc")
        delay(TestConfig.DELAY_MEDIUM)
    }

    fun tapNext() = apply {
        // can be flaky
        clickOnDisplayedView(R.id.edit_actionbar_button_text)
        delay(TestConfig.DELAY_MEDIUM)
    }

    fun clickDefaultEditSummaryChoices() = apply {
        scrollToTextAndClick("Fixed typo")
        delay(TestConfig.DELAY_MEDIUM)
    }

    fun navigateUp() = apply {
        clickOnDisplayedViewWithContentDescription("Navigate up")
        delay(TestConfig.DELAY_SHORT)
    }

    fun remainInEditWorkflow() = apply {
        clicksOnDisplayedViewWithText(android.R.id.button2, "No")
        delay(TestConfig.DELAY_SHORT)
    }

    fun leaveEditWorkflow() = apply {
        clicksOnDisplayedViewWithText(android.R.id.button1, "Yes")
        delay(TestConfig.DELAY_SHORT)
    }

    fun pressBack() = apply {
        goBack()
        delay(TestConfig.DELAY_SHORT)
    }
}
