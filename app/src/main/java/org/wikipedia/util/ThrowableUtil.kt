package org.wikipedia.util

import android.content.Context
import org.json.JSONException
import org.wikipedia.R
import org.wikipedia.createaccount.CreateAccountException
import org.wikipedia.dataclient.mwapi.MwException
import org.wikipedia.dataclient.okhttp.HttpStatusException
import org.wikipedia.login.LoginClient.LoginFailedException
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeoutException
import javax.net.ssl.SSLException

object ThrowableUtil {
    // TODO: replace with Apache Commons Lang ExceptionUtils.
    @JvmStatic
    fun getInnermostThrowable(e: Throwable): Throwable {
        var t = e
        while (t.cause != null) {
            t = t.cause!!
        }
        return t
    }

    // TODO: replace with Apache Commons Lang ExceptionUtils.
    private fun throwableContainsException(e: Throwable, exClass: Class<*>): Boolean {
        var t: Throwable? = e
        while (t != null) {
            if (exClass.isInstance(t)) {
                return true
            }
            t = t.cause
        }
        return false
    }

    /**
     * DEPRECATED: This is a rarely-used function intended to sift through server error responses
     * and pass through the relevant bits along with standardized strings in certain cases.
     *
     * Getting the handful of canned strings depends on processing contexts that might be null by
     * the time we make it here.  Further, we're moving away from using raw server messages in favor
     * of statically defined XML error views, which are safer.  This should no longer be used.
     */
    @JvmStatic
    @Deprecated("")
    fun getAppError(context: Context, e: Throwable): AppError {
        val inner = getInnermostThrowable(e)
        val result: AppError
        // look at what kind of exception it is...
        result = if (isNetworkError(e)) {
            AppError(context.getString(R.string.error_network_error),
                    context.getString(R.string.format_error_server_message,
                            inner.localizedMessage))
        } else if (e is HttpStatusException) {
            AppError(e.message!!, e.code().toString())
        } else if (inner is LoginFailedException || inner is CreateAccountException ||
                inner is MwException) {
            AppError(inner.localizedMessage!!, "")
        } else if (throwableContainsException(e, JSONException::class.java)) {
            AppError(context.getString(R.string.error_response_malformed),
                    inner.localizedMessage)
        } else {
            // everything else has fallen through, so just treat it as an "unknown" error
            AppError(context.getString(R.string.error_unknown),
                    inner.localizedMessage)
        }
        return result
    }

    @JvmStatic
    fun isOffline(caught: Throwable?): Boolean {
        return (caught is UnknownHostException ||
                caught is SocketException)
    }

    @JvmStatic
    fun isTimeout(caught: Throwable?): Boolean {
        return caught is SocketTimeoutException
    }

    @JvmStatic
    fun is404(caught: Throwable): Boolean {
        return caught is HttpStatusException && caught.code() == 404
    }

    @JvmStatic
    fun isNetworkError(e: Throwable): Boolean {
        return (throwableContainsException(e, UnknownHostException::class.java) ||
                throwableContainsException(e, TimeoutException::class.java) ||
                throwableContainsException(e, SSLException::class.java))
    }

    @Deprecated("")
    class AppError(val error: String, val detail: String?)
}
