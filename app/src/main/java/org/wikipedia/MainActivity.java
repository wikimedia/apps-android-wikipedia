package org.wikipedia;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
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
    public void onTabChanged(@NonNull NavTab tab, @NonNull Fragment fragment) {
        getToolbar().setTitle(tab.text());
        if (fragment instanceof MainActivityToolbarProvider) {
            setSupportActionBar(((MainActivityToolbarProvider) fragment).getToolbar());
            getToolbarContainer().setVisibility(View.GONE);
        } else {
            getToolbarContainer().setVisibility(View.VISIBLE);
            setSupportActionBar(getToolbar());
        }
        getSupportActionBar().setDisplayHomeAsUpEnabled(false);
    }
}