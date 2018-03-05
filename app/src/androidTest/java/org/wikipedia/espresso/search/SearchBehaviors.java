package org.wikipedia.espresso.search;

import android.support.annotation.NonNull;
import android.support.test.espresso.ViewInteraction;

import org.wikipedia.R;
import org.wikipedia.espresso.util.ScreenshotTools;
import org.wikipedia.espresso.util.ViewTools;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.closeSoftKeyboard;
import static android.support.test.espresso.action.ViewActions.replaceText;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withClassName;
import static android.support.test.espresso.matcher.ViewMatchers.withContentDescription;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.AllOf.allOf;
import static org.wikipedia.espresso.util.ViewTools.WAIT_FOR_1000;
import static org.wikipedia.espresso.util.ViewTools.WAIT_FOR_2000;
import static org.wikipedia.espresso.util.ViewTools.childAtPosition;
import static org.wikipedia.espresso.util.ViewTools.viewIsDisplayed;
import static org.wikipedia.espresso.util.ViewTools.waitFor;
import static org.wikipedia.espresso.util.ViewTools.whileWithMaxSteps;

public final class SearchBehaviors {

    public static void searchKeywordAndGo(@NonNull String keyword, boolean shouldTakeScreenshot) {

        whileWithMaxSteps(
                () -> !viewIsDisplayed(R.id.page_toolbar_button_search),
                () -> waitFor(WAIT_FOR_1000));
        waitFor(WAIT_FOR_2000);

        ViewInteraction layout = onView(withId(R.id.page_toolbar_button_search));
        layout.perform(click());

        ViewInteraction frameLayout = onView(
                allOf(withId(R.id.search_lang_button_container), withContentDescription("Wikipedia language"),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.search_toolbar),
                                        1),
                                1),
                        isDisplayed()));
        frameLayout.perform(click());

        ViewInteraction plainPasteEditText = onView(
                allOf(withId(R.id.preference_languages_filter),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("android.support.design.widget.TextInputLayout")),
                                        0),
                                0),
                        isDisplayed()));
        plainPasteEditText.perform(replaceText("test"), closeSoftKeyboard());
        waitFor(WAIT_FOR_2000);

        // take screenshot
        if (shouldTakeScreenshot) {
            ScreenshotTools.snap("SearchPage");
        }
        onView(
                allOf(
                        ViewTools.matchPosition(allOf(withText("Test")), 1),
                        isDisplayed())).perform(click());

        ViewInteraction searchAutoComplete = onView(
                allOf(withId(R.id.search_src_text),
                        childAtPosition(
                                allOf(withId(R.id.search_plate),
                                        childAtPosition(
                                                withId(R.id.search_edit_frame),
                                                1)),
                                0),
                        isDisplayed()));
        searchAutoComplete.perform(replaceText(keyword), closeSoftKeyboard());

        // hold until the result fetch
        whileWithMaxSteps(
                () -> !viewIsDisplayed(R.id.search_results_list),
                () -> waitFor(WAIT_FOR_1000));

        //Also hold to populate
        waitFor(WAIT_FOR_2000);

        // take screenshot
        if (shouldTakeScreenshot) {
            ScreenshotTools.snap("SearchSuggestionPage");
        }
    }

    private SearchBehaviors() {
    }
}
