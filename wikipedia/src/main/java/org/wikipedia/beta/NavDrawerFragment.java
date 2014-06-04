package org.wikipedia.beta;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.Fragment;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import org.wikipedia.beta.analytics.LoginFunnel;
import org.wikipedia.beta.bookmarks.BookmarksActivity;
import org.wikipedia.beta.events.RequestMainPageEvent;
import org.wikipedia.beta.history.HistoryActivity;
import org.wikipedia.beta.login.LoginActivity;
import org.wikipedia.beta.page.PageActivity;
import org.wikipedia.beta.random.RandomHandler;
import org.wikipedia.beta.settings.SettingsActivity;

public class NavDrawerFragment extends Fragment implements View.OnClickListener {
    private static final int[] ACTION_ITEMS_ALL = {
            R.id.nav_item_history,
            R.id.nav_item_saved_pages,
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

        randomHandler = new RandomHandler(getActivity());
    }

    private View usernameContainer;
    private View loginContainer;
    private TextView usernamePrimaryText;
    private void setupDynamicItems() {
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
        if (WikipediaApp.isWikipediaZeroDevmodeOn() && WikipediaApp.getWikipediaZeroDisposition()) {
            wikipediaZeroText.setText(WikipediaApp.getCarrierMessage());
            wikipediaZeroText.setVisibility(View.VISIBLE);
        } else {
            wikipediaZeroText.setVisibility(View.GONE);
        }
    }

    private void doLogout() {
        app.getEditTokenStorage().clearAllTokens();
        app.getCookieManager().clearAllCookies();
        app.getUserInfoStorage().clearUser();
        Toast.makeText(getActivity(), R.string.toast_logout_complete, Toast.LENGTH_LONG).show();
        setupDynamicItems();
    }

    @Override
    public void onClick(View view) {
        Intent intent = new Intent();
        switch (view.getId()) {
            case R.id.nav_item_history:
                intent.setClass(this.getActivity(), HistoryActivity.class);
                getActivity().startActivityForResult(intent, PageActivity.ACTIVITY_REQUEST_HISTORY);
                break;
            case R.id.nav_item_saved_pages:
                intent.setClass(this.getActivity(), BookmarksActivity.class);
                getActivity().startActivityForResult(intent, PageActivity.ACTIVITY_REQUEST_BOOKMARKS);
                break;
            case R.id.nav_item_more:
                intent.setClass(this.getActivity(), SettingsActivity.class);
                startActivityForResult(intent, SettingsActivity.ACTIVITY_REQUEST_SHOW_SETTINGS);
                break;
            case R.id.nav_item_login:
                intent.setClass(this.getActivity(), LoginActivity.class);
                intent.putExtra(LoginActivity.LOGIN_REQUEST_SOURCE, LoginFunnel.SOURCE_NAV);
                startActivity(intent);
                break;
            case R.id.nav_item_random:
                randomHandler.doVistRandomArticle();
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
                        WikipediaApp.getInstance().getBus().post(new RequestMainPageEvent());
                        Log.d("Wikipedia", "Show da main page yo");
                    }
                }, DateUtils.SECOND_IN_MILLIS);
            } else if (resultCode == SettingsActivity.ACTIVITY_RESULT_LOGOUT) {
                doLogout();
            }
        }
    }
}
