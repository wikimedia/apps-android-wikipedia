package org.wikipedia.main

import android.graphics.Color
import android.os.Build
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.IdlingPolicies
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition
import androidx.test.espresso.matcher.RootMatchers.withDecorView
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.espresso.web.assertion.WebViewAssertions
import androidx.test.espresso.web.sugar.Web.onWebView
import androidx.test.espresso.web.webdriver.DriverAtoms
import androidx.test.espresso.web.webdriver.DriverAtoms.findElement
import androidx.test.espresso.web.webdriver.DriverAtoms.webClick
import androidx.test.espresso.web.webdriver.Locator
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import org.hamcrest.Matchers
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.not
import org.hamcrest.core.IsInstanceOf
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.wikipedia.BuildConfig
import org.wikipedia.R
import org.wikipedia.TestUtil
import org.wikipedia.TestUtil.childAtPosition
import org.wikipedia.TestUtil.isDisplayed
import org.wikipedia.navtab.NavTab
import java.util.concurrent.TimeUnit

@LargeTest
@RunWith(AndroidJUnit4::class)
class SmokeTests {

    @Rule
    @JvmField
    var mActivityTestRule = ActivityScenarioRule(MainActivity::class.java)
    private lateinit var activity: MainActivity

    @Before
    fun setActivity() {
        mActivityTestRule.scenario.onActivity {
            activity = it
        }
    }

    @Test
    fun mainActivityTest() {
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        val screenWidth = device.displayWidth
        val screenHeight = device.displayHeight

        IdlingPolicies.setMasterPolicyTimeout(20, TimeUnit.SECONDS)

        TestUtil.delay(1)

        // Flip through the initial onboarding screens...
        onView(allOf(withId(R.id.fragment_onboarding_forward_button), isDisplayed()))
                .perform(click())

        onView(allOf(withId(R.id.fragment_onboarding_forward_button), isDisplayed()))
                .perform(click())

        onView(allOf(withId(R.id.fragment_onboarding_forward_button), isDisplayed()))
                .perform(click())

        // Dismiss initial onboarding by clicking on the "Get started" button
        onView(allOf(withId(R.id.fragment_onboarding_done_button), withText("Get started"), isDisplayed()))
                .perform(click())

        TestUtil.delay(2)

        // Dismiss the Feed customization onboarding card in the feed
        onView(allOf(withId(R.id.view_announcement_action_negative), withText("Got it"), isDisplayed()))
                .perform(click())

        TestUtil.delay(1)

        // Click the Search box
        onView(allOf(withId(R.id.search_container), isDisplayed()))
                .perform(click())

        TestUtil.delay(1)

        // Type in our search term
        onView(allOf(withId(androidx.appcompat.R.id.search_src_text), isDisplayed()))
                .perform(replaceText(SEARCH_TERM), closeSoftKeyboard())

        // Give the API plenty of time to return results
        TestUtil.delay(5)

        // Make sure one of the results matches the title that we expect
        onView(allOf(withId(R.id.page_list_item_title), withText(ARTICLE_TITLE), isDisplayed()))
                .check(matches(withText(ARTICLE_TITLE)))

        // Rotate the device
        device.setOrientationRight()

        TestUtil.delay(2)

        // Dismiss keyboard
        pressBack()

        TestUtil.delay(1)

        // Make sure the same title appears in the new screen orientation
        onView(allOf(withId(R.id.page_list_item_title), withText(ARTICLE_TITLE), isDisplayed()))
                .check(matches(withText(ARTICLE_TITLE)))

        // Rotate the device back to the original orientation
        device.setOrientationNatural()

        TestUtil.delay(2)

        device.unfreezeRotation()

        TestUtil.delay(2)

        // Click on the first search result
        onView(withId(R.id.search_results_list))
                .perform(actionOnItemAtPosition<RecyclerView.ViewHolder>(0, click()))

        // Give the page plenty of time to load fully
        TestUtil.delay(5)

        // Dismiss tooltip
        onView(allOf(withId(R.id.buttonView))).inRoot(withDecorView(not(Matchers.`is`(activity.window.decorView))))
            .perform(click())

        TestUtil.delay(2)

        // Click on a link to load a Link Preview dialog
        onWebView().withElement(findElement(Locator.CSS_SELECTOR, "a[title='3-sphere']"))
            .perform(webClick())

        TestUtil.delay(3)

        // Click through the preview to load a new article
        onView(allOf(withId(R.id.link_preview_toolbar)))
            .perform(click())

        TestUtil.delay(3)

        // Click on another link
        onWebView().withElement(findElement(Locator.CSS_SELECTOR, "a[title='Sphere']"))
            .perform(webClick())

        TestUtil.delay(3)

        // Open it in a new tab
        onView(allOf(withId(R.id.link_preview_secondary_button)))
            .perform(click())

        TestUtil.delay(2)

        // Ensure that there are now two tabs
        onView(allOf(withId(R.id.tabsCountText)))
            .check(matches(withText("2")))

        // Go back to the original article
        pressBack()

        TestUtil.delay(2)

        // Ensure the header view (with lead image) is displayed
        onView(allOf(withId(R.id.page_header_view)))
            .check(matches(isDisplayed()))

        // Click on the lead image to launch the full-screen gallery
        onView(allOf(withId(R.id.view_page_header_image)))
            .perform(click())

        TestUtil.delay(3)

        // Swipe to next image
        onView(allOf(withId(R.id.pager))).perform(swipeLeft())

        TestUtil.delay(2)

        // Click the overflow menu
        onView(allOf(withContentDescription("More options"), isDisplayed())).perform(click())

        TestUtil.delay(2)

        // Click to visit image page
        onView(allOf(withId(R.id.title), withText("Go to image page"), isDisplayed())).perform(click())

        TestUtil.delay(2)

        // Go back to gallery view
        pressBack()

        // Go back to the article
        pressBack()

        onWebView().forceJavascriptEnabled()

        // Ensure the article title matches what we expect
        onWebView().withElement(findElement(Locator.CSS_SELECTOR, "h1"))
                .check(WebViewAssertions.webMatches(DriverAtoms.getText(), Matchers.`is`(ARTICLE_TITLE)))

        // Rotate the display to landscape
        device.setOrientationRight()

        TestUtil.delay(2)

        // Make sure the header view (with lead image) is not shown in landscape mode
        onView(allOf(withId(R.id.page_header_view)))
                .check(matches(TestUtil.isNotVisible()))

        // Make sure the article title still matches what we expect
        onWebView().withElement(findElement(Locator.CSS_SELECTOR, "h1"))
                .check(WebViewAssertions.webMatches(DriverAtoms.getText(), Matchers.`is`(ARTICLE_TITLE)))

        // Rotate the device back to the original orientation
        device.setOrientationNatural()

        TestUtil.delay(2)

        device.unfreezeRotation()

        // Bring up the theme chooser dialog
        onView(withId(R.id.page_theme)).perform(click())

        TestUtil.delay(2)

        // Switch off the "match system theme" option
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            onView(withId(R.id.theme_chooser_match_system_theme_switch))
                    .check(matches(TestUtil.isNotVisible()))
        } else {
            onView(withId(R.id.theme_chooser_match_system_theme_switch))
                .perform(scrollTo(), click())

            TestUtil.delay(1)
        }

