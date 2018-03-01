package org.wikipedia.espresso.main.overflow;


import android.Manifest;
import android.support.test.espresso.ViewInteraction;
import android.support.test.espresso.contrib.RecyclerViewActions;
import android.support.test.rule.ActivityTestRule;
import android.support.test.rule.GrantPermissionRule;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.LargeTest;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wikipedia.R;
import org.wikipedia.espresso.util.ScreenshotTools;
import org.wikipedia.settings.SettingsActivity;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withClassName;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.AllOf.allOf;
import static org.wikipedia.espresso.util.CompareTools.assertScreenshotWithinTolerance;
import static org.wikipedia.espresso.util.ViewTools.WAIT_FOR_1000;
import static org.wikipedia.espresso.util.ViewTools.WAIT_FOR_2000;
import static org.wikipedia.espresso.util.ViewTools.WAIT_FOR_500;
import static org.wikipedia.espresso.util.ViewTools.childAtPosition;
import static org.wikipedia.espresso.util.ViewTools.viewWithTextIsDisplayed;
import static org.wikipedia.espresso.util.ViewTools.waitFor;
import static org.wikipedia.espresso.util.ViewTools.whileWithMaxSteps;

@LargeTest
@RunWith(AndroidJUnit4.class)
@SuppressWarnings("checkstyle:magicnumber")
public class SettingsScreenTest {

    @Rule
    public ActivityTestRule<SettingsActivity> activityTestRule = new ActivityTestRule<>(SettingsActivity.class);

    @Rule
    public GrantPermissionRule runtimePermissionRule = GrantPermissionRule.grant(
            Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE);

    @Test
    public void settingTest() throws Exception {

        // Wait for Screen display
        whileWithMaxSteps(
                () -> !viewWithTextIsDisplayed("General"),
                () -> waitFor(WAIT_FOR_2000));

        ScreenshotTools.snap("SettingsScreen1of3");


        //Click App Theme
        ViewInteraction recyclerView = onView(
                allOf(withId(R.id.list),
                        childAtPosition(
                                withId(android.R.id.list_container),
                                0)));
        recyclerView.perform(actionOnItemAtPosition(3, click()));
        waitFor(WAIT_FOR_2000);
        ScreenshotTools.snap("AppThemeChangeScreenLight");

        ViewInteraction appCompatTextView3 = onView(
                allOf(withId(R.id.button_theme_dark), withText("Dark"),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("android.widget.LinearLayout")),
                                        1),
                                0),
                        isDisplayed()));
        appCompatTextView3.perform(click());
        waitFor(WAIT_FOR_2000);
        ScreenshotTools.snap("AppThemeChangeScreenDark");

        ViewInteraction appCompatTextView4 = onView(
                allOf(withId(R.id.button_theme_black), withText("Black"),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("android.widget.LinearLayout")),
                                        2),
                                0),
                        isDisplayed()));
        appCompatTextView4.perform(click());

        waitFor(WAIT_FOR_2000);
        ScreenshotTools.snap("AppThemeChangeScreenBlack");

        ViewInteraction appCompatTextView5 = onView(
                allOf(withId(R.id.button_theme_light), withText("Light"),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("android.widget.LinearLayout")),
                                        0),
                                0),
                        isDisplayed()));
        appCompatTextView5.perform(click());
        waitFor(WAIT_FOR_500);

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
        waitFor(WAIT_FOR_1000);

        onView(withId(R.id.list)).perform(RecyclerViewActions.scrollToPosition(10));
        waitFor(WAIT_FOR_1000);
        ScreenshotTools.snap("SettingsScreen2of3");

        onView(withId(R.id.list)).perform(RecyclerViewActions.scrollToPosition(18));
        waitFor(WAIT_FOR_1000);
        ScreenshotTools.snap("SettingsScreen3of3");

        runComparisons();

    }

    private void runComparisons() throws Exception {
        assertScreenshotWithinTolerance("SettingsScreen1of3");
        assertScreenshotWithinTolerance("SettingsScreen2of3");
        assertScreenshotWithinTolerance("SettingsScreen3of3");
        assertScreenshotWithinTolerance("AppThemeChangeScreenLight");
        assertScreenshotWithinTolerance("AppThemeChangeScreenDark");
        assertScreenshotWithinTolerance("AppThemeChangeScreenBlack");
    }

}
