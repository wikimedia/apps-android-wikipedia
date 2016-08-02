package org.wikipedia.activity;

import android.support.annotation.LayoutRes;
import android.support.v4.app.Fragment;
import android.support.v7.widget.Toolbar;
import android.view.View;

import org.wikipedia.R;

/**
 * Boilerplate for a {@link android.support.v4.app.FragmentActivity} containing a single stack of
 * Fragments, with a Toolbar overlaid on top.
 */
public abstract class SingleFragmentActivityWithToolbar<T extends Fragment> extends SingleFragmentActivity<T> {

    @LayoutRes
    @Override
    protected int getLayout() {
        return R.layout.activity_single_fragment_with_toolbar;
    }

    protected View getToolbarContainer() {
        return findViewById(R.id.single_fragment_toolbar_container);
    }

    protected Toolbar getToolbar() {
        return (Toolbar) findViewById(R.id.single_fragment_toolbar);
    }
}
