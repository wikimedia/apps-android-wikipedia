package org.wikipedia.overhaul.navtab;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

public class NavViewPagerAdapter extends FragmentPagerAdapter {
    public NavViewPagerAdapter(FragmentManager mgr) {
        super(mgr);
    }

    @Override public Fragment getItem(int pos) {
        return NavViewTab.of(pos).newInstance();
    }

    @Override public int getCount() {
        return NavViewTab.size();
    }
}