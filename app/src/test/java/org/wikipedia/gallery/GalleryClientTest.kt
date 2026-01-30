package org.wikipedia.gallery

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
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
            assertEquals(1, getItems("image").size)
            assertEquals(1, getItems("video").size)
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
            assertEquals(1, result.size)
            assertEquals("image", result[0].type)
            assertEquals("File:BarackObamaportrait.jpg", result[0].title)
            assertTrue(result[0].showInGallery)
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
            assertEquals("video", result[0].type)
            assertEquals("File:20090124_WeeklyAddress.ogv", result[0].title)
            assertTrue(result[0].showInGallery)
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
                getMediaList()
            } catch (e: Exception) {
                assertNotNull(e)
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
