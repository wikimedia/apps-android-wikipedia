package org.wikipedia.test

import android.net.Uri
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import org.junit.Before
import org.wikipedia.dataclient.RestService
import org.wikipedia.dataclient.Service
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.WikiSite.Companion.forLanguageCode
import org.wikipedia.json.NamespaceTypeAdapter
import org.wikipedia.json.PostProcessingTypeAdapter
import org.wikipedia.json.UriTypeAdapter
import org.wikipedia.json.WikiSiteTypeAdapter
import org.wikipedia.page.Namespace
import retrofit2.Retrofit
import retrofit2.adapter.rxjava3.RxJava3CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory

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
            .addConverterFactory(GsonConverterFactory.create(gson))
            .baseUrl(server().url)
            .build()
        apiService = retrofit.create(Service::class.java)
        restService = retrofit.create(RestService::class.java)
    }

    private val gson: Gson
        get() = GsonBuilder()
            .registerTypeHierarchyAdapter(Uri::class.java, UriTypeAdapter().nullSafe())
            .registerTypeHierarchyAdapter(Namespace::class.java, NamespaceTypeAdapter().nullSafe())
            .registerTypeAdapter(WikiSite::class.java, WikiSiteTypeAdapter().nullSafe())
            .registerTypeAdapterFactory(PostProcessingTypeAdapter())
            .create()
}
