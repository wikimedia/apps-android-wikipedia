package org.wikipedia.dataclient

import androidx.collection.lruCache
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import org.wikipedia.WikipediaApp
import org.wikipedia.analytics.eventplatform.DestinationEventService
import org.wikipedia.analytics.eventplatform.EventService
import org.wikipedia.analytics.eventplatform.StreamConfig
import org.wikipedia.dataclient.okhttp.OkHttpConnectionFactory
import org.wikipedia.donate.GooglePayComponent
import org.wikipedia.json.JsonUtil
import org.wikipedia.settings.Prefs
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.create
import java.io.IOException
import java.util.concurrent.TimeUnit

object ServiceFactory {

    private const val SERVICE_CACHE_SIZE = 8

    private val SERVICE_CACHE = lruCache<WikiSite, Service>(SERVICE_CACHE_SIZE, create = {
        // This method is called in the get() method if a value does not already exist.
        createRetrofit(it, getBasePath(it)).create<Service>()
    })

    private val REST_SERVICE_CACHE = lruCache<WikiSite, RestService>(SERVICE_CACHE_SIZE, create = {
        createRetrofit(it, getRestBasePath(it)).create<RestService>()
    })

    private val CORE_REST_SERVICE_CACHE = lruCache<WikiSite, CoreRestService>(SERVICE_CACHE_SIZE, create = {
        createRetrofit(it, it.url() + "/" + CoreRestService.CORE_REST_API_PREFIX).create<CoreRestService>()
    })

    private val ANALYTICS_REST_SERVICE_CACHE = lruCache<DestinationEventService, EventService>(SERVICE_CACHE_SIZE, create = {
        val intakeBaseUriOverride = Prefs.eventPlatformIntakeUriOverride.orEmpty().ifEmpty { it.baseUri }
        createRetrofit(null, intakeBaseUriOverride).create<EventService>()
    })

    private var DONATE_SERVICE_CACHE: Service? = null

    fun get(wiki: WikiSite): Service {
        return SERVICE_CACHE[wiki]!!
    }

    fun getRest(wiki: WikiSite): RestService {
        return REST_SERVICE_CACHE[wiki]!!
    }

    fun getCoreRest(wiki: WikiSite): CoreRestService {
        return CORE_REST_SERVICE_CACHE[wiki]!!
    }

    fun getAnalyticsRest(streamConfig: StreamConfig): EventService {
        return ANALYTICS_REST_SERVICE_CACHE[streamConfig.destinationEventService]!!
    }

    fun getDonate(): Service {
        if (DONATE_SERVICE_CACHE == null) {
            val wikiSite = WikiSite(GooglePayComponent.PAYMENTS_API_URL)
            // https://phabricator.wikimedia.org/T412059
            // Explicitly increase the timeout for the donation API, since the payment processor
            // could occasionally take an increased amount of time to process transactions.
            DONATE_SERVICE_CACHE = createRetrofit(wikiSite, getBasePath(wikiSite), readTimeoutSec = 60L).create<Service>()
        }
        return DONATE_SERVICE_CACHE!!
    }

    operator fun <T> get(wiki: WikiSite, baseUrl: String?, service: Class<T>): T {
        val r = createRetrofit(wiki, baseUrl.orEmpty().ifEmpty { wiki.url() + "/" })
        return r.create(service)
    }

    private fun getBasePath(wiki: WikiSite): String {
        var path = wiki.url()
        if (!path.endsWith("/")) {
            path += "/"
        }
        return path
    }

    fun getRestBasePath(wiki: WikiSite): String {
        var path = if (Prefs.restbaseUriFormat.isEmpty()) wiki.url() + "/" + RestService.REST_API_PREFIX
        else String.format(Prefs.restbaseUriFormat, "https", wiki.authority())
        if (!path.endsWith("/")) {
            path += "/"
        }
        return path
    }

    private fun createRetrofit(wiki: WikiSite?, baseUrl: String, readTimeoutSec: Long = OkHttpConnectionFactory.DEFAULT_READ_TIMEOUT_SEC): Retrofit {
        val builder = OkHttpConnectionFactory.client.newBuilder()
        builder.readTimeout(readTimeoutSec, TimeUnit.SECONDS)
        builder.interceptors().add(builder.interceptors().indexOfFirst { it is HttpLoggingInterceptor }, LanguageVariantHeaderInterceptor(wiki))

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(builder.build())
            .addConverterFactory(JsonUtil.json.asConverterFactory("application/json".toMediaType()))
            .build()
    }

    private class LanguageVariantHeaderInterceptor(private val wiki: WikiSite?) : Interceptor {
        @Throws(IOException::class)
        override fun intercept(chain: Interceptor.Chain): Response {
            var request = chain.request().newBuilder()
                .header("Accept-Language", WikipediaApp.instance.getAcceptLanguage(wiki))
                .build()
            return chain.proceed(request)
        }
    }
}
