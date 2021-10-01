package org.wikipedia.gallery

import org.junit.Test
import org.wikipedia.test.MockRetrofitTest

class GalleryClientTest : MockRetrofitTest() {
    @Test
    @Throws(Throwable::class)
    fun testRequestAllSuccess() {
        enqueueFromFile(RAW_JSON_FILE)
        observable.test().await()
            .assertComplete().assertNoErrors()
            .assertValue { gallery ->
                (gallery.getItems("image").size == 1 && gallery.getItems("video").size == 1)
            }
    }

    @Test
    @Throws(Throwable::class)
    fun testRequestImageSuccess() {
        enqueueFromFile(RAW_JSON_FILE)
        observable.test().await()
            .assertComplete().assertNoErrors()
            .assertValue { gallery ->
                val result = gallery.getItems("image")
                result.size == 1 && result[0].type == "image" && result[0].title == "File:BarackObamaportrait.jpg" && result[0].showInGallery
            }
    }

    @Test
    @Throws(Throwable::class)
    fun testRequestVideoSuccess() {
        enqueueFromFile(RAW_JSON_FILE)
        observable.test().await()
            .assertComplete().assertNoErrors()
            .assertValue { gallery ->
                val result = gallery.getItems("video")
                result[0].type == "video" && result[0].title == "File:20090124_WeeklyAddress.ogv" && result[0].showInGallery
            }
    }

    @Test
    @Throws(Throwable::class)
    fun testRequestResponseFailure() {
        enqueue404()
        observable.test().await().assertError(Exception::class.java)
    }

    @Test
    @Throws(Throwable::class)
    fun testRequestResponseMalformed() {
        enqueueMalformed()
        observable.test().await().assertError(Exception::class.java)
    }

    private val observable
        get() = restService.getMediaList("foo", 0)

    companion object {
        private const val RAW_JSON_FILE = "gallery.json"
    }
}
