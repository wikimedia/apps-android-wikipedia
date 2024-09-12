package org.wikipedia.createaccount

import kotlinx.coroutines.runBlocking
import org.hamcrest.MatcherAssert
import org.hamcrest.Matchers
import org.junit.Test
import org.wikipedia.test.MockRetrofitTest

class CreateAccountInfoClientTest : MockRetrofitTest() {
    @Test
    @Throws(Throwable::class)
    fun testRequestSuccess() {
        enqueueFromFile("create_account_info.json")
        runBlocking {
            apiService.getAuthManagerInfo()
        }.run {
            MatcherAssert.assertThat(query!!.createAccountToken(), Matchers.`is`("5d78e6a823be0901eeae9f6486f752da59123760+\\"))
            MatcherAssert.assertThat(query!!.captchaId(), Matchers.`is`("272460457"))
        }
    }

    @Test
    @Throws(Throwable::class)
    fun testRequestResponse404() {
        enqueue404()
        runBlocking {
            try {
                apiService.getAuthManagerInfo()
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
                apiService.getAuthManagerInfo()
            } catch (e: Exception) {
                MatcherAssert.assertThat(e, Matchers.notNullValue())
            }
        }
    }
}