        // Select the Black theme
        onView(withId(R.id.button_theme_black)).perform(scrollTo(), click())

        TestUtil.delay(2)

        pressBack()

        TestUtil.delay(1)

        // Make sure the background is black
        onView(withId(R.id.page_actions_tab_layout)).check(matches(TestUtil.hasBackgroundColor(Color.BLACK)))

        // Go back to the Light theme
        onView(withId(R.id.page_theme))
                .perform(click())

        TestUtil.delay(1)

        onView(withId(R.id.button_theme_light)).perform(scrollTo(), click())

        TestUtil.delay(2)

        pressBack()

        // Click the edit pencil at the top of the article
        onWebView().withElement(findElement(Locator.CSS_SELECTOR, "a[data-id='0'].pcs-edit-section-link"))
                .perform(webClick())

        TestUtil.delay(1)

        // Click the "edit introduction" menu item
        onView(allOf(withId(R.id.title), withText("Edit introduction"), isDisplayed()))
                .perform(click())

        TestUtil.delay(3)

        // Click on the fonts and theme icon
        onView(allOf(withId(R.id.menu_edit_theme), isDisplayed()))
            .perform(click())

        TestUtil.delay(2)

        // Increase text size by clicking on increase text icon
        onView(allOf(withId(R.id.buttonIncreaseTextSize))).perform(scrollTo(), click())

        TestUtil.delay(2)

        onView(allOf(withId(R.id.buttonIncreaseTextSize))).perform(scrollTo(), click())

        TestUtil.delay(2)

        // Exit bottom sheet
        pressBack()

        TestUtil.delay(4)

        onView(allOf(withId(R.id.menu_edit_theme), isDisplayed()))
            .perform(click())

        TestUtil.delay(3)

        // Decrease text size by clicking on decrease text icon
        onView(allOf(withId(R.id.buttonDecreaseTextSize))).perform(scrollTo(), click())

        TestUtil.delay(2)

        onView(allOf(withId(R.id.buttonDecreaseTextSize))).perform(scrollTo(), click())

        TestUtil.delay(2)

        // Exit bottom sheet
        pressBack()

        TestUtil.delay(3)

        // Type in some stuff into the edit window
        onView(allOf(withId(R.id.edit_section_text))).perform(replaceText("abc"))

        TestUtil.delay(3)

        // Proceed to edit preview
        onView(allOf(withId(R.id.edit_actionbar_button_text), isDisplayed())).perform(click())

        // Give sufficient time for the API to load the preview
        TestUtil.delay(2)

        onView(allOf(withId(R.id.edit_actionbar_button_text), isDisplayed())).perform(click())

        TestUtil.delay(3)

        // Click one of the default edit summary choices
        onView(allOf(withText("Fixed typo"))).perform(scrollTo(), click())

        TestUtil.delay(3)

        // Go back out of the editing workflow
        onView(allOf(withContentDescription("Navigate up"), isDisplayed())).perform(click())

        TestUtil.delay(1)

        onView(allOf(withContentDescription("Navigate up"), isDisplayed())).perform(click())

        TestUtil.delay(1)

        onView(allOf(withContentDescription("Navigate up"), isDisplayed())).perform(click())

        TestUtil.delay(1)

        // Choose to remain in the editing workflow
        onView(allOf(withId(android.R.id.button2), withText("No"))).perform(scrollTo(), click())

        TestUtil.delay(1)

        pressBack()

        TestUtil.delay(1)

