package org.wikipedia.search

import org.junit.Test
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.test.MockRetrofitTest

class PrefixSearchClientTest : MockRetrofitTest() {
    private val observable
        get() = apiService.prefixSearch("foo", BATCH_SIZE, "foo")
            .map { response ->
                if (response.query?.pages != null) {
                    return@map SearchResults(response.query!!.pages!!, TESTWIKI, response.continuation, response.suggestion())
                }
                SearchResults()
            }

    @Test
    @Throws(Throwable::class)
    fun testRequestSuccess() {
        enqueueFromFile("prefix_search_results.json")
        observable.test().await()
            .assertComplete().assertNoErrors()
            .assertValue { (results) -> results[0].pageTitle.displayText == "Narthecium" }
    }

    @Test
    @Throws(Throwable::class)
    fun testRequestSuccessNoResults() {
        enqueueFromFile("prefix_search_results_empty.json")
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
