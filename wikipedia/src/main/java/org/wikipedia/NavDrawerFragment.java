package org.wikipedia;

import android.content.*;
import android.net.*;
import android.os.*;
import android.support.v4.app.*;
import android.view.*;
import android.widget.*;
import org.wikipedia.analytics.*;
import org.wikipedia.history.*;
import org.wikipedia.login.*;
import org.wikipedia.random.*;
import org.wikipedia.savedpages.*;
import org.wikipedia.settings.*;

public class NavDrawerFragment extends Fragment implements View.OnClickListener {
    private static final int[] ACTION_ITEMS_ALL = {
            R.id.nav_item_history,
            R.id.nav_item_saved_pages,
            R.id.nav_item_settings,
            R.id.nav_item_login,
            R.id.nav_item_random,
            R.id.nav_item_send_feedback,
            R.id.nav_item_logout
            // We don't actually need R.id.nav_item_zero here because we add it programmatically
            // below, and we don't need an on-tap as of now
    };

    private static final int[] ACTION_ITEMS_LOGGED_IN_ONLY = {
            R.id.nav_item_username,
            R.id.nav_item_logout
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

        ((TextView)getView().findViewById(R.id.nav_drawer_version)).setText(WikipediaApp.APP_VERSION_STRING);

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
            for (int i = 0; i < loggedInOnyActionViews.length; i++) {
                loggedInOnyActionViews[i].setVisibility(View.VISIBLE);
            }
            usernamePrimaryText.setText(app.getUserInfoStorage().getUser().getUsername());
        } else {
            loginContainer.setVisibility(View.VISIBLE);
            for (int i = 0; i < loggedInOnyActionViews.length; i++) {
                loggedInOnyActionViews[i].setVisibility(View.GONE);
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
                getActivity().startActivity(intent);
                break;
            case R.id.nav_item_saved_pages:
                intent.setClass(this.getActivity(), SavedPagesActivity.class);
                startActivity(intent);
                break;
            case R.id.nav_item_settings:
                intent.setClass(this.getActivity(), SettingsActivity.class);
                startActivity(intent);
                break;
            case R.id.nav_item_login:
                intent.setClass(this.getActivity(), LoginActivity.class);
                intent.putExtra(LoginActivity.LOGIN_REQUEST_SOURCE, LoginFunnel.SOURCE_NAV);
                startActivity(intent);
                break;
            case R.id.nav_item_random:
                randomHandler.doVistRandomArticle();
                break;
            case R.id.nav_item_send_feedback:
                // Will be stripped out in prod builds
                intent.setAction(Intent.ACTION_SENDTO);
                // Will be moved to a better email address at some point
                intent.setData(Uri.parse("mailto:mobile-android-wikipedia@wikimedia.org?subject=Android App " + WikipediaApp.APP_VERSION_STRING + " Feedback"));
                startActivity(intent);
                break;
            case R.id.nav_item_logout:
                doLogout();
                break;
            default:
                throw new RuntimeException("Unknown ID clicked!");
        }
    }

}
