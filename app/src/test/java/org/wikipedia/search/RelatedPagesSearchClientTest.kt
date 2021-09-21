package org.wikipedia.search

import com.google.gson.stream.MalformedJsonException
import io.reactivex.rxjava3.core.Observable
import org.junit.Test
import org.wikipedia.dataclient.page.PageSummary
import org.wikipedia.dataclient.restbase.RbRelatedPages
import org.wikipedia.test.MockRetrofitTest

class RelatedPagesSearchClientTest : MockRetrofitTest() {
    @Test
    @Throws(Throwable::class)
    fun testRequestSuccessWithNoLimit() {
        enqueueFromFile(RAW_JSON_FILE)
        observable.test().await()
            .assertComplete().assertNoErrors()
            .assertValue { it.size == 5 && it[4].thumbnailUrl == "https://upload.wikimedia.org/wikipedia/commons/thumb/8/8e/Vizsla_r%C3%A1h%C3%BAz_a_vadra.jpg/320px-Vizsla_r%C3%A1h%C3%BAz_a_vadra.jpg" && it[4].displayTitle == "Dog intelligence" && it[4].description == null }
    }

    @Test
    @Throws(Throwable::class)
    fun testRequestSuccessWithLimit() {
        enqueueFromFile(RAW_JSON_FILE)
        restService.getRelatedPages("foo")
            .map<List<PageSummary>> { response: RbRelatedPages -> response.getPages(3) }
            .test().await()
            .assertComplete().assertNoErrors()
            .assertValue { it.size == 3 && it[0].thumbnailUrl == "https://upload.wikimedia.org/wikipedia/commons/thumb/a/ab/European_grey_wolf_in_Prague_zoo.jpg/291px-European_grey_wolf_in_Prague_zoo.jpg" && it[0].displayTitle == "Wolf" && it[0].description == "species of mammal" }
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
        restService.getRelatedPages("foo")
            .map {
                it.pages!!
            }
            .test().await()
            .assertError(Exception::class.java)
    }

    @Test
    @Throws(Throwable::class)
    fun testRequestResponseMalformed() {
        enqueueMalformed()
        observable.test().await()
            .assertError(MalformedJsonException::class.java)
    }

    private val observable: Observable<List<PageSummary>>
        get() = restService.getRelatedPages("foo").map { it.pages!! }

    companion object {
        private const val RAW_JSON_FILE = "related_pages_search_results.json"
    }
}
