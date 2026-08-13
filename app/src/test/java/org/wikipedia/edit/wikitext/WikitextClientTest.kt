package org.wikipedia.edit.wikitext

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.wikipedia.dataclient.mwapi.MwQueryResponse
import org.wikipedia.test.MockRetrofitTest

class WikitextClientTest : MockRetrofitTest() {
    @Test
    @Throws(Throwable::class)
    fun testRequestSuccessHasResults() {
        enqueueFromFile("wikitext.json")
        runBlocking {
            requestWikiTextForSectionWithInfo()
        }.run {
            assertEquals("\\o/\n\ntest12\n\n3", query?.firstPage()?.revisions?.first()?.contentMain)
            assertEquals("2018-03-18T18:10:54Z", query?.firstPage()?.revisions?.first()?.timeStamp)
        }
    }

    @Test
    @Throws(Throwable::class)
    fun testRequestResponseApiError() {
        enqueueFromFile("api_error.json")
        runBlocking {
            try {
                requestWikiTextForSectionWithInfo()
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
                requestWikiTextForSectionWithInfo()
            } catch (e: Exception) {
                assertNotNull(e)
            }
        }
    }

    private suspend fun requestWikiTextForSectionWithInfo(): MwQueryResponse {
        return apiService.getWikiTextForSectionWithInfo("User:Mhollo/sandbox", 0)
    }
}
