package org.wikipedia.robots.screen

import androidx.test.espresso.action.ViewActions.click
import org.wikipedia.R
import org.wikipedia.base.BaseRobot
import org.wikipedia.base.TestConfig

class SuggestedEditsScreenRobot : BaseRobot() {

    fun verifyEditsIsVisible() = apply {
        checkViewWithTextDisplayed(text = "Edits")
        delay(TestConfig.DELAY_SHORT)
    }

    fun verifyViewsIsVisible() = apply {
        checkViewWithTextDisplayed(text = "Views")
        delay(TestConfig.DELAY_SHORT)
    }

    fun verifyLastEditedIsVisible() = apply {
        checkViewWithTextDisplayed(text = "Last edited")
        delay(TestConfig.DELAY_SHORT)
    }

    fun verifyEditQualityIsVisible() = apply {
        checkViewWithTextDisplayed(text = "Edit quality")
        delay(TestConfig.DELAY_SHORT)
    }

    fun verifyLastDonatedIsVisible() = apply {
        checkViewWithTextDisplayed(text = "Last donated")
        delay(TestConfig.DELAY_SHORT)
    }

    fun enterContributionScreen() = apply {
        clickOnDisplayedView(R.id.contributionsContainer)
        delay(TestConfig.DELAY_MEDIUM)
    }

    fun clickArticleDescriptions() = apply {
        scrollToRecyclerViewInsideNestedScrollView(recyclerViewId = R.id.tasksRecyclerView, position = 0, viewAction = click())
        delay(TestConfig.DELAY_MEDIUM)
    }

    fun clickImageCaptions() = apply {
        scrollToRecyclerViewInsideNestedScrollView(recyclerViewId = R.id.tasksRecyclerView, position = 1, viewAction = click())
        delay(TestConfig.DELAY_MEDIUM)
    }

    fun clickImageTags() = apply {
        scrollToRecyclerViewInsideNestedScrollView(recyclerViewId = R.id.tasksRecyclerView, position = 2, viewAction = click())
        delay(TestConfig.DELAY_MEDIUM)
    }

    fun clickSuggestedEdits() = apply {
        scrollToViewInsideNestedScrollView(viewId = R.id.learnMoreCard)
        delay(TestConfig.DELAY_SHORT)
    }

    fun pressBack() = apply {
        goBack()
        delay(TestConfig.DELAY_MEDIUM)
    }
}
