package org.wikipedia.robots.screen

import org.wikipedia.R
import org.wikipedia.base.BaseRobot
import org.wikipedia.base.TestConfig

class HistoryScreenRobot : BaseRobot() {
    fun clearHistory() = apply {
        clickOnDisplayedViewWithIdAnContentDescription(viewId = R.id.history_delete, description = "Clear history")
        delay(TestConfig.DELAY_MEDIUM)
    }

    fun assertDeletionMessage() = apply {
        checkIfViewIsDisplayingText(viewId = androidx.appcompat.R.id.alertTitle, text = "Clear browsing history")
        delay(TestConfig.DELAY_MEDIUM)
    }

    fun clickNoOnAlertDialog() = apply {
        clicksOnDisplayedViewWithText(viewId = android.R.id.button2, text = "No")
    }
}
