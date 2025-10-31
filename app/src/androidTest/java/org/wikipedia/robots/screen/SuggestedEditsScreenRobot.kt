package org.wikipedia.robots.screen

import BaseRobot
import android.content.Context
import androidx.test.espresso.action.ViewActions.click
import org.wikipedia.R
import org.wikipedia.base.TestConfig
import org.wikipedia.settings.Prefs

class SuggestedEditsScreenRobot : BaseRobot() {
    fun clickArticleDescriptions() = apply {
        list.scrollToRecyclerViewInsideNestedScrollView(recyclerViewId = R.id.tasksRecyclerView, position = 0, viewAction = click())
        delay(TestConfig.DELAY_MEDIUM)
    }

    fun clickImageCaptions() = apply {
        list.scrollToRecyclerViewInsideNestedScrollView(recyclerViewId = R.id.tasksRecyclerView, position = 2, viewAction = click())
        delay(TestConfig.DELAY_MEDIUM)
    }

    fun clickImageTags() = apply {
        list.scrollToRecyclerViewInsideNestedScrollView(recyclerViewId = R.id.tasksRecyclerView, position = 3, viewAction = click())
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

    fun verifyArticleDescriptionDoesNotExist(context: Context) = apply {
        list.verifyItemDoesNotExistWithText(
            recyclerViewId = R.id.tasksRecyclerView,
            text = context.getString(R.string.description_edit_tutorial_title_descriptions)
        )
    }

    fun verifyImageCaptionsExist(context: Context) = apply {
        list.verifyItemExistWithText(
            recyclerViewId = R.id.tasksRecyclerView,
            text = context.getString(R.string.suggested_edits_image_captions)
        )
    }

    fun verifyImageTagsExist(context: Context) = apply {
        list.verifyItemExistWithText(
            recyclerViewId = R.id.tasksRecyclerView,
            text = context.getString(R.string.suggested_edits_image_captions)
        )
    }

    fun increaseContribution() = apply {
        Prefs.overrideSuggestedEditContribution = 100
    }

    fun disableImageCaptionOnboarding() = apply {
        Prefs.showImageTagsOnboarding = false
        Prefs.suggestedEditsImageRecsOnboardingShown = true
    }
}
