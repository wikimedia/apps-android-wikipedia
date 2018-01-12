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
        if (fragment.getParentFragment() != null) {
            if (callback.isInstance(fragment.getParentFragment())) {
                //noinspection unchecked
                return (T) fragment.getParentFragment();
            } else if (callback.isInstance(fragment.getParentFragment().getParentFragment())) {
                //noinspection unchecked
                return (T) fragment.getParentFragment().getParentFragment();
            }
        }
        if (callback.isInstance(fragment.getActivity())) {
            //noinspection unchecked
            return (T) fragment.getActivity();
        }
        return null;
    }

    private FragmentUtil() { }
}
