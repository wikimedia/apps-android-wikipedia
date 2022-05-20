package org.wikipedia.util.log

import android.util.Log
import org.wikipedia.BuildConfig
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
    fun v(msg: String) {
        LEVEL_V.log(msg, null)
    }

    @JvmStatic
    fun d(msg: String) {
        LEVEL_D.log(msg, null)
    }

    @JvmStatic
    fun i(msg: String) {
        LEVEL_I.log(msg, null)
    }

    @JvmStatic
    fun w(msg: String) {
        LEVEL_W.log(msg, null)
    }

    @JvmStatic
    fun e(msg: String) {
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

    fun v(msg: String, t: Throwable?) {
        LEVEL_V.log(msg, t)
    }

    @JvmStatic
    fun d(msg: String, t: Throwable?) {
        LEVEL_D.log(msg, t)
    }

    fun i(msg: String, t: Throwable?) {
        LEVEL_I.log(msg, t)
    }

    @JvmStatic
    fun w(msg: String, t: Throwable?) {
        LEVEL_W.log(msg, t)
    }

    @JvmStatic
    fun e(msg: String, t: Throwable?) {
        LEVEL_E.log(msg, t)
    }

    @JvmStatic
    fun logRemoteErrorIfProd(t: Throwable) {
        if (ReleaseUtil.isProdRelease) {
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
        if (!ReleaseUtil.isPreBetaRelease) {
            WikipediaApp.instance.logCrashManually(t)
        }
    }

    private abstract class LogLevel {
        abstract fun logLevel(tag: String?, msg: String?, t: Throwable?)
        fun log(msg: String, t: Throwable?) {
            if (ReleaseUtil.isDevRelease) {
                val element = Thread.currentThread().stackTrace[STACK_INDEX]
                logLevel(element.className, stackTraceElementToMessagePrefix(element) + msg, t)
            } else {
                logLevel(BuildConfig.APPLICATION_ID, msg, t)
            }
        }

        private fun stackTraceElementToMessagePrefix(element: StackTraceElement): String {
            return element.methodName + "():" + element.lineNumber + ": "
        }

        companion object {
            private const val STACK_INDEX = 4
        }
    }
}
