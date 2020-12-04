package org.wikipedia.activity;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import org.wikipedia.R;

/**
 * Boilerplate for a FragmentActivity containing a single stack of
 * Fragments, with a transparent background.
 *
 * Set a theme on the activity in AndroidManifest.xml to specify a background tint.
 */
public abstract class SingleFragmentActivityTransparent<T extends Fragment> extends SingleFragmentActivity<T> {
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
        setTheme(R.style.ThemeDark_Translucent);
    }
}
