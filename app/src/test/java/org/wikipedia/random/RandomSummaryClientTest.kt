package org.wikipedia.random

import org.junit.Test
import org.wikipedia.test.MockRetrofitTest

class RandomSummaryClientTest : MockRetrofitTest() {
    @Test
    @Throws(Throwable::class)
    fun testRequestEligible() {
        enqueueFromFile("rb_page_summary_valid.json")
        restService.randomSummary.test().await()
            .assertComplete().assertNoErrors()
            .assertValue { it.displayTitle == "Fermat's Last Theorem" && it.description == "theorem in number theory" }
    }

    @Test
    @Throws(Throwable::class)
    fun testRequestMalformed() {
        enqueueMalformed()
        restService.randomSummary.test().await()
            .assertError(Exception::class.java)
    }

    @Test
    @Throws(Throwable::class)
    fun testRequestFailure() {
        enqueue404()
        restService.randomSummary.test().await()
            .assertError(Exception::class.java)
    }
}
