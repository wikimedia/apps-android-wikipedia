package org.wikipedia;

import android.app.*;
import android.content.*;
import android.net.*;
import android.os.*;
import android.support.v4.app.Fragment;
import android.view.*;
import android.widget.*;
import org.wikipedia.history.*;
import org.wikipedia.login.*;
import org.wikipedia.random.*;
import org.wikipedia.savedpages.*;
import org.wikipedia.settings.*;

public class NavDrawerFragment extends Fragment implements View.OnClickListener {
    private static final int[] ACTION_ITEMS_TEXT = {
            R.id.nav_item_history,
            R.id.nav_item_saved_pages,
            R.id.nav_item_settings,
            R.id.nav_item_login,
            R.id.nav_item_username,
            R.id.nav_item_random,
            R.id.nav_item_send_feedback
    };

    private View[] actionViews = new View[ACTION_ITEMS_TEXT.length];
    private WikipediaApp app;
    private RandomHandler randomHandler;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_navdrawer, container, false);
    }

    @Override
    public void onResume() {
        super.onResume();

        // Ensure that Login / Logout status is accurate
        setupDynamicItems();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        app = (WikipediaApp)getActivity().getApplicationContext();

        ((TextView)getView().findViewById(R.id.nav_drawer_version)).setText(WikipediaApp.APP_VERSION_STRING);

        for (int i = 0; i < ACTION_ITEMS_TEXT.length; i++) {
            actionViews[i] = getView().findViewById(ACTION_ITEMS_TEXT[i]);
            actionViews[i].setOnClickListener(this);
        }

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
            usernameContainer.setVisibility(View.VISIBLE);
            usernamePrimaryText.setText(app.getUserInfoStorage().getUser().getUsername());
        } else {
            usernameContainer.setVisibility(View.GONE);
            loginContainer.setVisibility(View.VISIBLE);
        }
    }

    private void doLogout() {
        final ProgressDialog progressDialog = new ProgressDialog(getActivity());
        progressDialog.setMessage(getString(R.string.logging_out_progress));
        progressDialog.setIndeterminate(true);

        new LogoutTask(app, app.getPrimarySite()) {
            @Override
            public void onBeforeExecute() {
                progressDialog.show();
            }

            @Override
            public void onFinish(Boolean result) {
                progressDialog.dismiss();
                setupDynamicItems();
            }
        }.execute();
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
                startActivity(intent);
                break;
            case R.id.nav_item_random:
                randomHandler.doVistRandomArticle();
                break;
            case R.id.nav_item_username:
                doLogout();
                break;
            case R.id.nav_item_send_feedback:
                // Will be stripped out in prod builds
                intent.setAction(Intent.ACTION_SENDTO);
                // Will be moved to a better email address at some point
                intent.setData(Uri.parse("mailto:yuvipanda@wikimedia.org?subject=Android App " + WikipediaApp.APP_VERSION_STRING + " Feedback"));
                startActivity(intent);
                break;
            default:
                throw new RuntimeException("Unknown ID clicked!");
        }
    }

}