        // Choose to leave the editing workflow
        onView(allOf(withId(android.R.id.button1), withText("Yes")))
                .perform(scrollTo(), click())

        TestUtil.delay(2)

        // Click on the Tabs button to launch the tabs screen
        onView(allOf(withId(R.id.page_toolbar_button_tabs), isDisplayed()))
                .perform(click())

        TestUtil.delay(2)

        // Create a new tab (which should load the Main Page)
        onView(allOf(withContentDescription("New tab"), isDisplayed()))
                .perform(click())

        TestUtil.delay(5)

        // Open the Tabs screen again
        onView(allOf(withId(R.id.page_toolbar_button_tabs), isDisplayed()))
                .perform(click())

        TestUtil.delay(2)

        // Click on the previous tab in the list
        device.click(screenWidth / 2, screenHeight * 50 / 100)

        TestUtil.delay(2)

        // Swipe down on the WebView to reload the contents
        onView(withId(R.id.page_contents_container))
                .perform(TestUtil.swipeDownWebView())

        TestUtil.delay(5)

        // Ensure that the title in the WebView is still what we expect
        onWebView().withElement(findElement(Locator.CSS_SELECTOR, "h1"))
                .check(WebViewAssertions.webMatches(DriverAtoms.getText(), Matchers.`is`(ARTICLE_TITLE)))

        // Swipe left to show the table of contents
        onView(allOf(withId(R.id.page_web_view)))
                .perform(swipeLeft())

        // Make sure the topmost item in the table of contents is the article title
        onView(allOf(withId(R.id.page_toc_item_text), withText(ARTICLE_TITLE)))
                .check(matches(isDisplayed()))

        // Swipe the table of contents to go all the way to the bottom
        onView(allOf(withId(R.id.toc_list)))
                .perform(swipeUp())

        // Select the "About this article" item
        onView(allOf(withId(R.id.page_toc_item_text), withText("About this article")))
                .perform(click())

        TestUtil.delay(2)

        // Go to the Talk page for this article
        onWebView().withElement(findElement(Locator.CSS_SELECTOR, "a[title='View talk page']"))
                .perform(webClick())

        TestUtil.delay(4)

        // Click on the 3rd topic
        onView(withId(R.id.talkRecyclerView))
            .perform(actionOnItemAtPosition<RecyclerView.ViewHolder>(2, click()))

        // Give the page plenty of time to load fully
        TestUtil.delay(5)

        // Go back out of the Talk interface
        pressBack()

        // Get back to article screen
        pressBack()

        TestUtil.delay(2)

        // Click on the Save button to add article to reading list
        onView(withId(R.id.page_save)).perform(click())

        TestUtil.delay(1)

        // Access article in a different language
        onView(allOf(withId(R.id.page_language), withContentDescription("Language"), isDisplayed())).perform(click())

        TestUtil.delay(2)

        onView(allOf(withId(R.id.langlinks_recycler))).perform(actionOnItemAtPosition<RecyclerView.ViewHolder>(3, click()))

        TestUtil.delay(2)

        // Ensure that the title in the WebView is still what we expect
        onWebView().withElement(findElement(Locator.CSS_SELECTOR, "h1"))
            .check(WebViewAssertions.webMatches(DriverAtoms.getText(), Matchers.`is`(ARTICLE_TITLE_ESPANOL)))

        TestUtil.delay(1)

        onView(withId(R.id.page_toolbar_button_show_overflow_menu)).perform(click())

        TestUtil.delay(1)

        // Navigate back to Explore feed
        onView(withText("Explore")).perform(click())

        TestUtil.delay(2)

        // Featured article card seen and saved to reading lists
        onView(allOf(withId(R.id.view_featured_article_card_content_container),
            childAtPosition(childAtPosition(withClassName(Matchers.`is`("org.wikipedia.feed.featured.FeaturedArticleCardView")), 0), 1), isDisplayed()))
            .perform(scrollTo(), longClick())

        onView(allOf(withId(R.id.title), withText("Save"),
            childAtPosition(childAtPosition(withId(androidx.appcompat.R.id.content), 0), 0), isDisplayed()))
            .perform(click())

        TestUtil.delay(2)

        onView(allOf(withId(R.id.feed_view), isNotFocused())).perform(RecyclerViewActions.scrollToPosition<RecyclerView.ViewHolder>(3))

        TestUtil.delay(2)

        // Top read card seen and saved to reading lists
        onView(allOf(withId(R.id.view_list_card_list), childAtPosition(withId(R.id.view_list_card_list_container), 0)))
            .perform(actionOnItemAtPosition<RecyclerView.ViewHolder>(1, longClick()))

        onView(allOf(withId(R.id.title), withText("Save"),
            childAtPosition(childAtPosition(withId(androidx.appcompat.R.id.content), 0), 0), isDisplayed()))
            .perform(click())

        TestUtil.delay(2)

        onView(allOf(withId(R.id.feed_view), isNotFocused())).perform(RecyclerViewActions.scrollToPosition<RecyclerView.ViewHolder>(4))
            .perform(click())

        TestUtil.delay(2)

        pressBack()

        TestUtil.delay(2)

        goToTop()

        TestUtil.delay(2)

        onView(allOf(withId(R.id.feed_view), isNotFocused())).perform(RecyclerViewActions.scrollToPosition<RecyclerView.ViewHolder>(6))

        TestUtil.delay(3)

