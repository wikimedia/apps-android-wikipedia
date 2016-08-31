package org.wikipedia.navtab;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.View;

public class NavTabLayout extends TabLayout {
    public NavTabLayout(Context context) {
        super(context);
    }

    public NavTabLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public NavTabLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override public void setupWithViewPager(@Nullable ViewPager viewPager, boolean autoRefresh) {
        super.setupWithViewPager(viewPager, autoRefresh);
        setTabViews();
    }

    private void setTabViews() {
        for (int i = 0; i < getTabCount(); i++) {
            TabLayout.Tab tab = getTabAt(i);
            NavTab navTab = NavTab.of(i);
            View view = new NavTabView(getContext())
                    .icon(navTab.icon())
                    .text(navTab.text());
            //noinspection ConstantConditions
            tab.setCustomView(view);
        }
    }
}