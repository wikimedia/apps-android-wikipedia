package org.wikipedia.onboarding

import androidx.compose.ui.graphics.Color
import androidx.core.content.ContextCompat
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.espresso.intent.matcher.IntentMatchers.hasExtra
import org.hamcrest.Matchers.allOf
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.wikipedia.Constants
import org.wikipedia.WikipediaApp
import org.wikipedia.base.BaseTest
import org.wikipedia.base.TestThemeColorType
import org.wikipedia.base.TestWikipediaColors
import org.wikipedia.feed.personalization.PersonalizationActivity
import org.wikipedia.robots.AddLanguagesRobot
import org.wikipedia.robots.OnboardingRobot
import org.wikipedia.robots.WikipediaLanguagesRobot
import org.wikipedia.settings.Prefs
import org.wikipedia.settings.languages.WikipediaLanguagesActivity
import org.wikipedia.theme.Theme

class InitialOnboardingActivityTest : BaseTest<InitialOnboardingActivity>(
    activityClass = InitialOnboardingActivity::class.java,
    beforeActivityLaunch = {
        Prefs.isInitialOnboardingEnabled = true
        Prefs.isExploreFeedUpdatePromptShown = false
        WikipediaApp.instance.languageState.setAppLanguageCodes(listOf(ENGLISH_LANGUAGE_CODE))
        WikipediaApp.instance.currentTheme = Theme.LIGHT
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
    fun completingOnboardingUpdatesPreferencesAndLaunchesPersonalization() {
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
    fun recreatingActivityRetainsOnboardingProgress() {
        val lightThemePrimaryColor = Color(
            ContextCompat.getColor(
                targetContext,
                TestWikipediaColors.getGetColor(Theme.LIGHT, TestThemeColorType.PRIMARY)
            )
        )

        onboardingRobot
            .tapForward()
            .assertDataPrivacyScreenIsDisplayed()
            .assertDataPrivacyTitleColor(lightThemePrimaryColor)

        val activityToRecreate = activity

        composeTestRule.runOnUiThread {
            activityToRecreate.recreate()
        }

        onboardingRobot
            .assertDataPrivacyScreenIsDisplayed()
            .assertDataPrivacyTitleColor(lightThemePrimaryColor)
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

    @Test
    fun changingPrimaryLanguageUpdatesHomeLanguageAndCompletesOnboarding() {
        val languageState = WikipediaApp.instance.languageState
        val spanishLanguageName = checkNotNull(
            languageState.getAppLanguageLocalizedName(SPANISH_LANGUAGE_CODE)
        )
        val updatedLanguageCodes = listOf(
            SPANISH_LANGUAGE_CODE,
            ENGLISH_LANGUAGE_CODE
        )

        onboardingRobot
            .tapForward()
            .tapForward()
            .assertLanguagesScreenIsDisplayed()
            .tapAddOrEditLanguages()
        Thread.sleep(2000)
        composeTestRule.runOnUiThread {
            languageState.setAppLanguageCodes(updatedLanguageCodes)
        }
        wikipediaLanguagesRobot.navigateBack()

        onboardingRobot
            .assertLanguagesScreenIsDisplayed()
            .assertLanguageIsDisplayed(spanishLanguageName)
        Thread.sleep(2000)
        assertEquals(updatedLanguageCodes, languageState.appLanguageCodes)
        assertEquals(SPANISH_LANGUAGE_CODE, Prefs.homeLanguageCode)

        onboardingRobot.tapForward()

        intended(hasComponent(PersonalizationActivity::class.java.name))
        assertFalse(Prefs.isInitialOnboardingEnabled)
        assertTrue(Prefs.isExploreFeedUpdatePromptShown)
    }

    private companion object {
        const val ENGLISH_LANGUAGE_CODE = "en"
        const val SPANISH_LANGUAGE_CODE = "es"
    }
}
