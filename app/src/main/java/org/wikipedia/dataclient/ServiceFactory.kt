package org.wikipedia.dataclient

import androidx.collection.lruCache
import okhttp3.Interceptor
import okhttp3.Response
import org.wikipedia.WikipediaApp
import org.wikipedia.analytics.eventplatform.DestinationEventService
import org.wikipedia.analytics.eventplatform.EventService
import org.wikipedia.analytics.eventplatform.StreamConfig
import org.wikipedia.dataclient.okhttp.OkHttpConnectionFactory
import org.wikipedia.json.MoshiUtil
import org.wikipedia.settings.Prefs
import retrofit2.Retrofit
import retrofit2.adapter.rxjava3.RxJava3CallAdapterFactory
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.create
import java.io.IOException

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
        val intakeBaseUriOverride = Prefs.getEventPlatformIntakeUriOverride().ifEmpty { it.baseUri }
        createRetrofit(null, intakeBaseUriOverride).create<EventService>()
    })

    @JvmStatic
    fun get(wiki: WikiSite): Service {
        return SERVICE_CACHE[wiki]!!
    }

    fun getRest(wiki: WikiSite): RestService {
        return REST_SERVICE_CACHE[wiki]!!
    }

    fun getCoreRest(wiki: WikiSite): CoreRestService {
        return CORE_REST_SERVICE_CACHE[wiki]!!
    }

    @JvmStatic
    fun getAnalyticsRest(streamConfig: StreamConfig): EventService {
        return ANALYTICS_REST_SERVICE_CACHE[streamConfig.getDestinationEventService()]!!
    }

    operator fun <T> get(wiki: WikiSite, baseUrl: String?, service: Class<T>?): T {
        val r = createRetrofit(wiki, baseUrl.orEmpty().ifEmpty { wiki.url() + "/" })
        return r.create(service)
    }

    private fun getBasePath(wiki: WikiSite): String {
        return Prefs.getMediaWikiBaseUrl().ifEmpty { wiki.url() + "/" }
    }

    fun getRestBasePath(wiki: WikiSite): String {
        var path = if (Prefs.getRestbaseUriFormat().isEmpty()) wiki.url() + "/" + RestService.REST_API_PREFIX
        else String.format(Prefs.getRestbaseUriFormat(), "https", wiki.authority())
        if (!path.endsWith("/")) {
            path += "/"
        }
        return path
    }

    private fun createRetrofit(wiki: WikiSite?, baseUrl: String): Retrofit {
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(OkHttpConnectionFactory.client.newBuilder().addInterceptor(LanguageVariantHeaderInterceptor(wiki)).build())
            .addCallAdapterFactory(RxJava3CallAdapterFactory.create())
            .addConverterFactory(MoshiConverterFactory.create(MoshiUtil.getDefaultMoshi()))
            .build()
    }

    private class LanguageVariantHeaderInterceptor(private val wiki: WikiSite?) : Interceptor {
        @Throws(IOException::class)
        override fun intercept(chain: Interceptor.Chain): Response {
            var request = chain.request()

            // TODO: remove when the https://phabricator.wikimedia.org/T271145 is resolved.
            if (!request.url.encodedPath.contains("/page/related")) {
                request = request.newBuilder()
                    .header("Accept-Language", WikipediaApp.getInstance().getAcceptLanguage(wiki))
                    .build()
            }
            return chain.proceed(request)
        }
    }
}
