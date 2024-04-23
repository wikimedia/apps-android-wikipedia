package org.wikipedia.random

import kotlinx.coroutines.runBlocking
import org.hamcrest.MatcherAssert
import org.hamcrest.Matchers
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
            MatcherAssert.assertThat(displayTitle, Matchers.`is`("Fermat's Last Theorem"))
            MatcherAssert.assertThat(description, Matchers.`is`("theorem in number theory"))
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
                MatcherAssert.assertThat(e, Matchers.notNullValue())
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
                MatcherAssert.assertThat(e, Matchers.notNullValue())
            }
        }
    }

    private suspend fun getRandomSummary(): PageSummary {
        return restService.getRandomSummary()
    }
}
