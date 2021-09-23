package org.wikipedia.test

import okhttp3.Dispatcher
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import org.junit.After
import org.junit.Before
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.wikipedia.dataclient.okhttp.OkHttpConnectionFactory.client

@RunWith(RobolectricTestRunner::class)
abstract class MockWebServerTest {
    private lateinit var okHttpClient: OkHttpClient
    private val server = TestWebServer()

    @Before
    @Throws(Throwable::class)
    open fun setUp() {
        val builder = client.newBuilder()
        okHttpClient = builder.dispatcher(Dispatcher(ImmediateExecutorService())).build()
        server.setUp()
    }

    @After
    @Throws(Throwable::class)
    fun tearDown() {
        server.tearDown()
    }

    protected fun server(): TestWebServer {
        return server
    }

    @Throws(Throwable::class)
    protected fun enqueueFromFile(filename: String) {
        val json = TestFileUtil.readRawFile(filename)
        server.enqueue(json)
    }

    protected fun enqueue404() {
        val code = 404
        server.enqueue(MockResponse().setResponseCode(code).setBody("Not Found"))
    }

    protected fun enqueueMalformed() {
        server.enqueue("(╯°□°）╯︵ ┻━┻")
    }

    protected fun enqueueEmptyJson() {
        server.enqueue(MockResponse().setBody("{}"))
    }
}
