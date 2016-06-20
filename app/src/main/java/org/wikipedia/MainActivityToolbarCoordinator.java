package org.wikipedia;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;

public class MainActivityToolbarCoordinator {
    @NonNull private AppCompatActivity activity;
    @NonNull private Toolbar defaultToolbar;
    @Nullable private Toolbar overrideToolbar;

    public MainActivityToolbarCoordinator(@NonNull AppCompatActivity activity, @NonNull Toolbar defaultToolbar) {
        this.activity = activity;
        this.defaultToolbar = defaultToolbar;
        setActivityToolbar(defaultToolbar);
    }

    public void setOverrideToolbar(@NonNull Toolbar toolbar) {
        overrideToolbar = toolbar;
        defaultToolbar.setVisibility(View.GONE);
        setActivityToolbar(overrideToolbar);
    }

    public void removeOverrideToolbar() {
        overrideToolbar = null;
        defaultToolbar.setVisibility(View.VISIBLE);
        setActivityToolbar(defaultToolbar);
    }

    private void setActivityToolbar(Toolbar toolbar) {
        activity.setSupportActionBar(toolbar);
        activity.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        activity.getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_menu_white_24dp);
    }
}
