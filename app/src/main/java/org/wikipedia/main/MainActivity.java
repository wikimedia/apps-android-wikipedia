package org.wikipedia.main;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.Toolbar;
import android.view.Gravity;
import android.view.View;

import org.wikipedia.Constants;
import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.activity.SingleFragmentActivity;
import org.wikipedia.appshortcuts.AppShortcuts;
import org.wikipedia.auth.AccountUtil;
import org.wikipedia.feed.FeedFragment;
import org.wikipedia.history.HistoryFragment;
import org.wikipedia.navtab.NavTab;
import org.wikipedia.notifications.NotificationActivity;
import org.wikipedia.onboarding.InitialOnboardingActivity;
import org.wikipedia.readinglist.ReadingListSyncBehaviorDialogs;
import org.wikipedia.readinglist.database.ReadingListDbHelper;
import org.wikipedia.settings.AboutActivity;
import org.wikipedia.settings.Prefs;
import org.wikipedia.settings.SettingsActivity;
import org.wikipedia.util.AnimationUtil;
import org.wikipedia.util.DimenUtil;
import org.wikipedia.util.FeedbackUtil;
import org.wikipedia.views.WikiDrawerLayout;

import butterknife.BindView;
import butterknife.ButterKnife;

import static org.wikipedia.Constants.ACTIVITY_REQUEST_INITIAL_ONBOARDING;

public class MainActivity extends SingleFragmentActivity<MainFragment>
        implements MainFragment.Callback {

    @BindView(R.id.navigation_drawer) WikiDrawerLayout drawerLayout;
    @BindView(R.id.navigation_drawer_view) MainDrawerView drawerView;
    @BindView(R.id.single_fragment_toolbar) Toolbar toolbar;
    @BindView(R.id.single_fragment_toolbar_wordmark) View wordMark;

    private boolean controlNavTabInFragment;

    public static Intent newIntent(@NonNull Context context) {
        return new Intent(context, MainActivity.class);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ButterKnife.bind(this);
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

        drawerLayout.setDragEdgeWidth(getResources().getDimensionPixelSize(R.dimen.drawer_drag_margin));
        drawerLayout.addDrawerListener(new DrawerLayout.SimpleDrawerListener() {
            @Override
            public void onDrawerStateChanged(int newState) {
                if (newState == DrawerLayout.STATE_DRAGGING || newState == DrawerLayout.STATE_SETTLING) {
                    drawerView.updateState();
                }
            }
        });
        drawerView.setCallback(new DrawerViewCallback());
        shouldShowMainDrawer(true);
    }

    @Override
    public void onResume() {
        super.onResume();
        // update main nav drawer after rotating screen
        drawerView.updateState();
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
            if (tab.equals(NavTab.HISTORY) && getFragment().getCurrentFragment() != null) {
                ((HistoryFragment) getFragment().getCurrentFragment()).refresh();
            }
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
        getFragment().getFloatingQueueView().hide();
    }

    @Override
    public void onSupportActionModeFinished(@NonNull ActionMode mode) {
        super.onSupportActionModeFinished(mode);
        getFragment().setBottomNavVisible(true);
        getFragment().getFloatingQueueView().show();
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
        if (drawerLayout.isDrawerOpen(Gravity.START)) {
            drawerLayout.closeDrawer(Gravity.START);
            return;
        }
        if (getFragment().onBackPressed()) {
            return;
        }
        super.onBackPressed();
    }

    public boolean isFloatingQueueEnabled() {
        return getFragment().getFloatingQueueView().getVisibility() == View.VISIBLE;
    }

    public View getFloatingQueueImageView() {
        return getFragment().getFloatingQueueView().getImageView();
    }

    public void closeMainDrawer() {
        drawerLayout.closeDrawer(GravityCompat.START);
    }

    public Toolbar getToolbar() {
        return toolbar;
    }

    public void shouldShowMainDrawer(boolean enabled) {
        getSupportActionBar().setDisplayHomeAsUpEnabled(enabled);
        drawerLayout.setSlidingEnabled(enabled);

        if (enabled) {
            ActionBarDrawerToggle drawerToggle = new ActionBarDrawerToggle(this,
                    drawerLayout, toolbar,
                    R.string.main_drawer_open, R.string.main_drawer_close);
            drawerToggle.syncState();
            getToolbar().setNavigationIcon(R.drawable.ic_menu_themed_24dp);
        }
    }

    protected View getToolbarWordmark() {
        return wordMark;
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

    private class DrawerViewCallback implements MainDrawerView.Callback {
        @Override public void loginLogoutClick() {
            if (AccountUtil.isLoggedIn()) {
                WikipediaApp.getInstance().logOut();
                FeedbackUtil.showMessage(MainActivity.this, R.string.toast_logout_complete);
                if (Prefs.isReadingListSyncEnabled() && !ReadingListDbHelper.instance().isEmpty()) {
                    ReadingListSyncBehaviorDialogs.removeExistingListsOnLogoutDialog(MainActivity.this);
                }
                Prefs.setReadingListsLastSyncTime(null);
                Prefs.setReadingListSyncEnabled(false);
            } else {
                getFragment().onLoginRequested();
            }
            closeMainDrawer();
        }

        @Override public void notificationsClick() {
            if (AccountUtil.isLoggedIn()) {
                startActivity(NotificationActivity.newIntent(MainActivity.this));
                closeMainDrawer();
            }
        }

        @Override public void settingsClick() {
            startActivityForResult(SettingsActivity.newIntent(MainActivity.this), Constants.ACTIVITY_REQUEST_SETTINGS);
            closeMainDrawer();
        }

        @Override public void configureFeedClick() {
            if (getFragment().getCurrentFragment() instanceof FeedFragment) {
                ((FeedFragment) getFragment().getCurrentFragment()).showConfigureActivity(-1);
            }
            closeMainDrawer();
        }

        @Override public void aboutClick() {
            startActivity(new Intent(MainActivity.this, AboutActivity.class));
            closeMainDrawer();
        }
    }
}
