package org.wikipedia.createaccount

import kotlinx.coroutines.runBlocking
import org.hamcrest.MatcherAssert
import org.hamcrest.Matchers
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
            MatcherAssert.assertThat(status, Matchers.`is`("PASS"))
            MatcherAssert.assertThat(user, Matchers.`is`("Farb0nucci"))
        }
    }

    @Test
    @Throws(Throwable::class)
    fun testRequestFailure() {
        enqueueFromFile("create_account_failure.json")
        runBlocking {
            createAccount()
        }.run {
            MatcherAssert.assertThat(status, Matchers.`is`("FAIL"))
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
                createAccount()
            } catch (e: Exception) {
                MatcherAssert.assertThat(e, Matchers.notNullValue())
            }
        }
    }

    private suspend fun createAccount(): CreateAccountResponse {
        return apiService.postCreateAccount("user", "pass", "pass", "token",
            Service.WIKIPEDIA_URL, null, null, null)
    }
}
