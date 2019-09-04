package org.wikipedia.navtab;

import android.view.ViewGroup;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

public class NavTabFragmentPagerAdapter extends FragmentPagerAdapter {
    private Fragment currentFragment;

    public NavTabFragmentPagerAdapter(FragmentManager mgr) {
        super(mgr);
    }

    @Nullable
    public Fragment getCurrentFragment() {
        return currentFragment;
    }

    @Override public Fragment getItem(int pos) {
        return NavTab.of(pos).newInstance();
    }

    @Override public int getCount() {
        return NavTab.size();
    }

    @Override
    public void setPrimaryItem(ViewGroup container, int position, Object object) {
        currentFragment = ((Fragment) object);
        super.setPrimaryItem(container, position, object);
    }
}
