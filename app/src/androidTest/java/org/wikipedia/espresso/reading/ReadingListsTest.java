package org.wikipedia.espresso.reading;

import android.Manifest;
import android.content.Intent;
import android.support.test.espresso.DataInteraction;
import android.support.test.espresso.contrib.RecyclerViewActions;
import android.support.test.rule.ActivityTestRule;
import android.support.test.rule.GrantPermissionRule;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wikipedia.R;
import org.wikipedia.espresso.page.PageActivityTest;
import org.wikipedia.espresso.search.SearchBehaviors;
import org.wikipedia.espresso.util.ScreenshotTools;
import org.wikipedia.espresso.util.ViewTools;
import org.wikipedia.main.MainActivity;
import org.wikipedia.navtab.NavTab;

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

        if (viewIsDisplayed(R.id.view_onboarding_action_negative)) {
            onView(ViewTools.first(withId(R.id.view_onboarding_action_negative))).perform(click());
        }
        captureInitialReadingTabViews();
        captureAddingArticleToDefaultList();

        navigateToReadingListsFromExploreTab();

        captureDefaultListViews();
        pressBack();

        captureMyListsTabWithUserCreatedLists();

        runComparisons();
    }


    private void captureMyListsTabWithUserCreatedLists() {
        onView(withId(R.id.menu_search_lists)).perform(click());

        onView(withId(android.support.design.R.id.search_src_text)).perform(typeText("obama"));
        waitFor(WAIT_FOR_1000);

        ScreenshotTools.snap("ReadingListSearchWithResults");
        waitFor(WAIT_FOR_1000);

        onView(withId(android.support.design.R.id.search_src_text)).perform(typeText("oooo"));
        ScreenshotTools.snap("ReadingListSearchWithNoResults");

        pressBack();
        pressBack();
        pressBack();
        waitFor(WAIT_FOR_1000);
        onView(withId(R.id.menu_sort_options)).perform(click());
        ScreenshotTools.snap("ReadingListsSortMenu");
        waitFor(WAIT_FOR_1000);

        pressBack();
        waitFor(WAIT_FOR_1000);
        onView(withId(R.id.menu_search_lists)).perform(click());
        onView(withId(android.support.design.R.id.search_src_text)).perform(typeText("my"));
        waitFor(WAIT_FOR_1000);
        ScreenshotTools.snap("ReadingListsSearchWithResults");
        waitFor(WAIT_FOR_1000);

        onView(withId(android.support.design.R.id.search_src_text)).perform(typeText("ooo"));
        ScreenshotTools.snap("ReadingListsSearchWithNoResults");
        waitFor(WAIT_FOR_1000);

        pressBack();
        pressBack();
        waitFor(WAIT_FOR_1000);
        ScreenshotTools.snap("ReadingListsFragmentWithNoEmptyStateMessages");
    }

    private void captureDefaultListViews() {
        ScreenshotTools.snap("ReadingListsFragmentNonEmptyDefaultList");
        waitFor(WAIT_FOR_1000);
        onView(withId(R.id.reading_list_list)).perform(RecyclerViewActions.scrollToPosition(0)).perform(click());
        waitFor(WAIT_FOR_1000);
        ScreenshotTools.snap("DefaultListNonEmpty");
        waitFor(WAIT_FOR_1000);

        onView(withId(R.id.reading_list_contents)).perform(
                RecyclerViewActions.actionOnItemAtPosition(0, ViewTools.clickChildViewWithId(R.id.item_overflow_menu)));
        waitFor(WAIT_FOR_1000);
        ScreenshotTools.snap("DefaultListOverflow");
        waitFor(WAIT_FOR_1000);

        onView(withText("Save all for offline")).perform(click());
        waitFor(WAIT_FOR_500);
        ScreenshotTools.snap("ReadingListSaveOffline");

        onView(withId(R.id.reading_list_contents)).perform(
                RecyclerViewActions.actionOnItemAtPosition(0, ViewTools.clickChildViewWithId(R.id.item_overflow_menu)));
        waitFor(WAIT_FOR_1000);

        onView(withText("Remove all from offline")).perform(click());
        waitFor(WAIT_FOR_500);
        ScreenshotTools.snap("ReadingListRemoveOffline");
        waitFor(WAIT_FOR_1000);
        onView(withId(R.id.reading_list_contents)).perform(
                RecyclerViewActions.actionOnItemAtPosition(1, ViewTools.clickChildViewWithId(R.id.page_list_item_action_primary)));
        waitFor(WAIT_FOR_1000);
        ScreenshotTools.snap("ReadingListFragmentArticleOverflow");
        waitFor(WAIT_FOR_1000);
        onView(withText("Add to another reading list")).perform(click());
        waitFor(WAIT_FOR_1000);
        ScreenshotTools.snap("AddToAnotherListDialog");

        onView(withText("Create new")).perform(click());
        waitFor(WAIT_FOR_2000);
        ScreenshotTools.snap("CreateNewReadingListDialog");

        onView(withText("OK")).perform(click());
        onView(withId(R.id.reading_list_contents)).perform(
                RecyclerViewActions.actionOnItemAtPosition(1, ViewTools.clickChildViewWithId(R.id.page_list_item_action_primary)));
        waitFor(WAIT_FOR_1000);
        onView(withText("Add to another reading list")).perform(click());
        waitFor(WAIT_FOR_1000);
        onView(withText("Create new")).perform(click());

        waitFor(WAIT_FOR_1000);
        ScreenshotTools.snap("CreateNewReadingListDialogNameError");

        onView(withText("Cancel")).perform(click());
        pressBack();

        onView(withId(R.id.reading_list_contents)).perform(
                RecyclerViewActions.actionOnItemAtPosition(1, ViewTools.clickChildViewWithId(R.id.page_list_item_action_primary)));
        onView(withText("Remove from Saved")).perform(click());
        ScreenshotTools.snap("ArticleDeletedRationale");

        pressBack();
        onView(withId(R.id.reading_list_list)).perform(
                RecyclerViewActions.actionOnItemAtPosition(1, click()));
        onView(withId(R.id.reading_list_contents)).perform(
                RecyclerViewActions.actionOnItemAtPosition(0, ViewTools.clickChildViewWithId(R.id.item_overflow_menu)));
        waitFor(WAIT_FOR_1000);
        ScreenshotTools.snap("ListOverflow");
        waitFor(WAIT_FOR_1000);

        onView(withText("Rename")).perform(click());
        waitFor(WAIT_FOR_1000);

        ScreenshotTools.snap("ListRenameDialog");

        onView(withText("Cancel")).perform(click());
        onView(withId(R.id.reading_list_contents)).perform(
                RecyclerViewActions.actionOnItemAtPosition(0, ViewTools.clickChildViewWithId(R.id.item_overflow_menu)));
        waitFor(WAIT_FOR_1000);

        onView(withText("Edit description")).perform(click());
        waitFor(WAIT_FOR_1000);
        ScreenshotTools.snap("ListEditDescDialog");

        onView(withId(R.id.text_input)).perform(replaceText("test"), closeSoftKeyboard());

        onView(withText("OK")).perform(click());
        waitFor(WAIT_FOR_1000);
        ScreenshotTools.snap("ListWithDesc");

        onView(withId(R.id.reading_list_contents)).perform(
                RecyclerViewActions.actionOnItemAtPosition(0, ViewTools.clickChildViewWithId(R.id.item_overflow_menu)));
        waitFor(WAIT_FOR_1000);

        onView(withText("Edit description")).perform(click());
        waitFor(WAIT_FOR_1000);
        ScreenshotTools.snap("ListEditDescDialogWithPreviousText");
        waitFor(WAIT_FOR_1000);

        onView(withText("Cancel")).perform(click());
        onView(withId(R.id.reading_list_contents)).perform(
                RecyclerViewActions.actionOnItemAtPosition(0, ViewTools.clickChildViewWithId(R.id.item_overflow_menu)));
        waitFor(WAIT_FOR_1000);

        onView(withText("Delete list")).perform(click());
        waitFor(WAIT_FOR_1000);
        ScreenshotTools.snap("ListDeleteConfirmationDialog");

        onView(withText("Cancel")).perform(click());

        onView(withId(R.id.menu_sort_options)).perform(click());
        waitFor(WAIT_FOR_1000);

        ScreenshotTools.snap("ReadingListSortMenu");
        waitFor(WAIT_FOR_1000);

    }

    private void captureAddingArticleToDefaultList() {
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

        ScreenshotTools.snap("AddToReadingListFirstTimeRationale");
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
        ScreenshotTools.snap("ObamaSavedToDefaultListRationale");
        waitFor(WAIT_FOR_1000);
        pressBack();
    }

    private void captureInitialReadingTabViews() {
        waitFor(WAIT_FOR_1000);
        ScreenshotTools.snap("ReadingTab");
        waitFor(WAIT_FOR_2000);

        onView(withId(R.id.reading_list_list)).perform(RecyclerViewActions.scrollToPosition(0)).perform(click());
        waitFor(WAIT_FOR_2000);
        ScreenshotTools.snap("DefaultListEmpty");
        pressBack();
        whileWithMaxSteps(
                () -> !viewIsDisplayed(R.id.fragment_main_nav_tab_layout),
                () -> waitFor(WAIT_FOR_2000),
                10);
    }

    private void runComparisons() throws Exception {
        assertScreenshotWithinTolerance("ReadingTab");
        assertScreenshotWithinTolerance("DefaultListEmpty");
        assertScreenshotWithinTolerance("AddToReadingListFirstTimeRationale");
        assertScreenshotWithinTolerance("ObamaSavedToDefaultListRationale");
        assertScreenshotWithinTolerance("DefaultListNonEmpty");
        assertScreenshotWithinTolerance("ReadingListsFragmentNonEmptyDefaultList");
        assertScreenshotWithinTolerance("DefaultListOverflow");
        assertScreenshotWithinTolerance("ReadingListSaveOffline");
        assertScreenshotWithinTolerance("ReadingListRemoveOffline");
        assertScreenshotWithinTolerance("ReadingListFragmentArticleOverflow");
        assertScreenshotWithinTolerance("AddToAnotherListDialog");
        assertScreenshotWithinTolerance("CreateNewReadingListDialog");
        assertScreenshotWithinTolerance("CreateNewReadingListDialogNameError");
        assertScreenshotWithinTolerance("ArticleDeletedRationale");
        assertScreenshotWithinTolerance("ListOverflow");
        assertScreenshotWithinTolerance("ListRenameDialog");
        assertScreenshotWithinTolerance("ListEditDescDialog");
        assertScreenshotWithinTolerance("ListWithDesc");
        assertScreenshotWithinTolerance("ListEditDescDialogWithPreviousText");
        assertScreenshotWithinTolerance("ListDeleteConfirmationDialog");
        assertScreenshotWithinTolerance("ReadingListSortMenu");
        assertScreenshotWithinTolerance("ReadingListSearchWithResults");
        assertScreenshotWithinTolerance("ReadingListSearchWithNoResults");
        assertScreenshotWithinTolerance("ReadingListsSortMenu");
        assertScreenshotWithinTolerance("ReadingListsSearchWithResults");
        assertScreenshotWithinTolerance("ReadingListsSearchWithNoResults");
        assertScreenshotWithinTolerance("ReadingListsFragmentWithNoEmptyStateMessages");
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
