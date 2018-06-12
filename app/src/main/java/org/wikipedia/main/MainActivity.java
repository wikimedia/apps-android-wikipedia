package org.wikipedia.main;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.view.ActionMode;
import android.view.View;

import org.wikipedia.R;
import org.wikipedia.activity.SingleFragmentToolbarActivity;
import org.wikipedia.appshortcuts.AppShortcuts;
import org.wikipedia.navtab.NavTab;
import org.wikipedia.onboarding.InitialOnboardingActivity;
import org.wikipedia.settings.Prefs;
import org.wikipedia.util.ResourceUtil;

import static org.wikipedia.Constants.ACTIVITY_REQUEST_INITIAL_ONBOARDING;

public class MainActivity extends SingleFragmentToolbarActivity<MainFragment>
        implements MainFragment.Callback {

    private boolean controlNavTabInFragment;

    public static Intent newIntent(@NonNull Context context) {
        return new Intent(context, MainActivity.class);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setSharedElementTransitions();
        new AppShortcuts().init();

        if (Prefs.isInitialOnboardingEnabled() && savedInstanceState == null) {
            // Updating preference so the search multilingual tooltip
            // is not shown again for first time users
            Prefs.setMultilingualSearchTutorialEnabled(false);

            // Use startActivityForResult to avoid preload the Feed contents before finishing the initial onboarding.
            // The ACTIVITY_REQUEST_INITIAL_ONBOARDING has not been used in any onActivityResult
            startActivityForResult(InitialOnboardingActivity.newIntent(this), ACTIVITY_REQUEST_INITIAL_ONBOARDING);
        }
    }

    @Override protected MainFragment createFragment() {
        return MainFragment.newInstance();
    }

    @Override
    public void onTabChanged(@NonNull NavTab tab) {
        if (tab.equals(NavTab.EXPLORE)) {
            getToolbarWordmark().setVisibility(View.VISIBLE);
            getSupportActionBar().setTitle("");
            controlNavTabInFragment = false;
        } else {
            getToolbarWordmark().setVisibility(View.GONE);
            getSupportActionBar().setTitle(tab.text());
            controlNavTabInFragment = true;
        }
        getFragment().requestUpdateToolbarElevation();
    }

    @Override
    public void onSearchOpen() {
        getToolbar().setVisibility(View.GONE);
        setStatusBarColor(ResourceUtil.getThemedAttributeId(this, R.attr.page_status_bar_color));
    }

    @Override
    public void onSearchClose(boolean shouldFinishActivity) {
        getToolbar().setVisibility(View.VISIBLE);
        setStatusBarColor(ResourceUtil.getThemedAttributeId(this, R.attr.main_status_bar_color));
        if (shouldFinishActivity) {
            finish();
        }
    }

    @Override
    public void onSupportActionModeStarted(@NonNull ActionMode mode) {
        super.onSupportActionModeStarted(mode);
        if (!controlNavTabInFragment) {
            getFragment().setBottomNavVisible(false);
        }
    }

    @Override
    public void onSupportActionModeFinished(@NonNull ActionMode mode) {
        super.onSupportActionModeFinished(mode);
        getFragment().setBottomNavVisible(true);
    }

    @NonNull
    @Override
    public View getOverflowMenuAnchor() {
        View view = getToolbar().findViewById(R.id.menu_overflow_button);
        return view == null ? getToolbar() : view;
    }

    @Override
    public void updateToolbarElevation(boolean elevate) {
        if (elevate) {
            setToolbarElevationDefault();
        } else {
            clearToolbarElevation();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        getFragment().handleIntent(intent);
    }

    @Override
    protected void onGoOffline() {
        getFragment().onGoOffline();
    }

    @Override
    protected void onGoOnline() {
        getFragment().onGoOnline();
    }

    @Override
    public void onBackPressed() {
        if (getFragment().onBackPressed()) {
            return;
        }
        finish();
    }
}
