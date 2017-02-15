package org.wikipedia.crash.hockeyapp;

import android.support.annotation.Nullable;
import android.util.Log;

import net.hockeyapp.android.Constants;
import net.hockeyapp.android.ExceptionHandler;

/** Wrapper around {@link ExceptionHandler} that calls {@link HockeyAppCrashListener#onCrash()}. */
/*package*/ class HockeyAppExceptionHandler extends ExceptionHandler {
    private final boolean ignoreDefaultHandler;
    private final Thread.UncaughtExceptionHandler defaultExceptionHandler;
    @Nullable private HockeyAppCrashListener listener;

    HockeyAppExceptionHandler(@Nullable HockeyAppCrashListener listener,
                              boolean ignoreDefaultHandler) {
        this(Thread.getDefaultUncaughtExceptionHandler(), listener, ignoreDefaultHandler);
    }

    HockeyAppExceptionHandler(Thread.UncaughtExceptionHandler defaultExceptionHandler,
                              @Nullable HockeyAppCrashListener listener,
                              boolean ignoreDefaultHandler) {
        super(defaultExceptionHandler, listener, ignoreDefaultHandler);
        this.defaultExceptionHandler = defaultExceptionHandler;
        this.listener = listener;
        this.ignoreDefaultHandler = ignoreDefaultHandler;
    }

    public void setListener(@Nullable HockeyAppCrashListener listener) {
        super.setListener(listener);
        this.listener = listener;
    }

    @Override
    public void uncaughtException(Thread thread, Throwable exception) {
        Log.e(getClass().getName(), "", exception);
        if (Constants.FILES_PATH == null) {
            // If the files path is null, the exception can't be stored
            // Always call the default handler instead
            defaultExceptionHandler.uncaughtException(thread, exception);
        } else {
            saveException(exception, null, listener);

            if (!ignoreDefaultHandler) {
                defaultExceptionHandler.uncaughtException(thread, exception);
            } else if (listener != null) {
                listener.onCrash();
            } else {
                android.os.Process.killProcess(android.os.Process.myPid());
                final int systemError = 10;
                System.exit(systemError);
            }
        }
    }

    public void install() {
        Thread.setDefaultUncaughtExceptionHandler(this);
    }
}
