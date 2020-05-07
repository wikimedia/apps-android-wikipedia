package org.wikipedia.edit;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.content.res.AppCompatResources;

import org.apache.commons.lang3.StringUtils;
import org.wikipedia.Constants;
import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.activity.BaseActivity;
import org.wikipedia.analytics.EditFunnel;
import org.wikipedia.analytics.LoginFunnel;
import org.wikipedia.auth.AccountUtil;
import org.wikipedia.captcha.CaptchaHandler;
import org.wikipedia.captcha.CaptchaResult;
import org.wikipedia.csrf.CsrfTokenClient;
import org.wikipedia.dataclient.ServiceFactory;
import org.wikipedia.dataclient.mwapi.MwException;
import org.wikipedia.dataclient.mwapi.MwQueryPage;
import org.wikipedia.edit.preview.EditPreviewFragment;
import org.wikipedia.edit.richtext.SyntaxHighlighter;
import org.wikipedia.edit.summaries.EditSummaryFragment;
import org.wikipedia.history.HistoryEntry;
import org.wikipedia.login.LoginActivity;
import org.wikipedia.login.LoginClient;
import org.wikipedia.page.ExclusiveBottomSheetPresenter;
import org.wikipedia.page.LinkMovementMethodExt;
import org.wikipedia.page.PageProperties;
import org.wikipedia.page.PageTitle;
import org.wikipedia.page.linkpreview.LinkPreviewDialog;
import org.wikipedia.settings.Prefs;
import org.wikipedia.util.FeedbackUtil;
import org.wikipedia.util.ResourceUtil;
import org.wikipedia.util.StringUtil;
import org.wikipedia.util.log.L;
import org.wikipedia.views.PlainPasteEditText;
import org.wikipedia.views.ViewAnimations;
import org.wikipedia.views.ViewUtil;
import org.wikipedia.views.WikiErrorView;
import org.wikipedia.views.WikiTextKeyboardView;

import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import retrofit2.Call;

import static org.wikipedia.util.DeviceUtil.hideSoftKeyboard;
import static org.wikipedia.util.L10nUtil.setConditionalTextDirection;
import static org.wikipedia.util.UriUtil.handleExternalLink;

public class EditSectionActivity extends BaseActivity {
    public static final String ACTION_EDIT_SECTION = "org.wikipedia.edit_section";
    public static final String EXTRA_TITLE = "org.wikipedia.edit_section.title";
    public static final String EXTRA_SECTION_ID = "org.wikipedia.edit_section.sectionid";
    public static final String EXTRA_SECTION_HEADING = "org.wikipedia.edit_section.sectionheading";
    public static final String EXTRA_PAGE_PROPS = "org.wikipedia.edit_section.pageprops";
    public static final String EXTRA_HIGHLIGHT_TEXT = "org.wikipedia.edit_section.highlight";

    @BindView(R.id.edit_section_text) PlainPasteEditText sectionText;
    @BindView(R.id.edit_section_container) View sectionContainer;
    @BindView(R.id.edit_section_scroll) ScrollView sectionScrollView;
    @BindView(R.id.edit_keyboard_overlay) WikiTextKeyboardView wikiTextKeyboardView;
    @BindView(R.id.view_edit_section_error) WikiErrorView errorView;
    @BindView(R.id.view_progress_bar) ProgressBar progressBar;
    @BindView(R.id.edit_section_abusefilter_container) View abusefilterContainer;
    @BindView(R.id.edit_section_abusefilter_image) ImageView abuseFilterImage;
    @BindView(R.id.edit_section_abusefilter_title) TextView abusefilterTitle;
    @BindView(R.id.edit_section_abusefilter_text) TextView abusefilterText;
    @BindView(R.id.edit_section_captcha_container) View captchaContainer;

    private CsrfTokenClient csrfClient;

    private PageTitle title;
    private int sectionID;
    private String sectionHeading;
    private PageProperties pageProps;
    private String textToHighlight;

    private String sectionWikitext;
    private SyntaxHighlighter syntaxHighlighter;
    private SectionTextWatcher textWatcher = new SectionTextWatcher();

