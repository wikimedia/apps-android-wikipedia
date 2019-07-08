package org.wikipedia.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;

import org.wikipedia.R;

public class ConfigurableTabLayout extends ConstraintLayout {
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
        View tab = getChildAt(index);
        if (tab != null) {
            setEnabled(tab, true);
        }
    }

    public void disableTab(int index) {
        View tab = getChildAt(index);
        if (tab != null) {
            setEnabled(tab, false);
        }
    }

    public void enableAllTabs() {
        for (int i = 0; i < getChildCount(); i++) {
            enableTab(i);
        }
    }

    public boolean isEnabled(@NonNull View tab) {
        return !isDisabled(tab);
    }

    public boolean isDisabled(@NonNull View tab) {
        return tab.getTag() != null && tab.getTag() instanceof DisabledTag;
    }

    private void setEnabled(@NonNull View tab, boolean enabled) {
        tab.setTag(enabled ? null : new DisabledTag());
    }

    private class DisabledTag {
    }
}
