package org.wikipedia;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
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
        setStatusBarColor(android.R.color.black);
    }

    @Override
    public void onSearchClose(boolean shouldFinishActivity) {
        getToolbar().setVisibility(View.VISIBLE);
        setStatusBarColor(R.color.dark_blue);
        if (shouldFinishActivity) {
            finish();
        }
    }

    @NonNull
    @Override
    public View getOverflowMenuAnchor() {
        View view = getToolbar().findViewById(R.id.menu_overflow_button);
        return view == null ? getToolbar() : view;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        getFragment().handleIntent(intent);
    }

    @Override
    public void onBackPressed() {
        if (getFragment().onBackPressed()) {
            return;
        }
        finish();
    }
}