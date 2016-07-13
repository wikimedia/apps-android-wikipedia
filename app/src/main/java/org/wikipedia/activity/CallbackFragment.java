package org.wikipedia.activity;

import android.support.annotation.Nullable;

import org.wikipedia.activity.CallbackFragment.Callback;

public interface CallbackFragment {
    interface Callback { }

    @Nullable Callback getCallback();
}