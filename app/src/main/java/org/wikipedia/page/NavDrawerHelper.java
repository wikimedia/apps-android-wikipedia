package org.wikipedia.page;

import android.content.Intent;
import android.net.Uri;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import org.wikipedia.BuildConfig;
import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.analytics.LoginFunnel;
import org.wikipedia.analytics.NavMenuFunnel;
import org.wikipedia.history.HistoryEntry;
import org.wikipedia.history.HistoryFragment;
import org.wikipedia.login.LoginActivity;
import org.wikipedia.nearby.NearbyFragment;
import org.wikipedia.random.RandomHandler;
import org.wikipedia.savedpages.SavedPagesFragment;
import org.wikipedia.settings.SettingsActivity;
import org.wikipedia.util.FeedbackUtil;
import org.wikipedia.util.UriUtil;

public class NavDrawerHelper {

    private final WikipediaApp app = WikipediaApp.getInstance();
    private final PageActivity activity;
    private NavMenuFunnel funnel;
    private TextView accountNameView;
    private ImageView accountNameArrow;
    private boolean accountToggle;

    public NavDrawerHelper(@NonNull PageActivity activity, View navDrawerHeader) {
        this.funnel = new NavMenuFunnel();
        this.activity = activity;
        activity.getSupportFragmentManager()
                .addOnBackStackChangedListener(new FragmentManager.OnBackStackChangedListener() {
                    @Override
                    public void onBackStackChanged() {
                        updateItemSelection(NavDrawerHelper.this.activity.getTopFragment());
                    }
                });
        accountNameView = (TextView) navDrawerHeader.findViewById(R.id.nav_account_text);
        accountNameArrow = (ImageView) navDrawerHeader.findViewById(R.id.nav_account_arrow);
        setLoginOnClick(navDrawerHeader.findViewById(R.id.nav_account_container));
        updateMenuGroupToggle();
    }

    public NavMenuFunnel getFunnel() {
        return funnel;
    }

    public void setupDynamicNavDrawerItems() {
        updateLoginButtonStatus();
        updateWikipediaZeroStatus();
        accountToggle = false;
        updateMenuGroupToggle();
    }