    private boolean sectionTextModified = false;
    private boolean sectionTextFirstLoad = true;

    // Timestamp received from server to track the time of the last revision, then passed
    // back to the server to detect possible edit conflicts.
    private String baseTimeStamp;

    private EditAbuseFilterResult abusefilterEditResult;
    private CaptchaHandler captchaHandler;

    private EditPreviewFragment editPreviewFragment;
    private EditSummaryFragment editSummaryFragment;

    private EditFunnel funnel;

    private ExclusiveBottomSheetPresenter bottomSheetPresenter = new ExclusiveBottomSheetPresenter();
    private ActionMode actionMode;
    private EditClient editClient = new EditClient();
    private EditCallback editCallback = new EditCallback();
    private CompositeDisposable disposables = new CompositeDisposable();

    private Runnable successRunnable = new Runnable() {
        @Override public void run() {
            showProgressBar(false);

            //Build intent that includes the section we were editing, so we can scroll to it later
            Intent data = new Intent();
            data.putExtra(EXTRA_SECTION_ID, sectionID);
            setResult(EditHandler.RESULT_REFRESH_PAGE, data);
            hideSoftKeyboard(EditSectionActivity.this);
            finish();
        }
    };

    public PageTitle getPageTitle() {
        return title;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_section);
        ButterKnife.bind(this);
        setNavigationBarColor(ResourceUtil.getThemedColor(this, android.R.attr.colorBackground));

        if (!getIntent().getAction().equals(ACTION_EDIT_SECTION)) {
            throw new RuntimeException("Much wrong action. Such exception. Wow");
        }

        title = getIntent().getParcelableExtra(EXTRA_TITLE);
        sectionID = getIntent().getIntExtra(EXTRA_SECTION_ID, 0);
        sectionHeading = getIntent().getStringExtra(EXTRA_SECTION_HEADING);
        pageProps = getIntent().getParcelableExtra(EXTRA_PAGE_PROPS);
        textToHighlight = getIntent().getStringExtra(EXTRA_HIGHLIGHT_TEXT);

        final ActionBar supportActionBar = getSupportActionBar();
        if (supportActionBar != null) {
            supportActionBar.setTitle("");
        }

        syntaxHighlighter = new SyntaxHighlighter(this, sectionText);
        sectionScrollView.setSmoothScrollingEnabled(false);

        captchaHandler = new CaptchaHandler(this, title.getWikiSite(), sectionText, "", null);

        editPreviewFragment = (EditPreviewFragment) getSupportFragmentManager().findFragmentById(R.id.edit_section_preview_fragment);
        editSummaryFragment = (EditSummaryFragment) getSupportFragmentManager().findFragmentById(R.id.edit_section_summary_fragment);
        editSummaryFragment.setTitle(title);

        funnel = WikipediaApp.getInstance().getFunnelManager().getEditFunnel(title);

        // Only send the editing start log event if the activity is created for the first time
        if (savedInstanceState == null) {
            funnel.logStart();
        }

        if (savedInstanceState != null && savedInstanceState.containsKey("hasTemporaryWikitextStored")) {
            sectionWikitext = Prefs.getTemporaryWikitext();
        }

        captchaHandler.restoreState(savedInstanceState);

        if (savedInstanceState != null && savedInstanceState.containsKey("abusefilter")) {
            abusefilterEditResult = savedInstanceState.getParcelable("abusefilter");
            handleAbuseFilter();
        }

        errorView.setRetryClickListener((v) -> {
            errorView.setVisibility(View.GONE);
            fetchSectionText();
        });

        errorView.setBackClickListener((v) -> onBackPressed());

        setConditionalTextDirection(sectionText, title.getWikiSite().languageCode());

        fetchSectionText();

        if (savedInstanceState != null && savedInstanceState.containsKey("sectionTextModified")) {
            sectionTextModified = savedInstanceState.getBoolean("sectionTextModified");
        }

