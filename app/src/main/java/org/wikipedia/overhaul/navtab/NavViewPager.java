package org.wikipedia.overhaul.navtab;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.support.annotation.ColorRes;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.design.widget.TabLayout;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.view.View;

import org.wikipedia.R;
import org.wikipedia.drawable.DrawableUtil;
import org.wikipedia.readinglist.NoSwipeViewPager;

import butterknife.BindView;
import butterknife.ButterKnife;

public class NavViewPager extends NoSwipeViewPager {
    @BindView(R.id.view_nav_view_pager_tab_layout) TabLayout tabLayout;

    public NavViewPager(Context context) {
        super(context);
        init();
    }

    public NavViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        inflate(getContext(), R.layout.view_nav_view_pager, this);
        ButterKnife.bind(this);
        tabLayout.addOnAttachStateChangeListener(new TabIconCallback(tabLayout, portraitOrientation()));
        tabLayout.addOnTabSelectedListener(new TabSelectedTintCallback(getContext()));
    }

    private boolean portraitOrientation() {
        return getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;
    }

    // TabLayouts check their parents on attach. If their parent is a ViewPager instance, they
    // populate themselves with tabs via TabLayout.setupWithViewPager(). Tab titles are retrieved
    // from FragmentPagerAdapter.getPageTitle() which may be rich text via Spanned. Since Spanneds
    // are a little cumbersome to work with and, excepting this entry point, the Tab API is simple,
    // use the latter. Since the selected callback is only invoked on tab selection, this callback
    // is necessary to initialize all tabs.
    private static class TabIconCallback implements OnAttachStateChangeListener {
        @NonNull private final TabLayout tabLayout;
        private final boolean showTitles;

        TabIconCallback(@NonNull TabLayout tabLayout, boolean showTitles) {
            this.tabLayout = tabLayout;
            this.showTitles = showTitles;
        }

        @Override public void onViewAttachedToWindow(View view) {
            setTabIconsAndTitles();
        }

        @Override public void onViewDetachedFromWindow(View view) { }

        private void setTabIconsAndTitles() {
            for (int i = 0; i < tabLayout.getTabCount(); i++) {
                TabLayout.Tab tab = tabLayout.getTabAt(i);
                NavTab navTab = NavTab.of(i);
                //noinspection ConstantConditions
                tab.setIcon(navTab.icon());
                if (showTitles) {
                    // todo: [overhaul] consider using a custom View for the tabs so we show text
                    //                  on the same line as the icon in landscape.
                    tab.setText(navTab.text());
                }
            }
        }
    }

    // It doesn't appear practical to tint an XML Drawable that itself is referenced in another XML
    // Drawable.
    private static class TabSelectedTintCallback implements TabLayout.OnTabSelectedListener {
        @NonNull private final Context ctx;

        TabSelectedTintCallback(@NonNull Context ctx) {
            this.ctx = ctx;
        }

        @Override public void onTabSelected(TabLayout.Tab tab) {
            setTabIconColor(tab, R.color.blue_liberal);
        }

        @Override public void onTabUnselected(TabLayout.Tab tab) {
            if (tab != null) {
                setTabIconColor(tab, R.color.gray_highlight);
            }
        }

        @Override public void onTabReselected(TabLayout.Tab tab) {
            onTabSelected(tab);
        }

        private void setTabIconColor(@NonNull TabLayout.Tab tab, @ColorRes int id) {
            NavTab navTab = NavTab.of(tab.getPosition());
            Drawable icon = getDrawable(navTab.icon());
            DrawableUtil.setTint(icon, getColor(id));
            tab.setIcon(icon);
        }

        private Drawable getDrawable(@DrawableRes int id) {
            return ContextCompat.getDrawable(ctx, id);
        }

        private int getColor(@ColorRes int id) {
            return ContextCompat.getColor(ctx, id);
        }
    }
}