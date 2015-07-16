package org.wikipedia;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import org.wikipedia.analytics.LoginFunnel;
import org.wikipedia.history.HistoryEntry;
import org.wikipedia.history.HistoryFragment;
import org.wikipedia.login.LoginActivity;
import org.wikipedia.nearby.NearbyFragment;
import org.wikipedia.page.PageActivity;
import org.wikipedia.page.PageTitle;
import org.wikipedia.page.PageViewFragmentInternal;
import org.wikipedia.random.RandomHandler;
import org.wikipedia.savedpages.SavedPagesFragment;
import org.wikipedia.settings.SettingsActivity;
import org.wikipedia.settings.SettingsActivityGB;
import org.wikipedia.util.ApiUtil;
import org.wikipedia.widgets.WidgetProviderFeaturedPage;

public class NavDrawerFragment extends Fragment implements View.OnClickListener {
    private static final int[] ACTION_ITEMS_ALL = {
            R.id.nav_item_today,
            R.id.nav_item_history,
            R.id.nav_item_saved_pages,
            R.id.nav_item_nearby,
            R.id.nav_item_more,
            R.id.nav_item_login,
            R.id.nav_item_random
            // We don't actually need R.id.nav_item_zero here because we add it programmatically
            // below, and we don't need an on-tap as of now
    };

    private static final int[] ACTION_ITEMS_LOGGED_IN_ONLY = {
            R.id.nav_item_username
    };

