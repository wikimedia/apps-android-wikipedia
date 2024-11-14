package org.wikipedia.robots.screen

import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withClassName
import androidx.test.espresso.matcher.ViewMatchers.withContentDescription
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withParent
import androidx.test.espresso.matcher.ViewMatchers.withText
import org.hamcrest.Matchers
import org.hamcrest.Matchers.allOf
import org.hamcrest.core.IsInstanceOf
import org.wikipedia.R
import org.wikipedia.TestUtil.childAtPosition
import org.wikipedia.TestUtil.isDisplayed
import org.wikipedia.base.BaseRobot
import org.wikipedia.base.TestConfig

class ContributionScreenScreenRobot : BaseRobot() {

    fun isDisabled(): Boolean {
        return onView(allOf(withId(R.id.disabledStatesView), withParent(withParent(withId(R.id.suggestedEditsScrollView))), isDisplayed())).isDisplayed()
    }

    fun clickThroughScreenStatsOnboarding() = apply {
        for (i in 1 until 5) {
            onView(allOf(withId(R.id.buttonView), withText("Got it"),
                childAtPosition(childAtPosition(withClassName(Matchers.`is`("android.widget.LinearLayout")), 1), 0), isDisplayed())).perform(click())
            delay(TestConfig.DELAY_MEDIUM)
        }
    }

    fun enterContributionScreen() = apply {
        clickOnDisplayedViewWithIdAnContentDescription(viewId = R.id.userStatsArrow, description = "My contributions")
        delay(TestConfig.DELAY_MEDIUM)
    }

    fun clickFilterButton() = apply {
        clickOnDisplayedViewWithIdAnContentDescription(viewId = R.id.filter_by_button, description = "Filter by")
        delay(TestConfig.DELAY_MEDIUM)
    }

    fun assertThePresenceOfAllFilters() = apply {
        listOf("Wikimedia Commons", "Wikidata", "Wikidata", "Article", "Talk", "User talk", "User")
            .forEach { title ->
                onView(
                    allOf(
                        withId(R.id.item_title),
                        withText(title),
                        withParent(withParent(withId(R.id.recycler_view))),
                        isDisplayed()
                    )
                ).check(matches(withText(title)))
            }
        delay(TestConfig.DELAY_MEDIUM)
    }

    fun clickAddDescriptionTask() = apply {
        clickTaskAtPosition(0)
        delay(TestConfig.DELAY_MEDIUM)
    }

    fun clickOnImageTask() = apply {
        clickTaskAtPosition(1)
        delay(TestConfig.DELAY_MEDIUM)
    }

    fun scrollToImageTagsTask() = apply {
        performOnTask(2, scrollTo())
        delay(TestConfig.DELAY_MEDIUM)
    }

    fun assertThePresenceOfCorrectActionButton() = apply {
        onView(allOf(withId(R.id.addContributionButton), withText("Add description"),
            withParent(allOf(withId(R.id.bottomButtonContainer))), isDisplayed()))
            .check(matches(isDisplayed()))
        delay(TestConfig.DELAY_MEDIUM)
    }

    fun assertTranslateButtonLeadingToAddLanguageScreen() = apply {
        onView(allOf(withId(R.id.secondaryButton), withText("Translate"),
            withContentDescription("Translate Article descriptions"), isDisplayed())).perform(click())
        delay(TestConfig.DELAY_MEDIUM)
    }

    fun clickAddLanguageButton() = apply {
        onView(allOf(withId(R.id.wikipedia_languages_recycler), childAtPosition(withClassName(Matchers.`is`("android.widget.LinearLayout")), 1)))
            .perform(actionOnItemAtPosition<RecyclerView.ViewHolder>(2, click()))
        delay(TestConfig.DELAY_MEDIUM)
    }

    fun selectLanguage() = apply {
        clickOnViewWithId(R.id.languages_list_recycler)
        delay(TestConfig.DELAY_MEDIUM)
    }

    fun assertTranslateButtonLeadingToTranslateDescriptionScreen() = apply {
        val button = onView(allOf(withId(R.id.secondaryButton), withText("Translate"), withContentDescription("Translate Article descriptions"),
            withParent(withParent(IsInstanceOf.instanceOf(androidx.cardview.widget.CardView::class.java))), isDisplayed()))
        button.check(matches(isDisplayed())).perform(click())
    }

    fun assertThePresenceOfCorrectActionButtonText() = apply {
        onView(allOf(withId(R.id.addContributionButton), withText("Add translation"),
            withParent(allOf(withId(R.id.bottomButtonContainer), withParent(IsInstanceOf.instanceOf(android.view.ViewGroup::class.java)))), isDisplayed()))
            .check(matches(isDisplayed()))

        onView(allOf(withContentDescription("Navigate up"), isDisplayed())).perform(click())
        delay(TestConfig.DELAY_MEDIUM)
    }

    fun assertImageCaptionTranslationTaskAndSubsequentActionText() = apply {
        onView(allOf(withId(R.id.secondaryButton), withText("Translate"),
            withContentDescription("Translate Image captions"),
            childAtPosition(childAtPosition(withClassName(Matchers.`is`("org.wikipedia.suggestededits.SuggestedEditsTaskView")), 0), 6), isDisplayed()))
            .perform(click())

        onView(allOf(withId(R.id.addContributionButton), withText("Add translation"),
            withParent(allOf(withId(R.id.bottomButtonContainer), withParent(IsInstanceOf.instanceOf(android.view.ViewGroup::class.java)))), isDisplayed()))
            .check(matches(isDisplayed()))
    }

    fun assertThePresenceOfAddCaptionButton() = apply {
        onView(allOf(withId(R.id.addContributionButton), withText("Add caption"), withParent(allOf(withId(R.id.bottomButtonContainer))), isDisplayed()))
            .check(matches(isDisplayed()))
        delay(TestConfig.DELAY_MEDIUM)
    }

    fun assertThePresenceOfAddTagButton() = apply {
        onView(allOf(withText("Add tag"), withParent(allOf(withId(R.id.tagsChipGroup))), isDisplayed()))
            .check(matches(isDisplayed()))
        delay(TestConfig.DELAY_MEDIUM)
    }

    fun assertThePresenceOfTutorialButton() = apply {
        onView(allOf(withId(R.id.learnMoreButton), withText("Learn more"),
            childAtPosition(allOf(withId(R.id.learnMoreCard)), 2))).perform(scrollTo())
        delay(TestConfig.DELAY_MEDIUM)
    }

    fun clickGetStartedButton() = apply {
        onView(allOf(withId(R.id.onboarding_done_button), withText("Get started"), isDisplayed())).perform(click())
        delay(TestConfig.DELAY_MEDIUM)
    }

    fun pressBack() = apply {
        goBack()
        delay(TestConfig.DELAY_MEDIUM)
    }

    /**
     * Helper methods
     */
    private fun clickTaskAtPosition(position: Int) = apply {
        performOnTask(position, click())
    }

    private fun performOnTask(position: Int, viewAction: ViewAction) = apply {
        onView(allOf(withId(R.id.tasksRecyclerView), childAtPosition(withId(R.id.tasksContainer), 2)))
            .perform(actionOnItemAtPosition<RecyclerView.ViewHolder>(position, viewAction))
    }
}
