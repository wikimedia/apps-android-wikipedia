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

import org.apache.commons.lang3.StringUtils;
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
import org.wikipedia.dataclient.mwapi.MwServiceError;
import org.wikipedia.dataclient.wikidata.EntityPostResponse;
import org.wikipedia.descriptions.DescriptionEditActivity.Action;
import org.wikipedia.json.GsonUnmarshaller;
import org.wikipedia.page.PageTitle;
import org.wikipedia.settings.Prefs;
import org.wikipedia.suggestededits.PageSummaryForEdit;
import org.wikipedia.suggestededits.SuggestedEditsSurvey;
import org.wikipedia.util.FeedbackUtil;
import org.wikipedia.util.StringUtil;
import org.wikipedia.util.log.L;

import java.io.IOException;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

import static android.app.Activity.RESULT_OK;
import static org.wikipedia.Constants.ACTIVITY_REQUEST_DESCRIPTION_EDIT_SUCCESS;
import static org.wikipedia.Constants.INTENT_EXTRA_ACTION;
import static org.wikipedia.Constants.INTENT_EXTRA_INVOKE_SOURCE;
import static org.wikipedia.Constants.InvokeSource;
import static org.wikipedia.Constants.InvokeSource.PAGE_ACTIVITY;
import static org.wikipedia.Constants.InvokeSource.SUGGESTED_EDITS;
import static org.wikipedia.descriptions.DescriptionEditActivity.Action.ADD_CAPTION;
import static org.wikipedia.descriptions.DescriptionEditActivity.Action.ADD_DESCRIPTION;
import static org.wikipedia.descriptions.DescriptionEditActivity.Action.TRANSLATE_CAPTION;
import static org.wikipedia.descriptions.DescriptionEditActivity.Action.TRANSLATE_DESCRIPTION;
import static org.wikipedia.descriptions.DescriptionEditUtil.ABUSEFILTER_DISALLOWED;
import static org.wikipedia.descriptions.DescriptionEditUtil.ABUSEFILTER_WARNING;
import static org.wikipedia.language.AppLanguageLookUpTable.CHINESE_LANGUAGE_CODE;
import static org.wikipedia.suggestededits.SuggestionsActivity.EXTRA_SOURCE_ADDED_CONTRIBUTION;
import static org.wikipedia.util.DeviceUtil.hideSoftKeyboard;

public class DescriptionEditFragment extends Fragment {

    public interface Callback {
        void onDescriptionEditSuccess();
        void onBottomBarContainerClicked(@NonNull DescriptionEditActivity.Action action);
    }

    public static final String TEMPLATE_PARSE_REGEX = "(\\{\\{[Ss]hort description\\|(?:1=)?)([^}|]+)([^}]*\\}\\})";
    private static final String[] DESCRIPTION_TEMPLATES = {"Short description", "SHORTDESC"};

    private static final String ARG_TITLE = "title";
    private static final String ARG_REVIEWING = "inReviewing";
    private static final String ARG_DESCRIPTION = "description";
    private static final String ARG_HIGHLIGHT_TEXT = "highlightText";
    private static final String ARG_ACTION = "action";
    private static final String ARG_SOURCE_SUMMARY = "sourceSummary";
    private static final String ARG_TARGET_SUMMARY = "targetSummary";

    @BindView(R.id.fragment_description_edit_view) DescriptionEditView editView;
    private Unbinder unbinder;
    private PageTitle pageTitle;
    private PageSummaryForEdit sourceSummary;
    private PageSummaryForEdit targetSummary;
    @Nullable private String highlightText;
    @Nullable private DescriptionEditFunnel funnel;
    private Action action;
    private InvokeSource invokeSource;
    private final CompositeDisposable disposables = new CompositeDisposable();
    private final Pattern templateParsePattern = Pattern.compile(TEMPLATE_PARSE_REGEX);

