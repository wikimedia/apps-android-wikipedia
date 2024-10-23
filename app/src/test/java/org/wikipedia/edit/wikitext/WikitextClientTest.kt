package org.wikipedia.edit.wikitext

import kotlinx.coroutines.runBlocking
import org.hamcrest.MatcherAssert
import org.hamcrest.Matchers
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
            MatcherAssert.assertThat(query?.firstPage()?.revisions?.first()?.contentMain, Matchers.`is`("\\o/\n\ntest12\n\n3"))
            MatcherAssert.assertThat(query?.firstPage()?.revisions?.first()?.timeStamp, Matchers.`is`("2018-03-18T18:10:54Z"))
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
                requestWikiTextForSectionWithInfo()
            } catch (e: Exception) {
                MatcherAssert.assertThat(e, Matchers.notNullValue())
            }
        }
    }

    private suspend fun requestWikiTextForSectionWithInfo(): MwQueryResponse {
        return apiService.getWikiTextForSectionWithInfo("User:Mhollo/sandbox", 0)
    }
}
