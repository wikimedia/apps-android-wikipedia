package org.wikipedia.test

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import okhttp3.MediaType.Companion.toMediaType
import org.junit.Before
import org.wikipedia.dataclient.RestService
import org.wikipedia.dataclient.Service
import org.wikipedia.dataclient.WikiSite.Companion.forLanguageCode
import org.wikipedia.json.JsonUtil
import retrofit2.Retrofit
import retrofit2.adapter.rxjava3.RxJava3CallAdapterFactory

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
            .addCallAdapterFactory(RxJava3CallAdapterFactory.create())
                .addConverterFactory(JsonUtil.json.asConverterFactory("application/json".toMediaType()))
            .baseUrl(server().url)
            .build()
        apiService = retrofit.create(Service::class.java)
        restService = retrofit.create(RestService::class.java)
    }
}
