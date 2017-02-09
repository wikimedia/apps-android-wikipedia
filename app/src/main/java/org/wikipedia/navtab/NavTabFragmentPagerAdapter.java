package org.wikipedia.navtab;

import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.view.ViewGroup;

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
