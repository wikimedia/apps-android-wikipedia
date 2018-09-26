package org.wikipedia.main;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.Toolbar;
import android.view.View;

import org.wikipedia.R;
import org.wikipedia.activity.SingleFragmentActivity;
import org.wikipedia.appshortcuts.AppShortcuts;
import org.wikipedia.navtab.NavTab;
import org.wikipedia.onboarding.InitialOnboardingActivity;
import org.wikipedia.settings.Prefs;
import org.wikipedia.util.AnimationUtil;
import org.wikipedia.util.DimenUtil;
import org.wikipedia.views.WikiDrawerLayout;

import static org.wikipedia.Constants.ACTIVITY_REQUEST_INITIAL_ONBOARDING;

public class MainActivity extends SingleFragmentActivity<MainFragment>
        implements MainFragment.Callback {

    private boolean controlNavTabInFragment;

    public static Intent newIntent(@NonNull Context context) {
        return new Intent(context, MainActivity.class);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AnimationUtil.setSharedElementTransitions(this);
        new AppShortcuts().init();

        if (Prefs.isInitialOnboardingEnabled() && savedInstanceState == null) {
            // Updating preference so the search multilingual tooltip
            // is not shown again for first time users
            Prefs.setMultilingualSearchTutorialEnabled(false);

            // Use startActivityForResult to avoid preload the Feed contents before finishing the initial onboarding.
            // The ACTIVITY_REQUEST_INITIAL_ONBOARDING has not been used in any onActivityResult
            startActivityForResult(InitialOnboardingActivity.newIntent(this), ACTIVITY_REQUEST_INITIAL_ONBOARDING);
        }

        setSupportActionBar(getToolbar());
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("");
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        }
    }

    @LayoutRes
    @Override
    protected int getLayout() {
        return R.layout.activity_main;
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
        shouldShowMainDrawer(!controlNavTabInFragment);
        getFragment().requestUpdateToolbarElevation();
    }

    @Override
    public void onSearchOpen() {
        getToolbar().setVisibility(View.GONE);
        shouldShowMainDrawer(false);
    }

    @Override
    public void onSearchClose(boolean shouldFinishActivity) {
        getToolbar().setVisibility(View.VISIBLE);
        shouldShowMainDrawer(true);
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
        getFragment().getFloatingQueueView().setVisibility(View.GONE);
    }

    @Override
    public void onSupportActionModeFinished(@NonNull ActionMode mode) {
        super.onSupportActionModeFinished(mode);
        getFragment().setBottomNavVisible(true);
        getFragment().getFloatingQueueView().setVisibility(View.VISIBLE);
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

    public boolean isFloatingQueueEnabled() {
        return getFragment().getFloatingQueueView().getVisibility() == View.VISIBLE;
    }

    public View getFloatingQueueImageView() {
        return getFragment().getFloatingQueueView().getImageView();
    }

    public WikiDrawerLayout getDrawerLayout() {
        return findViewById(R.id.navigation_drawer);
    }

    public MainDrawerView getDrawerView() {
        return findViewById(R.id.navigation_drawer_view);
    }

    public Toolbar getToolbar() {
        return (Toolbar) findViewById(R.id.single_fragment_toolbar);
    }

    public void shouldShowMainDrawer(boolean enabled) {
        getSupportActionBar().setDisplayHomeAsUpEnabled(enabled);
        getDrawerLayout().setSlidingEnabled(enabled);

        if (enabled) {
            ActionBarDrawerToggle drawerToggle = new ActionBarDrawerToggle(this,
                    getDrawerLayout(), getToolbar(),
                    R.string.main_drawer_open, R.string.main_drawer_close);
            drawerToggle.syncState();
            getToolbar().setNavigationIcon(R.drawable.ic_menu_black_24dp);
        }
    }

    protected View getToolbarWordmark() {
        return findViewById(R.id.single_fragment_toolbar_wordmark);
    }

    protected void setToolbarElevationDefault() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getToolbar().setElevation(DimenUtil
                    .dpToPx(DimenUtil.getDimension(R.dimen.toolbar_default_elevation)));
        }
    }

    protected void clearToolbarElevation() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getToolbar().setElevation(0f);
        }
    }
}
