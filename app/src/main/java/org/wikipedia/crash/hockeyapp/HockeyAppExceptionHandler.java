package org.wikipedia.crash.hockeyapp;

import androidx.annotation.Nullable;

import net.hockeyapp.android.ExceptionHandler;

import org.wikipedia.util.log.L;

/** Wrapper around {@link ExceptionHandler} that calls {@link HockeyAppCrashListener#onCrash()}. */
class HockeyAppExceptionHandler extends ExceptionHandler {
    private final boolean ignoreDefaultHandler;
    private final Thread.UncaughtExceptionHandler defaultExceptionHandler;
    @Nullable private HockeyAppCrashListener listener;

    HockeyAppExceptionHandler(@Nullable HockeyAppCrashListener listener, boolean ignoreDefaultHandler) {
        super(Thread.getDefaultUncaughtExceptionHandler(), listener, ignoreDefaultHandler);
        this.defaultExceptionHandler = Thread.getDefaultUncaughtExceptionHandler();
        this.listener = listener;
        this.ignoreDefaultHandler = ignoreDefaultHandler;
    }

    public void setListener(@Nullable HockeyAppCrashListener listener) {
        super.setListener(listener);
        this.listener = listener;
    }

    @Override
    public void uncaughtException(Thread thread, Throwable exception) {
        L.e("", exception);
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

    public void install() {
        Thread.setDefaultUncaughtExceptionHandler(this);
    }
}
