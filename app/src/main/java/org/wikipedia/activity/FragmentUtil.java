package org.wikipedia.activity;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;

public final class FragmentUtil {
    @Nullable public static <T> T getCallback(@NonNull Fragment fragment, @NonNull Class<T> callback) {
        if (callback.isInstance(fragment.getTargetFragment())) {
            //noinspection unchecked
            return (T) fragment.getTargetFragment();
        }
        if (callback.isInstance(fragment.getParentFragment())) {
            //noinspection unchecked
            return (T) fragment.getParentFragment();
        }
        if (callback.isInstance(fragment.getActivity())) {
            //noinspection unchecked
            return (T) fragment.getActivity();
        }
        return null;
    }

    private FragmentUtil() { }
}
