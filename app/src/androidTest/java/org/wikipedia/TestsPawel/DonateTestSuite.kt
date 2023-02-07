package org.wikipedia.testspawel

import androidx.test.espresso.intent.Intents
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import junit.framework.Assert.assertEquals
import junit.framework.Assert.assertTrue
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.wikipedia.main.MainActivity
import org.wikipedia.pageobjects.*
@LargeTest
@RunWith(AndroidJUnit4::class)
class DonateTestSuite {

    private val onboardingPage = OnboardingPage()
    private val navbarPage = NavbarPage()
    private val donatePage = DonatePage()

    @Before
    fun beforeTests() {
        onboardingPage.tapOnSkipButton()
        Intents.init()
    }
    @After
    fun afterTests() {
        Intents.release()
    }

    @Rule
    @JvmField
    var mActivityTestRule = ActivityScenarioRule(MainActivity::class.java)

    @Test
    fun donateTest() {
        navbarPage.tapOnMoreBtn()
        navbarPage.tapOnDonateBtn()
        assertTrue("Displayed Url is different than expected",donatePage.isCorrectIntentSendForDonate())
    }
}