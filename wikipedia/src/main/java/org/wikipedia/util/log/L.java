package org.wikipedia.util.log;

import android.util.Log;

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
        LEVEL_V.log(null, t);
    }

    public static void d(Throwable t) {
        LEVEL_D.log(null, t);
    }

    public static void i(Throwable t) {
        LEVEL_I.log(null, t);
    }

    public static void w(Throwable t) {
        LEVEL_W.log(null, t);
    }

    public static void e(Throwable t) {
        LEVEL_E.log(null, t);
    }

    public static void v(CharSequence msg, Exception e) {
        LEVEL_V.log(msg, e);
    }

    public static void d(CharSequence msg, Exception e) {
        LEVEL_D.log(msg, e);
    }

    public static void i(CharSequence msg, Exception e) {
        LEVEL_I.log(msg, e);
    }

    public static void w(CharSequence msg, Exception e) {
        LEVEL_W.log(msg, e);
    }

    public static void e(CharSequence msg, Exception e) {
        LEVEL_E.log(msg, e);
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

    private L() {
    }
}