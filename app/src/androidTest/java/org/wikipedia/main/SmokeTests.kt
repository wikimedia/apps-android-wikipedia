package org.wikipedia.main

import android.graphics.Color
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

        onView(allOf(withId(R.id.fragment_onboarding_forward_button), isDisplayed()))
                .perform(click())

        TestUtil.delay(1)

        onView(allOf(withId(R.id.fragment_onboarding_forward_button), isDisplayed()))
                .perform(click())

        TestUtil.delay(1)

        onView(allOf(withId(R.id.fragment_onboarding_forward_button), isDisplayed()))
                .perform(click())

        TestUtil.delay(1)

        onView(allOf(withId(R.id.switchView), withText("Send usage data"), isDisplayed()))
                .check(matches(isDisplayed()))

        onView(allOf(withId(R.id.fragment_onboarding_done_button), withText("Get started"), isDisplayed()))
                .perform(click())

        TestUtil.delay(2)

        onView(allOf(withId(R.id.view_announcement_action_negative), withText("Got it"), isDisplayed()))
                .perform(click())

        TestUtil.delay(1)


        onView(allOf(withId(R.id.search_container), isDisplayed()))
                .perform(click())

        TestUtil.delay(1)

        onView(allOf(withId(R.id.search_src_text), isDisplayed()))
                .perform(replaceText(SEARCH_TERM), closeSoftKeyboard())

        TestUtil.delay(5)

        onView(allOf(withId(R.id.page_list_item_title), withText(ARTICLE_TITLE), isDisplayed()))
                .check(matches(withText(ARTICLE_TITLE)))

        device.setOrientationRight()

        TestUtil.delay(2)

        onView(allOf(withId(R.id.page_list_item_title), withText(ARTICLE_TITLE), isDisplayed()))
                .check(matches(withText(ARTICLE_TITLE)))

        device.setOrientationNatural()
        device.unfreezeRotation()

        TestUtil.delay(2)




        onView(withId(R.id.search_results_list))
                .perform(actionOnItemAtPosition<ViewHolder>(0, click()))

        TestUtil.delay(5)



        onView(allOf(withId(R.id.page_header_view)))
                .check(matches(isDisplayed()))



        onWebView().forceJavascriptEnabled()

        onWebView().withElement(findElement(Locator.CSS_SELECTOR, "h1"))
                .check(WebViewAssertions.webMatches(DriverAtoms.getText(), `is`(ARTICLE_TITLE)))


        device.setOrientationRight()

        TestUtil.delay(2)



        onView(allOf(withId(R.id.page_header_view)))
                .check(matches(TestUtil.isNotVisible()))



        onWebView().withElement(findElement(Locator.CSS_SELECTOR, "h1"))
                .check(WebViewAssertions.webMatches(DriverAtoms.getText(), `is`(ARTICLE_TITLE)))

        device.setOrientationNatural()
        device.unfreezeRotation()

        TestUtil.delay(2)




        onView(withId(R.id.article_menu_font_and_theme))
                .perform(click())

        TestUtil.delay(1)

        onView(withId(R.id.theme_chooser_match_system_theme_switch))
                .perform(scrollTo(), click())

        TestUtil.delay(1)

        onView(withId(R.id.button_theme_black))
                .perform(scrollTo(), click())

        TestUtil.delay(2)


        pressBack()

        TestUtil.delay(1)

        onView(withId(R.id.page_actions_tab_layout))
                .check(matches(TestUtil.hasBackgroundColor(Color.BLACK)))

        onView(withId(R.id.article_menu_font_and_theme))
                .perform(click())

        TestUtil.delay(1)

        onView(withId(R.id.button_theme_light))
                .perform(scrollTo(), click())

        TestUtil.delay(2)

        pressBack()



        onWebView().withElement(findElement(Locator.CSS_SELECTOR, "a[data-id='0'].pcs-edit-section-link"))
                .perform(webClick())

        TestUtil.delay(1)


        onView(allOf(withId(R.id.title), withText("Edit introduction"), isDisplayed()))
                .perform(click())

        TestUtil.delay(2)

        onView(allOf(withId(R.id.menu_edit_zoom_in), isDisplayed()))
                .perform(click())

        TestUtil.delay(1)

        onView(allOf(withId(R.id.menu_edit_zoom_out), isDisplayed()))
                .perform(click())

        TestUtil.delay(1)

        onView(allOf(withId(R.id.edit_section_text)))
                .perform(replaceText("abc"))

        TestUtil.delay(1)

        onView(allOf(withId(R.id.edit_actionbar_button_text), isDisplayed()))
                .perform(click())

        TestUtil.delay(5)

        onView(allOf(withText("Fixed typo")))
                .perform(scrollTo(), click())

        TestUtil.delay(1)

        onView(allOf(withContentDescription("Navigate up"), isDisplayed()))
                .perform(click())

        TestUtil.delay(1)

        onView(allOf(withContentDescription("Navigate up"), isDisplayed()))
                .perform(click())

        TestUtil.delay(1)

        onView(allOf(withId(android.R.id.button2), withText("No")))
                .perform(scrollTo(), click())

        TestUtil.delay(1)

        pressBack()

        TestUtil.delay(1)

        onView(allOf(withId(android.R.id.button1), withText("Yes")))
                .perform(scrollTo(), click())


        TestUtil.delay(1)


        onView(allOf(withId(R.id.page_toolbar_button_tabs), isDisplayed()))
                .perform(click())

        TestUtil.delay(2)



        onView(allOf(withContentDescription("New tab"), isDisplayed()))
                .perform(click())

        TestUtil.delay(5)


        onView(allOf(withId(R.id.page_toolbar_button_tabs), isDisplayed()))
                .perform(click())

        TestUtil.delay(2)


        device.click(screenWidth / 2, screenHeight * 20 / 100)

        TestUtil.delay(2)



        onView(withId(R.id.page_contents_container))
                .perform(TestUtil.swipeDownWebView())

        TestUtil.delay(5)

        onWebView().withElement(findElement(Locator.CSS_SELECTOR, "h1"))
                .check(WebViewAssertions.webMatches(DriverAtoms.getText(), `is`(ARTICLE_TITLE)))





        onView(allOf(withId(R.id.page_web_view)))
                .perform(swipeLeft())

        onView(allOf(withId(R.id.page_toc_item_text), withText(ARTICLE_TITLE)))
                .check(matches(isDisplayed()))

        onView(allOf(withId(R.id.toc_list)))
                .perform(swipeUp())

        onView(allOf(withId(R.id.page_toc_item_text), withText("About this article")))
                .perform(click())

        TestUtil.delay(2)



        onWebView().withElement(findElement(Locator.CSS_SELECTOR, "a[title='View talk page']"))
                .perform(webClick())

        TestUtil.delay(4)

    }

    companion object {
        private val SEARCH_TERM = "hopf fibration"
        private val ARTICLE_TITLE = "Hopf fibration"
    }
}