        sectionText.addTextChangedListener(textWatcher);
        wikiTextKeyboardView.setEditText(sectionText);
        wikiTextKeyboardView.setCallback(titleStr -> bottomSheetPresenter.show(getSupportFragmentManager(),
                LinkPreviewDialog.newInstance(new HistoryEntry(new PageTitle(titleStr, title.getWikiSite()), HistoryEntry.SOURCE_INTERNAL_LINK), null)));
        sectionText.setOnClickListener(v -> finishActionMode());

        updateTextSize();

        // set focus to the EditText, but keep the keyboard hidden until the user changes the cursor location:
        sectionText.requestFocus();
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
    }

    @Override
    public void onStart() {
        super.onStart();
        updateEditLicenseText();
    }

    @Override
    public void onStop() {
        showProgressBar(false);
        editClient.cancel();
        super.onStop();
    }

    @Override
    public void onDestroy() {
        disposables.clear();
        captchaHandler.dispose();
        cancelCalls();
        sectionText.removeTextChangedListener(textWatcher);
        syntaxHighlighter.cleanup();
        super.onDestroy();
    }

    private void updateEditLicenseText() {
        TextView editLicenseText = findViewById(R.id.edit_section_license_text);
        editLicenseText.setText(StringUtil.fromHtml(String.format(getString(AccountUtil.isLoggedIn()
                        ? R.string.edit_save_action_license_logged_in
                        : R.string.edit_save_action_license_anon),
                getString(R.string.terms_of_use_url),
                getString(R.string.cc_by_sa_3_url))));

        editLicenseText.setMovementMethod(new LinkMovementMethodExt((@NonNull String url) -> {
            if (url.equals("https://#login")) {
                funnel.logLoginAttempt();
                Intent loginIntent = LoginActivity.newIntent(EditSectionActivity.this,
                        LoginFunnel.SOURCE_EDIT, funnel.getSessionToken());
                startActivityForResult(loginIntent, Constants.ACTIVITY_REQUEST_LOGIN);
            } else {
                handleExternalLink(EditSectionActivity.this, Uri.parse(url));
            }
        }));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == Constants.ACTIVITY_REQUEST_LOGIN) {
            if (resultCode == LoginActivity.RESULT_LOGIN_SUCCESS) {
                updateEditLicenseText();
                funnel.logLoginSuccess();
                FeedbackUtil.showMessage(this, R.string.login_success_toast);
            } else {
                funnel.logLoginFailure();
            }
        }
    }

    private void cancelCalls() {
        if (csrfClient != null) {
            csrfClient.cancel();
            csrfClient = null;
        }
    }

    private void getEditTokenThenSave(boolean forceLogin) {
        cancelCalls();
        captchaContainer.setVisibility(View.GONE);
        captchaHandler.hideCaptcha();
        editSummaryFragment.saveSummary();

        csrfClient = new CsrfTokenClient(title.getWikiSite(), title.getWikiSite());
        csrfClient.request(forceLogin, new CsrfTokenClient.Callback() {
            @Override
            public void success(@NonNull String token) {
                doSave(token);
            }

            @Override
            public void failure(@NonNull Throwable caught) {
                showError(caught);
            }

            @Override
            public void twoFactorPrompt() {
                showError(new LoginClient.LoginFailedException(getResources()
                        .getString(R.string.login_2fa_other_workflow_error_msg)));
            }
        });
    }

    private void doSave(@NonNull String token) {
        String summaryText = TextUtils.isEmpty(sectionHeading) ? "" : ("/* " + sectionHeading + " */ ");
        summaryText += editPreviewFragment.getSummary();
        // Summaries are plaintext, so remove any HTML that's made its way into the summary
        summaryText = StringUtil.fromHtml(summaryText).toString();

        if (!isFinishing()) {
            showProgressBar(true);
        }

        editClient.request(title.getWikiSite(), title, sectionID,
                sectionText.getText().toString(), token, summaryText, baseTimeStamp, AccountUtil.isLoggedIn(),
                captchaHandler.isActive() ? captchaHandler.captchaId() : "null",
                captchaHandler.isActive() ? captchaHandler.captchaWord() : "null", editCallback);
    }

    private class EditCallback implements EditClient.Callback {
        @Override
        public void success(@NonNull Call<Edit> call, @NonNull EditResult result) {
            if (isFinishing()) {
                // no longer attached to activity!
                return;
            }
            if (result instanceof EditSuccessResult) {
                funnel.logSaved(((EditSuccessResult) result).getRevID());
                // TODO: remove the artificial delay and use the new revision
                // ID returned to request the updated version of the page once
                // revision support for mobile-sections is added to RESTBase
                // See https://github.com/wikimedia/restbase/pull/729
                new Handler().postDelayed(successRunnable, TimeUnit.SECONDS.toMillis(2));
                return;
            }
            showProgressBar(false);

            if (result instanceof CaptchaResult) {
                if (captchaHandler.isActive()) {
                    // Captcha entry failed!
                    funnel.logCaptchaFailure();
                }
                captchaContainer.setVisibility(View.VISIBLE);
                captchaHandler.handleCaptcha(null, (CaptchaResult) result);
                funnel.logCaptchaShown();
            } else if (result instanceof EditAbuseFilterResult) {
                abusefilterEditResult = (EditAbuseFilterResult) result;
                handleAbuseFilter();
                if (abusefilterEditResult.getType() == EditAbuseFilterResult.TYPE_ERROR) {
                    editPreviewFragment.hide();
                }
            } else if (result instanceof EditSpamBlacklistResult) {
                FeedbackUtil.showMessage(EditSectionActivity.this,
                        R.string.editing_error_spamblacklist);
                editPreviewFragment.hide();
            } else {
                funnel.logError(result.getResult());
                // Expand to do everything.
                failure(call, new Throwable());
            }
        }

        @Override
        public void failure(@NonNull Call<Edit> call, @NonNull Throwable caught) {
            if (isFinishing()) {
                // no longer attached to activity!
                return;
            }
            showProgressBar(false);
            if (caught instanceof MwException) {
                handleEditingException((MwException) caught);
                L.e(caught);
            } else {
                showRetryDialog(caught);
                L.e(caught);
            }
        }
    }

    private void showRetryDialog(@NonNull Throwable t) {
        final AlertDialog retryDialog = new AlertDialog.Builder(EditSectionActivity.this)
                .setTitle(R.string.dialog_message_edit_failed)
                .setMessage(t.getLocalizedMessage())
                .setPositiveButton(R.string.dialog_message_edit_failed_retry, (dialog, which) -> {
                    getEditTokenThenSave(false);
                    dialog.dismiss();
                })
                .setNegativeButton(R.string.dialog_message_edit_failed_cancel, (dialog, which) -> {
                    dialog.dismiss();
                }).create();
        retryDialog.show();
    }

    /**
     * Processes API error codes encountered during editing, and handles them as appropriate.
     * @param caught The MwException to handle.
     */
    private void handleEditingException(@NonNull MwException caught) {
        String code = caught.getTitle();
        if (AccountUtil.isLoggedIn() && ("badtoken".equals(code) || "assertuserfailed".equals(code))) {
            getEditTokenThenSave(true);
            return;
        }

        if (StringUtils.defaultString(code).contains("abusefilter")) {
            abusefilterEditResult = new EditAbuseFilterResult(code, caught.getMessage(), caught.getMessage());
            handleAbuseFilter();
            if (abusefilterEditResult.getType() == EditAbuseFilterResult.TYPE_ERROR) {
                editPreviewFragment.hide();
            }
        } else if ("blocked".equals(code) || "wikimedia-globalblocking-ipblocked".equals(code)) {
            // User is blocked, locally or globally
            // If they were anon, canedit does not catch this, so we can't show them the locked pencil
            // If they not anon, this means they were blocked in the interim between opening the edit
            // window and clicking save. Less common, but might as well handle it
            AlertDialog.Builder builder = new AlertDialog.Builder(EditSectionActivity.this);
            builder.setTitle(R.string.user_blocked_from_editing_title);
            if (AccountUtil.isLoggedIn()) {
                builder.setMessage(R.string.user_logged_in_blocked_from_editing);
                builder.setPositiveButton(R.string.account_editing_blocked_dialog_ok_button_text, (dialog, i) -> dialog.dismiss());
            } else {
                builder.setMessage(R.string.user_anon_blocked_from_editing);
                builder.setPositiveButton(R.string.nav_item_login, (dialog, i) -> {
                    dialog.dismiss();
                    Intent loginIntent = LoginActivity.newIntent(EditSectionActivity.this,
                            LoginFunnel.SOURCE_BLOCKED);
                    startActivity(loginIntent);
                });
                builder.setNegativeButton(R.string.account_editing_blocked_dialog_cancel_button_text, (dialog, i) -> dialog.dismiss());
            }
            builder.show();
        } else if ("editconflict".equals(code)) {
            new AlertDialog.Builder(EditSectionActivity.this)
                    .setTitle(R.string.edit_conflict_title)
                    .setMessage(R.string.edit_conflict_message)
                    .setPositiveButton(R.string.edit_conflict_dialog_ok_button_text, null)
                    .show();
            resetToStart();
        } else {
            showError(caught);
        }
    }

    private void handleAbuseFilter() {
        if (abusefilterEditResult == null) {
            return;
        }
        if (abusefilterEditResult.getType() == EditAbuseFilterResult.TYPE_ERROR) {
            funnel.logAbuseFilterError(abusefilterEditResult.getCode());
            abuseFilterImage.setImageDrawable(AppCompatResources.getDrawable(this, R.drawable.ic_abusefilter_disallow));
            abusefilterTitle.setText(getString(R.string.abusefilter_title_disallow));
            abusefilterText.setText(StringUtil.fromHtml(getString(R.string.abusefilter_text_disallow)));
        } else {
            funnel.logAbuseFilterWarning(abusefilterEditResult.getCode());
            abuseFilterImage.setImageDrawable(AppCompatResources.getDrawable(this, R.drawable.ic_abusefilter_warn));
            abusefilterTitle.setText(getString(R.string.abusefilter_title_warn));
            abusefilterText.setText(StringUtil.fromHtml(getString(R.string.abusefilter_text_warn)));
        }

        hideSoftKeyboard(this);
        ViewAnimations.fadeIn(abusefilterContainer, this::supportInvalidateOptionsMenu);
    }


    private void cancelAbuseFilter() {
        abusefilterEditResult = null;
        ViewAnimations.fadeOut(abusefilterContainer, this::supportInvalidateOptionsMenu);
    }

    /**
     * Executes a click of the actionbar button, and performs the appropriate action
     * based on the current state of the button.
     */
    public void clickNextButton() {
        if (editSummaryFragment.isActive()) {
            //we're showing the custom edit summary window, so close it and
            //apply the provided summary.
            editSummaryFragment.hide();
            editPreviewFragment.setCustomSummary(editSummaryFragment.getSummary());
        } else if (editPreviewFragment.isActive()) {
            //we're showing the Preview window, which means that the next step is to save it!
            if (abusefilterEditResult != null) {
                //if the user was already shown an AbuseFilter warning, and they're ignoring it:
                funnel.logAbuseFilterWarningIgnore(abusefilterEditResult.getCode());
            }
            getEditTokenThenSave(false);
            funnel.logSaveAttempt();
        } else {
            //we must be showing the editing window, so show the Preview.
            hideSoftKeyboard(this);
            editPreviewFragment.showPreview(title, sectionText.getText().toString());
            funnel.logPreview();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_save_section:
                clickNextButton();
                return true;
            case R.id.menu_edit_zoom_in:
                Prefs.setEditingTextSizeExtra(Prefs.getEditingTextSizeExtra() + 1);
                updateTextSize();
                return true;
            case R.id.menu_edit_zoom_out:
                Prefs.setEditingTextSizeExtra(Prefs.getEditingTextSizeExtra() - 1);
                updateTextSize();
                return true;
            case R.id.menu_find_in_editor:
                showFindInEditor();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_edit_section, menu);
        MenuItem item = menu.findItem(R.id.menu_save_section);
        menu.findItem(R.id.menu_edit_zoom_in).setVisible(!editPreviewFragment.isActive());
        menu.findItem(R.id.menu_edit_zoom_out).setVisible(!editPreviewFragment.isActive());
        menu.findItem(R.id.menu_find_in_editor).setVisible(!editPreviewFragment.isActive());

        item.setTitle(getString(editPreviewFragment.isActive() ? R.string.edit_done : R.string.edit_next));
        if (progressBar.getVisibility() == View.GONE) {
            if (abusefilterEditResult != null) {
                item.setEnabled(abusefilterEditResult.getType() != EditAbuseFilterResult.TYPE_ERROR);
            } else {
                item.setEnabled(sectionTextModified);
            }
        } else {
            item.setEnabled(false);
        }

        View v = getLayoutInflater().inflate(R.layout.item_edit_actionbar_button, null);
        item.setActionView(v);
        TextView textView = v.findViewById(R.id.edit_actionbar_button_text);
        textView.setText(item.getTitle());
        textView.setTextColor(ResourceUtil.getThemedColor(this,
                item.isEnabled() ? R.attr.colorAccent : R.attr.material_theme_de_emphasised_color));
        v.setTag(item);
        v.setEnabled(item.isEnabled());
        v.setOnClickListener((view) -> onOptionsItemSelected((MenuItem) view.getTag()));
        return true;
    }

    @Override
    public void onActionModeStarted(ActionMode mode) {
        super.onActionModeStarted(mode);
        if (mode.getTag() == null) {
            // since we disabled the close button in the AndroidManifest.xml, we need to manually setup a close button when in an action mode if long pressed on texts.
            ViewUtil.setCloseButtonInActionMode(EditSectionActivity.this, mode);
        }
    }

    public void showError(@Nullable Throwable caught) {
        errorView.setError(caught);
        errorView.setVisibility(View.VISIBLE);
    }

    public void showFindInEditor() {
        startActionMode(new ActionMode.Callback() {
            private final String actionModeTag = "actionModeFindInEditor";

            @Override
            public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                actionMode = mode;
                MenuItem menuItem = menu.add(R.string.edit_section_find_in_page);
                menuItem.setActionProvider(new FindInEditorActionProvider(sectionScrollView, sectionText, syntaxHighlighter, actionMode));
                menuItem.expandActionView();
                return true;
            }

            @Override
            public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                mode.setTag(actionModeTag);
                return false;
            }

            @Override
            public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                return false;
            }

            @Override
            public void onDestroyActionMode(ActionMode mode) {
                sectionText.clearMatches(syntaxHighlighter);
                sectionText.setSelection(sectionText.getSelectionStart(), sectionText.getSelectionStart());
            }
        });
    }

    public void finishActionMode() {
        if (actionMode != null) {
            actionMode.finish();
            actionMode = null;
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("hasTemporaryWikitextStored", true);
        outState.putParcelable("abusefilter", abusefilterEditResult);
        outState.putBoolean("sectionTextModified", sectionTextModified);
        captchaHandler.saveState(outState);
        Prefs.storeTemporaryWikitext(sectionWikitext);
    }

    private void updateTextSize() {
        int extra = Prefs.getEditingTextSizeExtra();
        sectionText.setTextSize(WikipediaApp.getInstance().getFontSize(getWindow()) + ((float)extra));
    }

    private void resetToStart() {
        if (captchaHandler.isActive()) {
            captchaHandler.cancelCaptcha();
            captchaContainer.setVisibility(View.GONE);
        }
        if (editSummaryFragment.isActive()) {
            editSummaryFragment.hide();
        }
        if (editPreviewFragment.isActive()) {
            editPreviewFragment.hide();
        }
    }

    private void fetchSectionText() {
        if (sectionWikitext == null) {
            disposables.add(ServiceFactory.get(title.getWikiSite()).getWikiTextForSection(title.getPrefixedText(), sectionID)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(response -> {
                        MwQueryPage.Revision rev = response.query().firstPage().revisions().get(0);
                        title = new PageTitle(response.query().firstPage().title(), title.getWikiSite());
                        sectionWikitext = rev.content();
                        baseTimeStamp = rev.timeStamp();
                        displaySectionText();
                    }, throwable -> {
                        showProgressBar(false);
                        showError(throwable);
                        L.e(throwable);
                    }));
        } else {
            displaySectionText();
        }
    }

    private void displaySectionText() {
        sectionText.setText(sectionWikitext);
        ViewAnimations.crossFade(progressBar, sectionContainer);
        scrollToHighlight(textToHighlight);

        if (pageProps != null && !TextUtils.isEmpty(pageProps.getEditProtectionStatus())) {
            String message;
            switch (pageProps.getEditProtectionStatus()) {
                case "sysop":
                    message = getString(R.string.page_protected_sysop);
                    break;
                case "autoconfirmed":
                    message = getString(R.string.page_protected_autoconfirmed);
                    break;
                default:
                    message = getString(R.string.page_protected_other, pageProps.getEditProtectionStatus());
                    break;
            }
            FeedbackUtil.showMessage(this, message);
        }
    }

    private void scrollToHighlight(@Nullable final String highlightText) {
        if (highlightText == null || !TextUtils.isGraphic(highlightText)) {
            return;
        }
        sectionText.post(() -> {
            sectionScrollView.fullScroll(View.FOCUS_DOWN);
            final int scrollDelayMs = 500;
            sectionText.postDelayed(() -> StringUtil.highlightEditText(sectionText, sectionWikitext, highlightText), scrollDelayMs);
        });
    }

    public void showProgressBar(boolean enable) {
        progressBar.setVisibility(enable ? View.VISIBLE : View.GONE);
        supportInvalidateOptionsMenu();
    }

    /**
     * Shows the custom edit summary input fragment, where the user may enter a summary
     * that's different from the standard summary tags.
     */
    public void showCustomSummary() {
        editSummaryFragment.show();
    }

    @Override
    public void onBackPressed() {
        if (progressBar.getVisibility() == View.VISIBLE) {
            // If it is visible, it means we should wait until all the requests are done.
            return;
        }
        showProgressBar(false);
        if (captchaHandler.isActive()) {
            captchaHandler.cancelCaptcha();
            captchaContainer.setVisibility(View.GONE);
        }
        if (abusefilterEditResult != null) {
            if (abusefilterEditResult.getType() == EditAbuseFilterResult.TYPE_WARNING) {
                funnel.logAbuseFilterWarningBack(abusefilterEditResult.getCode());
            }
            cancelAbuseFilter();
            return;
        }
        if (errorView.getVisibility() == View.VISIBLE) {
            errorView.setVisibility(View.GONE);
        }
        if (editSummaryFragment.handleBackPressed()) {
            return;
        }
        if (editPreviewFragment.handleBackPressed()) {
            return;
        }

        hideSoftKeyboard(this);

        if (sectionTextModified) {
            AlertDialog.Builder alert = new AlertDialog.Builder(this);
            alert.setMessage(getString(R.string.edit_abandon_confirm));
            alert.setPositiveButton(getString(R.string.edit_abandon_confirm_yes), (dialog, id) -> {
                dialog.dismiss();
                finish();
            });
            alert.setNegativeButton(getString(R.string.edit_abandon_confirm_no), (dialog, id) -> dialog.dismiss());
            alert.create().show();
        } else {
            finish();
        }
    }

    private class SectionTextWatcher implements TextWatcher {
        @Override
        public void beforeTextChanged(CharSequence charSequence, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence charSequence, int start, int before, int count) {
        }

        @Override
        public void afterTextChanged(Editable editable) {
            if (sectionTextFirstLoad) {
                sectionTextFirstLoad = false;
                return;
            }
            if (!sectionTextModified) {
                sectionTextModified = true;
                // update the actionbar menu, which will enable the Next button.
                supportInvalidateOptionsMenu();
            }
        }
    }
}
