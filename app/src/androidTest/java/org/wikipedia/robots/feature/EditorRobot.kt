package org.wikipedia.robots.feature

import org.wikipedia.R
import org.wikipedia.base.TestConfig
import org.wikipedia.base.base.BaseRobot

class EditorRobot : BaseRobot() {
    fun clickEditIntroductionMenuItem() = apply {
       click.onDisplayedViewWithText(viewId = R.id.title, text = "Edit introduction")
        delay(TestConfig.DELAY_LARGE)
    }

    fun typeInEditWindow() = apply {
        input.typeTextInView(R.id.edit_section_text, "abc")
        delay(TestConfig.DELAY_MEDIUM)
    }

    fun tapNext() = apply {
        // can be flaky
        click.onDisplayedView(R.id.edit_actionbar_button_text)
        delay(TestConfig.DELAY_MEDIUM)
    }

    fun clickDefaultEditSummaryChoices() = apply {
        scroll.toTextAndClick("Fixed typo")
        delay(TestConfig.DELAY_MEDIUM)
    }

    fun navigateUp() = apply {
        click.onDisplayedViewWithContentDescription("Navigate up")
        delay(TestConfig.DELAY_SHORT)
    }

    fun remainInEditWorkflow() = apply {
        click.onDisplayedViewWithText(android.R.id.button2, "No")
        delay(TestConfig.DELAY_SHORT)
    }

    fun leaveEditWorkflow() = apply {
        click.onDisplayedViewWithText(android.R.id.button1, "Yes")
        delay(TestConfig.DELAY_SHORT)
    }

    fun pressBack() = apply {
        goBack()
        delay(TestConfig.DELAY_SHORT)
    }
}
