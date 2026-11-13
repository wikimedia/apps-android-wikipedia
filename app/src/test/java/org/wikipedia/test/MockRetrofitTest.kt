package org.wikipedia.test

import org.junit.Before
import org.wikipedia.dataclient.RestService
import org.wikipedia.dataclient.Service
import org.wikipedia.dataclient.WikiSite.Companion.forLanguageCode
import org.wikipedia.json.MwExceptionConverterFactory
import retrofit2.Retrofit

abstract class MockRetrofitTest : MockWebServerTest() {
    protected lateinit var apiService: Service
        private set
    protected lateinit var restService: RestService
        private set
    protected val wikiSite = forLanguageCode("en")

    @Before
    @Throws(Throwable::class)
    override fun setUp() {
        super.setUp()
        val retrofit = Retrofit.Builder()
            .addConverterFactory(MwExceptionConverterFactory())
            .baseUrl(server().url)
            .build()
        apiService = retrofit.create(Service::class.java)
        restService = retrofit.create(RestService::class.java)
    }
}
