package org.wikipedia.auth

import net.openid.appauth.AuthState
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.wikipedia.WikipediaApp
import org.wikipedia.settings.Prefs
import org.wikipedia.test.TestOAuthUtil
import org.wikipedia.test.TestOAuthUtil.HOUR_MILLIS
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
class OAuthClientTest {
    private val server = MockWebServer()

    @Before
    fun setUp() {
        server.start()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun testUnexpiredTokenIsNotRefreshed() {
        val client = createClient(expiresInMillis = HOUR_MILLIS)
        assertEquals("access1", client.getFreshAccessToken())
        assertEquals(0, server.requestCount)
    }

    @Test
    fun testNotLoggedIn() {
        val client = OAuthClient(WikipediaApp.instance)
        assertFalse(client.isLoggedIn)
        assertNull(client.getFreshAccessToken())
    }

    @Test
    fun testTokenAboutToExpireIsRefreshed() {
        server.enqueue(TestOAuthUtil.tokenResponse("access2", "refresh2"))
        val client = createClient(expiresInMillis = 30_000)

        assertEquals("access2", client.getFreshAccessToken())

        val body = server.takeRequest().body.readUtf8()
        assertTrue(body.contains("grant_type=refresh_token"))
        assertTrue(body.contains("refresh_token=refresh1"))
        assertTrue(body.contains("client_id=${OAuthClient.CLIENT_ID}"))

        // The new refresh token must be persisted, since the server will no longer accept the old one.
        val persistedState = AuthState.jsonDeserialize(Prefs.oauthState)
        assertEquals("access2", persistedState.accessToken)
        assertEquals("refresh2", persistedState.refreshToken)
    }

    @Test
    fun testConcurrentCallersShareOneRefresh() {
        server.enqueue(TestOAuthUtil.tokenResponse("access2", "refresh2").setHeadersDelay(500, TimeUnit.MILLISECONDS))
        // Any second refresh would reuse the old refresh token, which the server would reject.
        server.enqueue(TestOAuthUtil.invalidGrantResponse())
        val client = createClient(expiresInMillis = -HOUR_MILLIS)

        val executor = Executors.newFixedThreadPool(5)
        val accessTokens = List(5) { executor.submit<String?> { client.getFreshAccessToken() } }
            .map { it.get(10, TimeUnit.SECONDS) }
        executor.shutdown()

        assertEquals(List(5) { "access2" }, accessTokens)
        assertEquals(1, server.requestCount)
        assertTrue(client.isLoggedIn)
    }

    @Test
    fun testRejectedTokenIsRefreshedBeforeExpiry() {
        server.enqueue(TestOAuthUtil.tokenResponse("access2", "refresh2"))
        val client = createClient(expiresInMillis = HOUR_MILLIS)

        assertEquals("access2", client.getFreshAccessToken(rejectedAccessToken = "access1"))
        assertEquals(1, server.requestCount)
    }

    @Test
    fun testRejectedTokenThatWasAlreadyReplacedIsNotRefreshedAgain() {
        val client = createClient(expiresInMillis = HOUR_MILLIS)

        assertEquals("access1", client.getFreshAccessToken(rejectedAccessToken = "access0"))
        assertEquals(0, server.requestCount)
    }

    @Test
    fun testInvalidGrantLogsOut() {
        server.enqueue(TestOAuthUtil.invalidGrantResponse())
        val client = createClient(expiresInMillis = -HOUR_MILLIS)

        assertNull(client.getFreshAccessToken())
        assertFalse(client.isLoggedIn)
        assertNull(AuthState.jsonDeserialize(Prefs.oauthState).refreshToken)
    }

    @Test
    fun testServerErrorKeepsExistingTokens() {
        server.enqueue(MockResponse().setResponseCode(503).setBody("<html>Service Unavailable</html>"))
        val client = createClient(expiresInMillis = -HOUR_MILLIS)

        assertEquals("access1", client.getFreshAccessToken())
        assertTrue(client.isLoggedIn)

        // ...and tries again on the next call.
        server.enqueue(TestOAuthUtil.tokenResponse("access2", "refresh2"))
        assertEquals("access2", client.getFreshAccessToken())
    }

    private fun createClient(expiresInMillis: Long): OAuthClient {
        TestOAuthUtil.persistAuthState(server, "access1", "refresh1", expiresInMillis)
        return OAuthClient(WikipediaApp.instance)
    }
}
