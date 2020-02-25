package org.wikipedia.dataclient.okhttp

import okhttp3.Cache
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.wikipedia.WikipediaApp
import org.wikipedia.dataclient.SharedPreferenceCookieManager
import org.wikipedia.settings.Prefs
import java.io.File

object OkHttpConnectionFactory {
    public const val CACHE_DIR_NAME = "okhttp-cache"
    private const val NET_CACHE_SIZE = (64 * 1024 * 1024).toLong()
    private val NET_CACHE = Cache(File(WikipediaApp.getInstance().cacheDir, CACHE_DIR_NAME), NET_CACHE_SIZE)
    @JvmStatic val client = createClient()

    private fun createClient(): OkHttpClient {
        return OkHttpClient.Builder()
                .cookieJar(SharedPreferenceCookieManager.getInstance())
                .cache(NET_CACHE)
                .addInterceptor(HttpLoggingInterceptor().setLevel(Prefs.getRetrofitLogLevel()))
                .addInterceptor(UnsuccessfulResponseInterceptor())
                .addNetworkInterceptor(CacheControlInterceptor())
                .addInterceptor(CommonHeaderRequestInterceptor())
                .addInterceptor(DefaultMaxStaleRequestInterceptor())
                .addInterceptor(OfflineCacheInterceptor())
                .addInterceptor(TestStubInterceptor())
                .build()
    }
}
