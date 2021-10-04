package org.wikipedia.search

import org.junit.Test
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.test.MockRetrofitTest

class FullTextSearchClientTest : MockRetrofitTest() {
    private val observable
        get() = apiService.fullTextSearch("foo", BATCH_SIZE, null, null)
            .map { response ->
                if (response.query != null) {
                    return@map SearchResults(response.query!!.pages!!, TESTWIKI, response.continuation, null)
                }
                SearchResults()
            }

    @Test
    @Throws(Throwable::class)
    fun testRequestSuccessNoContinuation() {
        enqueueFromFile("full_text_search_results.json")
        observable.test().await()
            .assertComplete().assertNoErrors()
            .assertValue { (results) -> results[0].pageTitle.displayText == "IND Queens Boulevard Line" }
    }

    @Test
    @Throws(Throwable::class)
    fun testRequestSuccessWithContinuation() {
        enqueueFromFile("full_text_search_results.json")
        observable.test().await()
            .assertComplete().assertNoErrors()
            .assertValue { (_, continuation) -> continuation!!.continuation == "gsroffset||" && continuation.gsroffset == 20 }
    }

    @Test
    @Throws(Throwable::class)
    fun testRequestSuccessNoResults() {
        enqueueFromFile("full_text_search_results_empty.json")
        observable.test().await()
            .assertComplete().assertNoErrors()
            .assertValue { (results) -> results.isEmpty() }
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

    companion object {
        private val TESTWIKI = WikiSite("test.wikimedia.org")
        private const val BATCH_SIZE = 20
    }
}
