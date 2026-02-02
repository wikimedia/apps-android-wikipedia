package org.wikipedia.random

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.wikipedia.dataclient.page.PageSummary
import org.wikipedia.test.MockRetrofitTest

class RandomSummaryClientTest : MockRetrofitTest() {
    @Test
    @Throws(Throwable::class)
    fun testRequestEligible() {
        enqueueFromFile("rb_page_summary_valid.json")
        runBlocking {
            getRandomSummary()
        }.run {
            assertEquals("Fermat's Last Theorem", displayTitle)
            assertEquals("theorem in number theory", description)
        }
    }

    @Test
    @Throws(Throwable::class)
    fun testRequestMalformed() {
        enqueueMalformed()
        runBlocking {
            try {
                getRandomSummary()
            } catch (e: Exception) {
                assertNotNull(e)
            }
        }
    }

    @Test
    @Throws(Throwable::class)
    fun testRequestFailure() {
        enqueue404()
        runBlocking {
            try {
                getRandomSummary()
            } catch (e: Exception) {
                assertNotNull(e)
            }
        }
    }

    private suspend fun getRandomSummary(): PageSummary {
        return restService.getRandomSummary()
    }
}
