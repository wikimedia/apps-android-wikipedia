package org.wikipedia.dataclient.okhttp

import okhttp3.Cache
import okhttp3.CacheDelegate
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.wikipedia.WikipediaApp
import org.wikipedia.dataclient.SharedPreferenceCookieManager
import org.wikipedia.settings.Prefs
import org.wikipedia.settings.RbSwitch
import java.io.File

object OkHttpConnectionFactory {
    private const val CACHE_DIR_NAME = "okhttp-cache"
    private const val NET_CACHE_SIZE = (64 * 1024 * 1024).toLong()
    private const val SAVED_PAGE_CACHE_SIZE = NET_CACHE_SIZE * 1024
    private val NET_CACHE = Cache(File(WikipediaApp.getInstance().cacheDir, CACHE_DIR_NAME), NET_CACHE_SIZE)

    @JvmField val SAVE_CACHE = CacheDelegate(Cache(File(WikipediaApp.getInstance().filesDir, CACHE_DIR_NAME), SAVED_PAGE_CACHE_SIZE))
    @JvmStatic val client = createClient()

    private fun createClient(): OkHttpClient {
        return OkHttpClient.Builder()
                .cookieJar(SharedPreferenceCookieManager.getInstance())
                .cache(NET_CACHE)
                .addInterceptor(HttpLoggingInterceptor().setLevel(Prefs.getRetrofitLogLevel()))
                .addInterceptor(UnsuccessfulResponseInterceptor())
                .addInterceptor(StatusResponseInterceptor(RbSwitch.INSTANCE))
                .addNetworkInterceptor(StripMustRevalidateResponseInterceptor())
                .addInterceptor(CommonHeaderRequestInterceptor())
                .addInterceptor(DefaultMaxStaleRequestInterceptor())
                .addInterceptor(OfflineCacheInterceptor(SAVE_CACHE))
                .addInterceptor(TestStubInterceptor())
                .build()
    }
}
