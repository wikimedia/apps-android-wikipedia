package org.wikipedia.language

import io.reactivex.rxjava3.core.Observable
import org.junit.Test
import org.wikipedia.dataclient.mwapi.MwQueryResponse
import org.wikipedia.test.MockRetrofitTest

class LangLinksClientTest : MockRetrofitTest() {
    @Test
    @Throws(Throwable::class)
    fun testRequestSuccessHasResults() {
        enqueueFromFile("lang_links.json")
        observable.test().await()
            .assertComplete().assertNoErrors()
            .assertValue { it.query?.langLinks()?.firstOrNull()?.displayText == "Sciëntologie" }
    }

    @Test
    @Throws(Throwable::class)
    fun testRequestSuccessNoResults() {
        enqueueFromFile("lang_links_empty.json")
        observable.test().await()
            .assertComplete().assertNoErrors()
            .assertValue { it.query!!.langLinks().isEmpty() }
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
    fun testRequestResponseMalformed() {
        enqueueMalformed()
        observable.test().await()
            .assertError(Exception::class.java)
    }

    private val observable: Observable<MwQueryResponse>
        get() = apiService.getLangLinks("foo")
}
