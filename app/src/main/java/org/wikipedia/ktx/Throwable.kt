@file:JvmName("ThrowableUtil")

package org.wikipedia.ktx

import org.wikipedia.dataclient.okhttp.HttpStatusException
import java.lang.Exception
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeoutException
import javax.net.ssl.SSLException

val Throwable.innermostThrowable: Throwable
    get() {
        var t = this
        while (t.cause != null) {
            t = t.cause!!
        }
        return t
    }

val Throwable?.is404 get() = this is HttpStatusException && code() == 404

val Throwable?.isOffline get() = this is UnknownHostException || this is SocketException

val Throwable?.isTimeout get() = this is SocketTimeoutException

val Throwable.isNetworkError: Boolean
    get() = containsException<UnknownHostException>() || containsException<TimeoutException>() || containsException<SSLException>()

fun <E : Exception> Throwable.containsException(clazz: Class<E>): Boolean {
    var t: Throwable? = this
    while (t != null) {
        if (clazz.isInstance(t)) {
            return true
        }
        t = t.cause
    }
    return false
}

inline fun <reified E : Exception> Throwable.containsException() = containsException(E::class.java)
