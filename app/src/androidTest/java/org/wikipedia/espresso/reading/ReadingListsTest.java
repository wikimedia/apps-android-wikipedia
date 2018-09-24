package org.wikipedia.espresso.reading;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.test.espresso.DataInteraction;
import android.support.test.espresso.contrib.RecyclerViewActions;
import android.support.test.rule.ActivityTestRule;
import android.support.test.rule.GrantPermissionRule;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.espresso.page.PageActivityTest;
import org.wikipedia.espresso.search.SearchBehaviors;
import org.wikipedia.espresso.util.ScreenshotTools;
import org.wikipedia.espresso.util.ViewTools;
import org.wikipedia.main.MainActivity;
import org.wikipedia.navtab.NavTab;
import org.wikipedia.readinglist.database.ReadingListDbHelper;
import org.wikipedia.settings.PrefsIoUtil;

import static android.support.test.espresso.Espresso.onData;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.closeSoftKeyboard;
import static android.support.test.espresso.action.ViewActions.replaceText;
import static android.support.test.espresso.action.ViewActions.typeText;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.anything;
import static org.hamcrest.core.AllOf.allOf;
import static org.wikipedia.espresso.util.CompareTools.assertScreenshotWithinTolerance;
import static org.wikipedia.espresso.util.InstrumentationViewUtils.switchToBlackMode;
import static org.wikipedia.espresso.util.InstrumentationViewUtils.switchToDarkMode;
import static org.wikipedia.espresso.util.ViewTools.WAIT_FOR_1000;
import static org.wikipedia.espresso.util.ViewTools.WAIT_FOR_2000;
import static org.wikipedia.espresso.util.ViewTools.WAIT_FOR_500;
import static org.wikipedia.espresso.util.ViewTools.WAIT_FOR_6000;
import static org.wikipedia.espresso.util.ViewTools.childAtPosition;
import static org.wikipedia.espresso.util.ViewTools.pressBack;
import static org.wikipedia.espresso.util.ViewTools.selectTab;
import static org.wikipedia.espresso.util.ViewTools.viewIsDisplayed;
import static org.wikipedia.espresso.util.ViewTools.waitFor;
import static org.wikipedia.espresso.util.ViewTools.whileWithMaxSteps;

@RunWith(AndroidJUnit4.class)
@SuppressWarnings("checkstyle:methodlength")
public final class ReadingListsTest {
    @Rule
    public ActivityTestRule<MainActivity> activityTestRule = new ActivityTestRule<>(MainActivity.class);

    @Rule
    public GrantPermissionRule runtimePermissionRule = GrantPermissionRule.grant(
            Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE);

    @Test
    public void testReadingLists() throws Exception {
        navigateToReadingListsFromExploreTab();
        runTests("");
        clearLists();
        switchToDarkMode();
        navigateToReadingListsFromExploreTab();
        runTests("_Dark");
        clearLists();
        switchToBlackMode();
        navigateToReadingListsFromExploreTab();
        runTests("_Black");
        clearLists();
        runComparisons("");
        runComparisons("_Dark");
        runComparisons("_Black");
    }

    private void clearLists() {
        navigateToExploreTab();

        ReadingListDbHelper.instance().resetToDefaults();
        PrefsIoUtil.setBoolean(R.string.preference_key_toc_tutorial_enabled, false);
        PrefsIoUtil.setBoolean(R.string.preference_key_description_edit_tutorial_enabled, false);
    }

    private void runTests(String mode) {
        if (viewIsDisplayed(R.id.view_onboarding_action_negative)) {
            onView(ViewTools.first(withId(R.id.view_onboarding_action_negative))).perform(click());
        }

        captureInitialReadingTabViews(mode);
        captureAddingArticleToDefaultList(mode);

        navigateToReadingListsFromExploreTab();

        captureDefaultListViews(mode);
        pressBack();

        captureMyListsTabWithUserCreatedLists(mode);
    }


