package org.wikipedia.page;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.NavigationView;
import android.view.MenuItem;

import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.analytics.LoginFunnel;
import org.wikipedia.history.HistoryEntry;
import org.wikipedia.history.HistoryFragment;
import org.wikipedia.login.LoginActivity;
import org.wikipedia.nearby.NearbyFragment;
import org.wikipedia.random.RandomHandler;
import org.wikipedia.savedpages.SavedPagesFragment;
import org.wikipedia.settings.SettingsActivity;
import org.wikipedia.settings.SettingsActivityGB;
import org.wikipedia.util.ApiUtil;
import org.wikipedia.util.FeedbackUtil;

public class NavDrawerHelper {

    private static final int[] NAV_DRAWER_ACTION_ITEMS_LOGGED_IN_ONLY = {
            R.id.nav_item_username
    };

    private final WikipediaApp app = WikipediaApp.getInstance();
    private final PageActivity page;
    private MenuItem usernameContainer;
    private MenuItem loginContainer;
    private MenuItem[] loggedInOnyActionViews = new MenuItem[NAV_DRAWER_ACTION_ITEMS_LOGGED_IN_ONLY.length];

    public NavDrawerHelper(@NonNull PageActivity page) {
        this.page = page;
    }

    public void setupDynamicNavDrawerItems() {
        for (int i = 0; i < NAV_DRAWER_ACTION_ITEMS_LOGGED_IN_ONLY.length; i++) {
            loggedInOnyActionViews[i] = page.getNavMenu().findItem(NAV_DRAWER_ACTION_ITEMS_LOGGED_IN_ONLY[i]);
        }

        if (usernameContainer == null) {
            usernameContainer = page.getNavMenu().findItem(R.id.nav_item_username);
            loginContainer = page.getNavMenu().findItem(R.id.nav_item_login);
        }

        updateLoginButtonStatus();
        updateWikipediaZeroStatus();
    }

    public NavigationView.OnNavigationItemSelectedListener getNewListener() {
        return new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(MenuItem menuItem) {
                switch (menuItem.getItemId()) {
                    case R.id.nav_item_today:
                        page.displayMainPageInCurrentTab();
                        break;
                    case R.id.nav_item_history:
                        page.pushFragment(new HistoryFragment());
                        break;
                    case R.id.nav_item_saved_pages:
                        page.pushFragment(new SavedPagesFragment());
                        break;
                    case R.id.nav_item_nearby:
                        page.pushFragment(new NearbyFragment());
                        break;
                    case R.id.nav_item_more:
                        launchSettingsActivity();
                        break;
                    case R.id.nav_item_login:
                        launchLoginActivity();
                        break;
                    case R.id.nav_item_random:
                        page.getRandomHandler().doVisitRandomArticle();
                        break;
                    default:
                        return false;
                }
                clearItemHighlighting();
                menuItem.setChecked(true);
                return true;
            }
        };
    }

    public RandomHandler getNewRandomHandler() {
        return new RandomHandler(page.getNavMenu().findItem(R.id.nav_item_random),
                new RandomHandler.RandomListener() {
                    @Override
                    public void onRandomPageReceived(@Nullable PageTitle title) {
                        HistoryEntry historyEntry = new HistoryEntry(title, HistoryEntry.SOURCE_RANDOM);
                        page.displayNewPage(title, historyEntry, PageActivity.TabPosition.CURRENT_TAB, true);
                    }

                    @Override
                    public void onRandomPageFailed(Throwable caught) {
                        FeedbackUtil.showError(page.getContentView(), caught);
                    }
                });
    }

    /**
     * Update login menu item to reflect login status.
     */
    private void updateLoginButtonStatus() {
        if (app.getUserInfoStorage().isLoggedIn()) {
            loginContainer.setVisible(false);
            for (MenuItem loggedInOnyActionView : loggedInOnyActionViews) {
                loggedInOnyActionView.setVisible(true);
            }
            usernameContainer.setTitle(app.getUserInfoStorage().getUser().getUsername());
        } else {
            loginContainer.setVisible(true);
            for (MenuItem loggedInOnyActionView : loggedInOnyActionViews) {
                loggedInOnyActionView.setVisible(false);
            }
        }
    }

    /**
     * Add Wikipedia Zero entry to nav menu if W0 is active.
     */
    private void updateWikipediaZeroStatus() {
        MenuItem wikipediaZeroText = page.getNavMenu().findItem(R.id.nav_item_zero);
        if (app.getWikipediaZeroHandler().isZeroEnabled()) {
            wikipediaZeroText.setTitle(app.getWikipediaZeroHandler().getCarrierMessage().getMsg());
            wikipediaZeroText.setVisible(true);
        } else {
            wikipediaZeroText.setVisible(false);
        }
    }

    /**
     * Un-highlight all nav menu entries.
     */
    private void clearItemHighlighting() {
        for (int i = 0; i < page.getNavMenu().size(); i++) {
            page.getNavMenu().getItem(i).setChecked(false);
        }
    }

    private void launchSettingsActivity() {
        page.startActivityForResult(new Intent().setClass(app, ApiUtil.hasHoneyComb() ? SettingsActivity.class : SettingsActivityGB.class),
                SettingsActivity.ACTIVITY_REQUEST_SHOW_SETTINGS);
    }

    private void launchLoginActivity() {
        page.startActivity(new Intent(app, LoginActivity.class)
                .putExtra(LoginActivity.LOGIN_REQUEST_SOURCE, LoginFunnel.SOURCE_NAV));
    }
}