    private final Runnable successRunnable = new Runnable() {
        @Override public void run() {
            if (!AccountUtil.isLoggedIn()) {
                Prefs.incrementTotalAnonDescriptionsEdited();
            }

            if (invokeSource == SUGGESTED_EDITS) {
                SuggestedEditsSurvey.onEditSuccess();
            }

            Prefs.setLastDescriptionEditTime(new Date().getTime());
            Prefs.setSuggestedEditsReactivationPassStageOne(false);
            SuggestedEditsFunnel.get().success(action);

            if (getActivity() == null)  {
                return;
            }
            editView.setSaveState(false);
            if (Prefs.shouldShowDescriptionEditSuccessPrompt() && invokeSource == PAGE_ACTIVITY) {
                startActivityForResult(DescriptionEditSuccessActivity.newIntent(requireContext(), invokeSource),
                        ACTIVITY_REQUEST_DESCRIPTION_EDIT_SUCCESS);
                Prefs.shouldShowDescriptionEditSuccessPrompt(false);
            } else {
                Intent intent = new Intent();
                intent.putExtra(EXTRA_SOURCE_ADDED_CONTRIBUTION, editView.getDescription());
                intent.putExtra(INTENT_EXTRA_INVOKE_SOURCE, invokeSource);
                intent.putExtra(INTENT_EXTRA_ACTION, action);
                requireActivity().setResult(RESULT_OK, intent);
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
                                                      @NonNull Action action,
                                                      @NonNull InvokeSource source) {
        DescriptionEditFragment instance = new DescriptionEditFragment();
        Bundle args = new Bundle();
        args.putParcelable(ARG_TITLE, title);
        args.putString(ARG_HIGHLIGHT_TEXT, highlightText);
        args.putString(ARG_SOURCE_SUMMARY, sourceSummary);
        args.putString(ARG_TARGET_SUMMARY, targetSummary);
        args.putSerializable(ARG_ACTION, action);
        args.putSerializable(INTENT_EXTRA_INVOKE_SOURCE, source);
        instance.setArguments(args);
        return instance;
    }

    @Override public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        pageTitle = requireArguments().getParcelable(ARG_TITLE);
        DescriptionEditFunnel.Type type = pageTitle.getDescription() == null
                ? DescriptionEditFunnel.Type.NEW
                : DescriptionEditFunnel.Type.EXISTING;
        highlightText = requireArguments().getString(ARG_HIGHLIGHT_TEXT);
        action = (Action) requireArguments().getSerializable(ARG_ACTION);
        invokeSource = (InvokeSource) requireArguments().getSerializable(INTENT_EXTRA_INVOKE_SOURCE);

        if (requireArguments().getString(ARG_SOURCE_SUMMARY) != null) {
            sourceSummary = GsonUnmarshaller.unmarshal(PageSummaryForEdit.class, requireArguments().getString(ARG_SOURCE_SUMMARY));
        }

        if (requireArguments().getString(ARG_TARGET_SUMMARY) != null) {
            targetSummary = GsonUnmarshaller.unmarshal(PageSummaryForEdit.class, requireArguments().getString(ARG_TARGET_SUMMARY));
        }

        funnel = new DescriptionEditFunnel(WikipediaApp.getInstance(), pageTitle, type, invokeSource);
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
        disposables.clear();
    }

    private void loadPageSummaryIfNeeded(Bundle savedInstanceState) {
        editView.showProgressBar(true);
        if (invokeSource == PAGE_ACTIVITY && TextUtils.isEmpty(sourceSummary.getExtractHtml())) {
            disposables.add(ServiceFactory.getRest(pageTitle.getWikiSite()).getSummary(null, pageTitle.getPrefixedText())
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .doAfterTerminate(() -> setUpEditView(savedInstanceState))
                    .subscribe(summary -> sourceSummary.setExtractHtml(summary.getExtractHtml()), L::e));
        } else {
            setUpEditView(savedInstanceState);
        }
    }

    private void setUpEditView(Bundle savedInstanceState) {
        editView.setAction(action);
        editView.setPageTitle(pageTitle);
        editView.setHighlightText(highlightText);
        editView.setCallback(new EditViewCallback());
        editView.setSummaries(sourceSummary, targetSummary);
        if (savedInstanceState != null) {
            editView.setDescription(savedInstanceState.getString(ARG_DESCRIPTION));
            editView.loadReviewContent(savedInstanceState.getBoolean(ARG_REVIEWING));
        }
        editView.showProgressBar(false);
    }

    private Callback callback() {
        return FragmentUtil.getCallback(this, Callback.class);
    }

    private boolean shouldWriteToLocalWiki() {
        return (action == ADD_DESCRIPTION || action == TRANSLATE_DESCRIPTION) && pageTitle.getWikiSite().languageCode().equals("en");
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
                getEditTokenThenSave();

                if (funnel != null) {
                    funnel.logSaveAttempt();
                }
            }
        }