        onView(allOf(withId(R.id.news_cardview_recycler_view), childAtPosition(withId(R.id.rtl_container), 1)))
            .perform(actionOnItemAtPosition<RecyclerView.ViewHolder>(0, click()))

        TestUtil.delay(3)

        // News card seen and news item saved to reading lists
        onView(allOf(withId(R.id.news_story_items_recyclerview),
            childAtPosition(withClassName(Matchers.`is`("android.widget.LinearLayout")), 1)))
            .perform(actionOnItemAtPosition<RecyclerView.ViewHolder>(0, longClick()))

        onView(allOf(withId(R.id.title), withText("Save"),
            childAtPosition(childAtPosition(withId(androidx.appcompat.R.id.content), 0), 0), isDisplayed()))
            .perform(click())

        TestUtil.delay(2)

        pressBack()

        TestUtil.delay(2)

        onView(allOf(withId(R.id.feed_view), isNotFocused())).perform(RecyclerViewActions.scrollToPosition<RecyclerView.ViewHolder>(7))

        TestUtil.delay(2)

        // On this day card seen and saved to reading lists
        onView(allOf(withId(R.id.on_this_day_page), childAtPosition(allOf(withId(R.id.event_layout),
            childAtPosition(withId(R.id.on_this_day_card_view_click_container), 0)), 3), isDisplayed()))
            .perform(longClick())

        onView(allOf(withId(R.id.title), withText("Save"),
            childAtPosition(childAtPosition(withId(androidx.appcompat.R.id.content), 0), 0), isDisplayed()))
            .perform(click())

        TestUtil.delay(2)

        goToTop()

        TestUtil.delay(2)

        onView(allOf(withId(R.id.feed_view), isDisplayed())).perform(RecyclerViewActions.scrollToPosition<RecyclerView.ViewHolder>(8))

        TestUtil.delay(2)

        // Random article card seen and saved to reading lists
        onView(allOf(withId(R.id.view_featured_article_card_content_container),
            childAtPosition(childAtPosition(withClassName(Matchers.`is`("org.wikipedia.feed.random.RandomCardView")), 0), 1), isDisplayed()))
            .perform(scrollTo(), longClick())

        onView(allOf(withId(R.id.title), withText("Save"),
            childAtPosition(childAtPosition(withId(androidx.appcompat.R.id.content), 0), 0), isDisplayed()))
            .perform(click())

        TestUtil.delay(2)

        onView(allOf(withId(R.id.feed_view), isDisplayed())).perform(RecyclerViewActions.scrollToPosition<RecyclerView.ViewHolder>(9))

        TestUtil.delay(5)

        // Main page card seen clicked
        onView(allOf(withId(R.id.footerActionButton), withText("View main page  "),
            childAtPosition(allOf(withId(R.id.card_footer), childAtPosition(withClassName(Matchers.`is`("android.widget.LinearLayout")), 1)), 0), isDisplayed()))
            .perform(click())

        TestUtil.delay(2)

        pressBack()

        TestUtil.delay(2)

        // Access the other navigation tabs - `Saved`, `Search` and `Edits`
        onView(allOf(withId(R.id.nav_tab_reading_lists), withContentDescription("Saved"),
            childAtPosition(childAtPosition(withId(R.id.main_nav_tab_layout), 0), 1), isDisplayed())).perform(click())

        TestUtil.delay(2)

        onView(allOf(withId(R.id.nav_tab_search), withContentDescription("Search"),
            childAtPosition(childAtPosition(withId(R.id.main_nav_tab_layout), 0), 2), isDisplayed())).perform(click())

        TestUtil.delay(2)
        onView(allOf(withId(R.id.nav_tab_edits), withContentDescription("Edits"),
            childAtPosition(childAtPosition(withId(R.id.main_nav_tab_layout), 0), 3), isDisplayed())).perform(click())

        TestUtil.delay(2)

        // Click on `More` menu
        onView(allOf(withId(R.id.nav_more_container), withContentDescription("More"), isDisplayed())).perform(click())

        TestUtil.delay(2)

        // Click on `Settings` option
        onView(allOf(withId(R.id.main_drawer_settings_container), isDisplayed())).perform(click())

        TestUtil.delay(2)

        // Click on `Explore feed` option
        onView(allOf(withId(R.id.recycler_view),
            childAtPosition(withId(android.R.id.list_container), 0)))
            .perform(actionOnItemAtPosition<RecyclerView.ViewHolder>(2, click()))

        TestUtil.delay(2)

        onView(allOf(withContentDescription("More options"),
            childAtPosition(childAtPosition(withId(R.id.toolbar), 2), 0), isDisplayed()))
            .perform(click())

        TestUtil.delay(2)

        // Choose the option to hide all explore feed cards
        onView(allOf(withId(R.id.title), withText("Hide all"),
            childAtPosition(childAtPosition(withId(androidx.appcompat.R.id.content), 0), 0), isDisplayed()))
            .perform(click())

        TestUtil.delay(2)

        pressBack()

        TestUtil.delay(2)

        pressBack()

        TestUtil.delay(2)

        // Navigate to Explore feed
        onView(allOf(withId(R.id.nav_tab_explore), withContentDescription("Explore"),
            childAtPosition(childAtPosition(withId(R.id.main_nav_tab_layout), 0), 0), isDisplayed())).perform(click())

        TestUtil.delay(4)

