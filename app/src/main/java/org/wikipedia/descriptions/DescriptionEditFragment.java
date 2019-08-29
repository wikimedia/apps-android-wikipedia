package org.wikipedia.descriptions;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.speech.RecognizerIntent;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import org.wikipedia.Constants;
import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.activity.FragmentUtil;
import org.wikipedia.analytics.DescriptionEditFunnel;
import org.wikipedia.analytics.SuggestedEditsFunnel;
import org.wikipedia.auth.AccountUtil;
import org.wikipedia.csrf.CsrfTokenClient;
import org.wikipedia.dataclient.Service;
import org.wikipedia.dataclient.ServiceFactory;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.dataclient.mwapi.MwException;
import org.wikipedia.dataclient.mwapi.MwPostResponse;
import org.wikipedia.dataclient.mwapi.MwServiceError;
import org.wikipedia.dataclient.page.PageClientFactory;
import org.wikipedia.dataclient.retrofit.RetrofitException;
import org.wikipedia.json.GsonMarshaller;
import org.wikipedia.json.GsonUnmarshaller;
import org.wikipedia.login.LoginClient.LoginFailedException;
import org.wikipedia.page.PageTitle;
import org.wikipedia.settings.Prefs;
import org.wikipedia.suggestededits.SuggestedEditsSummary;
import org.wikipedia.util.FeedbackUtil;
import org.wikipedia.util.StringUtil;
import org.wikipedia.util.log.L;

import java.util.Date;
import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

import static android.app.Activity.RESULT_OK;
import static org.wikipedia.Constants.ACTIVITY_REQUEST_DESCRIPTION_EDIT_SUCCESS;
import static org.wikipedia.Constants.InvokeSource;
import static org.wikipedia.Constants.InvokeSource.FEED_CARD_SUGGESTED_EDITS_ADD_DESC;
import static org.wikipedia.Constants.InvokeSource.FEED_CARD_SUGGESTED_EDITS_IMAGE_CAPTION;
import static org.wikipedia.Constants.InvokeSource.FEED_CARD_SUGGESTED_EDITS_TRANSLATE_DESC;
import static org.wikipedia.Constants.InvokeSource.FEED_CARD_SUGGESTED_EDITS_TRANSLATE_IMAGE_CAPTION;
import static org.wikipedia.Constants.InvokeSource.PAGE_ACTIVITY;
import static org.wikipedia.Constants.InvokeSource.SUGGESTED_EDITS_ADD_CAPTION;
import static org.wikipedia.Constants.InvokeSource.SUGGESTED_EDITS_ADD_DESC;
import static org.wikipedia.Constants.InvokeSource.SUGGESTED_EDITS_TRANSLATE_CAPTION;
import static org.wikipedia.Constants.InvokeSource.SUGGESTED_EDITS_TRANSLATE_DESC;
import static org.wikipedia.descriptions.DescriptionEditUtil.ABUSEFILTER_DISALLOWED;
import static org.wikipedia.descriptions.DescriptionEditUtil.ABUSEFILTER_WARNING;
import static org.wikipedia.suggestededits.SuggestedEditsCardsActivity.EXTRA_SOURCE_ADDED_CONTRIBUTION;
import static org.wikipedia.util.DeviceUtil.hideSoftKeyboard;

public class DescriptionEditFragment extends Fragment {

    public interface Callback {
        void onDescriptionEditSuccess();
        void onBottomBarContainerClicked(@NonNull InvokeSource invokeSource);
    }

    private static final String ARG_TITLE = "title";
    private static final String ARG_REVIEWING = "inReviewing";
    private static final String ARG_DESCRIPTION = "description";
    private static final String ARG_HIGHLIGHT_TEXT = "highlightText";
    private static final String ARG_INVOKE_SOURCE = "invokeSource";
    private static final String ARG_SOURCE_SUMMARY = "sourceSummary";
    private static final String ARG_TARGET_SUMMARY = "targetSummary";

