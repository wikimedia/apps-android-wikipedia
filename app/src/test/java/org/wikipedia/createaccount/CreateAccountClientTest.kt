package org.wikipedia.createaccount

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.wikipedia.dataclient.Service
import org.wikipedia.dataclient.mwapi.CreateAccountResponse
import org.wikipedia.test.MockRetrofitTest

class CreateAccountClientTest : MockRetrofitTest() {

    @Test
    @Throws(Throwable::class)
    fun testRequestSuccess() {
        enqueueFromFile("create_account_success.json")
        runBlocking {
            createAccount()
        }.run {
            assertEquals("PASS", status)
            assertEquals("Farb0nucci", user)
        }
    }

    @Test
    @Throws(Throwable::class)
    fun testRequestFailure() {
        enqueueFromFile("create_account_failure.json")
        runBlocking {
            createAccount()
        }.run {
            assertEquals("FAIL", status)
        }
    }

    @Test
    @Throws(Throwable::class)
    fun testRequestResponse404() {
        enqueue404()
        runBlocking {
            try {
                createAccount()
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
                createAccount()
            } catch (e: Exception) {
                assertNotNull(e)
            }
        }
    }

    private suspend fun createAccount(): CreateAccountResponse {
        return apiService.postCreateAccount("user", "pass", "pass", "token",
            Service.WIKIPEDIA_URL, null, null, null)
    }
}