        // Assert that all cards are hidden and empty container is shown
        onView(allOf(withId(R.id.empty_container), withParent(withParent(withId(R.id.swipe_refresh_layout))), isDisplayed()))
            .check(matches(isDisplayed()))

        TestUtil.delay(2)

        // Click on `More` menu
        onView(allOf(withId(R.id.nav_more_container), withContentDescription("More"), isDisplayed())).perform(click())

        TestUtil.delay(2)

        // Click on `Settings` option
        onView(allOf(withId(R.id.main_drawer_settings_container), isDisplayed())).perform(click())

        TestUtil.delay(2)

        // Click on `Explore feed` option
        onView(allOf(withId(R.id.recycler_view),
            childAtPosition(withId(android.R.id.list_container), 0)))
            .perform(actionOnItemAtPosition<RecyclerView.ViewHolder>(2, click()))
        TestUtil.delay(2)

        onView(allOf(withContentDescription("More options"),
            childAtPosition(childAtPosition(withId(R.id.toolbar), 2), 0), isDisplayed()))
            .perform(click())

        TestUtil.delay(2)

        // Show all cards again
        onView(allOf(withId(R.id.title), withText("Show all"),
            childAtPosition(childAtPosition(withId(androidx.appcompat.R.id.content), 0), 0), isDisplayed()))
            .perform(click())

        TestUtil.delay(2)

        pressBack()

        TestUtil.delay(2)

        pressBack()

        TestUtil.delay(2)

        // Ensure that empty message is not shown on explore feed
        onView(allOf(withId(R.id.empty_container), withParent(withParent(withId(R.id.swipe_refresh_layout))),
            TestUtil.isNotVisible())).check(matches(TestUtil.isNotVisible()))

        TestUtil.delay(2)

        // Test `Developer settings activation process via `Settings` screen
        onView(allOf(withId(R.id.nav_more_container), withContentDescription("More"), isDisplayed())).perform(click())

        TestUtil.delay(2)

        // Open settings screen
        onView(allOf(withId(R.id.main_drawer_settings_container), isDisplayed())).perform(click())

        TestUtil.delay(2)

        // Click `About the wikipedia app` option
        onView(allOf(withId(R.id.recycler_view), childAtPosition(withId(android.R.id.list_container), 0)))
            .perform(actionOnItemAtPosition<RecyclerView.ViewHolder>(14, click()))

        TestUtil.delay(2)

        // Click 7 times to activate developer mode
        for (i in 1 until 8) {
            onView(allOf(withId(R.id.about_logo_image),
                childAtPosition(childAtPosition(withId(R.id.about_container), 0), 0)))
                .perform(scrollTo(), click())
            TestUtil.delay(2)
        }

        TestUtil.delay(2)

        pressBack()

        TestUtil.delay(2)

        // Assert that developer mode is activated
        onView(allOf(withId(R.id.developer_settings), withContentDescription("Developer settings"),
            childAtPosition(childAtPosition(withId(androidx.appcompat.R.id.action_bar), 2), 0), isDisplayed()))
            .perform(click())

        TestUtil.delay(2)

        onView(allOf(withText("Developer settings"),
            withParent(allOf(withId(androidx.appcompat.R.id.action_bar),
                withParent(withId(androidx.appcompat.R.id.action_bar_container)))), isDisplayed()))
            .check(matches(withText("Developer settings")))

        TestUtil.delay(2)

        // Go back to Settings
        onView(allOf(withContentDescription("Navigate up"), isDisplayed()))
            .perform(click())

        TestUtil.delay(2)

        // Test disabling of images from settings
        onView(withId(androidx.preference.R.id.recycler_view))
            .perform(RecyclerViewActions.actionOnItem<RecyclerView.ViewHolder>
                (hasDescendant(withText(R.string.preference_title_show_images)), click()))

        TestUtil.delay(2)

        // Go to explore feed
        onView(allOf(withContentDescription("Navigate up"), isDisplayed())).perform(click())

        TestUtil.delay(2)

        // Assert that images arent shown anymore
        onView(allOf(withId(R.id.articleImage), withParent(allOf(withId(R.id.articleImageContainer),
            withParent(withId(R.id.view_wiki_article_card)))), isDisplayed())).check(ViewAssertions.doesNotExist())

        TestUtil.delay(2)

        // Go to Saved tab
        onView(withId(NavTab.READING_LISTS.id)).perform(click())

        TestUtil.delay(1)

        // Click on first item in the list
        onView(withId(R.id.recycler_view))
            .perform(actionOnItemAtPosition<RecyclerView.ViewHolder>(0, click()))

        // Waiting for the article to be saved to the database
        TestUtil.delay(5)

        // Dismiss tooltip, if any
        onView(allOf(withId(R.id.buttonView)))
            .inRoot(withDecorView(not(Matchers.`is`(activity.window.decorView)))).perform(click())

        TestUtil.delay(1)

        // Make sure one of the list item matches the title that we expect
        onView(allOf(withId(R.id.page_list_item_title), withText(ARTICLE_TITLE), isDisplayed()))
            .check(matches(withText(ARTICLE_TITLE)))

        // Turn on airplane mode to test offline reading
        TestUtil.setAirplaneMode(true)

        TestUtil.delay(2)

        // Access article in offline mode
        onView(allOf(withId(R.id.page_list_item_title), withText(ARTICLE_TITLE), isDisplayed()))
            .perform(click())

        TestUtil.delay(5)

