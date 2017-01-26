package org.wikipedia.descriptions;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.wikipedia.Constants;
import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.activity.FragmentUtil;
import org.wikipedia.csrf.CsrfToken;
import org.wikipedia.csrf.CsrfTokenClient;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.json.GsonMarshaller;
import org.wikipedia.json.GsonUnmarshaller;
import org.wikipedia.login.LoginClient;
import org.wikipedia.login.LoginClient.LoginFailedException;
import org.wikipedia.login.LoginResult;
import org.wikipedia.login.User;
import org.wikipedia.page.PageTitle;
import org.wikipedia.settings.Prefs;
import org.wikipedia.util.log.L;

import java.util.Date;
import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import retrofit2.Call;

import static org.wikipedia.util.DeviceUtil.hideSoftKeyboard;

public class DescriptionEditFragment extends Fragment {

    public interface Callback {
        void onDescriptionEditSuccess();
    }

    private static final String ARG_TITLE = "title";

    @BindView(R.id.fragment_description_edit_view) DescriptionEditView editView;
    private Unbinder unbinder;
    private PageTitle pageTitle;
    @Nullable private Call<CsrfToken> editTokenCall;
    @Nullable private Call<DescriptionEdit> descriptionEditCall;

    private Runnable successRunnable = new Runnable() {
        @Override public void run() {
            if (!User.isLoggedIn()) {
                Prefs.incrementTotalAnonDescriptionsEdited();
            }
            Prefs.setLastDescriptionEditTime(new Date().getTime());
            WikipediaApp.getInstance().listenForNotifications();

            if (getActivity() == null)  {
                return;
            }
            editView.setSaveState(false);
            startActivityForResult(DescriptionEditSuccessActivity.newIntent(getContext()),
                    Constants.ACTIVITY_REQUEST_DESCRIPTION_EDIT_SUCCESS);
        }
    };

    @NonNull
    public static DescriptionEditFragment newInstance(@NonNull PageTitle title) {
        DescriptionEditFragment instance = new DescriptionEditFragment();
        Bundle args = new Bundle();
        args.putString(ARG_TITLE, GsonMarshaller.marshal(title));
        instance.setArguments(args);
        return instance;
    }

    @Override public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        pageTitle = GsonUnmarshaller.unmarshal(PageTitle.class, getArguments().getString(ARG_TITLE));
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View view = inflater.inflate(R.layout.fragment_description_edit, container, false);
        unbinder = ButterKnife.bind(this, view);

