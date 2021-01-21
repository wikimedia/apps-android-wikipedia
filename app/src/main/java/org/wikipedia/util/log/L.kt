package org.wikipedia.util.log

import android.util.Log
import org.wikipedia.WikipediaApp
import org.wikipedia.util.ReleaseUtil

/** Logging utility like [Log] but with implied tags.  */
object L {
    private val LEVEL_V: LogLevel = object : LogLevel() {
        override fun logLevel(tag: String?, msg: String?, t: Throwable?) {
            Log.v(tag, msg, t)
        }
    }
    private val LEVEL_D: LogLevel = object : LogLevel() {
        override fun logLevel(tag: String?, msg: String?, t: Throwable?) {
            Log.d(tag, msg, t)
        }
    }
    private val LEVEL_I: LogLevel = object : LogLevel() {
        override fun logLevel(tag: String?, msg: String?, t: Throwable?) {
            Log.i(tag, msg, t)
        }
    }
    private val LEVEL_W: LogLevel = object : LogLevel() {
        override fun logLevel(tag: String?, msg: String?, t: Throwable?) {
            Log.w(tag, msg, t)
        }
    }
    private val LEVEL_E: LogLevel = object : LogLevel() {
        override fun logLevel(tag: String?, msg: String?, t: Throwable?) {
            Log.e(tag, msg, t)
        }
    }

    @JvmStatic
    fun v(msg: CharSequence) {
        LEVEL_V.log(msg, null)
    }

    @JvmStatic
    fun d(msg: CharSequence) {
        LEVEL_D.log(msg, null)
    }

    @JvmStatic
    fun i(msg: CharSequence) {
        LEVEL_I.log(msg, null)
    }

    @JvmStatic
    fun w(msg: CharSequence) {
        LEVEL_W.log(msg, null)
    }

    @JvmStatic
    fun e(msg: CharSequence) {
        LEVEL_E.log(msg, null)
    }

    @JvmStatic
    fun v(t: Throwable?) {
        LEVEL_V.log("", t)
    }

    @JvmStatic
    fun d(t: Throwable?) {
        LEVEL_D.log("", t)
    }

    fun i(t: Throwable?) {
        LEVEL_I.log("", t)
    }

    @JvmStatic
    fun w(t: Throwable?) {
        LEVEL_W.log("", t)
    }

    @JvmStatic
    fun e(t: Throwable?) {
        LEVEL_E.log("", t)
    }

    fun v(msg: CharSequence, t: Throwable?) {
        LEVEL_V.log(msg, t)
    }

    @JvmStatic
    fun d(msg: CharSequence, t: Throwable?) {
        LEVEL_D.log(msg, t)
    }

    fun i(msg: CharSequence, t: Throwable?) {
        LEVEL_I.log(msg, t)
    }

    @JvmStatic
    fun w(msg: CharSequence, t: Throwable?) {
        LEVEL_W.log(msg, t)
    }

    @JvmStatic
    fun e(msg: CharSequence, t: Throwable?) {
        LEVEL_E.log(msg, t)
    }

    @JvmStatic
    fun logRemoteErrorIfProd(t: Throwable) {
        if (ReleaseUtil.isProdRelease()) {
            logRemoteError(t)
        } else {
            throw RuntimeException(t)
        }
    }

    // Favor logRemoteErrorIfProd(). If it's worth consuming bandwidth and developer hours, it's
    // worth crashing on everything but prod
    @JvmStatic
    fun logRemoteError(t: Throwable) {
        LEVEL_E.log("", t)
        if (!ReleaseUtil.isPreBetaRelease()) {
            WikipediaApp.getInstance().logCrashManually(t)
        }
    }

    private abstract class LogLevel {
        abstract fun logLevel(tag: String?, msg: String?, t: Throwable?)
        fun log(msg: CharSequence, t: Throwable?) {
            val element = Thread.currentThread().stackTrace[STACK_INDEX]
            logLevel(element.className, stackTraceElementToMessagePrefix(element) + msg, t)
        }

        private fun stackTraceElementToMessagePrefix(element: StackTraceElement): String {
            return element.methodName + "():" + element.lineNumber + ": "
        }

        companion object {
            private const val STACK_INDEX = 4
        }
    }
}