        // Click on bookmark icon and open the menu
        onView(withId(R.id.page_save)).perform(click())

        TestUtil.delay(2)

        // Remove article from reading list
        onView(withText("Remove from Saved")).perform(click())

        TestUtil.delay(5)

        // Back to reading list screen
        pressBack()

        TestUtil.delay(2)

        // Back to `Saved` tab
        pressBack()

        TestUtil.delay(2)

        // Test history clearing feature - Go to search tab
        onView(allOf(withId(R.id.nav_tab_search), withContentDescription("Search"),
            childAtPosition(childAtPosition(withId(R.id.main_nav_tab_layout), 0), 2), isDisplayed())).perform(click())

        TestUtil.delay(2)

        // Click on `Clear history` icon
        onView(allOf(withId(R.id.history_delete), withContentDescription("Clear history"), isDisplayed())).perform(click())

        TestUtil.delay(2)

        // Assert deletion message
        onView(allOf(withId(androidx.appcompat.R.id.alertTitle), isDisplayed())).check(matches(withText("Clear browsing history")))

        TestUtil.delay(2)

        onView(allOf(withId(android.R.id.button2), withText("No"), isDisplayed())).perform(scrollTo(), click())

        TestUtil.delay(2)

        // Turn off airplane mode
        TestUtil.setAirplaneMode(false)

        TestUtil.delay(5)

        // Click the More menu
        onView(allOf(withId(R.id.nav_more_container), isDisplayed())).perform(click())

        TestUtil.delay(1)

        // Log-in the user
        // Click the Login menu item
        onView(allOf(withId(R.id.main_drawer_login_button), isDisplayed())).perform(click())

        TestUtil.delay(2)

        // Click the login button
        onView(allOf(withId(R.id.create_account_login_button), withText("Log in"), isDisplayed()))
            .perform(click())

        TestUtil.delay(2)

        // Set environment variables to hold correct username and password
        onView(allOf(TestUtil.withGrandparent(withId(R.id.login_username_text)), withClassName(Matchers.`is`("org.wikipedia.views.PlainPasteEditText"))))
            .perform(replaceText(BuildConfig.TEST_LOGIN_USERNAME), closeSoftKeyboard())

        onView(allOf(TestUtil.withGrandparent(withId(R.id.login_password_input)), withClassName(Matchers.`is`("org.wikipedia.views.PlainPasteEditText"))))
            .perform(replaceText(BuildConfig.TEST_LOGIN_PASSWORD), closeSoftKeyboard())

        // Click the login button
        onView(withId(R.id.login_button)).perform(scrollTo(), click())

        TestUtil.delay(5)

        // Check if the list sync dialog is shown and subsequently dismiss it
        val listSyncDialogButton = onView(allOf(withId(android.R.id.button2), withText("No thanks"),
            childAtPosition(childAtPosition(withId(androidx.appcompat.R.id.buttonPanel), 0), 2)))

        if (listSyncDialogButton.isDisplayed()) {
            listSyncDialogButton.perform(scrollTo(), click())
        }

        TestUtil.delay(1)

        // Click on Notifications from the app bar on Explore feed
        onView(allOf(withId(R.id.menu_notifications), withContentDescription("Notifications"),
            isDisplayed())).perform(click())

        // Give the page plenty of time to load fully
        TestUtil.delay(3)

        // Click on the search bar
        onView(withId(R.id.notifications_recycler_view))
            .perform(actionOnItemAtPosition<RecyclerView.ViewHolder>(0, click()))

        // Make the keyboard disappear
        pressBack()

        TestUtil.delay(1)

        // Get out of search action mode
        onView(allOf(withContentDescription("Navigate up"), isDisplayed())).perform(click())

        TestUtil.delay(1)

        // Return to explore tab
        onView(allOf(withContentDescription("Navigate up"), isDisplayed())).perform(click())

        TestUtil.delay(1)

        onView(allOf(withId(R.id.nav_tab_explore), withContentDescription("Explore"),
            childAtPosition(childAtPosition(withId(R.id.main_nav_tab_layout), 0), 0), isDisplayed())).perform(click())

        TestUtil.delay(1)

        goToTop()

        // Access Suggested edits card
        onView(allOf(withId(R.id.feed_view), isDisplayed())).perform(RecyclerViewActions.scrollToPosition<RecyclerView.ViewHolder>(10))

        TestUtil.delay(2)

        onView(allOf(withId(R.id.callToActionButton), withText("Add article description")))
            .perform(scrollTo())

        onView(allOf(withId(R.id.callToActionButton), withText("Add article description"),
            childAtPosition(allOf(withId(R.id.viewArticleContainer), childAtPosition(withId(R.id.cardItemContainer), 1)), 6), isDisplayed()))
            .perform(click())

        TestUtil.delay(2)

        // Dismiss onboarding
        pressBack()

        TestUtil.delay(2)

        // Back to explore feed
        pressBack()

        TestUtil.delay(2)

        // Click on `More` menu
        onView(allOf(withId(R.id.nav_more_container), withContentDescription("More"), isDisplayed()))
            .perform(click())

        TestUtil.delay(1)

        // Click on `Watchlist` option
        onView(allOf(withId(R.id.main_drawer_watchlist_container), isDisplayed())).perform(click())

        TestUtil.delay(1)

        // Return to explore tab
        onView(allOf(withContentDescription("Navigate up"), isDisplayed())).perform(click())