    private View[] actionViews = new View[ACTION_ITEMS_ALL.length];
    private View[] loggedInOnyActionViews = new View[ACTION_ITEMS_LOGGED_IN_ONLY.length];
    private WikipediaApp app;
    private RandomHandler randomHandler;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_navdrawer, container, false);
    }

    @Override
    public void onResume() {
        super.onResume();

        // Ensure that Login / Logout and Wikipedia Zero status is accurate
        setupDynamicItems();
    }

    private TextView wikipediaZeroText;
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        app = (WikipediaApp)getActivity().getApplicationContext();

        for (int i = 0; i < ACTION_ITEMS_ALL.length; i++) {
            actionViews[i] = getView().findViewById(ACTION_ITEMS_ALL[i]);
            actionViews[i].setOnClickListener(this);
        }

        for (int i = 0; i < ACTION_ITEMS_LOGGED_IN_ONLY.length; i++) {
            loggedInOnyActionViews[i] = getView().findViewById(ACTION_ITEMS_LOGGED_IN_ONLY[i]);
        }

        wikipediaZeroText = (TextView) getView().findViewById(R.id.nav_item_zero);

        randomHandler = new RandomHandler(getView().findViewById(R.id.nav_item_random),
                getView().findViewById(R.id.nav_item_random_icon),
                getView().findViewById(R.id.nav_item_random_progressbar),
                new RandomHandler.RandomListener() {
                    @Override
                    public void onRandomPageReceived(@Nullable PageTitle title) {
                        if (!isAdded()) {
                            return;
                        }
                        if (title == null) {
                            // There was an error fetching the random page. Show a network error.
                            ((PageActivity) getActivity()).showNetworkError();
                        } else {
                            HistoryEntry historyEntry = new HistoryEntry(title, HistoryEntry.SOURCE_RANDOM);
                            ((PageActivity) getActivity()).displayNewPage(title, historyEntry, true);
                        }
                    }
                });
    }

    private View usernameContainer;
    private View loginContainer;
    private TextView usernamePrimaryText;
    public void setupDynamicItems() {
        if (usernameContainer == null) {
            usernameContainer = getView().findViewById(R.id.nav_item_username);
            usernamePrimaryText = (TextView) usernameContainer.findViewById(R.id.nav_item_username_primary_text);
            loginContainer = getView().findViewById(R.id.nav_item_login);
        }
        // Do login / logout swap
        if (app.getUserInfoStorage().isLoggedIn()) {
            loginContainer.setVisibility(View.GONE);
            for (View loggedInOnyActionView : loggedInOnyActionViews) {
                loggedInOnyActionView.setVisibility(View.VISIBLE);
            }
            usernamePrimaryText.setText(app.getUserInfoStorage().getUser().getUsername());
        } else {
            loginContainer.setVisibility(View.VISIBLE);
            for (View loggedInOnyActionView : loggedInOnyActionViews) {
                loggedInOnyActionView.setVisibility(View.GONE);
            }
        }

        // Show Wikipedia Zero if ON, otherwise hide it
        if (app.getWikipediaZeroHandler().isZeroEnabled()) {
            wikipediaZeroText.setText(app.getWikipediaZeroHandler().getCarrierMessage().getMsg());
            wikipediaZeroText.setVisibility(View.VISIBLE);
        } else {
            wikipediaZeroText.setVisibility(View.GONE);
        }

        // highlight the correct list item based on which fragment is on top.
        // un-highlight all of them first...
        for (int id : ACTION_ITEMS_ALL) {
            getView().findViewById(id).setBackgroundResource(R.drawable.nav_item_background);
        }
        PageActivity activity = (PageActivity)getActivity();
        int highlightItem = -1;
        if (activity.getTopFragment() instanceof HistoryFragment) {
            highlightItem = R.id.nav_item_history;
        } else if (activity.getTopFragment() instanceof SavedPagesFragment) {
            highlightItem = R.id.nav_item_saved_pages;
        } else if (activity.getTopFragment() instanceof NearbyFragment) {
            highlightItem = R.id.nav_item_nearby;
        } else if (activity.getTopFragment() instanceof PageViewFragmentInternal) {
            PageViewFragmentInternal fragment = (PageViewFragmentInternal)activity.getTopFragment();
            if (fragment.getHistoryEntry() != null) {
                if (fragment.getHistoryEntry().getSource() == HistoryEntry.SOURCE_MAIN_PAGE) {
                    highlightItem = R.id.nav_item_today;
                } else if (fragment.getHistoryEntry().getSource() == HistoryEntry.SOURCE_RANDOM) {
                    highlightItem = R.id.nav_item_random;
                }
            }
        }
        if (highlightItem != -1) {
            getView().findViewById(highlightItem).setBackgroundColor(getResources().getColor(R.color.nav_item_highlight));
        }
    }

    private void doLogout() {
        boolean doUpdate = true;
        if (app == null) {
            // we haven't yet been attached to our Activity, so don't worry about
            // updating dynamic items
            doUpdate = false;
            app = WikipediaApp.getInstance();
        }
        app.getEditTokenStorage().clearAllTokens();
        app.getCookieManager().clearAllCookies();
        app.getUserInfoStorage().clearUser();
        Toast.makeText(app, R.string.toast_logout_complete, Toast.LENGTH_LONG).show();
        if (doUpdate) {
            setupDynamicItems();
        }
    }

    @Override
    public void onClick(View view) {
        Intent intent = new Intent();
        switch (view.getId()) {
            case R.id.nav_item_today:
                ((PageActivity)getActivity()).displayMainPage();
                break;
            case R.id.nav_item_history:
                ((PageActivity)getActivity()).pushFragment(new HistoryFragment());
                break;
            case R.id.nav_item_saved_pages:
                ((PageActivity)getActivity()).pushFragment(new SavedPagesFragment());
                break;
            case R.id.nav_item_nearby:
                ((PageActivity)getActivity()).pushFragment(new NearbyFragment());
                break;
            case R.id.nav_item_more:
                if (ApiUtil.hasHoneyComb()) {
                    intent.setClass(this.getActivity(), SettingsActivity.class);
                } else {
                    intent.setClass(this.getActivity(), SettingsActivityGB.class);
                }
                startActivityForResult(intent, SettingsActivity.ACTIVITY_REQUEST_SHOW_SETTINGS);
                break;
            case R.id.nav_item_login:
                intent.setClass(this.getActivity(), LoginActivity.class);
                intent.putExtra(LoginActivity.LOGIN_REQUEST_SOURCE, LoginFunnel.SOURCE_NAV);
                startActivity(intent);
                break;
            case R.id.nav_item_random:
                randomHandler.doVisitRandomArticle();
                break;
            default:
                throw new RuntimeException("Unknown ID clicked!");
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == SettingsActivity.ACTIVITY_REQUEST_SHOW_SETTINGS) {
            if (resultCode == SettingsActivity.ACTIVITY_RESULT_LANGUAGE_CHANGED) {
                // Run the code a second later, to:
                // - Make sure that onStart in PageActivity gets called, thus
                //   registering the activity for the bus.
                // - The 1s delay ensures a smoother transition, otherwise was
                //   very jarring
                Handler uiThread = new Handler(Looper.getMainLooper());
                uiThread.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (!isAdded()) {
                            return;
                        }
                        ((PageActivity)getActivity()).displayMainPage(true);
                        // and update any instances of our Featured Page widget, since it will
                        // change with the currently selected language.
                        Intent widgetIntent = new Intent(getActivity(), WidgetProviderFeaturedPage.class);
                        widgetIntent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
                        int[] ids = AppWidgetManager.getInstance(getActivity().getApplication()).getAppWidgetIds(
                                new ComponentName(getActivity().getApplication(), WidgetProviderFeaturedPage.class));
                        widgetIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
                        getActivity().sendBroadcast(widgetIntent);
                    }
                }, DateUtils.SECOND_IN_MILLIS);
            } else if (resultCode == SettingsActivity.ACTIVITY_RESULT_LOGOUT) {
                doLogout();
            }
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        randomHandler.onStop();
    }
}
