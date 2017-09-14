package org.wikipedia.activity;

import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.annotation.LayoutRes;
import android.support.v4.app.Fragment;

import org.wikipedia.R;
import org.wikipedia.theme.Theme;

/**
 * Boilerplate for a {@link android.support.v4.app.FragmentActivity} containing a single stack of
 * Fragments, with a transparent background.
 *
 * Set a theme on the activity in AndroidManifest.xml to specify a background tint.
 */
public abstract class SingleFragmentActivityTransparent<T extends Fragment> extends BaseActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(getLayout());
        findViewById(getContainerId()).setBackground(null);

        if (!isFragmentCreated()) {
            addFragment(createFragment());
        }
    }

    @Override
    protected void setTheme() {
        setTheme(Theme.DARK.getResourceId());
        setTheme(R.style.AppTheme_FullScreen_TranslucentDark);
    }

    protected void addFragment(T fragment) {
        getSupportFragmentManager().beginTransaction().add(getContainerId(), fragment).commit();
    }

    protected abstract T createFragment();

    /** @return The Fragment added to the stack. */
    protected T getFragment() {
        //noinspection unchecked
        return (T) getSupportFragmentManager().findFragmentById(getContainerId());
    }

    /** @return The resource layout to inflate which must contain a {@link android.view.ViewGroup}
     * whose ID is {@link #getContainerId()}. */
    @LayoutRes
    protected int getLayout() {
        return R.layout.activity_single_fragment;
    }

    /** @return The resource identifier for the Fragment container. */
    @IdRes
    protected int getContainerId() {
        return R.id.fragment_container;
    }

    protected boolean isFragmentCreated() {
        return getFragment() != null;
    }
}
