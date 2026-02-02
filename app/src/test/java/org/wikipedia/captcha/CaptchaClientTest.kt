package org.wikipedia.captcha

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
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
            assertEquals("1572672319", captchaId)
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
                getNewCaptcha()
            } catch (e: Exception) {
                assertNotNull(e)
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
                assertNotNull(e)
            }
        }
    }

    private suspend fun getNewCaptcha(): CaptchaResult {
        val newCaptcha = apiService.getNewCaptcha()
        return CaptchaResult(newCaptcha.captchaId())
    }
}