        editView.setPageTitle(pageTitle);
        editView.setCallback(new EditViewCallback());
        return view;
    }

    @Override public void onDestroyView() {
        editView.setCallback(null);
        unbinder.unbind();
        unbinder = null;
        super.onDestroyView();
    }

    @Override public void onDestroy() {
        cancelCalls();
        pageTitle = null;
        super.onDestroy();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, final Intent data) {
        if (requestCode == Constants.ACTIVITY_REQUEST_DESCRIPTION_EDIT_SUCCESS
                && getActivity() != null) {
            if (callback() != null) {
                callback().onDescriptionEditSuccess();
            }
        }
    }

    private void cancelCalls() {
        // in reverse chronological order
        if (descriptionEditCall != null) {
            descriptionEditCall.cancel();
            descriptionEditCall = null;
        }
        if (editTokenCall != null) {
            editTokenCall.cancel();
            editTokenCall = null;
        }
    }

    private void finish() {
        hideSoftKeyboard(getActivity());
        getActivity().finish();
    }

    private Callback callback() {
        return FragmentUtil.getCallback(this, Callback.class);
    }

    private class EditViewCallback implements DescriptionEditView.Callback {
        private static final int MAX_RETRIES = 1;
        private int retries = 0;
        private final WikiSite wikiData = new WikiSite("www.wikidata.org", "");
        private final WikiSite loginWiki = pageTitle.getWikiSite();

        @Override
        public void onSaveClick() {
            editView.setError(null);
            editView.setSaveState(true);

            cancelCalls();
            requestEditToken();
        }

        /** fetch edit token from Wikidata */
        private void requestEditToken() {
            editTokenCall = new CsrfTokenClient().request(wikiData,
                    new CsrfTokenClient.Callback() {
                        @Override public void success(@NonNull Call<CsrfToken> call,
                                                      @NonNull String editToken) {
                            postDescription(editToken);
                        }

                        @Override public void failure(@NonNull Call<CsrfToken> call,
                                                      @NonNull Throwable caught) {
                            L.w("could not get edit token: ", caught);
                            if (retries < MAX_RETRIES && User.getUser() != null) {
                                retries++;
                                refreshLoginTokens(User.getUser(), new RetryCallback() {
                                    @Override public void retry() {
                                        L.i("retrying...");
                                        requestEditToken();
                                    }
                                });
                            } else {
                                editFailed(caught);
                            }
                        }
                    });
        }

        /**
         * Refresh all tokens/cookies, then retry the previous request.
         * This is needed when the edit token was not retrieved because the login session expired.
         *
         * @param user user info to be able to log in
         * @param retryCallback a repeat of the action which failed before
         * @throws IllegalArgumentException if user is not logged in
         */
        private void refreshLoginTokens(User user, @NonNull RetryCallback retryCallback) {
            WikipediaApp app = WikipediaApp.getInstance();
            app.getCsrfTokenStorage().clearAllTokens();
            app.getCookieManager().clearAllCookies();

            login(user, retryCallback);
        }

        private void login(@NonNull final User user, @NonNull final RetryCallback retryCallback) {
            new LoginClient().request(loginWiki,
                    user.getUsername(),
                    user.getPassword(),
                    new LoginClient.LoginCallback() {
                        @Override
                        public void success(@NonNull LoginResult loginResult) {
                            if (loginResult.pass()) {
                                retryCallback.retry();
                            } else {
                                editFailed(new LoginFailedException(loginResult.getMessage()));
                            }
                        }

                        @Override
                        public void twoFactorPrompt(@NonNull Throwable caught, @Nullable String token) {
                            editFailed(new LoginFailedException(getResources()
                                            .getString(R.string.login_2fa_other_workflow_error_msg)));
                        }

                        @Override
                        public void error(@NonNull Throwable caught) {
                            editFailed(caught);
                        }
                    });
        }

        /* send updated description to Wikidata */
        private void postDescription(@NonNull String editToken) {
            descriptionEditCall = new DescriptionEditClient().request(wikiData, pageTitle,
                    editView.getDescription(), editToken,
                    new DescriptionEditClient.Callback() {
                        @Override @SuppressWarnings("checkstyle:magicnumber")
                        public void success(@NonNull Call<DescriptionEdit> call) {
                            // TODO: remove this artificial delay if someday we get a reliable way
                            // to determine whether the change has propagated to the relevant
                            // RESTBase endpoints.
                            new Handler().postDelayed(successRunnable, TimeUnit.SECONDS.toMillis(4));
                        }

                        @Override public void abusefilter(@NonNull Call<DescriptionEdit> call,
                                                          String info) {
                            editView.setSaveState(false);
                            editView.setError(info);
                        }

                        @Override public void failure(@NonNull Call<DescriptionEdit> call,
                                                      @NonNull Throwable caught) {
                            editFailed(caught);
                        }
                    });
        }

        private void editFailed(@NonNull Throwable caught) {
            if (editView != null) {
                editView.setSaveState(false);
                editView.setError(caught.getMessage());
            }
        }

        @Override
        public void onHelpClick() {
            startActivity(DescriptionEditHelpActivity.newIntent(getContext()));
        }

        @Override
        public void onCancelClick() {
            finish();
        }
    }

    private interface RetryCallback {
        void retry();
    }
}