package org.wikipedia.robots.feature

import android.content.Context
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import org.junit.Assert.assertTrue
import org.wikipedia.base.livedata.ComposeRobot
import java.util.regex.Pattern

/**
 * Drives the article page ([org.wikipedia.page.PageActivity]). Unlike the feed/search screens the
 * article is rendered in a **WebView**, not Compose, so assertions go through UiAutomator. This is
 * the surface that proves the cross-Activity journey end-to-end: we don't stop at "navigation
 * fired", we confirm the real article content rendered.
 */
class ArticleRobot(
    composeTestRule: ComposeTestRule,
    device: UiDevice,
    context: Context
) : ComposeRobot(composeTestRule, device, context) {

    /**
     * Asserts the article actually rendered: first that a WebView is present on the page, then that
     * [titleFragment] appears in its content. A bare navigation check would pass even if the page
     * came up blank; this fails if it did. (Reached after navigation, so the search screen is gone
     * and the text search can't false-match a lingering search result.)
     */
    fun assertArticleRendered(titleFragment: String) = apply {
        // ObservableWebView extends WebView, and By.clazz matches the concrete class name, so match
        // any *WebView subclass by pattern rather than the exact framework class.
        val webView = device.wait(Until.findObject(By.clazz(WEB_VIEW_CLASS)), ARTICLE_RENDER_TIMEOUT_MS)
        assertTrue("Article page WebView never appeared", webView != null)
        val rendered = device.wait(Until.hasObject(By.textContains(titleFragment)), ARTICLE_RENDER_TIMEOUT_MS)
        assertTrue(
            "Navigated to the article, but '$titleFragment' never rendered in the page — " +
                "the page likely came up blank or failed to load.",
            rendered
        )
    }

    companion object {
        private val WEB_VIEW_CLASS: Pattern = Pattern.compile(".*WebView")
        // Article HTML loads over the live network after navigation, so allow a generous ceiling.
        private const val ARTICLE_RENDER_TIMEOUT_MS = 30_000L
    }
}