        TestUtil.delay(2)

        // Click on featured article card
        onView(allOf(withId(R.id.view_featured_article_card_content_container)))
            .perform(scrollTo(), click())

        TestUtil.delay(2)

        // Add the article to watchlist
        onView(withId(R.id.page_toolbar_button_show_overflow_menu)).perform(click())

        TestUtil.delay(1)

        onView(withText("Watch")).perform(click())

        TestUtil.delay(1)

        // Assert we see a snackbar after adding article to watchlist
        onView(allOf(withId(com.google.android.material.R.id.snackbar_text), isDisplayed()))
            .check(matches(isDisplayed()))

        onView(allOf(withId(com.google.android.material.R.id.snackbar_action), isDisplayed()))
                .check(matches(isDisplayed()))

        // Change article watchlist expiry via the snackbar action button
        onView(allOf(withId(com.google.android.material.R.id.snackbar_action), withText("Change"),
            isDisplayed())).perform(click())

        onView(allOf(withId(R.id.watchlistExpiryOneMonth), isDisplayed())).perform(click())

        TestUtil.delay(1)

        onView(allOf(withId(com.google.android.material.R.id.snackbar_text), isDisplayed()))
            .check(matches(isDisplayed()))

        TestUtil.delay(1)

        onView(withId(R.id.page_toolbar_button_show_overflow_menu)).perform(click())

        TestUtil.delay(1)

        // Make sure that the `Unwatch` option is shown for the article that is being watched
        onView(withText("Unwatch")).perform(click())

        TestUtil.delay(1)

        pressBack()

        TestUtil.delay(1)

        // Go to `Edits` tab
        onView(allOf(withId(R.id.nav_tab_edits), withContentDescription("Edits"), isDisplayed())).perform(click())

        TestUtil.delay(2)

        // If it is a new account, SE tasks will not be available. Check to make sure they are.
        val cardView = onView(allOf(withId(R.id.disabledStatesView), withParent(withParent(withId(R.id.suggestedEditsScrollView))), isDisplayed()))

