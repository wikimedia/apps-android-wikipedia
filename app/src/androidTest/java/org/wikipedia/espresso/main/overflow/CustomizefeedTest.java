package org.wikipedia.espresso.main.overflow;

import android.Manifest;
import android.support.test.espresso.ViewInteraction;
import android.support.test.espresso.contrib.RecyclerViewActions;
import android.support.test.rule.ActivityTestRule;
import android.support.test.rule.GrantPermissionRule;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.LargeTest;
import android.view.View;

import org.hamcrest.core.IsInstanceOf;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wikipedia.R;
import org.wikipedia.espresso.util.ScreenshotTools;
import org.wikipedia.espresso.util.ViewTools;
import org.wikipedia.main.MainActivity;

import static android.support.test.InstrumentationRegistry.getInstrumentation;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withClassName;
import static android.support.test.espresso.matcher.ViewMatchers.withContentDescription;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.AllOf.allOf;
import static org.wikipedia.espresso.util.CompareTools.assertScreenshotWithinTolerance;
import static org.wikipedia.espresso.util.ViewTools.WAIT_FOR_1000;
import static org.wikipedia.espresso.util.ViewTools.WAIT_FOR_2000;
import static org.wikipedia.espresso.util.ViewTools.WAIT_FOR_500;
import static org.wikipedia.espresso.util.ViewTools.childAtPosition;
import static org.wikipedia.espresso.util.ViewTools.viewIsDisplayed;
import static org.wikipedia.espresso.util.ViewTools.waitFor;
import static org.wikipedia.espresso.util.ViewTools.whileWithMaxSteps;

@LargeTest
@RunWith(AndroidJUnit4.class)
@SuppressWarnings("checkstyle:magicnumber")
public class CustomizefeedTest {
    @Rule
    public ActivityTestRule<MainActivity> mActivityTestRule = new ActivityTestRule<>(MainActivity.class);
    @Rule
    public GrantPermissionRule runtimePermissionRule = GrantPermissionRule.grant(
            Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE);

    @Test
    public void customizeFeed() throws Exception {
        whileWithMaxSteps(
                () -> !viewIsDisplayed(R.id.fragment_feed_feed),
                () -> waitFor(WAIT_FOR_2000));
        ViewInteraction actionMenuItemView = onView(
                allOf(withId(R.id.menu_overflow_button), withContentDescription("More options"),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.single_fragment_toolbar),
                                        1),
                                0),
                        isDisplayed()));
        actionMenuItemView.perform(click());
        ViewInteraction appCompatTextView2 = onView(
                allOf(withId(R.id.explore_overflow_configure_cards), withText("Customize the feed"),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("android.widget.LinearLayout")),
                                        1),
                                0),
                        isDisplayed()));
        appCompatTextView2.perform(click());
        waitFor(WAIT_FOR_500);
        ScreenshotTools.snap("CustomizeFeed1Of2");
        waitFor(WAIT_FOR_500);
        onView(withId(R.id.content_types_recycler)).perform(RecyclerViewActions.scrollToPosition(8));
        waitFor(WAIT_FOR_500);
        ScreenshotTools.snap("CustomizeFeed2Of2");
        waitFor(WAIT_FOR_500);
        onView(withId(R.id.content_types_recycler)).perform(RecyclerViewActions.scrollToPosition(0));
        waitFor(WAIT_FOR_1000);
        openActionBarOverflowOrOptionsMenu(getInstrumentation().getTargetContext());
        waitFor(WAIT_FOR_2000);
        ScreenshotTools.snap("CustomizeOverflowMenu");
        ViewInteraction appCompatTextView3 = onView(
                allOf(withId(R.id.title), withText("Deselect all"),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("android.support.v7.view.menu.ListMenuItemView")),
                                        0),
                                0),
                        isDisplayed()));
        appCompatTextView3.perform(click());
        waitFor(WAIT_FOR_2000);
        ScreenshotTools.snap("CustomizeDeselectAll");
        openActionBarOverflowOrOptionsMenu(getInstrumentation().getTargetContext());
        ViewInteraction appCompatTextView4 = onView(
                allOf(withId(R.id.title), withText("Select all"),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("android.support.v7.view.menu.ListMenuItemView")),
                                        0),
                                0),
                        isDisplayed()));
        appCompatTextView4.perform(click());
        waitFor(WAIT_FOR_2000);
        ScreenshotTools.snap("CustomizeSelectAll");
        waitFor(WAIT_FOR_2000);
        onView(withId(R.id.content_types_recycler)).perform(
                RecyclerViewActions.actionOnItemAtPosition(0, ViewTools.clickChildViewWithId(R.id.feed_content_type_checkbox)));
        ViewInteraction appCompatImageButton = onView(
                allOf(withContentDescription("Navigate up"),
                        childAtPosition(
                                allOf(withId(R.id.action_bar),
                                        childAtPosition(
                                                withId(R.id.action_bar_container),
                                                0)),
                                1),
                        isDisplayed()));
        appCompatImageButton.perform(click());
        ViewInteraction textView = onView(
                allOf(withId(R.id.view_card_header_title), withText("On this day"),
                        childAtPosition(
                                allOf(withId(R.id.view_on_this_day_card_header),
                                        childAtPosition(
                                                IsInstanceOf.<View>instanceOf(android.widget.LinearLayout.class),
                                                0)),
                                1),
                        isDisplayed()));
        textView.check(matches(withText("On this day")));
        waitFor(WAIT_FOR_2000);
        whileWithMaxSteps(
                () -> !viewIsDisplayed(R.id.menu_overflow_button),
                () -> waitFor(WAIT_FOR_1000));
        actionMenuItemView.perform(click());
        waitFor(WAIT_FOR_2000);
        appCompatTextView2.perform(click());
        waitFor(WAIT_FOR_2000);
        onView(withId(R.id.content_types_recycler)).perform(
                RecyclerViewActions.actionOnItemAtPosition(0, ViewTools.clickChildViewWithId(R.id.feed_content_type_checkbox)));
        ViewInteraction appCompatImageButton2 = onView(
                allOf(withContentDescription("Navigate up"),
                        childAtPosition(
                                allOf(withId(R.id.action_bar),
                                        childAtPosition(
                                                withId(R.id.action_bar_container),
                                                0)),
                                1),
                        isDisplayed()));
        appCompatImageButton2.perform(click());
        ViewInteraction textView2 = onView(
                allOf(withId(R.id.view_card_header_title), withText("In the news"),
                        childAtPosition(
                                allOf(withId(R.id.view_list_card_header),
                                        childAtPosition(
                                                IsInstanceOf.<View>instanceOf(android.view.ViewGroup.class),
                                                0)),
                                1),
                        isDisplayed()));
        textView2.check(matches(withText("In the news")));
        runComparisons();
    }

    private void runComparisons() throws Exception {
        assertScreenshotWithinTolerance("CustomizeFeed1Of2");
        assertScreenshotWithinTolerance("CustomizeFeed2Of2");
        assertScreenshotWithinTolerance("CustomizeSelectAll");
        assertScreenshotWithinTolerance("CustomizeDeselectAll");
        assertScreenshotWithinTolerance("CustomizeOverflowMenu");
    }
}
