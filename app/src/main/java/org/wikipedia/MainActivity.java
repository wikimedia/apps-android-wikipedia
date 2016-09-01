package org.wikipedia;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;

import org.wikipedia.activity.SingleFragmentActivityWithToolbar;
import org.wikipedia.navtab.NavTab;

public class MainActivity extends SingleFragmentActivityWithToolbar<MainFragment>
        implements MainFragment.Callback {
    public static Intent newIntent(@NonNull Context context) {
        return new Intent(context, MainActivity.class);
    }

    @Override protected MainFragment createFragment() {
        return MainFragment.newInstance();
    }

    @Override protected void setTheme() { }

    @Override
    public void onTabChanged(@NonNull NavTab tab) {
        if (tab.equals(NavTab.EXPLORE)) {
            getToolbarWordmark().setVisibility(View.VISIBLE);
            getSupportActionBar().setTitle("");
        } else {
            getToolbarWordmark().setVisibility(View.GONE);
            getSupportActionBar().setTitle(tab.text());
        }
    }

    @Override
    public void onSearchOpen() {
        getToolbar().setVisibility(View.GONE);
    }

    @Override
    public void onSearchClose() {
        getToolbar().setVisibility(View.VISIBLE);
    }

    @Nullable
    @Override
    public View getOverflowMenuButton() {
        return getToolbar().findViewById(R.id.main_menu_overflow);
    }
}