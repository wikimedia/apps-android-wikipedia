package org.wikipedia.search

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.mwapi.MwQueryResponse
import org.wikipedia.test.MockRetrofitTest

class FullTextSearchClientTest : MockRetrofitTest() {

    private suspend fun searchResults(): SearchResults {
        val mwQueryResponse = fullTextSearch()
        return if (mwQueryResponse.query?.pages != null) {
            SearchResults(mwQueryResponse.query!!.pages!!, TESTWIKI, mwQueryResponse.continuation)
        } else SearchResults()
    }

    @Test
    @Throws(Throwable::class)
    fun testRequestSuccessNoContinuation() {
        enqueueFromFile("full_text_search_results.json")
        runBlocking {
            searchResults()
        }.run {
            assertEquals("IND Queens Boulevard Line", results.first().pageTitle.displayText)
        }
    }

    @Test
    @Throws(Throwable::class)
    fun testRequestSuccessWithContinuation() {
        enqueueFromFile("full_text_search_results.json")
        runBlocking {
            searchResults()
        }.run {
            assertEquals("gsroffset||", continuation?.continuation)
            assertEquals(20, continuation?.gsroffset)
        }
    }

    @Test
    @Throws(Throwable::class)
    fun testRequestSuccessNoResults() {
        enqueueFromFile("full_text_search_results_empty.json")
        runBlocking {
            searchResults()
        }.run {
            assertTrue(results.isEmpty())
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
                assertNotNull(e)
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
                searchResults()
            } catch (e: Exception) {
                assertNotNull(e)
            }
        }
    }

    private suspend fun fullTextSearch(): MwQueryResponse {
        return apiService.fullTextSearch("foo", BATCH_SIZE, null)
    }

    companion object {
        private val TESTWIKI = WikiSite("test.wikimedia.org")
        private const val BATCH_SIZE = 20
    }
}
