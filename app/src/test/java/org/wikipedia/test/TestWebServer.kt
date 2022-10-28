package org.wikipedia.test

import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.wikipedia.util.ReleaseUtil.isDevRelease
import java.io.IOException
import java.util.concurrent.TimeUnit

class TestWebServer {
    private val server: MockWebServer = MockWebServer()

    @Throws(IOException::class)
    fun setUp() {
        server.start()
    }

    @Throws(IOException::class)
    fun tearDown() {
        server.shutdown()
    }

    val url: String
        get() = getUrl("")

    fun getUrl(path: String?): String {
        return path?.let {
            server.url(it).toUrl().toString()
        } ?: ""
    }

    fun enqueue(body: String) {
        enqueue(MockResponse().setBody(body))
    }

    fun enqueue(response: MockResponse?) {
        response?.let {
            server.enqueue(response)
        }
    }

    @Throws(InterruptedException::class)
    fun takeRequest(): RecordedRequest {
        return server.takeRequest(
            TIMEOUT_DURATION.toLong(),
            TIMEOUT_UNIT
        ) ?: throw InterruptedException("Timeout elapsed.")
    }

    companion object {
        private const val TIMEOUT_DURATION = 5
        private val TIMEOUT_UNIT = if (isDevRelease) TimeUnit.SECONDS else TimeUnit.MINUTES
    }
}