        if (!cardView.isDisplayed()) {
            // Click through `Edits` screen stats onboarding - also confirming tooltip display
            for (i in 1 until 5) {
                onView(allOf(withId(R.id.buttonView), withText("Got it"),
                    childAtPosition(childAtPosition(withClassName(Matchers.`is`("android.widget.LinearLayout")), 1), 0), isDisplayed())).perform(click())
                TestUtil.delay(2)
            }

            // User contributions screen tests. Enter contributions screen
            onView(allOf(withId(R.id.userStatsArrow), withContentDescription("My contributions"), isDisplayed())).perform(click())

            TestUtil.delay(2)

            // Click on filter button to view filter options
            onView(allOf(withId(R.id.filter_by_button), withContentDescription("Filter by"), isDisplayed())).perform(click())

            TestUtil.delay(2)

            // Assert the presence of all filters
            onView(allOf(withId(R.id.item_title), withText("Wikimedia Commons"), withParent(withParent(withId(R.id.recycler_view))), isDisplayed()))
                .check(matches(withText("Wikimedia Commons")))

            onView(allOf(withId(R.id.item_title), withText("Wikidata"), withParent(withParent(withId(R.id.recycler_view))), isDisplayed()))
                .check(matches(withText("Wikidata")))

            onView(allOf(withId(R.id.item_title), withText("Article"), withParent(withParent(withId(R.id.recycler_view))), isDisplayed()))
                .check(matches(withText("Article")))

            onView(allOf(withId(R.id.item_title), withText("Talk"), withParent(withParent(withId(R.id.recycler_view))), isDisplayed()))
                .check(matches(withText("Talk")))

            onView(allOf(withId(R.id.item_title), withText("User talk"), withParent(withParent(withId(R.id.recycler_view))), isDisplayed()))
                .check(matches(withText("User talk")))

            onView(allOf(withId(R.id.item_title), withText("User"), withParent(withParent(withId(R.id.recycler_view))), isDisplayed()))
                .check(matches(withText("User")))

            TestUtil.delay(2)

            // Navigate back to se tasks screen
            onView(allOf(withContentDescription("Navigate up"), isDisplayed())).perform(click())

            TestUtil.delay(2)

            // Click on one of the contributions
            onView(allOf(withId(R.id.user_contrib_recycler), isDisplayed())).perform(click())

            TestUtil.delay(2)

            onView(allOf(withContentDescription("Navigate up"), isDisplayed()))
                .perform(click())

            TestUtil.delay(2)

            // Click on `Add description` task
            onView(allOf(withId(R.id.tasksRecyclerView), childAtPosition(withId(R.id.tasksContainer), 2)))
                .perform(actionOnItemAtPosition<RecyclerView.ViewHolder>(0, click()))

            TestUtil.delay(2)

            // Assert the presence of correct action button
            onView(allOf(withId(R.id.addContributionButton), withText("Add description"),
                withParent(allOf(withId(R.id.bottomButtonContainer))), isDisplayed()))
                .check(matches(isDisplayed()))

            TestUtil.delay(2)

            onView(allOf(withContentDescription("Navigate up"), isDisplayed())).perform(click())

            TestUtil.delay(2)

            // Assert `Translate` button leading to add languages screen when there is only one language
            onView(allOf(withId(R.id.secondaryButton), withText("Translate"),
                withContentDescription("Translate Article descriptions"), isDisplayed())).perform(click())

            TestUtil.delay(2)

            // Click on add language button
            onView(allOf(withId(R.id.wikipedia_languages_recycler), childAtPosition(withClassName(Matchers.`is`("android.widget.LinearLayout")), 1)))
                .perform(actionOnItemAtPosition<RecyclerView.ViewHolder>(2, click()))

            TestUtil.delay(2)

            // Select a language
            onView(allOf(withId(R.id.languages_list_recycler), isDisplayed())).perform(click())

            TestUtil.delay(2)

            onView(allOf(withContentDescription("Navigate up"), isDisplayed())).perform(click())

            TestUtil.delay(2)

            // Assert `Translate` button leading to translate description screen, when there is more than one language
            val button = onView(allOf(withId(R.id.secondaryButton), withText("Translate"), withContentDescription("Translate Article descriptions"),
                    withParent(withParent(IsInstanceOf.instanceOf(androidx.cardview.widget.CardView::class.java))), isDisplayed()))
            button.check(matches(isDisplayed()))

            onView(allOf(withId(R.id.secondaryButton), withText("Translate"), withContentDescription("Translate Article descriptions"),
                    childAtPosition(childAtPosition(withClassName(Matchers.`is`("org.wikipedia.suggestededits.SuggestedEditsTaskView")), 0), 6), isDisplayed())).perform(click())

            TestUtil.delay(2)

            // Verify image caption translation task
            onView(allOf(withId(R.id.secondaryButton), withText("Translate"), withContentDescription("Translate Image captions"), isDisplayed())).perform(click())

            TestUtil.delay(2)

            // Assert the presence of correct action button text
            onView(allOf(withId(R.id.addContributionButton), withText("Add translation"), withParent(allOf(withId(R.id.bottomButtonContainer))), isDisplayed()))
                .check(matches(isDisplayed()))

            onView(allOf(withContentDescription("Navigate up"), isDisplayed())).perform(click())

            TestUtil.delay(2)

            // Click on `Image captions` task
            onView(allOf(withId(R.id.tasksRecyclerView), childAtPosition(withId(R.id.tasksContainer), 2)))
                .perform(actionOnItemAtPosition<RecyclerView.ViewHolder>(1, click()))

            TestUtil.delay(2)

            // Assert the presence of correct action button
            onView(allOf(withId(R.id.addContributionButton), withText("Add caption"), withParent(allOf(withId(R.id.bottomButtonContainer))), isDisplayed()))
                .check(matches(isDisplayed()))

            TestUtil.delay(2)

            onView(allOf(withContentDescription("Navigate up"), isDisplayed())).perform(click())

            TestUtil.delay(2)

            // Click on `Image tags` task
            onView(allOf(withId(R.id.tasksRecyclerView), childAtPosition(withId(R.id.tasksContainer), 2)))
                .perform(actionOnItemAtPosition<RecyclerView.ViewHolder>(2, click()))

            TestUtil.delay(2)

            onView(allOf(withId(R.id.onboarding_done_button), withText("Get started"), isDisplayed())).perform(click())

            TestUtil.delay(2)

            // Assert the presence of correct action button
            onView(allOf(withText("Add tag"), withParent(allOf(withId(R.id.tagsChipGroup))), isDisplayed()))
                .check(matches(isDisplayed()))

            TestUtil.delay(2)

            onView(allOf(withContentDescription("Navigate up"), isDisplayed())).perform(click())

            TestUtil.delay(2)

            // Assert the presence of tutorial button
            onView(allOf(withId(R.id.learnMoreButton), withText("Learn more"),
                childAtPosition(allOf(withId(R.id.learnMoreCard)), 2), isNotFocused())).perform(scrollTo())

            TestUtil.delay(2)

            onView(allOf(withText("What is Suggested edits?"), withParent(allOf(withId(R.id.learnMoreCard))), isDisplayed()))
                .check(matches(withText("What is Suggested edits?")))
        }

        TestUtil.delay(2)

        // Click on `More` menu
        onView(allOf(withId(R.id.nav_more_container), withContentDescription("More"), isDisplayed())).perform(click())

        TestUtil.delay(2)

        // Click on `Settings` option
        onView(allOf(withId(R.id.main_drawer_settings_container), isDisplayed())).perform(click())

        TestUtil.delay(2)

        // Scroll to logOut option and click
        onView(withId(androidx.preference.R.id.recycler_view)).perform(RecyclerViewActions.actionOnItem<RecyclerView.ViewHolder>
            (hasDescendant(withText(R.string.preference_title_logout)), click()))

        TestUtil.delay(2)

        onView(allOf(withText("Log out"), isDisplayed())).perform(scrollTo(), click())

        TestUtil.delay(2)

        onView(allOf(withId(android.R.id.message), isDisplayed()))
            .check(matches(withText("This will log you out on all devices where you are currently logged in. Do you want to continue?")))

        TestUtil.delay(2)
    }
    private fun goToTop() {
        onView(allOf(withId(R.id.feed_view))).perform(RecyclerViewActions.scrollToPosition<RecyclerView.ViewHolder>(0))
        TestUtil.delay(2)
    }

    companion object {
        private const val SEARCH_TERM = "hopf fibration"
        private const val ARTICLE_TITLE = "Hopf fibration"
        private const val ARTICLE_TITLE_ESPANOL = "Fibración de Hopf"
    }
}
