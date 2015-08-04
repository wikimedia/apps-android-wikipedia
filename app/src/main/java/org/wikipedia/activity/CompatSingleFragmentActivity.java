package org.wikipedia.activity;

import android.support.v4.app.Fragment;

/** Boilerplate for a {@link android.support.v4.app.FragmentActivity} containing a single stack of
 * compatibility {@link Fragment}s. */
public abstract class CompatSingleFragmentActivity<T extends Fragment> extends BaseSingleFragmentActivity<T> {
    @Override
    protected void addFragment(Fragment fragment) {
        getSupportFragmentManager().beginTransaction().add(getContainerId(), fragment).commit();
    }

    @Override
    protected T getFragment() {
        //noinspection unchecked
        return (T) getSupportFragmentManager().findFragmentById(getContainerId());
    }
}