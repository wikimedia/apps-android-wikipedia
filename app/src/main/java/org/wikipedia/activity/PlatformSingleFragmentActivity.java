package org.wikipedia.activity;

import android.annotation.TargetApi;
import android.app.Fragment;
import android.os.Build;

/** Boilerplate for a {@link android.support.v4.app.FragmentActivity} containing a single stack of
 * platform {@link Fragment}s. */
@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public abstract class PlatformSingleFragmentActivity<T extends Fragment> extends BaseSingleFragmentActivity<T> {
    @Override
    protected void addFragment(Fragment fragment) {
        getFragmentManager().beginTransaction().add(getContainerId(), fragment).commit();
    }

    @Override
    protected T getFragment() {
        //noinspection unchecked
        return (T) getFragmentManager().findFragmentById(getContainerId());
    }
}