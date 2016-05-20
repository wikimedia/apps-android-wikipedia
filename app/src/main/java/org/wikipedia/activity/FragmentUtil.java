package org.wikipedia.activity;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;

import org.wikipedia.activity.CallbackFragment.Callback;

public final class FragmentUtil {
    @Nullable public static Callback getCallback(@NonNull Fragment fragment) {
        return getCallback(fragment, Callback.class);
    }

    @Nullable public static <T extends Callback> T getCallback(@NonNull Fragment fragment,
                                                               @NonNull Class<T> callback) {
        if (callback.isInstance(fragment.getTargetFragment())) {
            //noinspection unchecked
            return (T) fragment.getTargetFragment();
        } else if (callback.isInstance(fragment.getActivity())) {
            //noinspection unchecked
            return (T) fragment.getActivity();
        } else {
            return null;
        }
    }

    private FragmentUtil() { }
}
