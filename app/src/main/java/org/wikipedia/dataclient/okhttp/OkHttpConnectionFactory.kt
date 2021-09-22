package org.wikipedia.dataclient.okhttp

import okhttp3.Cache
import okhttp3.CacheControl
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.wikipedia.WikipediaApp
import org.wikipedia.dataclient.SharedPreferenceCookieManager
import org.wikipedia.settings.Prefs
import java.io.File
import java.util.concurrent.TimeUnit

object OkHttpConnectionFactory {
    @JvmField val CACHE_CONTROL_FORCE_NETWORK = CacheControl.Builder().maxAge(0, TimeUnit.SECONDS).build()
    @JvmField val CACHE_CONTROL_MAX_STALE = CacheControl.Builder().maxStale(Int.MAX_VALUE, TimeUnit.SECONDS).build()
    @JvmField val CACHE_CONTROL_NONE = CacheControl.Builder().build()

    private const val CACHE_DIR_NAME = "okhttp-cache"
    private const val NET_CACHE_SIZE = (64 * 1024 * 1024).toLong()
    private val NET_CACHE = Cache(File(WikipediaApp.getInstance().cacheDir, CACHE_DIR_NAME), NET_CACHE_SIZE)
    @JvmStatic val client = createClient()

    private fun createClient(): OkHttpClient {
        return OkHttpClient.Builder()
                .cookieJar(SharedPreferenceCookieManager.instance)
                .cache(NET_CACHE)
                .readTimeout(20, TimeUnit.SECONDS)
                .addInterceptor(HttpLoggingInterceptor().setLevel(Prefs.retrofitLogLevel))
                .addInterceptor(UnsuccessfulResponseInterceptor())
                .addNetworkInterceptor(CacheControlInterceptor())
                .addInterceptor(CommonHeaderRequestInterceptor())
                .addInterceptor(DefaultMaxStaleRequestInterceptor())
                .addInterceptor(OfflineCacheInterceptor())
                .addInterceptor(TestStubInterceptor())
                .addInterceptor(TitleEncodeInterceptor())
                .build()
    }
}
