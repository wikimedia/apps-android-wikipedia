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
import org.wikipedia.analytics.DescriptionEditFunnel;
import org.wikipedia.auth.AccountUtil;
import org.wikipedia.csrf.CsrfTokenClient;
import org.wikipedia.dataclient.Service;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.dataclient.mwapi.MwPostResponse;
import org.wikipedia.dataclient.page.PageClientFactory;
import org.wikipedia.json.GsonMarshaller;
import org.wikipedia.json.GsonUnmarshaller;
import org.wikipedia.login.LoginClient.LoginFailedException;
import org.wikipedia.page.PageTitle;
import org.wikipedia.settings.Prefs;
import org.wikipedia.util.FeedbackUtil;
import org.wikipedia.util.StringUtil;
import org.wikipedia.util.log.L;

import java.util.Date;
import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import retrofit2.Call;

import static org.wikipedia.util.DeviceUtil.hideSoftKeyboard;

public class DescriptionEditFragment extends Fragment {

    public interface Callback {
        void onDescriptionEditSuccess();
        void onPageSummaryContainerClicked(@NonNull PageTitle pageTitle);
    }

    private static final String ARG_TITLE = "title";
    private static final String ARG_REVIEW_ENABLED = "reviewEnabled";
    private static final String ARG_IS_TRANSLATION = "isTranslation";
    private static final String ARG_REVIEWING = "inReviewing";
    private static final String ARG_HIGHLIGHT_TEXT = "highlightText";
    private static final String ARG_TRANSLATION_SOURCE_LANG_DESC = "source_lang_desc";

    @BindView(R.id.fragment_description_edit_view) DescriptionEditView editView;
    private Unbinder unbinder;
    private PageTitle pageTitle;
    private boolean reviewEnabled;
    @Nullable private String highlightText;
    @Nullable private CsrfTokenClient csrfClient;
    @Nullable private Call<MwPostResponse> descriptionEditCall;
    @Nullable private DescriptionEditFunnel funnel;
    private CompositeDisposable disposables = new CompositeDisposable();

    private Runnable successRunnable = new Runnable() {
        @Override public void run() {
            if (!AccountUtil.isLoggedIn()) {
                Prefs.incrementTotalAnonDescriptionsEdited();
            } else {
                Prefs.incrementTotalUserDescriptionsEdited();
            }
            Prefs.setLastDescriptionEditTime(new Date().getTime());

            if (getActivity() == null)  {
                return;
            }
            editView.setSaveState(false);
            startActivityForResult(DescriptionEditSuccessActivity.newIntent(requireContext()),
                    Constants.ACTIVITY_REQUEST_DESCRIPTION_EDIT_SUCCESS);
        }
    };

    @NonNull
    public static DescriptionEditFragment newInstance(@NonNull PageTitle title, @Nullable String highlightText, boolean reviewEnabled, boolean isTranslation, CharSequence sourceDescription) {
        DescriptionEditFragment instance = new DescriptionEditFragment();
        Bundle args = new Bundle();
        args.putString(ARG_TITLE, GsonMarshaller.marshal(title));
        args.putString(ARG_HIGHLIGHT_TEXT, highlightText);
        args.putBoolean(ARG_REVIEW_ENABLED, reviewEnabled);
        args.putBoolean(ARG_IS_TRANSLATION, isTranslation);
        args.putCharSequence(ARG_TRANSLATION_SOURCE_LANG_DESC, sourceDescription);
        instance.setArguments(args);
        return instance;
    }

    @Override public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        pageTitle = GsonUnmarshaller.unmarshal(PageTitle.class, getArguments().getString(ARG_TITLE));
        reviewEnabled = getArguments().getBoolean(ARG_REVIEW_ENABLED);
        DescriptionEditFunnel.Type type = pageTitle.getDescription() == null
                ? DescriptionEditFunnel.Type.NEW
                : DescriptionEditFunnel.Type.EXISTING;
        highlightText = getArguments().getString(ARG_HIGHLIGHT_TEXT);
        funnel = new DescriptionEditFunnel(WikipediaApp.getInstance(), pageTitle, type);
        funnel.logStart();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View view = inflater.inflate(R.layout.fragment_description_edit, container, false);
        unbinder = ButterKnife.bind(this, view);
        editView.setTranslationEdit(getArguments().getBoolean(ARG_IS_TRANSLATION));
        editView.setTranslationSourceLanguageDescription(getArguments().getCharSequence(ARG_TRANSLATION_SOURCE_LANG_DESC));
        editView.setPageTitle(pageTitle);
        editView.setHighlightText(highlightText);
        editView.setCallback(new EditViewCallback());

        if (reviewEnabled) {
            loadPageSummary(savedInstanceState);
        }

