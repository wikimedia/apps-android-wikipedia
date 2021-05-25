package org.wikipedia.util

import android.content.Context
import org.json.JSONException
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.createaccount.CreateAccountException
import org.wikipedia.dataclient.mwapi.MwException
import org.wikipedia.dataclient.mwapi.MwServiceError
import org.wikipedia.dataclient.okhttp.HttpStatusException
import org.wikipedia.login.LoginClient.LoginFailedException
import org.wikipedia.page.PageTitle
import org.wikipedia.staticdata.UserAliasData
import java.lang.Exception
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

    @JvmStatic
    fun getAppError(context: Context, e: Throwable): AppError {
        val inner = getInnermostThrowable(e)
        // look at what kind of exception it is...
        return if (isNetworkError(e)) {
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
    }

    @JvmStatic
    fun isOffline(caught: Throwable?): Boolean {
        return caught is UnknownHostException || caught is SocketException
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
    fun isEmptyException(caught: Throwable): Boolean {
        return caught is EmptyException
    }

    @JvmStatic
    fun isNetworkError(e: Throwable): Boolean {
        return throwableContainsException(e, UnknownHostException::class.java) ||
                throwableContainsException(e, TimeoutException::class.java) ||
                throwableContainsException(e, SSLException::class.java)
    }

    @JvmStatic
    fun parseBlockedError(info: MwServiceError.BlockInfo): String {
        val app = WikipediaApp.getInstance()
        var retStr = WikipediaApp.getInstance().getString(R.string.error_blocked_title)
        val title = PageTitle(UserAliasData.valueFor(app.appOrSystemLanguageCode), info.blockedBy, app.wikiSite)

        if (info.blockedBy.isNotEmpty()) {
            retStr += "<br />" + app.getString(R.string.error_blocked_by, title.text, title.mobileUri)
        }
        if (info.blockReason.isNotEmpty()) {
            retStr += "<br />" + app.getString(R.string.error_blocked_reason, info.blockReason)
        }
        retStr += "<br />" + app.getString(R.string.error_blocked_start, parseBlockedDate(info.blockTimeStamp))
        retStr += "<br />" + app.getString(R.string.error_blocked_expiry, parseBlockedDate(info.blockExpiry))
        retStr += "<br />" + app.getString(R.string.error_blocked_id, info.blockId.toString())
        if (info.blockedBy.isNotEmpty()) {
            retStr += "<br /><br />" + app.getString(R.string.error_blocked_footer, title.text, title.mobileUri)
        } else {
            retStr += "<br /><br />" + app.getString(R.string.error_blocked_footer_no_blocker)
        }
        return retStr
    }

    private fun parseBlockedDate(dateStr: String): String {
        try {
            return DateUtil.iso8601DateParse(dateStr).toString()
        } catch (e: Exception) {}
        return dateStr
    }

    class EmptyException : Exception()
    class AppError(val error: String, val detail: String?)
}
