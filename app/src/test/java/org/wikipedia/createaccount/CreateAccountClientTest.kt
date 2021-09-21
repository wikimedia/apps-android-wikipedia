package org.wikipedia.createaccount

import com.google.gson.stream.MalformedJsonException
import io.reactivex.rxjava3.core.Observable
import org.junit.Test
import org.wikipedia.dataclient.Service
import org.wikipedia.dataclient.mwapi.CreateAccountResponse
import org.wikipedia.test.MockRetrofitTest

class CreateAccountClientTest : MockRetrofitTest() {
    private val observable: Observable<CreateAccountResponse>
        get() = apiService.postCreateAccount("user", "pass", "pass", "token",
            Service.WIKIPEDIA_URL, null, null, null)

    @Test
    @Throws(Throwable::class)
    fun testRequestSuccess() {
        enqueueFromFile("create_account_success.json")
        observable.test().await()
            .assertComplete().assertNoErrors()
            .assertValue { result: CreateAccountResponse -> result.status == "PASS" && result.user == "Farb0nucci" }
    }

    @Test
    @Throws(Throwable::class)
    fun testRequestFailure() {
        enqueueFromFile("create_account_failure.json")
        observable.test().await()
            .assertComplete().assertNoErrors()
            .assertValue { result: CreateAccountResponse -> result.status == "FAIL" }
    }

    @Test
    @Throws(Throwable::class)
    fun testRequestResponse404() {
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
}
