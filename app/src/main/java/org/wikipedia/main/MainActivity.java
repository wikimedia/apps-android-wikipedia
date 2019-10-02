package org.wikipedia.main;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.view.ActionMode;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

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
import org.wikipedia.page.PageActivity;
import org.wikipedia.page.tabs.TabActivity;
import org.wikipedia.readinglist.ReadingListSyncBehaviorDialogs;
import org.wikipedia.readinglist.database.ReadingListDbHelper;
import org.wikipedia.settings.AboutActivity;
import org.wikipedia.settings.Prefs;
import org.wikipedia.settings.SettingsActivity;
import org.wikipedia.util.AnimationUtil;
import org.wikipedia.util.DimenUtil;
import org.wikipedia.util.FeedbackUtil;
import org.wikipedia.views.TabCountsView;
import org.wikipedia.views.WikiDrawerLayout;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static org.wikipedia.Constants.ACTIVITY_REQUEST_INITIAL_ONBOARDING;

public class MainActivity extends SingleFragmentActivity<MainFragment>
        implements MainFragment.Callback {

    @BindView(R.id.navigation_drawer) WikiDrawerLayout drawerLayout;
    @BindView(R.id.navigation_drawer_view) MainDrawerView drawerView;
    @BindView(R.id.single_fragment_toolbar) Toolbar toolbar;
    @BindView(R.id.drawer_icon_layout) View drawerIconLayout;
    @BindView(R.id.drawer_icon_dot) View drawerIconDot;
    @BindView(R.id.hamburger_and_wordmark_layout) View hamburgerAndWordmarkLayout;

    private boolean controlNavTabInFragment;

    public static Intent newIntent(@NonNull Context context) {
        return new Intent(context, MainActivity.class);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WikipediaApp.getInstance().checkCrashes(this);
        ButterKnife.bind(this);
        AnimationUtil.setSharedElementTransitions(this);
        AppShortcuts.setShortcuts(this);

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
                    if (drawerIconDot.getVisibility() == VISIBLE) {
                        Prefs.setShowActionFeedIndicator(false);
                        setUpHomeMenuIcon();
                    }
                }
            }
        });
        drawerView.setCallback(new DrawerViewCallback());
        shouldShowMainDrawer(true);
        setUpHomeMenuIcon();
        FeedbackUtil.setToolbarButtonLongPressToast(drawerIconLayout);
    }

    @Override
    public void onResume() {
        super.onResume();
        // update main nav drawer after rotating screen
        drawerView.updateState();
        setUpHomeMenuIcon();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem tabsItem = menu.findItem(R.id.menu_tabs);
        if (WikipediaApp.getInstance().getTabCount() < 1) {
            tabsItem.setVisible(false);
        } else {
            tabsItem.setVisible(true);
            TabCountsView tabCountsView = new TabCountsView(this);
            tabCountsView.setOnClickListener(v -> {
                if (WikipediaApp.getInstance().getTabCount() == 1) {
                    startActivity(PageActivity.newIntent(MainActivity.this));
                } else {
                    startActivityForResult(TabActivity.newIntent(MainActivity.this), Constants.ACTIVITY_REQUEST_BROWSE_TABS);
                }
            });
            tabCountsView.updateTabCount();
            tabsItem.setActionView(tabCountsView);
            tabsItem.expandActionView();
            FeedbackUtil.setToolbarButtonLongPressToast(tabCountsView);
        }
        return true;
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
            hamburgerAndWordmarkLayout.setVisibility(VISIBLE);
            toolbar.setTitle("");
            controlNavTabInFragment = false;
        } else {
            if (tab.equals(NavTab.HISTORY) && getFragment().getCurrentFragment() != null) {
                ((HistoryFragment) getFragment().getCurrentFragment()).refresh();
            }
            hamburgerAndWordmarkLayout.setVisibility(GONE);
            toolbar.setTitle(tab.text());
            controlNavTabInFragment = true;
        }
        shouldShowMainDrawer(!controlNavTabInFragment);
        getFragment().requestUpdateToolbarElevation();
    }

    void setUpHomeMenuIcon() {
        drawerIconDot.setVisibility(AccountUtil.isLoggedIn() && Prefs.showActionFeedIndicator() ? VISIBLE : GONE);
    }

    @OnClick(R.id.drawer_icon_layout) void onDrawerOpenClicked() {
        drawerLayout.openDrawer(GravityCompat.START);
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
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
            return;
        }
        if (getFragment().onBackPressed()) {
            return;
        }
        super.onBackPressed();
    }

    public void closeMainDrawer() {
        drawerLayout.closeDrawer(GravityCompat.START);
    }

    public Toolbar getToolbar() {
        return toolbar;
    }

    public void shouldShowMainDrawer(boolean enabled) {
        drawerLayout.setSlidingEnabled(enabled);

        if (enabled) {
            ActionBarDrawerToggle drawerToggle = new ActionBarDrawerToggle(this,
                    drawerLayout, toolbar,
                    R.string.main_drawer_open, R.string.main_drawer_close);
            drawerToggle.syncState();
            getToolbar().setNavigationIcon(null);
        }
    }

    protected void setToolbarElevationDefault() {
        getToolbar().setElevation(DimenUtil.dpToPx(DimenUtil.getDimension(R.dimen.toolbar_default_elevation)));
    }

    protected void clearToolbarElevation() {
        getToolbar().setElevation(0f);
    }

    private class DrawerViewCallback implements MainDrawerView.Callback {
        @Override public void loginLogoutClick() {
            if (AccountUtil.isLoggedIn()) {
                new AlertDialog.Builder(MainActivity.this)
                        .setMessage(R.string.logout_prompt)
                        .setNegativeButton(R.string.logout_dialog_cancel_button_text, null)
                        .setPositiveButton(R.string.preference_title_logout, (dialog, which) -> {
                            WikipediaApp.getInstance().logOut();
                            FeedbackUtil.showMessage(MainActivity.this, R.string.toast_logout_complete);
                            if (Prefs.isReadingListSyncEnabled() && !ReadingListDbHelper.instance().isEmpty()) {
                                ReadingListSyncBehaviorDialogs.removeExistingListsOnLogoutDialog(MainActivity.this);
                            }
                            Prefs.setReadingListsLastSyncTime(null);
                            Prefs.setReadingListSyncEnabled(false);
                        }).show();
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