    private void captureMyListsTabWithUserCreatedLists(String mode) {
        onView(withId(R.id.menu_search_lists)).perform(click());

        onView(withId(android.support.design.R.id.search_src_text)).perform(typeText("obama"));
        waitFor(WAIT_FOR_1000);

        ScreenshotTools.snap("ReadingListSearchWithResults" + mode);
        waitFor(WAIT_FOR_1000);

        onView(withId(android.support.design.R.id.search_src_text)).perform(typeText("oooo"));
        ScreenshotTools.snap("ReadingListSearchWithNoResults" + mode);

        pressBack();
        pressBack();
        waitFor(WAIT_FOR_1000);
        onView(withId(R.id.menu_sort_options)).perform(click());
        ScreenshotTools.snap("ReadingListsSortMenu" + mode);
        waitFor(WAIT_FOR_1000);

        pressBack();
        waitFor(WAIT_FOR_1000);
        onView(withId(R.id.menu_search_lists)).perform(click());
        onView(withId(android.support.design.R.id.search_src_text)).perform(typeText("my"));
        waitFor(WAIT_FOR_1000);
        ScreenshotTools.snap("ReadingListsSearchWithResults" + mode);
        waitFor(WAIT_FOR_1000);

        onView(withId(android.support.design.R.id.search_src_text)).perform(typeText("ooo"));
        ScreenshotTools.snap("ReadingListsSearchWithNoResults" + mode);
        waitFor(WAIT_FOR_1000);

        pressBack();
        waitFor(WAIT_FOR_1000);
        ScreenshotTools.snap("ReadingListsFragmentWithNoEmptyStateMessages" + mode);
    }

    private void navigateToExploreTab() {
        whileWithMaxSteps(
                () -> !viewIsDisplayed(R.id.fragment_main_nav_tab_layout),
                () -> waitFor(WAIT_FOR_2000));

        onView(withId(R.id.fragment_main_nav_tab_layout))
                .perform(selectTab(NavTab.EXPLORE.code()))
                .check(matches(isDisplayed()));
        SharedPreferences prefs =
                PreferenceManager.getDefaultSharedPreferences(WikipediaApp.getInstance());
        prefs.edit().clear().commit();
        waitFor(WAIT_FOR_2000);
    }

