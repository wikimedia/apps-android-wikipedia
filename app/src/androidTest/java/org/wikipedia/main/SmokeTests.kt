package org.wikipedia.main

import android.graphics.Color
import android.os.Build
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.IdlingPolicies
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition
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
import org.hamcrest.Matchers.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.wikipedia.R
import org.wikipedia.TestUtil
import java.util.concurrent.TimeUnit

@LargeTest
@RunWith(AndroidJUnit4::class)
class SmokeTests {

    @Rule
    @JvmField
    var mActivityTestRule = ActivityScenarioRule(MainActivity::class.java)

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

        TestUtil.delay(1)

        onView(allOf(withId(R.id.fragment_onboarding_forward_button), isDisplayed()))
                .perform(click())

        TestUtil.delay(1)

        onView(allOf(withId(R.id.fragment_onboarding_forward_button), isDisplayed()))
                .perform(click())

        TestUtil.delay(1)

        // Make sure the user sees the usage data opt-in switch
        onView(allOf(withId(R.id.switchView), withText("Send usage data"), isDisplayed()))
                .check(matches(isDisplayed()))

        // Dismiss initial onboarding
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
        onView(allOf(withId(R.id.search_src_text), isDisplayed()))
                .perform(replaceText(SEARCH_TERM), closeSoftKeyboard())

        // Give the API plenty of time to return results
        TestUtil.delay(5)

        // Make sure one of the results matches the title that we expect
        onView(allOf(withId(R.id.page_list_item_title), withText(ARTICLE_TITLE), isDisplayed()))
                .check(matches(withText(ARTICLE_TITLE)))

        // Rotate the device
        device.setOrientationRight()

        TestUtil.delay(2)

        // Make sure the same title appears in the new screen orientation
        onView(allOf(withId(R.id.page_list_item_title), withText(ARTICLE_TITLE), isDisplayed()))
                .check(matches(withText(ARTICLE_TITLE)))

        // Rotate the device back to the original orientation
        device.setOrientationNatural()
        device.unfreezeRotation()

        TestUtil.delay(2)

        // Click on the first search result
        onView(withId(R.id.search_results_list))
                .perform(actionOnItemAtPosition<ViewHolder>(0, click()))

        // Give the page plenty of time to load fully
        TestUtil.delay(5)

        // Ensure the header view (with lead image) is displayed
        onView(allOf(withId(R.id.page_header_view)))
            .check(matches(isDisplayed()))

        // Click on the lead image to launch the full-screen gallery
        onView(allOf(withId(R.id.view_page_header_image)))
            .perform(click())

        TestUtil.delay(3)

        // Flip through the gallery a couple of times
        onView(allOf(withId(R.id.pager)))
            .perform(swipeLeft())

        TestUtil.delay(2)

        onView(allOf(withId(R.id.pager)))
            .perform(swipeLeft())

        TestUtil.delay(2)

        // Go back to the article
        pressBack()

        onWebView().forceJavascriptEnabled()

        // Ensure the article title matches what we expect
        onWebView().withElement(findElement(Locator.CSS_SELECTOR, "h1"))
                .check(WebViewAssertions.webMatches(DriverAtoms.getText(), `is`(ARTICLE_TITLE)))

        // Rotate the display to landscape
        device.setOrientationRight()

        TestUtil.delay(2)

        // Make sure the header view (with lead image) is not shown in landscape mode
        onView(allOf(withId(R.id.page_header_view)))
                .check(matches(TestUtil.isNotVisible()))

        // Make sure the article title still matches what we expect
        onWebView().withElement(findElement(Locator.CSS_SELECTOR, "h1"))
                .check(WebViewAssertions.webMatches(DriverAtoms.getText(), `is`(ARTICLE_TITLE)))

        // Rotate the device back to the original orientation
        device.setOrientationNatural()
        device.unfreezeRotation()

        TestUtil.delay(2)

        // Bring up the theme chooser dialog
        onView(withId(R.id.article_menu_font_and_theme))
                .perform(click())

        TestUtil.delay(1)

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
        onView(withId(R.id.button_theme_black))
                .perform(scrollTo(), click())

        TestUtil.delay(2)

        pressBack()

        TestUtil.delay(1)

        // Make sure the background is black
        onView(withId(R.id.page_actions_tab_layout))
                .check(matches(TestUtil.hasBackgroundColor(Color.BLACK)))

        // Go back to the Light theme
        onView(withId(R.id.article_menu_font_and_theme))
                .perform(click())

        TestUtil.delay(1)

        onView(withId(R.id.button_theme_light))
                .perform(scrollTo(), click())

        TestUtil.delay(2)

        pressBack()

        // Click the edit pencil at the top of the article
        onWebView().withElement(findElement(Locator.CSS_SELECTOR, "a[data-id='0'].pcs-edit-section-link"))
                .perform(webClick())

        TestUtil.delay(1)

        // Click the "edit introduction" menu item
        onView(allOf(withId(R.id.title), withText("Edit introduction"), isDisplayed()))
                .perform(click())

        TestUtil.delay(2)

        // Increase text size
        onView(allOf(withId(R.id.menu_edit_zoom_in), isDisplayed()))
                .perform(click())

        TestUtil.delay(1)

        // Decrease text size
        onView(allOf(withId(R.id.menu_edit_zoom_out), isDisplayed()))
                .perform(click())

        TestUtil.delay(1)

        // Type in some stuff into the edit window
        onView(allOf(withId(R.id.edit_section_text)))
                .perform(replaceText("abc"))

        TestUtil.delay(1)

        // Proceed to edit preview
        onView(allOf(withId(R.id.edit_actionbar_button_text), isDisplayed()))
                .perform(click())

        // Give sufficient time for the API to load the preview
        TestUtil.delay(5)

        // Click one of the default edit summary choices
        onView(allOf(withText("Fixed typo")))
                .perform(scrollTo(), click())

        TestUtil.delay(1)

        // Go back out of the editing workflow
        onView(allOf(withContentDescription("Navigate up"), isDisplayed()))
                .perform(click())

        TestUtil.delay(1)

        onView(allOf(withContentDescription("Navigate up"), isDisplayed()))
                .perform(click())

        TestUtil.delay(1)

        // Choose to remain in the editing workflow
        onView(allOf(withId(android.R.id.button2), withText("No")))
                .perform(scrollTo(), click())

        TestUtil.delay(1)

        pressBack()

        TestUtil.delay(1)

        // Choose to leave the editing workflow
        onView(allOf(withId(android.R.id.button1), withText("Yes")))
                .perform(scrollTo(), click())

        TestUtil.delay(1)

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
        device.click(screenWidth / 2, screenHeight * 20 / 100)

        TestUtil.delay(2)

        // Swipe down on the WebView to reload the contents
        onView(withId(R.id.page_contents_container))
                .perform(TestUtil.swipeDownWebView())

        TestUtil.delay(5)

        // Ensure that the title in the WebView is still what we expect
        onWebView().withElement(findElement(Locator.CSS_SELECTOR, "h1"))
                .check(WebViewAssertions.webMatches(DriverAtoms.getText(), `is`(ARTICLE_TITLE)))

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

        // Go back out of the Talk interface
        pressBack()

        TestUtil.delay(2)
    }

    companion object {
        private val SEARCH_TERM = "hopf fibration"
        private val ARTICLE_TITLE = "Hopf fibration"
    }
}
