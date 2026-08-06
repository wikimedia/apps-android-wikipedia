package org.wikipedia.robots.feature

import android.content.Context
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import org.wikipedia.R
import org.wikipedia.base.livedata.ComposeRobot
import org.wikipedia.feed.HomeScreenTestTags
import org.wikipedia.widgets.SearchWidgetInstallDialogTestTags

/**
 * Drives the Search-widget install bottom sheet that [org.wikipedia.main.MainFragment] shows the
 * first time the user opens the Search tab (gated by [org.wikipedia.settings.Prefs.searchWidgetInstallPromptShown]
 * and whether the widget is already installed).
 *
 * The bottom-nav tabs are legacy Views, so tab taps go through Espresso; the dialog body is Compose
 * (an [org.wikipedia.compose.components.InstallWidgetScreen] in a [androidx.compose.ui.platform.ComposeView]),
 * so it is asserted via Compose semantics. All synchronization lives in [ComposeRobot].
 */
class SearchWidgetInstallRobot(
    composeTestRule: ComposeTestRule,
    device: UiDevice,
    context: Context
) : ComposeRobot(composeTestRule, device, context) {

    /**
     * Waits for the home feed to finish loading before any bottom-nav interaction. The Compose tag
     * appears only once MainActivity is resumed and the feed list has composed, so this both proves
     * the Activity is up (avoiding Espresso's NoActivityResumedException on a cold launch) and gives
     * a deterministic anchor to act from.
     */
    fun waitForHomeLoaded() = apply {
        awaitTag(HomeScreenTestTags.COMMUNITY_FEED_LIST)
    }

    fun openSearchTab() = apply {
        onView(withId(R.id.nav_tab_search)).perform(click())
    }

    fun openHomeTab() = apply {
        onView(withId(R.id.nav_tab_home)).perform(click())
    }

    fun waitForInstallPrompt() = apply {
        awaitTag(SearchWidgetInstallDialogTestTags.ROOT)
    }

    fun assertInstallPromptGone() = apply {
        awaitPromptGone()
    }

    /**
     * Taps the primary action. On a device where pinning is supported the button reads "Add" and
     * the app fires the OS "Add to home screen" dialog — a system window outside the app that leaves
     * no Activity resumed. We dismiss that system dialog so the app returns to the foreground and
     * neither this test's later steps nor a following test trips Espresso's NoActivityResumedException.
     */
    fun dismissViaPrimaryButton() = apply {
        clickTag(SearchWidgetInstallDialogTestTags.PRIMARY_BUTTON)
        dismissSystemPinWidgetDialogIfPresent()
        awaitPromptGone()
    }

    private fun dismissSystemPinWidgetDialogIfPresent() {
        device.waitForIdle()
        if (device.currentPackageName != context.packageName) {
            device.pressBack()
            device.wait(Until.hasObject(By.pkg(context.packageName).depth(0)), DEFAULT_TIMEOUT_MS)
        }
    }

    fun dismissViaCloseButton() = apply {
        composeTestRule
            .onNodeWithContentDescription(context.getString(R.string.dialog_close_description))
            .performClick()
        awaitPromptGone()
    }

    /**
     * Waits until the bottom sheet's [SearchWidgetInstallDialogTestTags.ROOT] is gone. This cannot
     * use the base [awaitTagGone]: once the sheet is dismissed the screen underneath is the Search
     * tab ([org.wikipedia.history.HistoryFragment]), a legacy View hierarchy with **no Compose at
     * all**, so `fetchSemanticsNodes()` throws "No compose hierarchies found" instead of returning
     * empty. An absent Compose tree means the sheet is unambiguously gone, so we treat that as
     * success.
     */
    private fun awaitPromptGone() {
        composeTestRule.waitUntil(DEFAULT_TIMEOUT_MS) {
            try {
                composeTestRule
                    .onAllNodesWithTag(SearchWidgetInstallDialogTestTags.ROOT, useUnmergedTree = true)
                    .fetchSemanticsNodes().isEmpty()
            } catch (e: IllegalStateException) {
                true
            }
        }
    }
}