    private void captureDefaultListViews(String mode) {
        ScreenshotTools.snap("ReadingListsFragmentNonEmptyDefaultList" + mode);
        waitFor(WAIT_FOR_1000);
        onView(withId(R.id.reading_list_list)).perform(RecyclerViewActions.scrollToPosition(0)).perform(click());
        waitFor(WAIT_FOR_1000);
        ScreenshotTools.snap("DefaultListNonEmpty" + mode);
        waitFor(WAIT_FOR_1000);

        onView(withId(R.id.reading_list_contents)).perform(
                RecyclerViewActions.actionOnItemAtPosition(0, ViewTools.clickChildViewWithId(R.id.item_overflow_menu)));
        waitFor(WAIT_FOR_1000);
        ScreenshotTools.snap("DefaultListOverflow" + mode);
        waitFor(WAIT_FOR_1000);

        onView(withText("Save all for offline")).perform(click());
        waitFor(WAIT_FOR_500);
        ScreenshotTools.snap("ReadingListSaveOffline" + mode);

        onView(withId(R.id.reading_list_contents)).perform(
                RecyclerViewActions.actionOnItemAtPosition(0, ViewTools.clickChildViewWithId(R.id.item_overflow_menu)));
        waitFor(WAIT_FOR_1000);

        onView(withText("Remove all from offline")).perform(click());
        waitFor(WAIT_FOR_500);
        ScreenshotTools.snap("ReadingListRemoveOffline" + mode);
        waitFor(WAIT_FOR_1000);
        onView(withId(R.id.reading_list_contents)).perform(
                RecyclerViewActions.actionOnItemAtPosition(1, ViewTools.clickChildViewWithId(R.id.page_list_item_action_primary)));
        waitFor(WAIT_FOR_1000);
        ScreenshotTools.snap("ReadingListFragmentArticleOverflow" + mode);
        waitFor(WAIT_FOR_1000);
        onView(withText("Add to another reading list")).perform(click());
        waitFor(WAIT_FOR_1000);
        ScreenshotTools.snap("AddToAnotherListDialog" + mode);

        onView(withText("Create new")).perform(click());
        waitFor(WAIT_FOR_2000);
        ScreenshotTools.snap("CreateNewReadingListDialog" + mode);

        onView(withText("OK")).perform(click());
        onView(withId(R.id.reading_list_contents)).perform(
                RecyclerViewActions.actionOnItemAtPosition(1, ViewTools.clickChildViewWithId(R.id.page_list_item_action_primary)));
        waitFor(WAIT_FOR_1000);
        onView(withText("Add to another reading list")).perform(click());
        waitFor(WAIT_FOR_1000);
        onView(withText("Create new")).perform(click());

        waitFor(WAIT_FOR_1000);
        ScreenshotTools.snap("CreateNewReadingListDialogNameError" + mode);

        onView(withText("Cancel")).perform(click());
        pressBack();

        onView(withId(R.id.reading_list_contents)).perform(
                RecyclerViewActions.actionOnItemAtPosition(1, ViewTools.clickChildViewWithId(R.id.page_list_item_action_primary)));
        onView(withText("Remove from Saved")).perform(click());
        ScreenshotTools.snap("ArticleDeletedRationale" + mode);

        pressBack();
        onView(withId(R.id.reading_list_list)).perform(
                RecyclerViewActions.actionOnItemAtPosition(1, click()));
        onView(withId(R.id.reading_list_contents)).perform(
                RecyclerViewActions.actionOnItemAtPosition(0, ViewTools.clickChildViewWithId(R.id.item_overflow_menu)));
        waitFor(WAIT_FOR_1000);
        ScreenshotTools.snap("ListOverflow" + mode);
        waitFor(WAIT_FOR_1000);

        onView(withText("Rename")).perform(click());
        waitFor(WAIT_FOR_1000);

        ScreenshotTools.snap("ListRenameDialog" + mode);

        onView(withText("Cancel")).perform(click());
        onView(withId(R.id.reading_list_contents)).perform(
                RecyclerViewActions.actionOnItemAtPosition(0, ViewTools.clickChildViewWithId(R.id.item_overflow_menu)));
        waitFor(WAIT_FOR_1000);

        onView(withText("Edit description")).perform(click());
        waitFor(WAIT_FOR_1000);
        ScreenshotTools.snap("ListEditDescDialog" + mode);

        onView(withId(R.id.text_input)).perform(replaceText("test"), closeSoftKeyboard());

        onView(withText("OK")).perform(click());
        waitFor(WAIT_FOR_1000);
        ScreenshotTools.snap("ListWithDesc" + mode);

        onView(withId(R.id.reading_list_contents)).perform(
                RecyclerViewActions.actionOnItemAtPosition(0, ViewTools.clickChildViewWithId(R.id.item_overflow_menu)));
        waitFor(WAIT_FOR_1000);

        onView(withText("Edit description")).perform(click());
        waitFor(WAIT_FOR_1000);
        ScreenshotTools.snap("ListEditDescDialogWithPreviousText" + mode);
        waitFor(WAIT_FOR_1000);

        onView(withText("Cancel")).perform(click());
        onView(withId(R.id.reading_list_contents)).perform(
                RecyclerViewActions.actionOnItemAtPosition(0, ViewTools.clickChildViewWithId(R.id.item_overflow_menu)));
        waitFor(WAIT_FOR_1000);

        onView(withText("Delete list")).perform(click());
        waitFor(WAIT_FOR_1000);
        ScreenshotTools.snap("ListDeleteConfirmationDialog" + mode);

        onView(withText("Cancel")).perform(click());

        onView(withId(R.id.menu_sort_options)).perform(click());
        waitFor(WAIT_FOR_1000);

        ScreenshotTools.snap("ReadingListSortMenu" + mode);
        waitFor(WAIT_FOR_1000);

    }

    private void captureAddingArticleToDefaultList(String mode) {
        PageActivityTest pageActivityTest = new PageActivityTest();
        Intent intent = new Intent();
        pageActivityTest.activityTestRule.launchActivity(intent);
        waitFor(WAIT_FOR_6000);

        SearchBehaviors.searchKeywordAndGo("Barack Obama", false);
        whileWithMaxSteps(
                () -> !viewIsDisplayed(R.id.search_results_list),
                () -> waitFor(WAIT_FOR_1000),
                10);


        DataInteraction view = onData(anything())
                .inAdapterView(allOf(withId(R.id.search_results_list),
                        childAtPosition(
                                withId(R.id.search_results_container),
                                1)))
                .atPosition(0);
        view.perform(click());
        waitFor(WAIT_FOR_1000);


        onView(withId(R.id.page_actions_tab_layout))
                .perform(selectTab(0));
        waitFor(WAIT_FOR_1000);

        ScreenshotTools.snap("AddToReadingListFirstTimeRationale" + mode);
        waitFor(WAIT_FOR_500);

        if (viewIsDisplayed(R.id.onboarding_button)) {
            onView(ViewTools.first(withText("Got it"))).perform(click());
        }
        whileWithMaxSteps(
                () -> !viewIsDisplayed(R.id.list_of_lists),
                () -> waitFor(WAIT_FOR_1000),
                10);

        onView(withId(R.id.list_of_lists)).perform(RecyclerViewActions.scrollToPosition(0)).perform(click());
        waitFor(WAIT_FOR_1000);
        ScreenshotTools.snap("ObamaSavedToDefaultListRationale" + mode);
        waitFor(WAIT_FOR_1000);
        pressBack();
    }

