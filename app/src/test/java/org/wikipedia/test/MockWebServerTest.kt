package org.wikipedia.test

import okhttp3.Dispatcher
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import org.junit.After
import org.junit.Before
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.wikipedia.dataclient.okhttp.OkHttpConnectionFactory.client
import org.wikipedia.json.GsonUtil
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

@RunWith(RobolectricTestRunner::class)
abstract class MockWebServerTest {
    private lateinit var okHttpClient: OkHttpClient
    private val server = TestWebServer()

    @Before
    @Throws(Throwable::class)
    open fun setUp() {
        val builder: OkHttpClient.Builder = client.newBuilder()
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

    protected fun <T> service(clazz: Class<T>): T {
        return service(clazz, server().url)
    }

    protected fun <T> service(clazz: Class<T>, url: String): T {
        return Retrofit.Builder()
            .baseUrl(url)
            .callbackExecutor(ImmediateExecutor())
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(GsonUtil.getDefaultGson()))
            .build()
            .create(clazz)
    }
}
