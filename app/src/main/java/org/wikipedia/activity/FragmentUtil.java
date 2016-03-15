package org.wikipedia.activity;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;

public final class FragmentUtil {
    @Nullable public static FragmentCallback getCallback(@NonNull Fragment fragment) {
        return getCallback(fragment, FragmentCallback.class);
    }

    @Nullable public static <T extends FragmentCallback> T getCallback(@NonNull Fragment fragment,
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