    public NavigationView.OnNavigationItemSelectedListener getNewListener() {
        return new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(MenuItem menuItem) {
                switch (menuItem.getItemId()) {
                    case R.id.nav_item_today:
                        activity.loadMainPageInCurrentTab();
                        funnel.logToday();
                        break;
                    case R.id.nav_item_history:
                        activity.pushFragment(new HistoryFragment());
                        funnel.logHistory();
                        break;
                    case R.id.nav_item_saved_pages:
                        activity.pushFragment(new SavedPagesFragment());
                        funnel.logSavedPages();
                        break;
                    case R.id.nav_item_nearby:
                        activity.pushFragment(new NearbyFragment());
                        funnel.logNearby();
                        break;
                    case R.id.nav_item_more:
                        launchSettingsActivity();
                        funnel.logMore();
                        break;
                    case R.id.nav_item_logout:
                        logout();
                        break;
                    case R.id.nav_item_random:
                        activity.getRandomHandler().doVisitRandomArticle();
                        funnel.logRandom();
                        break;
                    case R.id.nav_item_donate:
                        openDonatePage();
                        break;
                    default:
                        return false;
                }
                clearItemHighlighting();
                menuItem.setChecked(true);
                activity.setNavItemSelected(true);
                return true;
            }
        };
    }

    public RandomHandler getNewRandomHandler() {
        return new RandomHandler(activity, new RandomHandler.RandomListener() {
                    @Override
                    public void onRandomPageReceived(@Nullable PageTitle title) {
                        HistoryEntry historyEntry = new HistoryEntry(title, HistoryEntry.SOURCE_RANDOM);
                        activity.loadPage(title, historyEntry, PageActivity.TabPosition.CURRENT_TAB, true);
                    }

                    @Override
                    public void onRandomPageFailed(Throwable caught) {
                        FeedbackUtil.showError(activity.getContentView(), caught);
                    }
                });
    }

    public void updateItemSelection(Fragment fragment) {
        @IdRes Integer id = fragmentToMenuId(fragment);
        if (id != null) {
            setMenuItemSelection(id);
        }
    }

    private void setMenuItemSelection(@IdRes int id) {
        clearItemHighlighting();

        // Special case: don't highlight today if it's not the main page.
        if (id != R.id.nav_item_today || isMainPage()) {
            MenuItem menuItem = activity.getNavMenu().findItem(id);
            menuItem.setChecked(true);
        }
    }

    private void toggleAccountMenu() {
        accountToggle = !accountToggle;
        updateMenuGroupToggle();
    }

    private void updateMenuGroupToggle() {
        activity.getNavMenu().setGroupVisible(R.id.group_main, !accountToggle);
        activity.getNavMenu().setGroupVisible(R.id.group_user, accountToggle);
        accountNameArrow.setVisibility(app.getUserInfoStorage().isLoggedIn() ? View.VISIBLE : View.INVISIBLE);
        accountNameArrow.setImageDrawable(accountToggle
                ? activity.getResources().getDrawable(R.drawable.ic_arrow_drop_up_white_24dp)
                : activity.getResources().getDrawable(R.drawable.ic_arrow_drop_down_white_24dp));
    }

    private void setLoginOnClick(View view) {
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (app.getUserInfoStorage().isLoggedIn()) {
                    toggleAccountMenu();
                } else {
                    launchLoginActivity();
                    funnel.logLogin();
                }
            }
        });
    }

    private boolean isMainPage() {
        return activity.getCurPageFragment() != null
                && activity.getCurPageFragment().getPage() != null
                && activity.getCurPageFragment().getPage().isMainPage();
    }

    @Nullable @IdRes private Integer fragmentToMenuId(Fragment fragment) {
        if (fragment instanceof PageFragment) {
            return R.id.nav_item_today;
        } else if (fragment instanceof HistoryFragment) {
            return R.id.nav_item_history;
        } else if (fragment instanceof SavedPagesFragment) {
            return R.id.nav_item_saved_pages;
        } else if (fragment instanceof NearbyFragment) {
            return R.id.nav_item_nearby;
        }
        return null;
    }

    /**
     * Update login menu item to reflect login status.
     */
    private void updateLoginButtonStatus() {
        if (app.getUserInfoStorage().isLoggedIn()) {
            accountNameView.setText(app.getUserInfoStorage().getUser().getUsername());
        } else {
            accountNameView.setText(app.getResources().getString(R.string.nav_item_login));
        }
    }

    /**
     * Add Wikipedia Zero entry to nav menu if W0 is active.
     */
    private void updateWikipediaZeroStatus() {
        MenuItem wikipediaZeroText = activity.getNavMenu().findItem(R.id.nav_item_zero);
        if (app.getWikipediaZeroHandler().isZeroEnabled()) {
            wikipediaZeroText.setTitle(app.getWikipediaZeroHandler().getZeroConfig().getMessage());
            wikipediaZeroText.setVisible(true);
        } else {
            wikipediaZeroText.setVisible(false);
        }
    }

    /**
     * Un-highlight all nav menu entries.
     */
    private void clearItemHighlighting() {
        for (int i = 0; i < activity.getNavMenu().size(); i++) {
            activity.getNavMenu().getItem(i).setChecked(false);
        }
    }

    private void launchSettingsActivity() {
        activity.closeNavDrawer();
        activity.startActivityForResult(new Intent().setClass(app, SettingsActivity.class),
                SettingsActivity.ACTIVITY_REQUEST_SHOW_SETTINGS);
    }

    private void launchLoginActivity() {
        activity.closeNavDrawer();
        activity.startActivityForResult(LoginActivity.newIntent(app, LoginFunnel.SOURCE_NAV),
                LoginActivity.REQUEST_LOGIN);
    }

    private void logout() {
        app.getEditTokenStorage().clearAllTokens();
        app.getCookieManager().clearAllCookies();
        app.getUserInfoStorage().clearUser();
        activity.closeNavDrawer();
        FeedbackUtil.showMessage(activity, R.string.toast_logout_complete);
    }

    private void openDonatePage() {
        activity.closeNavDrawer();
        UriUtil.visitInExternalBrowser(activity,
                Uri.parse(String.format(activity.getString(R.string.donate_url),
                        BuildConfig.VERSION_NAME, app.getSystemLanguageCode())));
    }
}
