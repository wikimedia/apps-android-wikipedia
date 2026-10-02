package org.wikipedia.auth

import io.mockk.every
import io.mockk.just
import io.mockk.mockkObject
import io.mockk.runs
import io.mockk.unmockkAll
import kotlinx.coroutines.runBlocking
import net.openid.appauth.AuthState
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.wikipedia.WikipediaApp
import org.wikipedia.dataclient.okhttp.HttpStatusException
import org.wikipedia.dataclient.okhttp.TestStubInterceptor
import org.wikipedia.notifications.PollNotificationWorker
import org.wikipedia.test.TestOAuthUtil

@RunWith(RobolectricTestRunner::class)
class OAuthLoginTest {
    private val server = MockWebServer()
    private val apiRequests = mutableListOf<Request>()

    @Before
    fun setUp() {
        server.start()
        // WorkManager isn't available in unit tests.
        mockkObject(PollNotificationWorker.Companion)
        every { PollNotificationWorker.schedulePollNotificationJob(any()) } just runs
    }

    @After
    fun tearDown() {
        unmockkAll()
        TestStubInterceptor.CALLBACK = null
        server.shutdown()
    }

    @Test
    fun testLoginExchangesCodeAndCreatesAccount() {
        server.enqueue(TestOAuthUtil.tokenResponse("access1", "refresh1"))
        stubApiResponses(200, """{"sub":123,"username":"Example","groups":["*","user"]}""")
        val oauthClient = WikipediaApp.instance.oauthClient

        runBlocking { oauthClient.handleAuthorizationResponse(TestOAuthUtil.authorizationResponse(server)) }

        val body = server.takeRequest().body.readUtf8()
        assertTrue(body.contains("grant_type=authorization_code"))
        assertTrue(body.contains("code=code"))
        assertTrue(body.contains("code_verifier="))
        assertTrue(body.contains("client_id=${OAuthClient.CLIENT_ID}"))
        // The profile is requested with the new access token, before the account exists to store it.
        assertEquals(listOf("Bearer access1"), apiRequests.map { it.header("Authorization") })

        assertTrue(oauthClient.isLoggedIn)
        assertEquals("Example", AccountUtil.account()?.name)
        assertEquals(setOf("*", "user"), AccountUtil.groups)
        assertEquals("refresh1", AuthState.jsonDeserialize(AccountUtil.oauthState!!).refreshToken)
    }

    @Test
    fun testFailedLoginLeavesNoTokensBehind() {
        server.enqueue(TestOAuthUtil.tokenResponse("access1", "refresh1"))
        stubApiResponses(503, "{}")
        val oauthClient = WikipediaApp.instance.oauthClient

        assertThrows(HttpStatusException::class.java) {
            runBlocking { oauthClient.handleAuthorizationResponse(TestOAuthUtil.authorizationResponse(server)) }
        }

        assertFalse(oauthClient.isLoggedIn)
        assertNull(AccountUtil.account())
    }

    @Test
    fun testRejectedAuthorizationCodeFailsLogin() {
        server.enqueue(TestOAuthUtil.invalidGrantResponse())
        val oauthClient = WikipediaApp.instance.oauthClient

        assertThrows(Exception::class.java) {
            runBlocking { oauthClient.handleAuthorizationResponse(TestOAuthUtil.authorizationResponse(server)) }
        }

        assertFalse(oauthClient.isLoggedIn)
        assertNull(AccountUtil.account())
    }

    // Stubs the responses to API requests (as opposed to the token requests, which go to the server).
    private fun stubApiResponses(code: Int, body: String) {
        TestStubInterceptor.CALLBACK = object : TestStubInterceptor.Callback {
            override fun getResponse(request: Interceptor.Chain): Response {
                apiRequests.add(request.request())
                return Response.Builder()
                    .request(request.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(code)
                    .message("")
                    .body(body.toResponseBody("application/json".toMediaType()))
                    .build()
            }
        }
    }
}
