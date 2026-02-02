package org.wikipedia.csrf

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.wikipedia.test.MockRetrofitTest

class CsrfTokenClientTest : MockRetrofitTest() {

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
}
