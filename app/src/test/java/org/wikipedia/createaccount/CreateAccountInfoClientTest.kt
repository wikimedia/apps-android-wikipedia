package org.wikipedia.createaccount

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
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
            assertEquals("5d78e6a823be0901eeae9f6486f752da59123760+\\", query!!.createAccountToken())
            assertEquals("272460457", query!!.captchaId())
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
                apiService.getAuthManagerInfo()
            } catch (e: Exception) {
                assertNotNull(e)
            }
        }
    }
}
