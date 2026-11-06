package org.wikipedia.dataclient.okhttp

import io.mockk.every
import io.mockk.mockk
import okhttp3.Interceptor
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.wikipedia.WikipediaApp
import org.wikipedia.test.TestOAuthUtil
import org.wikipedia.test.TestOAuthUtil.HOUR_MILLIS

@RunWith(RobolectricTestRunner::class)
class OAuthInterceptorTest {
    private val server = MockWebServer()
    private val apiRequest = Request.Builder().url("https://en.wikipedia.org/w/api.php?action=query&meta=userinfo").build()

    @Before
    fun setUp() {
        server.start()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun testShouldAuthorizeWikimediaApis() {
        assertTrue(shouldAuthorize("https://en.wikipedia.org/w/api.php?action=query"))
        assertTrue(shouldAuthorize("https://commons.wikimedia.org/w/api.php?action=edit"))
        assertTrue(shouldAuthorize("https://www.wikidata.org/w/api.php?action=wbsetdescription"))
        assertTrue(shouldAuthorize("https://en.wikipedia.org/w/rest.php/oauth2/resource/profile"))
        assertTrue(shouldAuthorize("https://en.wikipedia.org/api/rest_v1/data/lists/"))
    }

    @Test
    fun testShouldNotAuthorizeOtherRequests() {
        assertFalse(shouldAuthorize("http://en.wikipedia.org/w/api.php?action=query"))
        assertFalse(shouldAuthorize("https://upload.wikimedia.org/wikipedia/commons/a/a9/Example.jpg"))
        assertFalse(shouldAuthorize("https://intake-analytics.wikimedia.org/v1/events?hasty=true"))
        assertFalse(shouldAuthorize("https://maps.wikimedia.org/osm-intl/1/0/0.png"))
        assertFalse(shouldAuthorize("https://github.com/wikimedia/apps-android-wikipedia/releases/download/latest/rev-hash.txt"))
        assertFalse(shouldAuthorize("https://en.wikipedia.org.example.com/w/api.php"))
        assertFalse(OAuthInterceptor.shouldAuthorize(apiRequest.newBuilder().header("Authorization", "Bearer other").build()))
    }

    @Test
    fun testFreshTokenIsAttached() {
        TestOAuthUtil.persistAuthState(server, "access1", "refresh1", expiresInMillis = HOUR_MILLIS)

        val sentRequests = interceptWithResponseCodes(200)

        assertEquals(listOf("Bearer access1"), sentRequests.map { it.header("Authorization") })
    }

    @Test
    fun testNoTokenWhenNotLoggedIn() {
        val sentRequests = interceptWithResponseCodes(200)

        assertEquals(listOf(null), sentRequests.map { it.header("Authorization") })
    }

    @Test
    fun testRejectedTokenIsRefreshedAndRetried() {
        TestOAuthUtil.persistAuthState(server, "access1", "refresh1", expiresInMillis = HOUR_MILLIS)
        server.enqueue(TestOAuthUtil.tokenResponse("access2", "refresh2"))

        val sentRequests = interceptWithResponseCodes(401, 200)

        assertEquals(listOf("Bearer access1", "Bearer access2"), sentRequests.map { it.header("Authorization") })
    }

    @Test
    fun testRejectedTokenThatCannotBeRefreshedIsNotRetried() {
        TestOAuthUtil.persistAuthState(server, "access1", "refresh1", expiresInMillis = HOUR_MILLIS)
        server.enqueue(TestOAuthUtil.invalidGrantResponse())

        val sentRequests = interceptWithResponseCodes(401)

        assertEquals(1, sentRequests.size)
        assertFalse(WikipediaApp.instance.oauthClient.isLoggedIn)
    }

    private fun shouldAuthorize(url: String): Boolean {
        return OAuthInterceptor.shouldAuthorize(Request.Builder().url(url).build())
    }

    // Runs apiRequest through the interceptor, with the server responding to each attempt with the
    // next of the given codes, and returns the requests that were sent.
    private fun interceptWithResponseCodes(vararg codes: Int): List<Request> {
        val sentRequests = mutableListOf<Request>()
        val chain = mockk<Interceptor.Chain> {
            every { request() } returns apiRequest
            every { proceed(capture(sentRequests)) } answers {
                Response.Builder()
                    .request(sentRequests.last())
                    .protocol(Protocol.HTTP_1_1)
                    .code(codes[sentRequests.size - 1])
                    .message("")
                    .body("{}".toResponseBody())
                    .build()
            }
        }
        val response = OAuthInterceptor().intercept(chain)
        assertEquals(codes.last(), response.code)
        return sentRequests
    }
}
