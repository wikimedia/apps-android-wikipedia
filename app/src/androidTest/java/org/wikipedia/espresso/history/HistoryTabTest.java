package org.wikipedia.espresso.history;

import android.Manifest;
import android.content.Intent;

import androidx.test.espresso.NoMatchingViewException;
import androidx.test.espresso.PerformException;
import androidx.test.espresso.ViewInteraction;
import androidx.test.rule.ActivityTestRule;
import androidx.test.rule.GrantPermissionRule;
import androidx.test.runner.AndroidJUnit4;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wikipedia.R;
import org.wikipedia.espresso.page.PageActivityTest;
import org.wikipedia.espresso.util.ScreenshotTools;
import org.wikipedia.espresso.util.ViewTools;
import org.wikipedia.main.MainActivity;
import org.wikipedia.navtab.NavTab;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.pressBack;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.longClick;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withClassName;
import static androidx.test.espresso.matcher.ViewMatchers.withContentDescription;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.AllOf.allOf;
import static org.wikipedia.espresso.util.CompareTools.assertScreenshotWithinTolerance;
import static org.wikipedia.espresso.util.InstrumentationViewUtils.switchToBlackMode;
import static org.wikipedia.espresso.util.InstrumentationViewUtils.switchToDarkMode;
import static org.wikipedia.espresso.util.ViewTools.WAIT_FOR_1000;
import static org.wikipedia.espresso.util.ViewTools.WAIT_FOR_2000;
import static org.wikipedia.espresso.util.ViewTools.WAIT_FOR_500;
import static org.wikipedia.espresso.util.ViewTools.childAtPosition;
import static org.wikipedia.espresso.util.ViewTools.selectTab;
import static org.wikipedia.espresso.util.ViewTools.setTextInTextView;
import static org.wikipedia.espresso.util.ViewTools.viewIsDisplayed;
import static org.wikipedia.espresso.util.ViewTools.waitFor;
import static org.wikipedia.espresso.util.ViewTools.whileWithMaxSteps;

@RunWith(AndroidJUnit4.class)
@SuppressWarnings("checkstyle:magicnumber")
public class HistoryTabTest {

    @Rule
    public ActivityTestRule<MainActivity> activityTestRule = new ActivityTestRule<>(MainActivity.class);

    @Rule
    public GrantPermissionRule runtimePermissionRule = GrantPermissionRule.grant(
            Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE);


    @Test
    public void testHistoryTab() throws Exception {
        runTests("");

        waitFor(WAIT_FOR_2000);

        switchToDarkMode();

        waitFor(WAIT_FOR_1000);
        runTests("_Dark");
        switchToBlackMode();

        waitFor(WAIT_FOR_1000);

        runTests("_Black");

        runComparisons();
    }

    private void runTests(String mode) throws Exception {
        //Empty tab
        testEmptyHistoryTab(mode);

        PageActivityTest pageActivityTest = new PageActivityTest();
        Intent intent = new Intent();
        pageActivityTest.activityTestRule.launchActivity(intent);
        waitFor(WAIT_FOR_1000);

        pressBack();
        whileWithMaxSteps(
                () -> !viewIsDisplayed(R.id.fragment_main_nav_tab_layout),
                () -> waitFor(WAIT_FOR_2000));

        deleteItem(mode, true);
        ViewInteraction appCompatButton = onView(
                allOf(withId(R.id.snackbar_action), withText("Undo"),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("android.support.design.widget.Snackbar$SnackbarLayout")),
                                        0),
                                1),
                        isDisplayed()));
        appCompatButton.perform(click());
        waitFor(WAIT_FOR_500);
        setDate();
        waitFor(WAIT_FOR_500);
        ScreenshotTools.snap("HistoryItemAfterUndo" + mode);
        waitFor(WAIT_FOR_500);
        onView(withId(R.id.fragment_main_nav_tab_layout))
                .perform(selectTab(NavTab.EXPLORE.code()))
                .check(matches(isDisplayed()));
        deleteItem("", false);
        onView(withId(R.id.fragment_main_nav_tab_layout))
                .perform(selectTab(NavTab.EXPLORE.code()))
                .check(matches(isDisplayed()));
    }

    private void deleteItem(String mode, boolean takeScreenshot) {
        onView(withId(R.id.fragment_main_nav_tab_layout))
                .perform(selectTab(NavTab.HISTORY.code()))
                .check(matches(isDisplayed()));
        waitFor(WAIT_FOR_1000);
        setDate();
        waitFor(WAIT_FOR_1000);
        if (takeScreenshot) {
            ScreenshotTools.snap("HistoryTab" + mode);
        }
        waitFor(WAIT_FOR_1000);
        onView(withId(R.id.history_list))
                .perform(actionOnItemAtPosition(0, longClick()));
        waitFor(WAIT_FOR_1000);
        setDate();
        waitFor(WAIT_FOR_1000);
        if (takeScreenshot) {
            ScreenshotTools.snap("HistoryTabItemSelected" + mode);
        }
        ViewInteraction actionMenuItemView2 = onView(
                allOf(withId(R.id.menu_delete_selected), withContentDescription("Delete selected items"),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.action_mode_bar),
                                        1),
                                0),
                        isDisplayed()));
        actionMenuItemView2.perform(click());
        waitFor(WAIT_FOR_500);
        if (takeScreenshot) {
            ScreenshotTools.snap("HistoryItemDeleteSnackBar" + mode);
        }
    }

    public void testEmptyHistoryTab(String mode) {

        whileWithMaxSteps(
                () -> !viewIsDisplayed(R.id.fragment_feed_feed),
                () -> waitFor(WAIT_FOR_2000));

        onView(withId(R.id.fragment_main_nav_tab_layout))
                .perform(selectTab(NavTab.HISTORY.code()))
                .check(matches(isDisplayed()));
        waitFor(WAIT_FOR_1000);
        ScreenshotTools.snap("EmptyHistoryTab" + mode);
        waitFor(WAIT_FOR_1000);

    }

    private void runComparisons() throws Exception {
        assertScreenshotWithinTolerance("EmptyHistoryTab");
        assertScreenshotWithinTolerance("EmptyHistoryTab_Dark");
        assertScreenshotWithinTolerance("EmptyHistoryTab_Black");
        assertScreenshotWithinTolerance("HistoryTab");
        assertScreenshotWithinTolerance("HistoryTab_Dark");
        assertScreenshotWithinTolerance("HistoryTab_Black");
        assertScreenshotWithinTolerance("HistoryTabItemSelected");
        assertScreenshotWithinTolerance("HistoryTabItemSelected_Dark");
        assertScreenshotWithinTolerance("HistoryTabItemSelected_Black");
        assertScreenshotWithinTolerance("HistoryItemDeleteSnackBar");
        assertScreenshotWithinTolerance("HistoryItemDeleteSnackBar_Dark");
        assertScreenshotWithinTolerance("HistoryItemDeleteSnackBar_Black");
        assertScreenshotWithinTolerance("HistoryItemAfterUndo");
        assertScreenshotWithinTolerance("HistoryItemAfterUndo_Dark");
        assertScreenshotWithinTolerance("HistoryItemAfterUndo_Black");
    }

    private static void setDate() {
        try {
            for (int i = 0; i < 3; i++) {
                ViewInteraction colorButton = onView(
                        allOf(
                                ViewTools.matchPosition(allOf(withId(R.id.page_list_header_text)), i),
                                isDisplayed()));
                colorButton.perform(setTextInTextView("Feb 5, 2017"));
            }
        } catch (NoMatchingViewException | PerformException e) {
            return;
        }
    }

}
