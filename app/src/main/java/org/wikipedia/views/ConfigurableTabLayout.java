package org.wikipedia.views;

import android.content.Context;
import android.support.annotation.ColorRes;
import android.support.annotation.NonNull;
import android.support.design.widget.TabLayout;
import android.util.AttributeSet;

import org.wikipedia.R;

import static android.support.v4.content.ContextCompat.getColor;
import static android.support.v4.graphics.drawable.DrawableCompat.setTint;

public class ConfigurableTabLayout extends TabLayout {
    @ColorRes private static final int TAB_ENABLED_COLOR = android.R.color.white;
    @ColorRes private static final int TAB_DISABLED_COLOR = R.color.base30;

    public ConfigurableTabLayout(Context context) {
        this(context, null);
    }

    public ConfigurableTabLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ConfigurableTabLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void enableTab(int index) {
        Tab tab = getTabAt(index);
        if (tab != null) {
            setEnabled(tab, true);
        }
    }

    public void disableTab(int index) {
        Tab tab = getTabAt(index);
        if (tab != null) {
            setEnabled(tab, false);
        }
    }

    public void enableAllTabs() {
        for (int i = 0; i < getTabCount(); i++) {
            enableTab(i);
        }
    }

    public boolean isEnabled(@NonNull Tab tab) {
        return !isDisabled(tab);
    }

    public boolean isDisabled(@NonNull Tab tab) {
        return tab.getTag() != null && tab.getTag() instanceof DisabledTag;
    }

    private void setEnabled(@NonNull Tab tab, boolean enabled) {
        tab.setTag(enabled ? null : new DisabledTag());
        // noinspection ConstantConditions
        setTint(tab.getIcon(), getColor(getContext(), enabled ? TAB_ENABLED_COLOR : TAB_DISABLED_COLOR));
    }

    private class DisabledTag {
    }
}
