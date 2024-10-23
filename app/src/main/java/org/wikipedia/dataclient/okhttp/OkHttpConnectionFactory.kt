package org.wikipedia.dataclient.okhttp

import android.os.Build
import okhttp3.Cache
import okhttp3.CacheControl
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import okhttp3.tls.HandshakeCertificates
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.dataclient.SharedPreferenceCookieManager
import org.wikipedia.settings.Prefs
import java.io.File
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit

object OkHttpConnectionFactory {
    val CACHE_CONTROL_FORCE_NETWORK = CacheControl.Builder().maxAge(0, TimeUnit.SECONDS).build()
    val CACHE_CONTROL_MAX_STALE = CacheControl.Builder().maxStale(Int.MAX_VALUE, TimeUnit.SECONDS).build()
    val CACHE_CONTROL_NONE = CacheControl.Builder().build()

    private const val CACHE_DIR_NAME = "okhttp-cache"
    private const val NET_CACHE_SIZE = (64 * 1024 * 1024).toLong()
    private val NET_CACHE = Cache(File(WikipediaApp.instance.cacheDir, CACHE_DIR_NAME), NET_CACHE_SIZE)
    val client = createClient()

    private fun createClient(): OkHttpClient {
        val builder = OkHttpClient.Builder()
                .cookieJar(SharedPreferenceCookieManager.instance)
                .cache(NET_CACHE)
                .readTimeout(20, TimeUnit.SECONDS)
                .addInterceptor(UnsuccessfulResponseInterceptor())
                .addNetworkInterceptor(CacheControlInterceptor())
                .addInterceptor(CommonHeaderRequestInterceptor())
                .addInterceptor(DefaultMaxStaleRequestInterceptor())
                .addInterceptor(OfflineCacheInterceptor())
                .addInterceptor(TestStubInterceptor())
                .addInterceptor(TitleEncodeInterceptor())
                .addInterceptor(HttpLoggingInterceptor().setLevel(Prefs.retrofitLogLevel))

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            val certFactory = CertificateFactory.getInstance("X.509")
            val certificates = HandshakeCertificates.Builder()
                .addPlatformTrustedCertificates()
                .addTrustedCertificate(certFactory.generateCertificate(WikipediaApp.instance.resources.openRawResource(R.raw.isrg_root_x1)) as X509Certificate)
                .addTrustedCertificate(certFactory.generateCertificate(WikipediaApp.instance.resources.openRawResource(R.raw.isrg_root_x2)) as X509Certificate)
                .build()
            builder.sslSocketFactory(certificates.sslSocketFactory(), certificates.trustManager)
        }

        return builder.build()
    }
}
