package org.wikipedia.base.livedata

import android.content.Context
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import org.junit.Assert.assertTrue

/**
 * Base class for feature "robots" that drive a Compose screen backed by **live data**.
 *
 * It provides the reusable, hard-to-get-right synchronization vocabulary so feature robots only
 * describe *what* to do, not *how* to wait. The golden rule: never gate on `waitForIdle()` — a
 * screen that is loading from the network (or animating) may never become idle. Always poll a
 * concrete semantic anchor with [ComposeTestRule.waitUntil], which waits against real elapsed
 * time and is therefore correct under both the v1 and v2 Compose testing APIs.
 *
 * Feature robots extend this, keep their public methods fluent (`= apply { ... }` so calls chain),
 * and build them out of the `protected` helpers below.
 *
 * Example:
 * ```
 * class SearchRobot(rule, device, context) : ComposeRobot(rule, device, context) {
 *     fun waitForResults() = apply { awaitTag(SearchTestTags.RESULTS_LIST) }
 *     fun openFirstResult() = apply { clickTag(SearchTestTags.result(0)) }
 * }
 * ```
 */
abstract class ComposeRobot(
    protected val composeTestRule: ComposeTestRule,
    protected val device: UiDevice,
    protected val context: Context
) {

    /** Waits until at least one node with [tag] is present (e.g. content finished loading). */
    protected fun awaitTag(tag: String, timeoutMillis: Long = DEFAULT_TIMEOUT_MS) {
        composeTestRule.waitUntil(timeoutMillis) {
            composeTestRule.onAllNodesWithTag(tag, useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
    }

    /** Waits until no node with [tag] remains (e.g. an item was dismissed/hidden). */
    protected fun awaitTagGone(tag: String, timeoutMillis: Long = DEFAULT_TIMEOUT_MS) {
        composeTestRule.waitUntil(timeoutMillis) {
            composeTestRule.onAllNodesWithTag(tag, useUnmergedTree = true)
                .fetchSemanticsNodes().isEmpty()
        }
    }

    /** Waits until a node displaying [text] is present. */
    protected fun awaitText(text: String, timeoutMillis: Long = DEFAULT_TIMEOUT_MS) {
        composeTestRule.waitUntil(timeoutMillis) {
            composeTestRule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
    }

    protected fun clickTag(tag: String) {
        composeTestRule.onNodeWithTag(tag).performClick()
    }

    protected fun clickText(text: String) {
        composeTestRule.onNodeWithText(text).performClick()
    }

    /** Scrolls the lazy list tagged [listTag] until the node matching [targetTag] is composed, then clicks it. */
    protected fun scrollToAndClick(listTag: String, targetTag: String) {
        scrollTo(listTag, targetTag)
        clickTag(targetTag)
    }

    protected fun scrollTo(listTag: String, targetTag: String) {
        composeTestRule.onNodeWithTag(listTag).performScrollToNode(hasTestTag(targetTag))
    }

    /**
     * Scrolls [listTag] until [targetTag] composes, asserting it is reachable. On failure rethrows
     * with [failureReason] so the report explains *why* the absence is a bug (e.g. a backend
     * contract said this should render) rather than a bare "no node found".
     */
    protected fun assertReachableByScroll(listTag: String, targetTag: String, failureReason: String) {
        try {
            scrollTo(listTag, targetTag)
        } catch (e: AssertionError) {
            throw AssertionError(failureReason, e)
        }
    }

    /** Asserts the (lazy) container tagged [tag] has rendered at least one child item. */
    protected fun assertHasChildren(tag: String) {
        val childCount = composeTestRule.onNodeWithTag(tag, useUnmergedTree = true)
            .fetchSemanticsNode().children.size
        assertTrue("Expected '$tag' to render at least one item", childCount > 0)
    }

    /**
     * Waits for a Material Snackbar to appear. Snackbars are Android Views (not Compose), and
     * confirmations often follow a network round-trip, so this polls with UiAutomator. Returns
     * true if shown within [timeoutMillis].
     */
    protected fun awaitSnackbar(timeoutMillis: Long = SNACKBAR_TIMEOUT_MS): Boolean {
        return device.wait(
            Until.hasObject(By.res(context.packageName, SNACKBAR_TEXT_ID)),
            timeoutMillis
        ) != null
    }

    protected fun assertSnackbarShown(message: String = "Expected a snackbar to appear") {
        assertTrue(message, awaitSnackbar())
    }

    companion object {
        // Generous upper bound: live network on CI emulators can be slow. We poll, so a higher
        // ceiling never slows down the happy path — it only bounds genuine failures.
        const val DEFAULT_TIMEOUT_MS = 20_000L
        const val SNACKBAR_TIMEOUT_MS = 10_000L
        private const val SNACKBAR_TEXT_ID = "snackbar_text"
    }
}
