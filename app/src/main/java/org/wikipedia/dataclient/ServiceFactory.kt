package org.wikipedia.dataclient

import androidx.collection.LruCache
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import org.apache.commons.lang3.StringUtils
import org.wikipedia.WikipediaApp
import org.wikipedia.analytics.eventplatform.EventService
import org.wikipedia.analytics.eventplatform.StreamConfig
import org.wikipedia.dataclient.okhttp.OkHttpConnectionFactory.client
import org.wikipedia.json.GsonUtil
import org.wikipedia.settings.Prefs
import retrofit2.Retrofit
import retrofit2.adapter.rxjava3.RxJava3CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException

object ServiceFactory {

    private const val SERVICE_CACHE_SIZE = 8
    private val SERVICE_CACHE = LruCache<Long, Service?>(SERVICE_CACHE_SIZE)
    private val REST_SERVICE_CACHE = LruCache<Long, RestService?>(SERVICE_CACHE_SIZE)
    private val CORE_REST_SERVICE_CACHE = LruCache<Long, CoreRestService?>(SERVICE_CACHE_SIZE)
    private val ANALYTICS_REST_SERVICE_CACHE = LruCache<String, EventService?>(SERVICE_CACHE_SIZE)

    @JvmStatic
    fun get(wiki: WikiSite): Service {
        val hashCode = wiki.hashCode().toLong()
        if (SERVICE_CACHE[hashCode] != null) {
            return SERVICE_CACHE[hashCode]!!
        }
        val r = createRetrofit(wiki, getBasePath(wiki))
        val s = r.create(Service::class.java)
        SERVICE_CACHE.put(hashCode, s)
        return s
    }

    fun getRest(wiki: WikiSite): RestService {
        val hashCode = wiki.hashCode().toLong()
        if (REST_SERVICE_CACHE[hashCode] != null) {
            return REST_SERVICE_CACHE[hashCode]!!
        }
        val r = createRetrofit(wiki, getRestBasePath(wiki))
        val s = r.create(RestService::class.java)
        REST_SERVICE_CACHE.put(hashCode, s)
        return s
    }

    fun getCoreRest(wiki: WikiSite): CoreRestService {
        val hashCode = wiki.hashCode().toLong()
        if (CORE_REST_SERVICE_CACHE[hashCode] != null) {
            return CORE_REST_SERVICE_CACHE[hashCode]!!
        }
        val r = createRetrofit(wiki, wiki.url() + "/" + CoreRestService.CORE_REST_API_PREFIX)
        val s = r.create(CoreRestService::class.java)
        CORE_REST_SERVICE_CACHE.put(hashCode, s)
        return s
    }

    @JvmStatic
    fun getAnalyticsRest(streamConfig: StreamConfig): EventService {
        val destinationEventService = streamConfig.destinationEventService
        if (ANALYTICS_REST_SERVICE_CACHE[destinationEventService.id] != null) {
            return ANALYTICS_REST_SERVICE_CACHE[destinationEventService.id]!!
        }
        val intakeBaseUriOverride = Prefs.getEventPlatformIntakeUriOverride()
        val r = createRetrofit(null, StringUtils.defaultString(intakeBaseUriOverride, destinationEventService.baseUri))
        val s = r.create(EventService::class.java)
        ANALYTICS_REST_SERVICE_CACHE.put(destinationEventService.id, s)
        return s
    }

    operator fun <T> get(wiki: WikiSite, baseUrl: String?, service: Class<T>?): T {
        val r = createRetrofit(wiki, (if (baseUrl.isNullOrEmpty()) wiki.url() + "/" else baseUrl))
        return r.create(service)
    }

    private fun getBasePath(wiki: WikiSite): String {
        return if (Prefs.getMediaWikiBaseUrl().isEmpty()) wiki.url() + "/" else Prefs.getMediaWikiBaseUrl()
    }

    fun getRestBasePath(wiki: WikiSite): String {
        var path =
            if (Prefs.getRestbaseUriFormat().isEmpty()) wiki.url() + "/" + RestService.REST_API_PREFIX else String.format(
                Prefs.getRestbaseUriFormat(),
                "https",
                wiki.authority())
        if (!path.endsWith("/")) {
            path += "/"
        }
        return path
    }

    private fun createRetrofit(wiki: WikiSite?, baseUrl: String): Retrofit {
        val okHttpClientBuilder = client.newBuilder()
        if (wiki != null) {
            okHttpClientBuilder.addInterceptor(LanguageVariantHeaderInterceptor(wiki))
        }
        return Retrofit.Builder()
            .client(okHttpClientBuilder.build())
            .baseUrl(baseUrl)
            .client(client.newBuilder().addInterceptor(LanguageVariantHeaderInterceptor(
                wiki!!)).build())
            .addCallAdapterFactory(RxJava3CallAdapterFactory.create())
            .addConverterFactory(GsonConverterFactory.create(GsonUtil.getDefaultGson()))
            .build()
    }

    private class LanguageVariantHeaderInterceptor(private val wiki: WikiSite) :
        Interceptor {
        @Throws(IOException::class)
        override fun intercept(chain: Interceptor.Chain): Response {
            var request: Request = chain.request()

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
