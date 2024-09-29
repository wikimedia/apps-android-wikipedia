package org.wikipedia.gallery

import kotlinx.coroutines.runBlocking
import org.hamcrest.MatcherAssert
import org.hamcrest.Matchers
import org.junit.Test
import org.wikipedia.test.MockRetrofitTest

class GalleryClientTest : MockRetrofitTest() {
    @Test
    @Throws(Throwable::class)
    fun testRequestAllSuccess() {
        enqueueFromFile(RAW_JSON_FILE)
        runBlocking {
            getMediaList()
        }.run {
            MatcherAssert.assertThat(getItems("image").size, Matchers.`is`(1))
            MatcherAssert.assertThat(getItems("video").size, Matchers.`is`(1))
        }
    }

    @Test
    @Throws(Throwable::class)
    fun testRequestImageSuccess() {
        enqueueFromFile(RAW_JSON_FILE)
        runBlocking {
            getMediaList()
        }.run {
            val result = getItems("image")
            MatcherAssert.assertThat(result.size, Matchers.`is`(1))
            MatcherAssert.assertThat(result[0].type, Matchers.`is`("image"))
            MatcherAssert.assertThat(result[0].title, Matchers.`is`("File:BarackObamaportrait.jpg"))
            MatcherAssert.assertThat(result[0].showInGallery, Matchers.`is`(true))
        }
    }

    @Test
    @Throws(Throwable::class)
    fun testRequestVideoSuccess() {
        enqueueFromFile(RAW_JSON_FILE)
        runBlocking {
            getMediaList()
        }.run {
            val result = getItems("video")
            MatcherAssert.assertThat(result[0].type, Matchers.`is`("video"))
            MatcherAssert.assertThat(result[0].title, Matchers.`is`("File:20090124_WeeklyAddress.ogv"))
            MatcherAssert.assertThat(result[0].showInGallery, Matchers.`is`(true))
        }
    }

    @Test
    @Throws(Throwable::class)
    fun testRequestResponseFailure() {
        enqueue404()
        runBlocking {
            try {
                getMediaList()
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
                getMediaList()
            } catch (e: Exception) {
                MatcherAssert.assertThat(e, Matchers.notNullValue())
            }
        }
    }

    private suspend fun getMediaList(): MediaList {
        return restService.getMediaList("foo", 0)
    }

    companion object {
        private const val RAW_JSON_FILE = "gallery.json"
    }
}
