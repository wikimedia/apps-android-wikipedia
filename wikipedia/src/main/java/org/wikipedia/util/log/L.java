package org.wikipedia.util.log;

import android.util.Log;

/** Logging utility like {@link Log} but with implied tags. */
public final class L {
    private static final LogLevel LEVEL_V = new LogLevel() {
        @Override
        public void logLevel(String tag, String msg, Exception e) {
            Log.v(tag, msg, e);
        }
    };

    private static final LogLevel LEVEL_D = new LogLevel() {
        @Override
        public void logLevel(String tag, String msg, Exception e) {
            Log.d(tag, msg, e);
        }
    };

    private static final LogLevel LEVEL_I = new LogLevel() {
        @Override
        public void logLevel(String tag, String msg, Exception e) {
            Log.i(tag, msg, e);
        }
    };

    private static final LogLevel LEVEL_W = new LogLevel() {
        @Override
        public void logLevel(String tag, String msg, Exception e) {
            Log.w(tag, msg, e);
        }
    };

    private static final LogLevel LEVEL_E = new LogLevel() {
        @Override
        public void logLevel(String tag, String msg, Exception e) {
            Log.e(tag, msg, e);
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

    public static void v(Exception e) {
        LEVEL_V.log(null, e);
    }

    public static void d(Exception e) {
        LEVEL_D.log(null, e);
    }

    public static void i(Exception e) {
        LEVEL_I.log(null, e);
    }

    public static void w(Exception e) {
        LEVEL_W.log(null, e);
    }

    public static void e(Exception e) {
        LEVEL_E.log(null, e);
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

        public abstract void logLevel(String tag, String msg, Exception e);

        public final void log(CharSequence msg, Exception e) {
            StackTraceElement element = Thread.currentThread().getStackTrace()[STACK_INDEX];
            logLevel(element.getClassName(), stackTraceElementToMessagePrefix(element) + msg, e);
        }

        private String stackTraceElementToMessagePrefix(StackTraceElement element) {
            return element.getMethodName() + "():" + element.getLineNumber() + ": ";
        }
    }

    private L() {
    }
}