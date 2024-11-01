package org.wikipedia.robots

import androidx.test.espresso.web.sugar.Web.onWebView
import androidx.test.espresso.web.webdriver.DriverAtoms.findElement
import androidx.test.espresso.web.webdriver.DriverAtoms.webClick
import androidx.test.espresso.web.webdriver.Locator
import org.wikipedia.R
import org.wikipedia.base.BaseRobot
import org.wikipedia.base.TestConfig

class EditorRobot: BaseRobot() {
    fun clickEditPencilAtTopOfArticle() = apply {
        onWebView()
            .withElement(findElement(Locator.CSS_SELECTOR, "a[data-id='0'].pcs-edit-section-link"))
            .perform(webClick())
        delay(TestConfig.DELAY_SHORT)
    }

    fun clickEditIntroductionMenuItem() = apply {
        clicksOnDisplayedViewWithText(viewId = R.id.title, text = "Edit introduction")
        delay(TestConfig.DELAY_LARGE)
    }

    fun dismissDialogIfShown() = apply {
        performIfDialogShown(dialogText = "Got it", action = {
            clickWithText("Got it")
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
        scrollToTextAndClick("Fixed Typo")
        delay(TestConfig.DELAY_MEDIUM)
    }

    fun goBackOutOfEditingWorkflow() = apply {
        clickOnDisplayedViewWithContentDescription("Navigate up")
    }
}