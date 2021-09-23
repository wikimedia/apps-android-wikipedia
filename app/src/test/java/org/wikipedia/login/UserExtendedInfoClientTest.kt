package org.wikipedia.login

import com.google.gson.stream.MalformedJsonException
import org.junit.Test
import org.wikipedia.test.MockRetrofitTest

class UserExtendedInfoClientTest : MockRetrofitTest() {
    @Test
    @Throws(Throwable::class)
    fun testRequestSuccess() {
        enqueueFromFile("user_extended_info.json")
        val id = 24531888
        apiService.userInfo.test().await()
            .assertComplete().assertNoErrors()
            .assertValue {
                it.query?.userInfo?.id == id &&
                        it.query?.getUserResponse("USER")?.name == "USER"
            }
    }

    @Test
    @Throws(Throwable::class)
    fun testRequestResponse404() {
        enqueue404()
        apiService.userInfo.test().await()
            .assertError(Exception::class.java)
    }

    @Test
    @Throws(Throwable::class)
    fun testRequestResponseMalformed() {
        enqueueMalformed()
        apiService.userInfo.test().await()
            .assertError(MalformedJsonException::class.java)
    }
}
