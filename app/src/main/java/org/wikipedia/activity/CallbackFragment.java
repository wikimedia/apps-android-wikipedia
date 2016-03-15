package org.wikipedia.activity;

import android.support.annotation.Nullable;

public interface CallbackFragment<T extends FragmentCallback> {
    @Nullable T getCallback();
}