        private void getEditTokenThenSave() {
            CsrfTokenClient csrfClient = (action == ADD_CAPTION || action == TRANSLATE_CAPTION)
                    ? new CsrfTokenClient(wikiCommons)
                    : new CsrfTokenClient(shouldWriteToLocalWiki() ? pageTitle.getWikiSite() : wikiData, pageTitle.getWikiSite());

            disposables.add(csrfClient.getToken()
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(token -> {
                        if (shouldWriteToLocalWiki()) {
                            // If the description is being applied to an article on English Wikipedia, it
                            // should be written directly to the article instead of Wikidata.
                            postDescriptionToArticle(token);
                        } else {
                            postDescriptionToWikidata(token);
                        }
                    }, t -> editFailed(t, false)));
        }

        @SuppressWarnings("checkstyle:magicnumber")
        private void postDescriptionToArticle(@NonNull String editToken) {
            WikiSite wikiSite = WikiSite.forLanguageCode(pageTitle.getWikiSite().languageCode());

            disposables.add(ServiceFactory.get(wikiSite).getWikiTextForSection(pageTitle.getPrefixedText(), 0)
                    .subscribeOn(Schedulers.io())
                    .flatMap(mwQueryResponse -> {
                        String text = mwQueryResponse.query().firstPage().revisions().get(0).content();
                        long baseRevId = mwQueryResponse.query().firstPage().revisions().get(0).getRevId();

                        text = updateDescriptionInArticle(text, editView.getDescription());

                        return ServiceFactory.get(wikiSite).postEditSubmit(pageTitle.getPrefixedText(),
                                "0", null, action == ADD_DESCRIPTION ? SuggestedEditsFunnel.SUGGESTED_EDITS_ADD_COMMENT
                                        : action == TRANSLATE_DESCRIPTION ? SuggestedEditsFunnel.SUGGESTED_EDITS_TRANSLATE_COMMENT : "",
                                AccountUtil.isLoggedIn() ? "user" : null, text, null, baseRevId, editToken, null, null)
                                .subscribeOn(Schedulers.io());
                    })
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(result -> {
                        if (result.hasEditResult() && result.edit() != null) {
                            if (result.edit().editSucceeded()) {
                                new Handler().postDelayed(successRunnable, TimeUnit.SECONDS.toMillis(4));
                                if (funnel != null) {
                                    funnel.logSaved(result.edit().newRevId());
                                }
                            } else if (result.edit().hasEditErrorCode()) {

                                // TODO: handle AbuseFilter messages
                                // new EditAbuseFilterResult(result.edit().code(), result.edit().info(), result.edit().warning());

                                editFailed(new IOException(StringUtils.defaultString(result.edit().warning())), false);
                            } else if (result.edit().hasCaptchaResponse()) {

                                // TODO: handle captcha.
                                // new CaptchaResult(result.edit().captchaId());

                                if (funnel != null) {
                                    funnel.logCaptchaShown();
                                }
                            } else if (result.edit().hasSpamBlacklistResponse()) {
                                editFailed(new IOException(getString(R.string.editing_error_spamblacklist)), false);
                            } else {
                                editFailed(new IOException("Received unrecognized edit response"), true);
                            }
                        } else {
                            editFailed(new IOException("An unknown error occurred."), true);
                        }
                    }, caught -> editFailed(caught, true)));
        }

