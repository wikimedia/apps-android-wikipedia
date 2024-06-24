package org.wikipedia.csrf

import kotlinx.coroutines.runBlocking
import org.hamcrest.MatcherAssert
import org.hamcrest.Matchers
import org.junit.Test
import org.wikipedia.test.MockRetrofitTest

class CsrfTokenClientTest : MockRetrofitTest() {
    @Test
    @Throws(Throwable::class)
    fun testRequestSuccess() {
        val expected = "b6f7bd58c013ab30735cb19ecc0aa08258122cba+\\"
        enqueueFromFile("csrf_token.json")
        CsrfTokenClient.getToken(wikiSite, "csrf", apiService).test().await()
            .assertComplete().assertNoErrors()
            .assertValue { result -> result == expected }
    }

    @Test
    fun testRequestSuccessCoroutine() {
        val expected = "b6f7bd58c013ab30735cb19ecc0aa08258122cba+\\"
        enqueueFromFile("csrf_token.json")
        runBlocking {
            val result = CsrfTokenClient.getTokenBlocking(wikiSite, "csrf", apiService)
            assert(result == expected)
        }
    }

    @Test
    @Throws(Throwable::class)
    fun testRequestResponseApiError() {
        enqueueFromFile("api_error.json")
        CsrfTokenClient.getToken(wikiSite, "csrf", apiService).test().await()
            .assertError(Exception::class.java)
    }

    @Test
    @Throws(Throwable::class)
    fun testRequestResponseApiErrorCoroutine() {
        enqueueFromFile("api_error.json")
        runBlocking {
            try {
                CsrfTokenClient.getTokenBlocking(wikiSite, "csrf", apiService)
            } catch (e: Exception) {
                MatcherAssert.assertThat(e, Matchers.notNullValue())
            }
        }
    }

    @Test
    @Throws(Throwable::class)
    fun testRequestResponseFailure() {
        enqueue404()
        CsrfTokenClient.getToken(wikiSite, "csrf", apiService).test().await()
            .assertError(Exception::class.java)
    }

    @Test
    @Throws(Throwable::class)
    fun testRequestResponseFailureCoroutine() {
        enqueue404()
        runBlocking {
            try {
                CsrfTokenClient.getTokenBlocking(wikiSite, "csrf", apiService)
            } catch (e: Exception) {
                MatcherAssert.assertThat(e, Matchers.notNullValue())
            }
        }
    }
}