    @BindView(R.id.fragment_description_edit_view) DescriptionEditView editView;
    private Unbinder unbinder;
    private PageTitle pageTitle;
    private SuggestedEditsSummary sourceSummary;
    private SuggestedEditsSummary targetSummary;
    @Nullable private String highlightText;
    @Nullable private CsrfTokenClient csrfClient;
    @Nullable private DescriptionEditFunnel funnel;
    private InvokeSource invokeSource;
    private CompositeDisposable disposables = new CompositeDisposable();

    private Runnable successRunnable = new Runnable() {
        @Override public void run() {
            if (!AccountUtil.isLoggedIn()) {
                Prefs.incrementTotalAnonDescriptionsEdited();
            }
            Prefs.setLastDescriptionEditTime(new Date().getTime());
            SuggestedEditsFunnel.get().success(invokeSource);

            if (getActivity() == null)  {
                return;
            }
            editView.setSaveState(false);
            if (Prefs.shouldShowDescriptionEditSuccessPrompt() && invokeSource == PAGE_ACTIVITY) {
                startActivityForResult(DescriptionEditSuccessActivity.newIntent(requireContext()),
                        ACTIVITY_REQUEST_DESCRIPTION_EDIT_SUCCESS);
                Prefs.shouldShowDescriptionEditSuccessPrompt(false);
            } else {
                requireActivity().setResult(RESULT_OK,
                        new Intent().putExtra(EXTRA_SOURCE_ADDED_CONTRIBUTION, editView.getDescription()));
                hideSoftKeyboard(requireActivity());
                requireActivity().finish();
            }
        }
    };

    @NonNull
    public static DescriptionEditFragment newInstance(@NonNull PageTitle title,
                                                      @Nullable String highlightText,
                                                      @Nullable String sourceSummary,
                                                      @Nullable String targetSummary,
                                                      @NonNull InvokeSource source) {
        DescriptionEditFragment instance = new DescriptionEditFragment();
        Bundle args = new Bundle();
        args.putString(ARG_TITLE, GsonMarshaller.marshal(title));
        args.putString(ARG_HIGHLIGHT_TEXT, highlightText);
        args.putString(ARG_SOURCE_SUMMARY, sourceSummary);
        args.putString(ARG_TARGET_SUMMARY, targetSummary);
        args.putSerializable(ARG_INVOKE_SOURCE, source);
        instance.setArguments(args);
        return instance;
    }

    @Override public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        pageTitle = GsonUnmarshaller.unmarshal(PageTitle.class, getArguments().getString(ARG_TITLE));
        DescriptionEditFunnel.Type type = pageTitle.getDescription() == null
                ? DescriptionEditFunnel.Type.NEW
                : DescriptionEditFunnel.Type.EXISTING;
        highlightText = getArguments().getString(ARG_HIGHLIGHT_TEXT);
        invokeSource = (InvokeSource) getArguments().getSerializable(ARG_INVOKE_SOURCE);

        if (getArguments().getString(ARG_SOURCE_SUMMARY) != null) {
            sourceSummary = GsonUnmarshaller.unmarshal(SuggestedEditsSummary.class, getArguments().getString(ARG_SOURCE_SUMMARY));
        }

        if (getArguments().getString(ARG_TARGET_SUMMARY) != null) {
            targetSummary = GsonUnmarshaller.unmarshal(SuggestedEditsSummary.class, getArguments().getString(ARG_TARGET_SUMMARY));
        }

