package org.wikipedia.page

import android.app.Activity
import android.content.Context
import android.net.Uri
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.places.PlacesActivity
import org.wikipedia.util.FeedbackUtil
import org.wikipedia.util.StringUtil

@RunWith(RobolectricTestRunner::class)
class LinkHandlerTest {
    private lateinit var handler: TestLinkHandler
    private val context = mockk<Activity>(relaxed = true)
    private val wikiSite = WikiSite.forLanguageCode("en")

    class TestLinkHandler(context: Context, override var wikiSite: WikiSite) : LinkHandler(context) {
        var internalLinkTitle: PageTitle? = null
        var mediaLinkTitle: PageTitle? = null
        var pageLinkAnchor: String? = null
        var diffLinkTitle: PageTitle? = null
        var diffLinkRev: Long = -1
        var externalLinkUri: Uri? = null

        override fun onInternalLinkClicked(title: PageTitle) {
            internalLinkTitle = title
        }

        override fun onMediaLinkClicked(title: PageTitle) {
            mediaLinkTitle = title
        }

        override fun onPageLinkClicked(anchor: String, linkText: String) {
            pageLinkAnchor = anchor
        }

        override fun onDiffLinkClicked(title: PageTitle, revisionId: Long) {
            diffLinkTitle = title
            diffLinkRev = revisionId
        }

        override fun onExternalLinkClicked(uri: Uri) {
            super.onExternalLinkClicked(uri)
            externalLinkUri = uri
        }
    }

    @Before
    fun setUp() {
        handler = TestLinkHandler(context, wikiSite)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun testOnMessage() {
        val payload = buildJsonObject {
            put("href", "/wiki/Test")
            put("title", "Title")
            put("text", "Text")
        }
        handler.onMessage("link", payload)
        assertEquals("Title", handler.internalLinkTitle?.prefixedText)
    }

    @Test
    fun testInternalLink() {
        handler.onUrlClick("/wiki/India", null, "")
        assertEquals("India", handler.internalLinkTitle?.prefixedText)
    }

    @Test
    fun testInternalLinkWithTitle() {
        handler.onUrlClick("/wiki/India", "The Republic of India", "")
        // The LinkHandler logic actually overrides the titleString with one derived from the URL
        // if they differ, so we expect "India".
        assertEquals("India", handler.internalLinkTitle?.prefixedText)
    }

    @Test
    fun testMediaLink() {
        handler.onUrlClick("/wiki/File:Test.jpg", null, "")
        assertEquals("File:Test.jpg", handler.mediaLinkTitle?.prefixedText)
    }

    @Test
    fun testPageLink() {
        handler.onUrlClick("#History", null, "History")
        assertEquals("History", handler.pageLinkAnchor)
    }

    @Test
    fun testCiteLink() {
        handler.onUrlClick("/wiki/India#cite_note-1", null, "[1]")
        assertEquals("cite_note-1", handler.pageLinkAnchor)
    }

    @Test
    fun testDiffLink() {
        handler.onUrlClick("https://en.wikipedia.org/w/index.php?title=India&diff=prev&oldid=123", null, "")
        assertEquals("India", handler.diffLinkTitle?.prefixedText)
        assertEquals(123L, handler.diffLinkRev)
    }

    @Test
    fun testExternalLink() {
        handler.onUrlClick("https://foo.bar?param=https%3A%2F%2Fsub.foo.bar", null, "")
        assertEquals("https://foo.bar?param=https%3A%2F%2Fsub.foo.bar", handler.externalLinkUri?.toString())
    }

    @Test
    fun testMailtoLink() {
        mockkObject(FeedbackUtil)
        every { FeedbackUtil.composeEmail(any(), any(), any(), any()) } returns Unit
        handler.onUrlClick("mailto:test@example.com", null, "")
        verify { FeedbackUtil.composeEmail(context, emailAddress = "test@example.com") }
    }

    @Test
    fun testRelativeLink() {
        handler.onUrlClick("./Test", null, "")
        assertEquals("Test", handler.internalLinkTitle?.prefixedText)
    }

    @Test
    fun testProtocolRelativeLink() {
        handler.onUrlClick("//es.wikipedia.org/wiki/India", null, "")
        assertEquals("India", handler.internalLinkTitle?.prefixedText)
        assertEquals("es", handler.internalLinkTitle?.wikiSite?.languageCode)
    }

    @Test
    fun testGeoHackLink() {
        mockkStatic(StringUtil::class)
        mockkStatic(PlacesActivity::class)
        val location = mockk<android.location.Location>()
        every { StringUtil.geoHackToLocation(any()) } returns location
        every { PlacesActivity.newIntent(any(), any(), any()) } returns mockk(relaxed = true)

        handler.onUrlClick("https://geohack.toolforge.org/geohack.php?params=1_2_N_3_4_E", null, "")

        verify { context.startActivity(any()) }
    }

    @Test
    fun testEventLoggingParam() {
        handler.onUrlClick("/wiki/India?event-logging-label=footer", null, "")
        assertEquals("India", handler.internalLinkTitle?.prefixedText)
    }

    @Test
    fun testUnknownScheme() {
        // For a link like "foo:bar", it's not in KNOWN_SCHEMES ("http", "https", "geo", "file", "content")
        // The handler will try to build a URI using the wikiSite scheme and authority.
        handler.onUrlClick("foo:bar", null, "")
        assertEquals("https://en.wikipedia.org/foo:bar", handler.externalLinkUri?.toString())
    }

    @Test
    fun testSpecialPage() {
        handler.onUrlClick("/wiki/Special:RecentChanges", null, "")
        assertEquals("https://en.wikipedia.org/wiki/Special:RecentChanges", handler.externalLinkUri?.toString())
    }
}
