package org.wikipedia.createaccount

import com.google.gson.stream.MalformedJsonException
import org.junit.Test
import org.wikipedia.dataclient.mwapi.MwQueryResponse
import org.wikipedia.test.MockRetrofitTest

class CreateAccountInfoClientTest : MockRetrofitTest() {
    @Test
    @Throws(Throwable::class)
    fun testRequestSuccess() {
        enqueueFromFile("create_account_info.json")
        apiService.authManagerInfo.test().await()
            .assertComplete().assertNoErrors()
            .assertValue { response: MwQueryResponse ->
                val token = response.query!!.createAccountToken()
                val captchaId = response.query!!.captchaId()
                token == "5d78e6a823be0901eeae9f6486f752da59123760+\\" && captchaId == "272460457"
            }
    }

    @Test
    @Throws(Throwable::class)
    fun testRequestResponse404() {
        enqueue404()
        apiService.authManagerInfo.test().await()
            .assertError(Exception::class.java)
    }

    @Test
    @Throws(Throwable::class)
    fun testRequestResponseMalformed() {
        enqueueMalformed()
        apiService.authManagerInfo.test().await()
            .assertError(MalformedJsonException::class.java)
    }
}
