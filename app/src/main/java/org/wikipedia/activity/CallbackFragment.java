package org.wikipedia.activity;

import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;

public class CallbackFragment<T extends FragmentCallback> extends Fragment {
    @Nullable protected T getCallback() {
        if (getTargetFragment() instanceof FragmentCallback) {
            //noinspection unchecked
            return (T) getTargetFragment();
        } else if (getActivity() instanceof FragmentCallback) {
            //noinspection unchecked
            return (T) getActivity();
        } else {
            return null;
        }
    }
}