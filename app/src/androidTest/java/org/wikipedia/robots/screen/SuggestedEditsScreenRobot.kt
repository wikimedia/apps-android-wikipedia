package org.wikipedia.robots.screen

import BaseRobot
import androidx.test.espresso.action.ViewActions.click
import org.wikipedia.R
import org.wikipedia.base.TestConfig

class SuggestedEditsScreenRobot : BaseRobot() {

    fun verifyContributionsIsVisible() = apply {
        verify.viewWithTextDisplayed(text = "Contributions")
        delay(TestConfig.DELAY_SHORT)
    }

    fun verifyViewsIsVisible() = apply {
        verify.viewWithTextDisplayed(text = "Views")
        delay(TestConfig.DELAY_SHORT)
    }

    fun verifyLastEditedIsVisible() = apply {
        verify.viewWithTextDisplayed(text = "Last edited")
        delay(TestConfig.DELAY_SHORT)
    }

    fun verifyEditQualityIsVisible() = apply {
        verify.viewWithTextDisplayed(text = "Edit quality")
        delay(TestConfig.DELAY_SHORT)
    }

    fun verifyLastDonatedIsVisible() = apply {
        verify.viewWithTextDisplayed(text = "Last donated")
        delay(TestConfig.DELAY_SHORT)
    }

    fun enterContributionScreen() = apply {
        click.onDisplayedView(R.id.contributionsContainer)
        delay(TestConfig.DELAY_MEDIUM)
    }

    fun clickArticleDescriptions() = apply {
        list.scrollToRecyclerViewInsideNestedScrollView(recyclerViewId = R.id.tasksRecyclerView, position = 0, viewAction = click())
        delay(TestConfig.DELAY_MEDIUM)
    }

    fun clickImageCaptions() = apply {
        list.scrollToRecyclerViewInsideNestedScrollView(recyclerViewId = R.id.tasksRecyclerView, position = 1, viewAction = click())
        delay(TestConfig.DELAY_MEDIUM)
    }

    fun clickImageTags() = apply {
        list.scrollToRecyclerViewInsideNestedScrollView(recyclerViewId = R.id.tasksRecyclerView, position = 2, viewAction = click())
        delay(TestConfig.DELAY_MEDIUM)
    }

    fun clickSuggestedEdits() = apply {
        list.scrollToViewInsideNestedScrollView(viewId = R.id.learnMoreCard)
        delay(TestConfig.DELAY_SHORT)
    }

    fun pressBack() = apply {
        goBack()
        delay(TestConfig.DELAY_MEDIUM)
    }
}
