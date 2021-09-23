package org.wikipedia.captcha

import org.junit.Test
import org.wikipedia.test.MockRetrofitTest

class CaptchaClientTest : MockRetrofitTest() {
    @Test
    @Throws(Throwable::class)
    fun testRequestSuccess() {
        enqueueFromFile("captcha.json")
        observable.test().await()
            .assertComplete().assertNoErrors()
            .assertValue { it.captchaId == "1572672319" }
    }

    @Test
    @Throws(Throwable::class)
    fun testRequestResponseApiError() {
        enqueueFromFile("api_error.json")
        observable.test().await()
            .assertError(Exception::class.java)
    }

    @Test
    @Throws(Throwable::class)
    fun testRequestResponseFailure() {
        enqueue404()
        observable.test().await()
            .assertError(Exception::class.java)
    }

    @Test
    @Throws(Throwable::class)
    fun testRequestResponseMalformed() {
        enqueueMalformed()
        observable.test().await()
            .assertError(Exception::class.java)
    }

    private val observable
        get() = apiService.newCaptcha.map { CaptchaResult(it.captchaId()) }
}
