package org.wikipedia.dataclient.okhttp

import androidx.annotation.NonNull
import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.Response
import org.wikipedia.dataclient.okhttp.util.HttpUrlUtil
import org.wikipedia.settings.RbSwitch
import java.io.IOException

class StatusResponseInterceptor(private val cb: RbSwitch) : Interceptor {

    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val url = chain.request().url()

        val rsp: Response
        try {
            rsp = chain.proceed(chain.request())
        } catch (t: Throwable) {
            failure(url, t)
            throw t
        }

        success(url)

        return rsp
    }

    private fun success(url: HttpUrl) {
        if (HttpUrlUtil.isMobileView(url)) {
            cb.onMwSuccess()
        }
    }

    private fun failure(url: HttpUrl, @NonNull t: Throwable?) {
        if (HttpUrlUtil.isRestBase(url)) {
            cb.onRbRequestFailed(t)
        }
    }
}
