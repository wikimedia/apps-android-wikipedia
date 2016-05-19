package org.wikipedia.activity;

import android.support.annotation.Nullable;

import org.wikipedia.activity.CallbackFragment.Callback;

public interface CallbackFragment<T extends Callback> {
    interface Callback { }

    @Nullable T getCallback();
}