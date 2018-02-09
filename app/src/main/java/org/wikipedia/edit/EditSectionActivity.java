package org.wikipedia.edit;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

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
import org.wikipedia.dataclient.mwapi.MwException;
import org.wikipedia.dataclient.mwapi.MwQueryResponse;
import org.wikipedia.edit.preview.EditPreviewFragment;
import org.wikipedia.edit.richtext.SyntaxHighlighter;
import org.wikipedia.edit.summaries.EditSummaryFragment;
import org.wikipedia.edit.wikitext.WikitextClient;
import org.wikipedia.login.LoginActivity;
import org.wikipedia.login.LoginClient;
import org.wikipedia.page.LinkMovementMethodExt;
import org.wikipedia.page.PageProperties;
import org.wikipedia.page.PageTitle;
import org.wikipedia.util.FeedbackUtil;
import org.wikipedia.util.ResourceUtil;
import org.wikipedia.util.StringUtil;
import org.wikipedia.util.log.L;
import org.wikipedia.views.ViewAnimations;
import org.wikipedia.views.WikiErrorView;

import java.util.concurrent.TimeUnit;

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

    private CsrfTokenClient csrfClient;

    private PageTitle title;
    private int sectionID;
    private String sectionHeading;
    private PageProperties pageProps;
    private String textToHighlight;

    private String sectionWikitext;
    private SyntaxHighlighter syntaxHighlighter;

    private EditText sectionText;
    private boolean sectionTextModified = false;
    private boolean sectionTextFirstLoad = true;

    private View sectionProgress;
    private ScrollView sectionContainer;
    private WikiErrorView errorView;

    private View abusefilterContainer;
    private ImageView abuseFilterImage;
    private TextView abusefilterTitle;
    private TextView abusefilterText;

    private EditAbuseFilterResult abusefilterEditResult;

    private CaptchaHandler captchaHandler;

    private EditPreviewFragment editPreviewFragment;

    private EditSummaryFragment editSummaryFragment;

    private EditFunnel funnel;

    private ProgressDialog progressDialog;

    private Runnable successRunnable = new Runnable() {
        @Override public void run() {
            progressDialog.dismiss();

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
        setStatusBarColor(ResourceUtil.getThemedAttributeId(this, R.attr.page_status_bar_color));

        if (!getIntent().getAction().equals(ACTION_EDIT_SECTION)) {
            throw new RuntimeException("Much wrong action. Such exception. Wow");
        }

        title = getIntent().getParcelableExtra(EXTRA_TITLE);
        sectionID = getIntent().getIntExtra(EXTRA_SECTION_ID, 0);
        sectionHeading = getIntent().getStringExtra(EXTRA_SECTION_HEADING);
        pageProps = getIntent().getParcelableExtra(EXTRA_PAGE_PROPS);
        textToHighlight = getIntent().getStringExtra(EXTRA_HIGHLIGHT_TEXT);

        progressDialog = new ProgressDialog(this);
        progressDialog.setIndeterminate(true);
        progressDialog.setCancelable(false);
        progressDialog.setMessage(getString(R.string.dialog_saving_in_progress));

        final ActionBar supportActionBar = getSupportActionBar();
        if (supportActionBar != null) {
            supportActionBar.setTitle("");
        }

        sectionText = findViewById(R.id.edit_section_text);

        syntaxHighlighter = new SyntaxHighlighter(this, sectionText);

        sectionProgress = findViewById(R.id.edit_section_load_progress);
        sectionContainer = findViewById(R.id.edit_section_container);
        sectionContainer.setSmoothScrollingEnabled(false);
        errorView = findViewById(R.id.view_edit_section_error);

        abusefilterContainer = findViewById(R.id.edit_section_abusefilter_container);
        abuseFilterImage = findViewById(R.id.edit_section_abusefilter_image);
        abusefilterTitle = findViewById(R.id.edit_section_abusefilter_title);
        abusefilterText = findViewById(R.id.edit_section_abusefilter_text);

        captchaHandler = new CaptchaHandler(this, title.getWikiSite(), progressDialog, sectionContainer, "", null);

        editPreviewFragment = (EditPreviewFragment) getSupportFragmentManager().findFragmentById(R.id.edit_section_preview_fragment);
        editSummaryFragment = (EditSummaryFragment) getSupportFragmentManager().findFragmentById(R.id.edit_section_summary_fragment);

        updateEditLicenseText();
        editSummaryFragment.setTitle(title);

        funnel = WikipediaApp.getInstance().getFunnelManager().getEditFunnel(title);

        // Only send the editing start log event if the activity is created for the first time
        if (savedInstanceState == null) {
            funnel.logStart();
        }

        if (savedInstanceState != null && savedInstanceState.containsKey("sectionWikitext")) {
            sectionWikitext = savedInstanceState.getString("sectionWikitext");
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

        sectionText.addTextChangedListener(new TextWatcher() {
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
        });

        // set focus to the EditText, but keep the keyboard hidden until the user changes the cursor location:
        sectionText.requestFocus();
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
    }

    @Override
    public void onDestroy() {
        cancelCalls();
        if (progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
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

        editLicenseText.setMovementMethod(new LinkMovementMethodExt((@NonNull String url, @Nullable String notUsed) -> {
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
            progressDialog.show();
        }

        new EditClient().request(title.getWikiSite(), title, sectionID,
                sectionText.getText().toString(), token, summaryText, AccountUtil.isLoggedIn(),
                captchaHandler.isActive() ? captchaHandler.captchaId() : "null",
                captchaHandler.isActive() ? captchaHandler.captchaWord() : "null",
                new EditClient.Callback() {
                    @Override
                    public void success(@NonNull Call<Edit> call, @NonNull EditResult result) {
                        if (isFinishing() || !progressDialog.isShowing()) {
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
                        } else if (result instanceof CaptchaResult) {
                            if (captchaHandler.isActive()) {
                                // Captcha entry failed!
                                funnel.logCaptchaFailure();
                            }
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
                            progressDialog.dismiss();
                            editPreviewFragment.hide();
                        } else {
                            funnel.logError(result.getResult());
                            // Expand to do everything.
                            failure(call, new Throwable());
                        }
                    }

                    @Override
                    public void failure(@NonNull Call<Edit> call, @NonNull Throwable caught) {
                        if (isFinishing() || !progressDialog.isShowing()) {
                            // no longer attached to activity!
                            return;
                        }
                        if (caught instanceof MwException) {
                            handleEditingException((MwException) caught);
                            L.e(caught);
                        } else {
                            showRetryDialog(caught);
                            L.e(caught);
                        }
                    }
                });
    }

    private void showRetryDialog(@NonNull Throwable t) {
        final AlertDialog retryDialog = new AlertDialog.Builder(EditSectionActivity.this)
                .setTitle(R.string.dialog_message_edit_failed)
                .setMessage(t.getLocalizedMessage())
                .setPositiveButton(R.string.dialog_message_edit_failed_retry, (dialog, which) -> {
                    getEditTokenThenSave(false);
                    dialog.dismiss();
                    progressDialog.dismiss();
                })
                .setNegativeButton(R.string.dialog_message_edit_failed_cancel, (dialog, which) -> {
                    dialog.dismiss();
                    progressDialog.dismiss();
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
        } else if ("blocked".equals(code) || "wikimedia-globalblocking-ipblocked".equals(code)) {
            // User is blocked, locally or globally
            // If they were anon, canedit does not catch this, so we can't show them the locked pencil
            // If they not anon, this means they were blocked in the interim between opening the edit
            // window and clicking save. Less common, but might as well handle it
            progressDialog.dismiss();
            AlertDialog.Builder builder = new AlertDialog.Builder(EditSectionActivity.this);
            builder.setTitle(R.string.user_blocked_from_editing_title);
            if (AccountUtil.isLoggedIn()) {
                builder.setMessage(R.string.user_logged_in_blocked_from_editing);
                builder.setPositiveButton(android.R.string.ok, (dialog, i) -> dialog.dismiss());
            } else {
                builder.setMessage(R.string.user_anon_blocked_from_editing);
                builder.setPositiveButton(R.string.nav_item_login, (dialog, i) -> {
                    dialog.dismiss();
                    Intent loginIntent = LoginActivity.newIntent(EditSectionActivity.this,
                            LoginFunnel.SOURCE_BLOCKED);
                    startActivity(loginIntent);
                });
                builder.setNegativeButton(android.R.string.cancel, (dialog, i) -> dialog.dismiss());
            }
            builder.show();
        } else {
            progressDialog.dismiss();
            showError(caught);
        }
    }

    private void handleAbuseFilter() {
        if (abusefilterEditResult == null) {
            return;
        }
        if (abusefilterEditResult.getType() == EditAbuseFilterResult.TYPE_ERROR) {
            funnel.logAbuseFilterError(abusefilterEditResult.getCode());
            abuseFilterImage.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_abusefilter_disallow));
            abusefilterTitle.setText(getString(R.string.abusefilter_title_disallow));
            abusefilterText.setText(StringUtil.fromHtml(getString(R.string.abusefilter_text_disallow)));
        } else {
            funnel.logAbuseFilterWarning(abusefilterEditResult.getCode());
            abuseFilterImage.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_abusefilter_warn));
            abusefilterTitle.setText(getString(R.string.abusefilter_title_warn));
            abusefilterText.setText(StringUtil.fromHtml(getString(R.string.abusefilter_text_warn)));
        }

        hideSoftKeyboard(this);
        ViewAnimations.fadeIn(abusefilterContainer, this::supportInvalidateOptionsMenu);

        if (progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
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
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_edit_section, menu);
        MenuItem item = menu.findItem(R.id.menu_save_section);

        if (editSummaryFragment.isActive()) {
            item.setTitle(getString(R.string.edit_next));
        } else if (editPreviewFragment.isActive()) {
            item.setTitle(getString(R.string.edit_done));
        } else {
            item.setTitle(getString(R.string.edit_next));
        }

        if (abusefilterEditResult != null) {
            if (abusefilterEditResult.getType() == EditAbuseFilterResult.TYPE_ERROR) {
                item.setEnabled(false);
            } else {
                item.setEnabled(true);
            }
        } else {
            item.setEnabled(sectionTextModified);
        }

        View v = getLayoutInflater().inflate(R.layout.item_edit_actionbar_button, null);
        item.setActionView(v);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.MATCH_PARENT);
        v.setLayoutParams(params);
        TextView txtView = v.findViewById(R.id.edit_actionbar_button_text);
        txtView.setText(item.getTitle());
        txtView.setTypeface(null, item.isEnabled() ? Typeface.BOLD : Typeface.NORMAL);
        v.setTag(item);
        v.setClickable(true);
        v.setEnabled(item.isEnabled());
        v.setOnClickListener((view) -> onOptionsItemSelected((MenuItem) view.getTag()));

        v.setBackgroundColor(ContextCompat.getColor(this, item.isEnabled()
                ? (editPreviewFragment.isActive() ? R.color.accent50
                : ResourceUtil.getThemedAttributeId(this, R.attr.colorAccent)) : R.color.base50));

        return true;
    }

    public void showError(@Nullable Throwable caught) {
        errorView.setError(caught);
        errorView.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("sectionWikitext", sectionWikitext);
        outState.putParcelable("abusefilter", abusefilterEditResult);
        outState.putBoolean("sectionTextModified", sectionTextModified);
        captchaHandler.saveState(outState);
    }

    private void fetchSectionText() {
        if (sectionWikitext == null) {
            new WikitextClient().request(title.getWikiSite(), title, sectionID, new WikitextClient.Callback() {
                @Override
                public void success(@NonNull Call<MwQueryResponse> call, @NonNull String normalizedTitle, @NonNull String wikitext) {
                    title = new PageTitle(normalizedTitle, title.getWikiSite());
                    sectionWikitext = wikitext;
                    displaySectionText();
                }

                @Override
                public void failure(@NonNull Call<MwQueryResponse> call, @NonNull Throwable caught) {
                    sectionProgress.setVisibility(View.GONE);
                    showError(caught);
                    L.e(caught);
                }
            });
        } else {
            displaySectionText();
        }
    }

    private void displaySectionText() {
        sectionText.setText(sectionWikitext);
        ViewAnimations.crossFade(sectionProgress, sectionContainer);
        supportInvalidateOptionsMenu();
        scrollToHighlight(textToHighlight);

        if (pageProps != null && pageProps.getEditProtectionStatus() != null) {
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
            sectionContainer.fullScroll(View.FOCUS_DOWN);
            final int scrollDelayMs = 500;
            sectionText.postDelayed(() -> setHighlight(highlightText), scrollDelayMs);
        });
    }

    private void setHighlight(@NonNull String highlightText) {
        String[] words = highlightText.split("\\s+");
        int pos = 0;
        for (String word : words) {
            pos = sectionWikitext.indexOf(word, pos);
            if (pos == -1) {
                break;
            }
        }
        if (pos == -1) {
            pos = sectionWikitext.indexOf(words[words.length - 1]);
        }
        if (pos > 0) {
            // TODO: Programmatic selection doesn't seem to work with RTL content...
            sectionText.setSelection(pos, pos + words[words.length - 1].length());
            sectionText.performLongClick();
        }
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
        if (captchaHandler.isActive()) {
            captchaHandler.cancelCaptcha();
        }
        if (abusefilterEditResult != null) {
            if (abusefilterEditResult.getType() == EditAbuseFilterResult.TYPE_WARNING) {
                funnel.logAbuseFilterWarningBack(abusefilterEditResult.getCode());
            }
            cancelAbuseFilter();
            return;
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
}
