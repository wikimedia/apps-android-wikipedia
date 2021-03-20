package org.wikipedia.main

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
import org.wikipedia.BuildConfig
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
                .perform(replaceText("quantum teleportation"), closeSoftKeyboard())

        TestUtil.delay(5)

        onView(allOf(withId(R.id.page_list_item_title), withText("Quantum teleportation"), isDisplayed()))
                .check(matches(withText("Quantum teleportation")))

        device.setOrientationRight()

        TestUtil.delay(2)

        onView(allOf(withId(R.id.page_list_item_title), withText("Quantum teleportation"), isDisplayed()))
                .check(matches(withText("Quantum teleportation")))

        device.setOrientationNatural()
        device.unfreezeRotation()

        TestUtil.delay(2)




        onView(withId(R.id.search_results_list))
                .perform(actionOnItemAtPosition<ViewHolder>(0, click()))

        TestUtil.delay(5)



        onView(allOf(withId(R.id.page_header_view)))
                .check(matches(TestUtil.isNotVisible()))



        onWebView().forceJavascriptEnabled()


        onWebView().withElement(findElement(Locator.CSS_SELECTOR, "h1"))
                .check(WebViewAssertions.webMatches(DriverAtoms.getText(), `is`("Quantum teleportation")))


        device.setOrientationRight()

        TestUtil.delay(2)

        onWebView().withElement(findElement(Locator.CSS_SELECTOR, "h1"))
                .check(WebViewAssertions.webMatches(DriverAtoms.getText(), `is`("Quantum teleportation")))

        device.setOrientationNatural()
        device.unfreezeRotation()

        TestUtil.delay(2)


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

        pressBack()

        TestUtil.delay(2)

        onView(withId(R.id.page_contents_container))
                .perform(TestUtil.swipeDownWebView())

        TestUtil.delay(5)

        onWebView().withElement(findElement(Locator.CSS_SELECTOR, "h1"))
                .check(WebViewAssertions.webMatches(DriverAtoms.getText(), `is`("Quantum teleportation")))

    }

    @Test
    fun mainActivityTest2() {

        // Skip over onboarding screens
        onView(allOf(withId(R.id.fragment_onboarding_skip_button), isDisplayed()))
                .perform(click())

        TestUtil.delay(2)

        // Click the More menu
        onView(allOf(withId(R.id.nav_more_container), isDisplayed()))
                .perform(click())

        TestUtil.delay(1)

        // Click the Login menu item
        onView(allOf(withId(R.id.main_drawer_login_button), isDisplayed()))
                .perform(click())

        TestUtil.delay(2)

        // Click the login button
        onView(allOf(withId(R.id.create_account_login_button), withText("Log in"), isDisplayed()))
                .perform(click())

        TestUtil.delay(2)

        // Type in an incorrect username and password
        onView(allOf(TestUtil.withGrandparent(withId(R.id.login_username_text)), withClassName(`is`("org.wikipedia.views.PlainPasteEditText"))))
                .perform(replaceText(BuildConfig.TEST_LOGIN_USERNAME), closeSoftKeyboard())

        onView(allOf(TestUtil.withGrandparent(withId(R.id.login_password_input)), withClassName(`is`("org.wikipedia.views.PlainPasteEditText"))))
                .perform(replaceText(BuildConfig.TEST_LOGIN_PASSWORD), closeSoftKeyboard())

        // Click the login button
        onView(withId(R.id.login_button))
                .perform(scrollTo(), click())

        TestUtil.delay(5)

        // Verify that a snackbar appears (because the login failed.)
        onView(withId(R.id.snackbar_text))
                .check(matches(isDisplayed()))
    }
}
