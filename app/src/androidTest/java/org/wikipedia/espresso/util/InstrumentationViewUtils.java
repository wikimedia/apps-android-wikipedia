package org.wikipedia.espresso.util;

import android.support.test.espresso.Espresso;
import android.support.test.espresso.ViewInteraction;

import org.wikipedia.R;

import static android.support.test.InstrumentationRegistry.getInstrumentation;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withClassName;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.AllOf.allOf;
import static org.wikipedia.espresso.util.ViewTools.WAIT_FOR_1000;
import static org.wikipedia.espresso.util.ViewTools.WAIT_FOR_2000;
import static org.wikipedia.espresso.util.ViewTools.WAIT_FOR_500;
import static org.wikipedia.espresso.util.ViewTools.childAtPosition;
import static org.wikipedia.espresso.util.ViewTools.viewIsDisplayed;
import static org.wikipedia.espresso.util.ViewTools.viewWithTextIsDisplayed;
import static org.wikipedia.espresso.util.ViewTools.waitFor;
import static org.wikipedia.espresso.util.ViewTools.whileWithMaxSteps;

@SuppressWarnings("checkstyle:magicnumber")
public final class InstrumentationViewUtils {
    // TODO: re design the steps of tests

    //Make sure to call the switch from Explore tab
    public static void switchToDarkMode() {
        whileWithMaxSteps(
                () -> !viewIsDisplayed(R.id.fragment_feed_feed),
                () -> waitFor(2000));

        whileWithMaxSteps(
                () -> !viewWithTextIsDisplayed("General"),
                () -> waitFor(WAIT_FOR_2000));

        //Click App Theme
        ViewInteraction recyclerView = onView(
                allOf(withId(android.R.id.list),
                        childAtPosition(
                                withId(android.R.id.list_container),
                                0)));
        recyclerView.perform(actionOnItemAtPosition(3, click()));
        waitFor(WAIT_FOR_2000);

        ViewInteraction appCompatTextView3 = onView(
                allOf(withId(R.id.button_theme_dark), withText("Dark"),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("android.widget.LinearLayout")),
                                        1),
                                0),
                        isDisplayed()));
        appCompatTextView3.perform(click());

        waitFor(WAIT_FOR_500);
        whileWithMaxSteps(
                () -> !viewIsDisplayed(R.id.fragment_feed_feed),
                Espresso::pressBack);
        waitFor(WAIT_FOR_1000);
    }

    public static void switchToBlackMode() {
        whileWithMaxSteps(
                () -> !viewIsDisplayed(R.id.fragment_feed_feed),
                () -> waitFor(2000));

        // TODO: redesign the way of entering SettingsActivity

        whileWithMaxSteps(
                () -> !viewWithTextIsDisplayed("General"),
                () -> waitFor(WAIT_FOR_2000));


        //Click App Theme
        ViewInteraction recyclerView = onView(
                allOf(withId(android.R.id.list),
                        childAtPosition(
                                withId(android.R.id.list_container),
                                0)));
        recyclerView.perform(actionOnItemAtPosition(3, click()));
        waitFor(WAIT_FOR_2000);

        ViewInteraction appCompatTextView4 = onView(
                allOf(withId(R.id.button_theme_black), withText("Black"),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("android.widget.LinearLayout")),
                                        2),
                                0),
                        isDisplayed()));
        appCompatTextView4.perform(click());

        waitFor(WAIT_FOR_500);
        whileWithMaxSteps(
                () -> !viewIsDisplayed(R.id.fragment_feed_feed),
                Espresso::pressBack);
        waitFor(WAIT_FOR_1000);
    }

    public static void switchPageModeToDark() {
        openActionBarOverflowOrOptionsMenu(getInstrumentation().getTargetContext());

        ViewInteraction appCompatTextView2 = onView(
                allOf(withId(R.id.title), withText("Font and theme"),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("android.support.v7.view.menu.ListMenuItemView")),
                                        0),
                                0),
                        isDisplayed()));
        appCompatTextView2.perform(click());

        ViewInteraction appCompatTextView3 = onView(
                allOf(withId(R.id.button_theme_dark), withText("Dark"),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("android.widget.LinearLayout")),
                                        1),
                                0),
                        isDisplayed()));
        appCompatTextView3.perform(click());

        ViewInteraction view = onView(
                allOf(withId(R.id.touch_outside),
                        childAtPosition(
                                allOf(withId(R.id.coordinator),
                                        childAtPosition(
                                                withId(R.id.container),
                                                0)),
                                0),
                        isDisplayed()));
        view.perform(click());
    }

    public static void switchPageModeToBlack() {
        openActionBarOverflowOrOptionsMenu(getInstrumentation().getTargetContext());

        ViewInteraction appCompatTextView2 = onView(
                allOf(withId(R.id.title), withText("Font and theme"),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("android.support.v7.view.menu.ListMenuItemView")),
                                        0),
                                0),
                        isDisplayed()));
        appCompatTextView2.perform(click());

        ViewInteraction appCompatTextView5 = onView(
                allOf(withId(R.id.button_theme_black), withText("Black"),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("android.widget.LinearLayout")),
                                        2),
                                0),
                        isDisplayed()));
        appCompatTextView5.perform(click());

        ViewInteraction view = onView(
                allOf(withId(R.id.touch_outside),
                        childAtPosition(
                                allOf(withId(R.id.coordinator),
                                        childAtPosition(
                                                withId(R.id.container),
                                                0)),
                                0),
                        isDisplayed()));
        view.perform(click());
    }

    private InstrumentationViewUtils() {
    }
}
