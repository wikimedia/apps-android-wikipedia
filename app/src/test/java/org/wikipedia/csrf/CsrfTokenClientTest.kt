package org.wikipedia.csrf

import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Test
import org.wikipedia.auth.AccountUtil
import org.wikipedia.dataclient.Service
import org.wikipedia.dataclient.mwapi.MwQueryResponse
import org.wikipedia.dataclient.okhttp.HttpStatusException
import org.wikipedia.json.JsonUtil
import org.wikipedia.test.MockRetrofitTest
import org.wikipedia.test.TestOAuthUtil
import java.io.IOException

class CsrfTokenClientTest : MockRetrofitTest() {

    @Test
    fun testAnonymousTokenWhenNotLoggedIn() {
        server().enqueue(ANON_TOKEN_RESPONSE)
        runBlocking {
            assertEquals("+\\", CsrfTokenClient.getToken(wikiSite, "csrf", apiService))
        }
    }

    @Test
    fun testAnonymousTokenForOAuthAccountLogsOut() {
        AccountUtil.updateAccount(TestOAuthUtil.profile("Example"), "{}")
        server().enqueue(ANON_TOKEN_RESPONSE)

        assertThrows(IOException::class.java) {
            runBlocking { CsrfTokenClient.getToken(wikiSite, "csrf", apiService) }
        }
        assertFalse(AccountUtil.isLoggedIn)
    }

    @Test
    fun testNoAnonymousTokenIfLoggedOutDuringRequest() {
        AccountUtil.updateAccount(TestOAuthUtil.profile("Example"), "{}")
        var requestCount = 0
        val service = mockk<Service> {
            coEvery { getToken(any()) } coAnswers {
                if (requestCount++ == 0) {
                    // As OAuthInterceptor does when it finds that the refresh token has expired.
                    AccountUtil.bailWithLogout()
                    throw HttpStatusException(401, "https://en.wikipedia.org/w/api.php", null)
                }
                JsonUtil.decodeFromString<MwQueryResponse>(ANON_TOKEN_RESPONSE)!!
            }
        }

        assertThrows(IOException::class.java) {
            runBlocking { CsrfTokenClient.getToken(wikiSite, "csrf", service) }
        }
        assertEquals(2, requestCount)
    }

    @Test
    fun testRequestSuccess() {
        val expected = "b6f7bd58c013ab30735cb19ecc0aa08258122cba+\\"
        enqueueFromFile("csrf_token.json")
        runBlocking {
            val result = CsrfTokenClient.getToken(wikiSite, "csrf", apiService)
            assert(result == expected)
        }
    }

    @Test
    @Throws(Throwable::class)
    fun testRequestResponseApiError() {
        enqueueFromFile("api_error.json")
        runBlocking {
            try {
                CsrfTokenClient.getToken(wikiSite, "csrf", apiService)
            } catch (e: Exception) {
                assertNotNull(e)
            }
        }
    }

    @Test
    @Throws(Throwable::class)
    fun testRequestResponseFailure() {
        enqueue404()
        runBlocking {
            try {
                CsrfTokenClient.getToken(wikiSite, "csrf", apiService)
            } catch (e: Exception) {
                assertNotNull(e)
            }
        }
    }

    companion object {
        private const val ANON_TOKEN_RESPONSE = """{"batchcomplete":true,"query":{"tokens":{"csrftoken":"+\\"}}}"""
    }
}
