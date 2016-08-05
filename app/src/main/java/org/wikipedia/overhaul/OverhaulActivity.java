package org.wikipedia.overhaul;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.view.View;

import org.wikipedia.MainActivityToolbarProvider;
import org.wikipedia.activity.SingleFragmentActivityWithToolbar;
import org.wikipedia.overhaul.navtab.NavTab;

public class OverhaulActivity extends SingleFragmentActivityWithToolbar<OverhaulFragment>
        implements OverhaulFragment.Callback {
    public static Intent newIntent(@NonNull Context context) {
        return new Intent(context, OverhaulActivity.class);
    }

    @Override protected OverhaulFragment createFragment() {
        return OverhaulFragment.newInstance();
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