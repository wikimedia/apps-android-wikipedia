package org.wikipedia.robots.feature

import android.content.Context
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.test.uiautomator.UiDevice
import org.wikipedia.base.livedata.ComposeRobot
import org.wikipedia.search.SEARCH_LIST_TAG

/**
 * Drives the (Compose) search results screen against live data, for the live-data E2E framework
 * (distinct from the legacy Espresso [SearchRobot]). Anchors on the always-present results list tag;
 * the first result is `${SEARCH_LIST_TAG}0`.
 */
class SearchResultsRobot(
    composeTestRule: ComposeTestRule,
    device: UiDevice,
    context: Context
) : ComposeRobot(composeTestRule, device, context) {

    fun waitForResults() = apply {
        awaitTag(SEARCH_LIST_TAG)
    }

    fun openFirstResult() = apply {
        clickTag("${SEARCH_LIST_TAG}0")
    }
}
