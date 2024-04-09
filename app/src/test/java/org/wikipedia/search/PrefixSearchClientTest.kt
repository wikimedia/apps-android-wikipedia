package org.wikipedia.search

import kotlinx.coroutines.runBlocking
import org.hamcrest.MatcherAssert
import org.hamcrest.Matchers
import org.junit.Test
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.mwapi.MwQueryResponse
import org.wikipedia.test.MockRetrofitTest

class PrefixSearchClientTest : MockRetrofitTest() {

    private suspend fun searchResults(): SearchResults {
        val mwQueryResponse = prefixSearch()
        return if (mwQueryResponse.query?.pages != null) {
            SearchResults(mwQueryResponse.query!!.pages!!, TESTWIKI, mwQueryResponse.continuation)
        } else SearchResults()
    }

    @Test
    @Throws(Throwable::class)
    fun testRequestSuccess() {
        enqueueFromFile("prefix_search_results.json")
        runBlocking {
            searchResults()
        }.run {
            MatcherAssert.assertThat(results.first().pageTitle.displayText, Matchers.`is`("Narthecium"))
        }
    }

    @Test
    @Throws(Throwable::class)
    fun testRequestSuccessNoResults() {
        enqueueFromFile("prefix_search_results_empty.json")
        runBlocking {
            searchResults()
        }.run {
            MatcherAssert.assertThat(results, Matchers.empty())
        }
    }

    @Test
    @Throws(Throwable::class)
    fun testRequestResponseApiError() {
        enqueueFromFile("api_error.json")
        runBlocking {
            try {
                searchResults()
            } catch (e: Exception) {
                MatcherAssert.assertThat(e, Matchers.notNullValue())
            }
        }
    }

    @Test
    @Throws(Throwable::class)
    fun testRequestResponseFailure() {
        enqueue404()
        runBlocking {
            try {
                searchResults()
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
                searchResults()
            } catch (e: Exception) {
                MatcherAssert.assertThat(e, Matchers.notNullValue())
            }
        }
    }

    private suspend fun prefixSearch(): MwQueryResponse {
        return apiService.prefixSearch("foo", BATCH_SIZE, 0)
    }

    companion object {
        private val TESTWIKI = WikiSite("test.wikimedia.org")
        private const val BATCH_SIZE = 20
    }
}
