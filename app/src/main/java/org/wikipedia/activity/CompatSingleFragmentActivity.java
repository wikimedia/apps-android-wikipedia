package org.wikipedia.activity;

/** Boilerplate for a {@link android.support.v4.app.FragmentActivity} containing a single stack of
 * compatibility {@link android.support.v4.app.Fragment}s. */
public abstract class CompatSingleFragmentActivity<T extends CallbackFragment>
        extends BaseSingleFragmentActivity<T> {
    @Override
    protected void addFragment(CallbackFragment fragment) {
        getSupportFragmentManager().beginTransaction().add(getContainerId(), fragment).commit();
    }

    @Override
    protected T getFragment() {
        //noinspection unchecked
        return (T) getSupportFragmentManager().findFragmentById(getContainerId());
    }
}