package org.wikipedia.captcha

import kotlinx.coroutines.runBlocking
import org.hamcrest.MatcherAssert
import org.hamcrest.Matchers
import org.junit.Test
import org.wikipedia.test.MockRetrofitTest

class CaptchaClientTest : MockRetrofitTest() {
    @Test
    @Throws(Throwable::class)
    fun testRequestSuccess() {
        enqueueFromFile("captcha.json")
        runBlocking {
            getNewCaptcha()
        }.run {
            MatcherAssert.assertThat(captchaId, Matchers.`is`("1572672319"))
        }
    }

    @Test
    @Throws(Throwable::class)
    fun testRequestResponseApiError() {
        enqueueFromFile("api_error.json")
        runBlocking {
            try {
                getNewCaptcha()
            } catch (e: Exception) {
                MatcherAssert.assertThat(e, Matchers.notNullValue())
            }
        }
    }

    @Test
    @Throws(Throwable::class)
    fun testRequestResponseFailure() {
        enqueue404()
        runBlocking {
            try {
                getNewCaptcha()
            } catch (e: Exception) {
                MatcherAssert.assertThat(e, Matchers.notNullValue())
            }
        }
    }

    @Test
    @Throws(Throwable::class)
    fun testRequestResponseMalformed() {
        enqueueMalformed()
        runBlocking {
            try {
                getNewCaptcha()
            } catch (e: Exception) {
                MatcherAssert.assertThat(e, Matchers.notNullValue())
            }
        }
    }

    private suspend fun getNewCaptcha(): CaptchaResult {
        val newCaptcha = apiService.getNewCaptcha()
        return CaptchaResult(newCaptcha.captchaId())
    }
}
