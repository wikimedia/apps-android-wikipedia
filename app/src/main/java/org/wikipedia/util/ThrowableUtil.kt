package org.wikipedia.util

import android.content.Context
import androidx.annotation.WorkerThread
import io.reactivex.rxjava3.core.Observable
import org.json.JSONException
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.createaccount.CreateAccountException
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.mwapi.MwException
import org.wikipedia.dataclient.mwapi.MwServiceError
import org.wikipedia.dataclient.okhttp.HttpStatusException
import org.wikipedia.login.LoginClient.LoginFailedException
import org.wikipedia.util.log.L
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeoutException
import javax.net.ssl.SSLException

object ThrowableUtil {
    // TODO: replace with Apache Commons Lang ExceptionUtils.
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
            AppError(e.message!!, e.code.toString())
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
        return caught is HttpStatusException && caught.code == 404
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
    @WorkerThread
    fun getBlockMessageHtml(blockInfo: MwServiceError.BlockInfo): String {
        var html = ""
        Observable.zip(ServiceFactory.get(WikipediaApp.getInstance().wikiSite).userInfo,
            ServiceFactory.get(WikipediaApp.getInstance().wikiSite).parsePage("MediaWiki:Blockedtext"),
            ServiceFactory.get(WikipediaApp.getInstance().wikiSite).parseText(blockInfo.blockReason),
            { userInfoResponse, blockedParseResponse, reasonParseResponse ->
                parseBlockedError(blockedParseResponse.text, blockInfo,
                    reasonParseResponse.text, userInfoResponse.query?.userInfo()!!.name)
            }
        ).blockingSubscribe({ html = it }) { L.e(it) }
        return html
    }

    @JvmStatic
    fun parseBlockedError(template: String, info: MwServiceError.BlockInfo, reason: String, userName: String): String {
        return template.replace("$1", "<a href=\"${StringUtil.userPageTitleFromName(info.blockedBy, WikipediaApp.getInstance().wikiSite).mobileUri}\">${info.blockedBy}</a>")
            .replace("$2", reason)
            .replace("$3", "") // IP address of user (TODO: somehow get from API?)
            .replace("$4", "") // unknown parameter (unused?)
            .replace("$5", info.blockId.toString())
            .replace("$6", parseBlockedDate(info.blockExpiry))
            .replace("$7", "<a href=\"${StringUtil.userPageTitleFromName(userName, WikipediaApp.getInstance().wikiSite).mobileUri}\">$userName</a>")
            .replace("$8", parseBlockedDate(info.blockTimeStamp))
    }

    private fun parseBlockedDate(dateStr: String): String {
        try {
            return Instant.parse(dateStr).atZone(ZoneOffset.UTC)
                .format(DateTimeFormatter.ofPattern("EEE MMM dd HH:mm:ss zzz yyyy"))
        } catch (e: Exception) {}
        return dateStr
    }

    class EmptyException : Exception()
    class AppError(val error: String, val detail: String?)
}