        if (funnel != null) {
            funnel.logReady();
        }
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

    @Override public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(ARG_REVIEWING, reviewEnabled && editView.showingReviewContent());
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

    private void loadPageSummary(@Nullable Bundle savedInstanceState) {
        disposables.add(PageClientFactory.create(pageTitle.getWikiSite(), pageTitle.namespace())
                .summary(pageTitle.getWikiSite(), pageTitle.getPrefixedText(), null)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(summary -> {
                    editView.setPageSummary(summary);
                    editView.getPageSummaryContainer().setOnClickListener(view -> {
                        if (callback() != null) {
                            callback().onPageSummaryContainerClicked(pageTitle);
                        }
                    });
                    if (savedInstanceState != null) {
                        editView.loadReviewContent(savedInstanceState.getBoolean(ARG_REVIEWING));
                    }
                }, L::e));
    }

    private void cancelCalls() {
        // in reverse chronological order
        if (descriptionEditCall != null) {
            descriptionEditCall.cancel();
            descriptionEditCall = null;
        }
        if (csrfClient != null) {
            csrfClient.cancel();
            csrfClient = null;
        }
    }

    private void finish() {
        hideSoftKeyboard(requireActivity());
        requireActivity().finish();
    }

    private Callback callback() {
        return FragmentUtil.getCallback(this, Callback.class);
    }

    private class EditViewCallback implements DescriptionEditView.Callback {
        private final WikiSite wikiData = new WikiSite(Service.WIKIDATA_URL, "");

        @Override
        public void onSaveClick() {
            if (reviewEnabled && !editView.showingReviewContent()) {
                editView.loadReviewContent(true);
            } else {
                editView.setError(null);
                editView.setSaveState(true);

                cancelCalls();

                csrfClient = new CsrfTokenClient(new WikiSite(Service.WIKIDATA_URL, ""),
                        pageTitle.getWikiSite());
                getEditTokenThenSave(false);

                if (funnel != null) {
                    funnel.logSaveAttempt();
                }
            }
        }

        private void getEditTokenThenSave(boolean forceLogin) {
            if (csrfClient == null) {
                return;
            }
            csrfClient.request(forceLogin, new CsrfTokenClient.Callback() {
                @Override
                public void success(@NonNull String token) {
                    postDescription(token);
                }

                @Override
                public void failure(@NonNull Throwable caught) {
                    editFailed(caught);
                }

                @Override
                public void twoFactorPrompt() {
                    editFailed(new LoginFailedException(getResources()
                            .getString(R.string.login_2fa_other_workflow_error_msg)));
                }
            });
        }

        /* send updated description to Wikidata */
        private void postDescription(@NonNull String editToken) {
            descriptionEditCall = new DescriptionEditClient().request(wikiData, pageTitle,
                    editView.getDescription(), editToken,
                    new DescriptionEditClient.Callback() {
                        @Override @SuppressWarnings("checkstyle:magicnumber")
                        public void success(@NonNull Call<MwPostResponse> call) {
                            // TODO: remove this artificial delay if someday we get a reliable way
                            // to determine whether the change has propagated to the relevant
                            // RESTBase endpoints.
                            new Handler().postDelayed(successRunnable, TimeUnit.SECONDS.toMillis(4));
                            if (funnel != null) {
                                funnel.logSaved();
                            }
                        }

                        @Override public void abusefilter(@NonNull Call<MwPostResponse> call,
                                                          @Nullable String code,
                                                          @Nullable String info) {
                            editView.setSaveState(false);
                            if (info != null) {
                                editView.setError(StringUtil.fromHtml(info));
                            }
                            if (funnel != null) {
                                funnel.logAbuseFilterWarning(code);
                            }
                        }

                        @Override
                        public void invalidLogin(@NonNull Call<MwPostResponse> call,
                                                 @NonNull Throwable caught) {
                            getEditTokenThenSave(true);
                        }

                        @Override public void failure(@NonNull Call<MwPostResponse> call,
                                                      @NonNull Throwable caught) {
                            editFailed(caught);
                            if (funnel != null) {
                                funnel.logError(caught.getMessage());
                            }
                        }
                    });
        }

        private void editFailed(@NonNull Throwable caught) {
            if (editView != null) {
                editView.setSaveState(false);
                FeedbackUtil.showError(getActivity(), caught);
                L.e(caught);
            }
        }

        @Override
        public void onHelpClick() {
            startActivity(DescriptionEditHelpActivity.newIntent(requireContext()));
        }

        @Override
        public void onCancelClick() {
            if (reviewEnabled && editView.showingReviewContent()) {
                editView.loadReviewContent(false);
            } else {
                finish();
            }
        }
    }
}
