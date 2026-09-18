package org.wikipedia.page

import android.net.Uri
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.wikipedia.WikipediaApp
import org.wikipedia.dataclient.WikiSite

@RunWith(RobolectricTestRunner::class)
class LinkHandlerTest {
    private val context get() = WikipediaApp.instance

    @Test
    fun testOnMessageWithEncodedUrl() {
        var lastExternalUri: Uri? = null
        val handler = object : LinkHandler(context) {
            override var wikiSite: WikiSite = WikiSite("en.wikipedia.org")
            override fun onExternalLinkClicked(uri: Uri) {
                lastExternalUri = uri
            }
        }

        // A URL with an encoded space (%20)
        val payload = buildJsonObject {
            put("href", "https://example.com/foo%20bar")
        }

        handler.onMessage("test", payload)

        // The URI passed to onExternalLinkClicked should preserve the encoded space
        assertEquals("https://example.com/foo%20bar", lastExternalUri.toString())
    }

    @Test
    fun testOnMessageWithInternalLink() {
        var lastInternalTitle: PageTitle? = null
        val handler = object : LinkHandler(context) {
            override var wikiSite: WikiSite = WikiSite("en.wikipedia.org")
            override fun onInternalLinkClicked(title: PageTitle) {
                lastInternalTitle = title
            }
        }

        // An internal link with an encoded space
        val payload = buildJsonObject {
            put("href", "/wiki/Foo%20Bar")
        }

        handler.onMessage("test", payload)

        // PageTitle should correctly interpret "Foo%20Bar" as "Foo Bar" for display
        // because PageTitle handles its own decoding.
        assertEquals("Foo Bar", lastInternalTitle?.displayText)
        // And use underscore for the DB/Internal title
        assertEquals("Foo_Bar", lastInternalTitle?.prefixedText)
    }

    @Test
    fun testOnMessageWithQueryParameters() {
        var lastExternalUri: Uri? = null
        val handler = object : LinkHandler(context) {
            override var wikiSite: WikiSite = WikiSite("en.wikipedia.org")
            override fun onExternalLinkClicked(uri: Uri) {
                lastExternalUri = uri
            }
        }

        val payload = buildJsonObject {
            put("href", "https://example.com/search?q=foo%20bar")
        }

        handler.onMessage("test", payload)

        // The full URL should be encoded
        assertEquals("https://example.com/search?q=foo%20bar", lastExternalUri.toString())
        // But extracting a query parameter should yield the decoded value automatically
        assertEquals("foo bar", lastExternalUri?.getQueryParameter("q"))
    }

    @Test
    fun testOnMessageWithMailto() {
        val handler = object : LinkHandler(context) {
            override var wikiSite: WikiSite = WikiSite("en.wikipedia.org")
        }

        val payload = buildJsonObject {
            put("href", "mailto:test@example.com")
        }

        // Verify that special schemes like mailto: are still processed without error
        handler.onMessage("test", payload)
    }

    @Test
    fun testOnMessageWithTitleAndEncodedUrl() {
        var lastInternalTitle: PageTitle? = null
        val handler = object : LinkHandler(context) {
            override var wikiSite: WikiSite = WikiSite("en.wikipedia.org")
            override fun onInternalLinkClicked(title: PageTitle) {
                lastInternalTitle = title
            }
        }

        // JS payload where title is also encoded
        val payload = buildJsonObject {
            put("href", "/wiki/Foo%20Bar")
            put("title", "Foo%20Bar")
        }

        handler.onMessage("test", payload)

        // PageTitle should correctly interpret "Foo%20Bar" as "Foo Bar"
        assertEquals("Foo Bar", lastInternalTitle?.displayText)
    }
}