        funnel = new DescriptionEditFunnel(WikipediaApp.getInstance(), pageTitle, type);
        funnel.logStart();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View view = inflater.inflate(R.layout.fragment_description_edit, container, false);
        unbinder = ButterKnife.bind(this, view);
        loadPageSummaryIfNeeded(savedInstanceState);

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
        outState.putString(ARG_DESCRIPTION, editView.getDescription());
        outState.putBoolean(ARG_REVIEWING, editView.showingReviewContent());
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, final Intent data) {
        if (requestCode == ACTIVITY_REQUEST_DESCRIPTION_EDIT_SUCCESS && getActivity() != null) {
            if (callback() != null) {
                callback().onDescriptionEditSuccess();
            }
        } else if (requestCode == Constants.ACTIVITY_REQUEST_VOICE_SEARCH
                && resultCode == Activity.RESULT_OK && data != null
                && data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS) != null) {
            String text = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS).get(0);
            editView.setDescription(text);
        }
    }

    private void cancelCalls() {
        if (csrfClient != null) {
            csrfClient.cancel();
            csrfClient = null;
        }
        disposables.clear();
    }

    private void loadPageSummaryIfNeeded(Bundle savedInstanceState) {
        editView.showProgressBar(true);
        if (invokeSource == PAGE_ACTIVITY && TextUtils.isEmpty(sourceSummary.getExtractHtml())) {
            disposables.add(PageClientFactory.create(pageTitle.getWikiSite(), pageTitle.namespace())
                    .summary(pageTitle.getWikiSite(), pageTitle.getPrefixedText(), null)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .doAfterTerminate(() -> setUpEditView(savedInstanceState))
                    .subscribe(summary -> sourceSummary.setExtractHtml(summary.getExtractHtml()), L::e));
        } else {
            setUpEditView(savedInstanceState);
        }
    }

    private void setUpEditView(Bundle savedInstanceState) {
        editView.setInvokeSource(invokeSource);
        editView.setPageTitle(pageTitle);
        editView.setHighlightText(highlightText);
        editView.setCallback(new EditViewCallback());
        editView.setSummaries(requireActivity(), sourceSummary, targetSummary);
        if (savedInstanceState != null) {
            editView.setDescription(savedInstanceState.getString(ARG_DESCRIPTION));
            editView.loadReviewContent(savedInstanceState.getBoolean(ARG_REVIEWING));
        }
        editView.showProgressBar(false);
    }

    private Callback callback() {
        return FragmentUtil.getCallback(this, Callback.class);
    }

    private class EditViewCallback implements DescriptionEditView.Callback {
        private final WikiSite wikiData = new WikiSite(Service.WIKIDATA_URL, "");
        private final WikiSite wikiCommons = new WikiSite(Service.COMMONS_URL);
        private final String commonsDbName = "commonswiki";

        @Override
        public void onSaveClick() {
            if (!editView.showingReviewContent()) {
                editView.loadReviewContent(true);
            } else {
                editView.setError(null);
                editView.setSaveState(true);

                cancelCalls();

                if (invokeSource == SUGGESTED_EDITS_ADD_CAPTION || invokeSource == SUGGESTED_EDITS_TRANSLATE_CAPTION || invokeSource == FEED_CARD_SUGGESTED_EDITS_IMAGE_CAPTION || invokeSource == FEED_CARD_SUGGESTED_EDITS_TRANSLATE_IMAGE_CAPTION) {
                    csrfClient = new CsrfTokenClient(wikiCommons, wikiCommons);
                } else {
                    csrfClient = new CsrfTokenClient(wikiData, pageTitle.getWikiSite());
                }
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
                    editFailed(caught, false);
                }

                @Override
                public void twoFactorPrompt() {
                    editFailed(new LoginFailedException(getResources()
                            .getString(R.string.login_2fa_other_workflow_error_msg)), false);
                }
            });
        }

        /* send updated description to Wikidata */
        @SuppressWarnings("checkstyle:magicnumber")
        private void postDescription(@NonNull String editToken) {

            disposables.add(ServiceFactory.get(pageTitle.getWikiSite()).getSiteInfo()
                    .flatMap(response -> {
                        String languageCode = response.query().siteInfo() != null && response.query().siteInfo().lang() != null
                                ? response.query().siteInfo().lang() : pageTitle.getWikiSite().languageCode();
                        return getPostObservable(editToken, languageCode);
                    })
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(response -> {
                        if (response.getSuccessVal() > 0) {
                            new Handler().postDelayed(successRunnable, TimeUnit.SECONDS.toMillis(4));
                            if (funnel != null) {
                                funnel.logSaved();
                            }
                        } else {
                            editFailed(RetrofitException.unexpectedError(new RuntimeException(
                                    "Received unrecognized description edit response")), true);
                        }
                    }, caught -> {
                        if (caught instanceof MwException) {
                            MwServiceError error = ((MwException) caught).getError();
                            if (error.badLoginState() || error.badToken()) {
                                getEditTokenThenSave(true);
                            } else if (error.hasMessageName(ABUSEFILTER_DISALLOWED) || error.hasMessageName(ABUSEFILTER_WARNING)) {
                                String code = error.hasMessageName(ABUSEFILTER_DISALLOWED) ? ABUSEFILTER_DISALLOWED : ABUSEFILTER_WARNING;
                                String info = error.getMessageHtml(code);
                                editView.setSaveState(false);
                                if (info != null) {
                                    editView.setError(StringUtil.fromHtml(info));
                                }
                                if (funnel != null) {
                                    funnel.logAbuseFilterWarning(code);
                                }
                            } else {
                                editFailed(caught, true);
                            }
                        } else {
                            editFailed(caught, true);
                        }
                    }));
        }

        private Observable<MwPostResponse> getPostObservable(@NonNull String editToken, @Nullable String languageCode) {
            if (invokeSource == SUGGESTED_EDITS_ADD_CAPTION || invokeSource == FEED_CARD_SUGGESTED_EDITS_IMAGE_CAPTION || invokeSource == SUGGESTED_EDITS_TRANSLATE_CAPTION || invokeSource == FEED_CARD_SUGGESTED_EDITS_TRANSLATE_IMAGE_CAPTION) {
                return ServiceFactory.get(wikiCommons).postLabelEdit(pageTitle.getWikiSite().languageCode(),
                        pageTitle.getWikiSite().languageCode(), commonsDbName,
                        pageTitle.getConvertedText(), editView.getDescription(),
                        invokeSource == SUGGESTED_EDITS_ADD_CAPTION || invokeSource == FEED_CARD_SUGGESTED_EDITS_IMAGE_CAPTION ? SuggestedEditsFunnel.SUGGESTED_EDITS_ADD_COMMENT
                                : invokeSource == SUGGESTED_EDITS_TRANSLATE_CAPTION || invokeSource == FEED_CARD_SUGGESTED_EDITS_TRANSLATE_IMAGE_CAPTION ? SuggestedEditsFunnel.SUGGESTED_EDITS_TRANSLATE_COMMENT : null,
                        editToken, AccountUtil.isLoggedIn() ? "user" : null);
            } else {
                return ServiceFactory.get(wikiData).postDescriptionEdit(languageCode,
                        pageTitle.getWikiSite().languageCode(), pageTitle.getWikiSite().dbName(),
                        pageTitle.getConvertedText(), editView.getDescription(),
                        invokeSource == SUGGESTED_EDITS_ADD_DESC || invokeSource == FEED_CARD_SUGGESTED_EDITS_ADD_DESC ? SuggestedEditsFunnel.SUGGESTED_EDITS_ADD_COMMENT
                                : invokeSource == SUGGESTED_EDITS_TRANSLATE_DESC || invokeSource == FEED_CARD_SUGGESTED_EDITS_TRANSLATE_DESC ? SuggestedEditsFunnel.SUGGESTED_EDITS_TRANSLATE_COMMENT : null,
                        editToken, AccountUtil.isLoggedIn() ? "user" : null);
            }
        }

        private void editFailed(@NonNull Throwable caught, boolean logError) {
            if (editView != null) {
                editView.setSaveState(false);
                FeedbackUtil.showError(getActivity(), caught);
                L.e(caught);
            }
            if (funnel != null && logError) {
                funnel.logError(caught.getMessage());
            }
            SuggestedEditsFunnel.get().cancel(invokeSource);
        }

        @Override
        public void onHelpClick() {
            FeedbackUtil.showAndroidAppEditingFAQ(requireContext());
        }

        @Override
        public void onCancelClick() {
            if (editView.showingReviewContent()) {
                editView.loadReviewContent(false);
            } else {
                hideSoftKeyboard(requireActivity());
                requireActivity().onBackPressed();
            }
        }

        @Override
        public void onBottomBarClick() {
            callback().onBottomBarContainerClicked(invokeSource);
        }

        @Override
        public void onVoiceInputClick() {
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            try {
                startActivityForResult(intent, Constants.ACTIVITY_REQUEST_VOICE_SEARCH);
            } catch (ActivityNotFoundException a) {
                FeedbackUtil.showMessage(requireActivity(), R.string.error_voice_search_not_available);
            }
        }
    }
}
