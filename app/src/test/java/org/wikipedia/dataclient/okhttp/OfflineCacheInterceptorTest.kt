package org.wikipedia.dataclient.okhttp

import okhttp3.Request
import org.hamcrest.MatcherAssert
import org.hamcrest.Matchers
import org.junit.Test
import org.wikipedia.dataclient.Service
import org.wikipedia.dataclient.okhttp.OfflineCacheInterceptor.Companion.shouldSave

class OfflineCacheInterceptorTest {
    @Test
    fun testIsCacheableTrue() {
        val request = Request.Builder()
            .url(Service.WIKIPEDIA_URL)
            .addHeader(
                OfflineCacheInterceptor.SAVE_HEADER,
                OfflineCacheInterceptor.SAVE_HEADER_SAVE
            )
            .build()
        MatcherAssert.assertThat(shouldSave(request), Matchers.`is`(true))
    }

    @Test
    fun testIsCacheableFalse() {
        val request = Request.Builder().url(Service.WIKIPEDIA_URL).build()
        MatcherAssert.assertThat(shouldSave(request), Matchers.`is`(false))
    }
}
