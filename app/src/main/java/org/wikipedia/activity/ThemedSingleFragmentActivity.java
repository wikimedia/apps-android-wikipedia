package org.wikipedia.activity;

import android.os.Bundle;
import android.support.annotation.LayoutRes;
import android.support.v4.app.Fragment;

import org.wikipedia.R;
import org.wikipedia.WikipediaApp;

public abstract class ThemedSingleFragmentActivity<T extends Fragment> extends SingleFragmentActivity<T> {
    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme();
    }

    protected void setTheme() {
        setTheme(WikipediaApp.getInstance().getCurrentTheme().getResourceId());
    }

    /** @return The resource layout to inflate which must contain a {@link android.view.ViewGroup}
     * whose ID is {@link #getContainerId()}. */
    @LayoutRes @Override
    protected int getLayout() {
        return R.layout.activity_themed_single_fragment;
    }
}
