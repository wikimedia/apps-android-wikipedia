package org.wikipedia.tests.widgets

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.wikipedia.base.livedata.LiveDataComposeTest
import org.wikipedia.feed.personalization.homepreference.HomePreferenceType
import org.wikipedia.main.MainActivity
import org.wikipedia.robots.feature.SearchWidgetInstallRobot
import org.wikipedia.settings.Prefs

/**
 * Drives the Search-widget install bottom sheet added in #6674. The dialog itself talks to no
 * network — it is local UI gated by [Prefs.searchWidgetInstallPromptShown] — but it is reached
 * through a real [MainActivity] tab switch, so the [LiveDataComposeTest] scaffolding (empty Compose
 * rule + ActivityScenario, animation disabling, dialog suppression) is the cleanest harness for
 * asserting it across the View bottom-nav → ComposeView boundary.
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class SearchWidgetInstallLiveDataTest : LiveDataComposeTest<MainActivity>(MainActivity::class.java) {

    private val robot by lazy { SearchWidgetInstallRobot(composeTestRule, device, context) }

    override fun prepareDeviceState() {
        // Re-arm the one-time prompt so it is eligible to show on the first Search-tab open,
        // regardless of what a prior run left behind.
        Prefs.searchWidgetInstallPromptShown = false
        // Start on a deterministic home tab so waitForHomeLoaded() has a stable anchor to confirm
        // MainActivity is up before the bottom-nav tap.
        Prefs.homePreferenceSelection = HomePreferenceType.COMMUNITY
    }

    @Test
    fun openingSearchTab_showsInstallPrompt() {
        robot
            .waitForHomeLoaded()
            .openSearchTab()
            .waitForInstallPrompt()

        assertTrue(
            "Showing the install prompt must mark it as shown so it never reappears",
            Prefs.searchWidgetInstallPromptShown
        )
    }

    @Test
    fun tappingPrimaryButton_dismissesPrompt() {
        robot
            .waitForHomeLoaded()
            .openSearchTab()
            .waitForInstallPrompt()
            .dismissViaPrimaryButton()
            .assertInstallPromptGone()
    }

    @Test
    fun tappingClose_dismissesPrompt() {
        robot
            .waitForHomeLoaded()
            .openSearchTab()
            .waitForInstallPrompt()
            .dismissViaCloseButton()
            .assertInstallPromptGone()
    }

    @Test
    fun promptShowsOnlyOnce_notOnSecondSearchVisit() {
        // Dismiss via Close, not the "Add" button: tapping "Add" fires the OS pin-widget dialog,
        // which covers the app and would break the subsequent in-app tab navigation this test needs.
        robot
            .waitForHomeLoaded()
            .openSearchTab()
            .waitForInstallPrompt()
            .dismissViaCloseButton()
            .openHomeTab()
            .openSearchTab()
            .assertInstallPromptGone()

        assertTrue(
            "The one-time prompt must stay marked as shown after it has been shown once",
            Prefs.searchWidgetInstallPromptShown
        )
    }
}
