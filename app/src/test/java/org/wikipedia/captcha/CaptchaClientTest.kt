package org.wikipedia.captcha

import com.google.gson.stream.MalformedJsonException
import org.junit.Test
import org.wikipedia.test.MockRetrofitTest

class CaptchaClientTest : MockRetrofitTest() {
    @Test
    @Throws(Throwable::class)
    fun testRequestSuccess() {
        enqueueFromFile("captcha.json")
        observable.test().await()
            .assertComplete().assertNoErrors()
            .assertValue { result -> result.captchaId == "1572672319" }
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
            .assertError(MalformedJsonException::class.java)
    }

    private val observable
        get() = apiService.newCaptcha.map { response -> CaptchaResult(response.captchaId()) }
}
