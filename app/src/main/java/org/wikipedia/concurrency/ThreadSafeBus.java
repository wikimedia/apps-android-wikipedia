package org.wikipedia.concurrency;

import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;

import com.squareup.otto.Bus;

// https://github.com/square/otto/issues/38
public class ThreadSafeBus extends Bus {
    private final Handler handler = new Handler(Looper.getMainLooper());

    public ThreadSafeBus() {
        super();
    }

    public ThreadSafeBus(String identifier) {
        super(identifier);
    }

    @Override public void post(@NonNull final Object event) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            super.post(event);
        } else {
            handler.post(new Runnable() {
                @Override public void run() {
                    ThreadSafeBus.super.post(event);
                }
            });
        }
    }
}
