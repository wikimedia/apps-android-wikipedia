package org.wikipedia.onboarding

import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.espresso.intent.matcher.IntentMatchers.hasExtra
import org.hamcrest.Matchers.allOf
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.wikipedia.Constants
import org.wikipedia.WikipediaApp
import org.wikipedia.base.BaseTest
import org.wikipedia.feed.personalization.PersonalizationActivity
import org.wikipedia.robots.AddLanguagesRobot
import org.wikipedia.robots.OnboardingRobot
import org.wikipedia.robots.WikipediaLanguagesRobot
import org.wikipedia.settings.Prefs
import org.wikipedia.settings.languages.WikipediaLanguagesActivity

class InitialOnboardingActivityTest : BaseTest<InitialOnboardingActivity>(
    activityClass = InitialOnboardingActivity::class.java,
    beforeActivityLaunch = {
        Prefs.isInitialOnboardingEnabled = true
        Prefs.isExploreFeedUpdatePromptShown = false
        WikipediaApp.instance.languageState.setAppLanguageCodes(listOf(ENGLISH_LANGUAGE_CODE))
    }
) {
    private val onboardingRobot by lazy {
        OnboardingRobot(composeTestRule, targetContext)
    }
    private val wikipediaLanguagesRobot = WikipediaLanguagesRobot()
    private val addLanguagesRobot by lazy {
        AddLanguagesRobot(composeTestRule, targetContext)
    }

    @Before
    fun setUpIntents() {
        Intents.init()
    }

    @After
    fun releaseIntents() {
        Intents.release()
    }

    @Test
    fun freshUserCompletesInitialOnboarding() {
        onboardingRobot
            .assertIntroScreenIsDisplayed()
            .tapForward()
            .assertDataPrivacyScreenIsDisplayed()
            .tapForward()
            .assertLanguagesScreenIsDisplayed()
            .tapForward()

        intended(hasComponent(PersonalizationActivity::class.java.name))
        assertFalse(Prefs.isInitialOnboardingEnabled)
        assertTrue(Prefs.isExploreFeedUpdatePromptShown)
    }

    @Test
    fun addOrEditLanguagesOpensLanguageSettings() {
        onboardingRobot
            .tapForward()
            .tapForward()
            .assertLanguagesScreenIsDisplayed()
            .tapAddOrEditLanguages()

        intended(
            allOf(
                hasComponent(WikipediaLanguagesActivity::class.java.name),
                hasExtra(
                    Constants.INTENT_EXTRA_INVOKE_SOURCE,
                    Constants.InvokeSource.ONBOARDING_DIALOG
                )
            )
        )
    }

    @Test
    fun addingSpanishDuringOnboardingDisplaysItOnLanguagesScreen() {
        val languageState = WikipediaApp.instance.languageState
        val spanishLanguageName = checkNotNull(
            languageState.getAppLanguageLocalizedName(SPANISH_LANGUAGE_CODE)
        )
        val spanishCanonicalName = checkNotNull(
            languageState.getAppLanguageCanonicalName(SPANISH_LANGUAGE_CODE)
        )

        onboardingRobot
            .tapForward()
            .tapForward()
            .assertLanguagesScreenIsDisplayed()
            .assertLanguageIsNotDisplayed(spanishLanguageName)

        onboardingRobot
            .tapAddOrEditLanguages()

        wikipediaLanguagesRobot
            .tapAddLanguage()

        addLanguagesRobot
            .searchForLanguage(spanishCanonicalName)
            .selectLanguage(spanishCanonicalName)

        wikipediaLanguagesRobot
            .navigateBack()

        onboardingRobot
            .assertLanguageIsDisplayed(spanishLanguageName)
    }

    private companion object {
        const val ENGLISH_LANGUAGE_CODE = "en"
        const val SPANISH_LANGUAGE_CODE = "es"
    }
}
