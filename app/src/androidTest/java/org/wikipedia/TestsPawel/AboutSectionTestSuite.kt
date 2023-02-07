package org.wikipedia.TestsPawel

import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.wikipedia.pageobjects.*
import androidx.test.espresso.intent.*
import junit.framework.Assert.*
import org.junit.After
import org.wikipedia.main.MainActivity

@RunWith(AndroidJUnit4::class)
class AboutSectionTestSuite {

    private val onboardingPage = OnboardingPage()
    private val settingsPage = SettingsPage()
    private val navbarPage = NavbarPage()

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
    fun aboutSection() {
        val selectedSectionFromAbout = "Terms of use"

        navbarPage.tapOnMoreBtn()
        navbarPage.tapOnSettingsBtn()
        settingsPage.tapSelectedSection(selectedSectionFromAbout)
        assertTrue("Displayed Url and host are different than expected",settingsPage.isCorrectIntentSendForSettings())
    }
}
