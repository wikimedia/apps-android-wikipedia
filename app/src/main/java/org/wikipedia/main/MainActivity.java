package org.wikipedia.main;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
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
import androidx.drawerlayout.widget.FixedDrawerLayout;
import androidx.fragment.app.Fragment;

import org.wikipedia.Constants;
import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.activity.SingleFragmentActivity;
import org.wikipedia.appshortcuts.AppShortcuts;
import org.wikipedia.auth.AccountUtil;
import org.wikipedia.databinding.ActivityMainBinding;
import org.wikipedia.feed.FeedFragment;
import org.wikipedia.history.HistoryFragment;
import org.wikipedia.navtab.NavTab;
import org.wikipedia.notifications.NotificationActivity;
import org.wikipedia.onboarding.InitialOnboardingActivity;
import org.wikipedia.page.PageActivity;
import org.wikipedia.page.tabs.TabActivity;
import org.wikipedia.settings.AboutActivity;
import org.wikipedia.settings.Prefs;
import org.wikipedia.settings.SettingsActivity;
import org.wikipedia.suggestededits.SuggestedEditsTasksFragment;
import org.wikipedia.util.DimenUtil;
import org.wikipedia.util.FeedbackUtil;
import org.wikipedia.util.ResourceUtil;
import org.wikipedia.views.FrameLayoutNavMenuTriggerer;
import org.wikipedia.views.ImageZoomHelper;
import org.wikipedia.views.TabCountsView;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static org.wikipedia.Constants.ACTIVITY_REQUEST_INITIAL_ONBOARDING;

public class MainActivity extends SingleFragmentActivity<MainFragment>
        implements MainFragment.Callback, FrameLayoutNavMenuTriggerer.Callback {

    private FixedDrawerLayout drawerLayout;
    private MainDrawerView drawerView;
    private Toolbar toolbar;
    private View drawerIconDot;
    private View hamburgerAndWordmarkLayout;
    private ImageZoomHelper imageZoomHelper;
    @Nullable private ActionMode currentActionMode;

    private boolean controlNavTabInFragment;

    public static Intent newIntent(@NonNull Context context) {
        return new Intent(context, MainActivity.class);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final ActivityMainBinding binding = ActivityMainBinding.inflate(getLayoutInflater());

        drawerLayout = binding.navigationDrawer;
        drawerView = binding.navigationDrawerView;
        toolbar = binding.singleFragmentToolbar;
        drawerIconDot = binding.drawerIconDot;
        hamburgerAndWordmarkLayout = binding.hamburgerAndWordmarkLayout;

        binding.drawerIconLayout.setOnClickListener(v -> onDrawerOpenClicked());

        AppShortcuts.setShortcuts(this);
        imageZoomHelper = new ImageZoomHelper(this);

        if (Prefs.isInitialOnboardingEnabled() && savedInstanceState == null) {
            // Updating preference so the search multilingual tooltip
            // is not shown again for first time users
            Prefs.setMultilingualSearchTutorialEnabled(false);

            // Use startActivityForResult to avoid preload the Feed contents before finishing the initial onboarding.
            // The ACTIVITY_REQUEST_INITIAL_ONBOARDING has not been used in any onActivityResult
            startActivityForResult(InitialOnboardingActivity.newIntent(this), ACTIVITY_REQUEST_INITIAL_ONBOARDING);
        }

        setNavigationBarColor(ResourceUtil.getThemedColor(this, R.attr.nav_tab_background_color));
        setSupportActionBar(getToolbar());
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("");
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        }

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

        ActionBarDrawerToggle drawerToggle = new ActionBarDrawerToggle(this,
                drawerLayout, toolbar,
                R.string.main_drawer_open, R.string.main_drawer_close);
        drawerToggle.syncState();
        getToolbar().setNavigationIcon(null);

        setUpHomeMenuIcon();
        FeedbackUtil.setToolbarButtonLongPressToast(binding.drawerIconLayout);
        binding.navigationDrawerTriggerer.setCallback(this);
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
        getFragment().requestUpdateToolbarElevation();
        MenuItem tabsItem = menu.findItem(R.id.menu_tabs);
        if (WikipediaApp.getInstance().getTabCount() < 1 || (getFragment().getCurrentFragment() instanceof SuggestedEditsTasksFragment)) {
            tabsItem.setVisible(false);
        } else {
            tabsItem.setVisible(true);
            TabCountsView tabCountsView = new TabCountsView(this, null);
            tabCountsView.setOnClickListener(v -> {
                if (WikipediaApp.getInstance().getTabCount() == 1) {
                    startActivity(PageActivity.newIntent(MainActivity.this));
                } else {
                    startActivityForResult(TabActivity.newIntent(MainActivity.this), Constants.ACTIVITY_REQUEST_BROWSE_TABS);
                }
            });
            tabCountsView.updateTabCount();
            tabCountsView.setContentDescription(getString(R.string.menu_page_show_tabs));
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

            if (tab.equals(NavTab.SUGGESTED_EDITS)) {
                getFragment().hideNavTabOverlayLayout();
            }

            hamburgerAndWordmarkLayout.setVisibility(GONE);
            toolbar.setTitle(tab.text());
            controlNavTabInFragment = true;
        }
        getFragment().requestUpdateToolbarElevation();
    }

    void setUpHomeMenuIcon() {
        drawerIconDot.setVisibility(AccountUtil.isLoggedIn() && Prefs.showActionFeedIndicator() ? VISIBLE : GONE);
    }

    private void onDrawerOpenClicked() {
        drawerLayout.openDrawer(drawerView);
    }

    @Override
    public void onNavMenuSwipeRequest(int gravity) {
        if (currentActionMode == null && gravity == Gravity.START) {
            drawerLayout.post(this::onDrawerOpenClicked);
        }
    }

    @Override
    public void onSupportActionModeStarted(@NonNull ActionMode mode) {
        super.onSupportActionModeStarted(mode);
        currentActionMode = mode;
        if (!controlNavTabInFragment) {
            getFragment().setBottomNavVisible(false);
        }
    }

    @Override
    public void onSupportActionModeFinished(@NonNull ActionMode mode) {
        super.onSupportActionModeFinished(mode);
        getFragment().setBottomNavVisible(true);
        currentActionMode = null;
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

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        return imageZoomHelper.onDispatchTouchEvent(event) || super.dispatchTouchEvent(event);
    }

    public void closeMainDrawer() {
        drawerLayout.closeDrawer(GravityCompat.START);
    }

    public Toolbar getToolbar() {
        return toolbar;
    }

    public boolean isCurrentFragmentSelected(@NonNull Fragment fragment) {
        return getFragment().getCurrentFragment() == fragment;
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
                            Prefs.setReadingListsLastSyncTime(null);
                            Prefs.setReadingListSyncEnabled(false);
                            getFragment().resetNavTabLayouts();
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
            getFragment().startActivityForResult(SettingsActivity.newIntent(MainActivity.this), Constants.ACTIVITY_REQUEST_SETTINGS);
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