        @SuppressWarnings("checkstyle:magicnumber")
        private void postDescriptionToWikidata(@NonNull String editToken) {
            disposables.add(ServiceFactory.get(WikiSite.forLanguageCode(pageTitle.getWikiSite().languageCode())).getSiteInfo()
                    .flatMap(response -> {
                        String languageCode = response.query().siteInfo() != null
                                && response.query().siteInfo().getLang() != null
                                && !response.query().siteInfo().getLang().equals(CHINESE_LANGUAGE_CODE)
                                ? response.query().siteInfo().getLang() : pageTitle.getWikiSite().languageCode();
                        return getPostObservable(editToken, languageCode);
                    })
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(response -> {
                        if (response.getSuccessVal() > 0) {
                            new Handler().postDelayed(successRunnable, TimeUnit.SECONDS.toMillis(4));
                            if (funnel != null) {
                                funnel.logSaved(response.getEntity() != null ? response.getEntity().getLastRevId() : 0);
                            }
                        } else {
                            editFailed(new RuntimeException("Received unrecognized description edit response"), true);
                        }
                    }, caught -> {
                        if (caught instanceof MwException) {
                            MwServiceError error = ((MwException) caught).getError();
                            if (error.hasMessageName(ABUSEFILTER_DISALLOWED) || error.hasMessageName(ABUSEFILTER_WARNING)) {
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

        private Observable<EntityPostResponse> getPostObservable(@NonNull String editToken, @NonNull String languageCode) {
            String comment = null;
            if (action == ADD_CAPTION || action == TRANSLATE_CAPTION) {
                if (invokeSource == SUGGESTED_EDITS || invokeSource == InvokeSource.FEED) {
                    comment = action == ADD_CAPTION ? SuggestedEditsFunnel.SUGGESTED_EDITS_ADD_COMMENT
                            : action == TRANSLATE_CAPTION ? SuggestedEditsFunnel.SUGGESTED_EDITS_TRANSLATE_COMMENT : null;
                }
                return ServiceFactory.get(wikiCommons).postLabelEdit(languageCode, languageCode, commonsDbName,
                        pageTitle.getPrefixedText(), editView.getDescription(), comment, editToken, AccountUtil.isLoggedIn() ? "user" : null);
            } else {
                if (invokeSource == SUGGESTED_EDITS || invokeSource == InvokeSource.FEED) {
                    comment = action == ADD_DESCRIPTION ? SuggestedEditsFunnel.SUGGESTED_EDITS_ADD_COMMENT
                            : action == TRANSLATE_DESCRIPTION ? SuggestedEditsFunnel.SUGGESTED_EDITS_TRANSLATE_COMMENT : null;
                }
                return ServiceFactory.get(wikiData).postDescriptionEdit(languageCode, languageCode, pageTitle.getWikiSite().dbName(),
                        pageTitle.getPrefixedText(), editView.getDescription(), comment, editToken, AccountUtil.isLoggedIn() ? "user" : null);
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
            SuggestedEditsFunnel.get().failure(action);
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
            callback().onBottomBarContainerClicked(action);
        }

        @Override
        public void onVoiceInputClick() {
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                    .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            try {
                startActivityForResult(intent, Constants.ACTIVITY_REQUEST_VOICE_SEARCH);
            } catch (ActivityNotFoundException a) {
                FeedbackUtil.showMessage(requireActivity(), R.string.error_voice_search_not_available);
            }
        }
    }

    @NonNull
    private String updateDescriptionInArticle(@NonNull String articleText, @NonNull String newDescription) {
        String newText;
        if (templateParsePattern.matcher(articleText).find()) {
            // update existing description template
            newText = articleText.replaceFirst(TEMPLATE_PARSE_REGEX, "$1" + newDescription + "$3");
        } else {
            // add new description template
            newText = "{{" + DESCRIPTION_TEMPLATES[0] + "|" + newDescription + "}}\n" + articleText;
        }
        return newText;
    }
}