    private void captureInitialReadingTabViews(String mode) {
        waitFor(WAIT_FOR_1000);
        ScreenshotTools.snap("ReadingTab" + mode);
        waitFor(WAIT_FOR_2000);

        onView(withId(R.id.reading_list_list)).perform(RecyclerViewActions.scrollToPosition(0)).perform(click());
        waitFor(WAIT_FOR_2000);
        ScreenshotTools.snap("DefaultListEmpty" + mode);
        pressBack();
        whileWithMaxSteps(
                () -> !viewIsDisplayed(R.id.fragment_main_nav_tab_layout),
                () -> waitFor(WAIT_FOR_2000),
                10);
    }

    private void runComparisons(String mode) throws Exception {
        assertScreenshotWithinTolerance("ReadingTab" + mode);
        assertScreenshotWithinTolerance("DefaultListEmpty" + mode);
        assertScreenshotWithinTolerance("AddToReadingListFirstTimeRationale" + mode);
        assertScreenshotWithinTolerance("ObamaSavedToDefaultListRationale" + mode);
        assertScreenshotWithinTolerance("DefaultListNonEmpty" + mode);
        assertScreenshotWithinTolerance("ReadingListsFragmentNonEmptyDefaultList" + mode);
        assertScreenshotWithinTolerance("DefaultListOverflow" + mode);
        assertScreenshotWithinTolerance("ReadingListSaveOffline" + mode);
        assertScreenshotWithinTolerance("ReadingListRemoveOffline" + mode);
        assertScreenshotWithinTolerance("ReadingListFragmentArticleOverflow" + mode);
        assertScreenshotWithinTolerance("AddToAnotherListDialog" + mode);
        assertScreenshotWithinTolerance("CreateNewReadingListDialog" + mode);
        assertScreenshotWithinTolerance("CreateNewReadingListDialogNameError" + mode);
        assertScreenshotWithinTolerance("ArticleDeletedRationale" + mode);
        assertScreenshotWithinTolerance("ListOverflow" + mode);
        assertScreenshotWithinTolerance("ListRenameDialog" + mode);
        assertScreenshotWithinTolerance("ListEditDescDialog" + mode);
        assertScreenshotWithinTolerance("ListWithDesc" + mode);
        assertScreenshotWithinTolerance("ListEditDescDialogWithPreviousText" + mode);
        assertScreenshotWithinTolerance("ListDeleteConfirmationDialog" + mode);
        assertScreenshotWithinTolerance("ReadingListSortMenu" + mode);
        assertScreenshotWithinTolerance("ReadingListSearchWithResults" + mode);
        assertScreenshotWithinTolerance("ReadingListSearchWithNoResults" + mode);
        assertScreenshotWithinTolerance("ReadingListsSortMenu" + mode);
        assertScreenshotWithinTolerance("ReadingListsSearchWithResults" + mode);
        assertScreenshotWithinTolerance("ReadingListsSearchWithNoResults" + mode);
        assertScreenshotWithinTolerance("ReadingListsFragmentWithNoEmptyStateMessages" + mode);
    }

    private static void navigateToReadingListsFromExploreTab() {
        whileWithMaxSteps(
                () -> !viewIsDisplayed(R.id.fragment_main_nav_tab_layout),
                ViewTools::pressBack);
        whileWithMaxSteps(
                () -> !viewIsDisplayed(R.id.fragment_main_nav_tab_layout),
                () -> waitFor(WAIT_FOR_2000));

        onView(withId(R.id.fragment_main_nav_tab_layout))
                .perform(selectTab(NavTab.READING_LISTS.code()))
                .check(matches(isDisplayed()));

        waitFor(WAIT_FOR_2000);
    }

}
