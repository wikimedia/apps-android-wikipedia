package org.wikipedia.util.log;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import org.wikipedia.util.ReleaseUtil;

/** Logging utility like {@link Log} but with implied tags. */
public final class L {
    private static final LogLevel LEVEL_V = new LogLevel() {
        @Override
        public void logLevel(String tag, String msg, Throwable t) {
            Log.v(tag, msg, t);
        }
    };

    private static final LogLevel LEVEL_D = new LogLevel() {
        @Override
        public void logLevel(String tag, String msg, Throwable t) {
            Log.d(tag, msg, t);
        }
    };

    private static final LogLevel LEVEL_I = new LogLevel() {
        @Override
        public void logLevel(String tag, String msg, Throwable t) {
            Log.i(tag, msg, t);
        }
    };

    private static final LogLevel LEVEL_W = new LogLevel() {
        @Override
        public void logLevel(String tag, String msg, Throwable t) {
            Log.w(tag, msg, t);
        }
    };

    private static final LogLevel LEVEL_E = new LogLevel() {
        @Override
        public void logLevel(String tag, String msg, Throwable t) {
            Log.e(tag, msg, t);
        }
    };

    @Nullable private static RemoteExceptionLogger REMOTE_EXCEPTION_LOGGER;

    public static void v(CharSequence msg) {
        LEVEL_V.log(msg, null);
    }

    public static void d(CharSequence msg) {
        LEVEL_D.log(msg, null);
    }

    public static void i(CharSequence msg) {
        LEVEL_I.log(msg, null);
    }

    public static void w(CharSequence msg) {
        LEVEL_W.log(msg, null);
    }

    public static void e(CharSequence msg) {
        LEVEL_E.log(msg, null);
    }

    public static void v(Throwable t) {
        LEVEL_V.log("", t);
    }

    public static void d(Throwable t) {
        LEVEL_D.log("", t);
    }

    public static void i(Throwable t) {
        LEVEL_I.log("", t);
    }

    public static void w(Throwable t) {
        LEVEL_W.log("", t);
    }

    public static void e(Throwable t) {
        LEVEL_E.log("", t);
    }

    public static void v(CharSequence msg, Throwable t) {
        LEVEL_V.log(msg, t);
    }

    public static void d(CharSequence msg, Throwable t) {
        LEVEL_D.log(msg, t);
    }

    public static void i(CharSequence msg, Throwable t) {
        LEVEL_I.log(msg, t);
    }

    public static void w(CharSequence msg, Throwable t) {
        LEVEL_W.log(msg, t);
    }

    public static void e(CharSequence msg, Throwable t) {
        LEVEL_E.log(msg, t);
    }

    public static void logRemoteErrorIfProd(@NonNull Throwable t) {
        if (ReleaseUtil.isProdRelease()) {
            logRemoteError(t);
        } else {
            throw new RuntimeException(t);
        }
    }

    public static void setRemoteLogger(@Nullable RemoteExceptionLogger logger) {
        REMOTE_EXCEPTION_LOGGER = logger;
    }

    // Favor logRemoteErrorIfProd(). If it's worth consuming bandwidth and developer hours, it's
    // worth crashing on everything but prod
    public static void logRemoteError(@NonNull Throwable t) {
        LEVEL_E.log("", t);
        if (REMOTE_EXCEPTION_LOGGER != null) {
            REMOTE_EXCEPTION_LOGGER.log(t);
        }
    }

    private abstract static class LogLevel {
        private static final int STACK_INDEX = 4;

        public abstract void logLevel(String tag, String msg, Throwable t);

        public final void log(CharSequence msg, Throwable t) {
            StackTraceElement element = Thread.currentThread().getStackTrace()[STACK_INDEX];
            logLevel(element.getClassName(), stackTraceElementToMessagePrefix(element) + msg, t);
        }

        private String stackTraceElementToMessagePrefix(StackTraceElement element) {
            return element.getMethodName() + "():" + element.getLineNumber() + ": ";
        }
    }

    private L() { }
}
