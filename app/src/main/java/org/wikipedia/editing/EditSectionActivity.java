package org.wikipedia.editing;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.Html;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import com.squareup.otto.Bus;
import org.mediawiki.api.json.Api;
import org.mediawiki.api.json.ApiException;
import org.mediawiki.api.json.RequestBuilder;
import org.wikipedia.activity.ActivityUtil;
import org.wikipedia.login.LoginResult;
import org.wikipedia.login.LoginTask;
import org.wikipedia.login.authmanager.AMLoginInfoResult;
import org.wikipedia.login.authmanager.AMLoginInfoTask;
import org.wikipedia.page.PageTitle;
import org.wikipedia.R;
import org.wikipedia.activity.ThemedActionBarActivity;
import org.wikipedia.ViewAnimations;
import org.wikipedia.WikipediaApp;
import org.wikipedia.analytics.EditFunnel;
import org.wikipedia.analytics.LoginFunnel;
import org.wikipedia.editing.summaries.EditSummaryFragment;
import org.wikipedia.editing.richtext.SyntaxHighlighter;
import org.wikipedia.login.LoginActivity;
import org.wikipedia.login.authmanager.AMLoginResult;
import org.wikipedia.login.authmanager.AMLoginTask;
import org.wikipedia.login.User;
import org.wikipedia.page.LinkMovementMethodExt;
import org.wikipedia.page.PageProperties;
import org.wikipedia.util.FeedbackUtil;
import org.wikipedia.util.log.L;

import static org.wikipedia.util.L10nUtil.setConditionalTextDirection;
import static org.wikipedia.util.DeviceUtil.hideSoftKeyboard;
import static org.wikipedia.util.UriUtil.handleExternalLink;

public class EditSectionActivity extends ThemedActionBarActivity {
    public static final String ACTION_EDIT_SECTION = "org.wikipedia.edit_section";
    public static final String EXTRA_TITLE = "org.wikipedia.edit_section.title";
    public static final String EXTRA_SECTION_ID = "org.wikipedia.edit_section.sectionid";
    public static final String EXTRA_SECTION_HEADING = "org.wikipedia.edit_section.sectionheading";
    public static final String EXTRA_PAGE_PROPS = "org.wikipedia.edit_section.pageprops";
    public static final String EXTRA_HIGHLIGHT_TEXT = "org.wikipedia.edit_section.highlight";

    private WikipediaApp app;
    private Bus bus;

    private PageTitle title;
    public PageTitle getPageTitle() {
        return title;
    }

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
    private View sectionError;

    private View abusefilterContainer;
    private ImageView abuseFilterImage;
    private TextView abusefilterTitle;
    private TextView abusefilterText;

    private AbuseFilterEditResult abusefilterEditResult;

    private CaptchaHandler captchaHandler;

    private EditPreviewFragment editPreviewFragment;

    private EditSummaryFragment editSummaryFragment;

    private EditFunnel funnel;

    private ProgressDialog progressDialog;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_section);

        if (!getIntent().getAction().equals(ACTION_EDIT_SECTION)) {
            throw new RuntimeException("Much wrong action. Such exception. Wow");
        }

        app = (WikipediaApp)getApplicationContext();

        title = getIntent().getParcelableExtra(EXTRA_TITLE);
        sectionID = getIntent().getIntExtra(EXTRA_SECTION_ID, 0);
        sectionHeading = getIntent().getStringExtra(EXTRA_SECTION_HEADING);
        pageProps = getIntent().getParcelableExtra(EXTRA_PAGE_PROPS);
        textToHighlight = getIntent().getStringExtra(EXTRA_HIGHLIGHT_TEXT);

        progressDialog = new ProgressDialog(this);
        progressDialog.setIndeterminate(true);
        progressDialog.setCancelable(false);
        progressDialog.setMessage(getString(R.string.dialog_saving_in_progress));

        getSupportActionBar().setTitle("");

        sectionText = (EditText) findViewById(R.id.edit_section_text);

        syntaxHighlighter = new SyntaxHighlighter(this, sectionText);

        sectionProgress = findViewById(R.id.edit_section_load_progress);
        sectionContainer = (ScrollView) findViewById(R.id.edit_section_container);
        sectionContainer.setSmoothScrollingEnabled(false);
        sectionError = findViewById(R.id.edit_section_error);
        Button sectionErrorRetry = (Button) findViewById(R.id.edit_section_error_retry);

        abusefilterContainer = findViewById(R.id.edit_section_abusefilter_container);
        abuseFilterImage = (ImageView) findViewById(R.id.edit_section_abusefilter_image);
        abusefilterTitle = (TextView) findViewById(R.id.edit_section_abusefilter_title);
        abusefilterText = (TextView) findViewById(R.id.edit_section_abusefilter_text);

        captchaHandler = new CaptchaHandler(this, title.getSite(), progressDialog, sectionContainer, "", null);

        editPreviewFragment = (EditPreviewFragment) getSupportFragmentManager().findFragmentById(R.id.edit_section_preview_fragment);
        editSummaryFragment = (EditSummaryFragment) getSupportFragmentManager().findFragmentById(R.id.edit_section_summary_fragment);

        updateEditLicenseText();
        editSummaryFragment.setTitle(title);

        bus = app.getBus();
        bus.register(this);

        funnel = app.getFunnelManager().getEditFunnel(title);

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

        sectionErrorRetry.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ViewAnimations.crossFade(sectionError, sectionProgress);
                fetchSectionText();
            }
        });


        setConditionalTextDirection(sectionText, title.getSite().languageCode());

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
    protected void setTheme() {
        setActionBarTheme();
    }

    private void updateEditLicenseText() {
        TextView editLicenseText = (TextView) findViewById(R.id.edit_section_license_text);
        if (app.getUserInfoStorage().isLoggedIn()) {
            editLicenseText.setText(Html.fromHtml(getString(R.string.edit_save_action_license_logged_in)));
        } else {
            editLicenseText.setText(Html.fromHtml(getString(R.string.edit_save_action_license_anon)));
        }

        editLicenseText.setMovementMethod(new LinkMovementMethodExt(new LinkMovementMethodExt.UrlHandler() {
            @Override
            public void onUrlClick(String url) {
                if (url.equals("https://#login")) {
                    funnel.logLoginAttempt();
                    Intent loginIntent = LoginActivity.newIntent(EditSectionActivity.this,
                            LoginFunnel.SOURCE_EDIT, funnel.getSessionToken());
                    startActivityForResult(loginIntent, LoginActivity.REQUEST_LOGIN);
                } else {
                    handleExternalLink(EditSectionActivity.this, Uri.parse(url));
                }
            }
        }));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == LoginActivity.REQUEST_LOGIN) {
            if (resultCode == LoginActivity.RESULT_LOGIN_SUCCESS) {
                updateEditLicenseText();
                funnel.logLoginSuccess();
                FeedbackUtil.showMessage(this, R.string.login_success_toast);
            } else {
                funnel.logLoginFailure();
            }
        }
    }

    private void doSave() {
        captchaHandler.hideCaptcha();
        editSummaryFragment.saveSummary();
        app.getEditTokenStorage().get(title.getSite(), new EditTokenStorage.TokenRetrievedCallback() {
            @Override
            public void onTokenRetrieved(final String token) {

                String summaryText = TextUtils.isEmpty(sectionHeading) ? "" : ("/* " + sectionHeading + " */ ");
                summaryText += editPreviewFragment.getSummary();
                // Summaries are plaintext, so remove any HTML that's made its way into the summary
                summaryText = Html.fromHtml(summaryText).toString();

                new EditTask(EditSectionActivity.this, title, sectionText.getText().toString(),
                        sectionID, token, summaryText, app.getUserInfoStorage().isLoggedIn()) {
                    @Override
                    public void onBeforeExecute() {
                        if (!isFinishing()) {
                            progressDialog.show();
                        }
                    }

                    @Override
                    public RequestBuilder buildRequest(Api api) {
                        return captchaHandler.populateBuilder(super.buildRequest(api));
                    }

                    @Override
                    public void onCatch(Throwable caught) {
                        if (isFinishing() || !progressDialog.isShowing()) {
                            // no longer attached to activity!
                            return;
                        }
                        if (caught instanceof ApiException) {
                            // This is a fairly standard editing exception. Handle it appropriately.
                            handleEditingException((ApiException) caught);
                        } else {
                            // If it's not an API exception, we have no idea what's wrong.
                            // Show the user a generic error message.
                            Log.w("Wikipedia", "Caught " + caught.toString());
                            showRetryDialog();
                        }
                    }

                    @Override
                    public void onFinish(EditingResult result) {
                        if (isFinishing() || !progressDialog.isShowing()) {
                            // no longer attached to activity!
                            return;
                        }
                        if (result instanceof SuccessEditResult) {
                            funnel.logSaved(((SuccessEditResult) result).getRevID());
                            progressDialog.dismiss();

                            //Build intent that includes the section we were editing, so we can scroll to it later
                            Intent data = new Intent();
                            data.putExtra(EXTRA_SECTION_ID, sectionID);
                            setResult(EditHandler.RESULT_REFRESH_PAGE, data);
                            hideSoftKeyboard(EditSectionActivity.this);
                            finish();
                        } else if (result instanceof CaptchaResult) {
                            if (captchaHandler.isActive()) {
                                // Captcha entry failed!
                                funnel.logCaptchaFailure();
                            }
                            captchaHandler.handleCaptcha((CaptchaResult) result);
                            funnel.logCaptchaShown();
                        } else if (result instanceof AbuseFilterEditResult) {
                            abusefilterEditResult = (AbuseFilterEditResult) result;
                            handleAbuseFilter();
                            if (abusefilterEditResult.getType() == AbuseFilterEditResult.TYPE_ERROR) {
                                editPreviewFragment.hide();
                            }
                        } else if (result instanceof SpamBlacklistEditResult) {
                            FeedbackUtil.showMessage(EditSectionActivity.this,
                                    R.string.editing_error_spamblacklist);
                            progressDialog.dismiss();
                            editPreviewFragment.hide();
                        } else {
                            funnel.logError(result.getResult());
                            // Expand to do everything.
                            onCatch(null);
                        }

                    }
                }.execute();
            }

            @Override
            public void onTokenFailed(Throwable caught) {
                if (isFinishing()) {
                    return;
                }
                if (!(caught instanceof ApiException)) {
                    throw new RuntimeException(caught);
                }
                showRetryDialog();
            }
        });
    }

    private void showRetryDialog() {
        final AlertDialog retryDialog = new AlertDialog.Builder(EditSectionActivity.this)
                .setMessage(R.string.dialog_message_edit_failed)
                .setPositiveButton(R.string.dialog_message_edit_failed_retry, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        doSave();
                        dialog.dismiss();
                        progressDialog.dismiss();
                    }
                })
                .setNegativeButton(R.string.dialog_message_edit_failed_cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        progressDialog.dismiss();
                    }
                }).create();
        retryDialog.show();
    }

    /**
     * Processes API error codes encountered during editing, and handles them as appropriate.
     * @param e The ApiException to handle.
     */
    private void handleEditingException(@NonNull ApiException e) {
        String code = e.getCode();
        if (app.getUserInfoStorage().isLoggedIn() && ("badtoken".equals(code)
                || "assertuserfailed".equals(code))) {
            // looks like our session expired.
            app.getEditTokenStorage().clearAllTokens();
            app.getCookieManager().clearAllCookies();

            User user = app.getUserInfoStorage().getUser();
            attemptLoginAndSave(user);
        } else if ("blocked".equals(code) || "wikimedia-globalblocking-ipblocked".equals(code)) {
            // User is blocked, locally or globally
            // If they were anon, canedit does not catch this, so we can't show them the locked pencil
            // If they not anon, this means they were blocked in the interim between opening the edit
            // window and clicking save. Less common, but might as well handle it
            progressDialog.dismiss();
            AlertDialog.Builder builder = new AlertDialog.Builder(EditSectionActivity.this);
            builder.setTitle(R.string.user_blocked_from_editing_title);
            if (app.getUserInfoStorage().isLoggedIn()) {
                builder.setMessage(R.string.user_logged_in_blocked_from_editing);
                builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                    }
                });
            } else {
                builder.setMessage(R.string.user_anon_blocked_from_editing);
                builder.setPositiveButton(R.string.nav_item_login, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                        Intent loginIntent = LoginActivity.newIntent(EditSectionActivity.this,
                                LoginFunnel.SOURCE_BLOCKED);
                        startActivity(loginIntent);
                    }
                });
                builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                    }
                });
            }
            builder.show();
        } else {
            // an unknown error occurred, so just dismiss the progress dialog and show a message.
            progressDialog.dismiss();
            FeedbackUtil.showError(this, e);
        }
    }

    private void attemptLoginAndSave(final User user) {
        new AMLoginInfoTask() {
            @Override
            public void onCatch(Throwable caught) {
                L.e("AMLoginInfoTask failed: " + caught.getMessage());
            }

            @Override
            public void onFinish(AMLoginInfoResult result) {
                if (result.getEnabled()) {
                    L.i("Logging in with AuthManager");
                    doAuthManagerLoginAndSave(user);
                } else {
                    L.i("Logging in with legacy login");
                    doLegacyLoginAndSave(user);
                }
            }
        }.execute();
    }

    private void doAuthManagerLoginAndSave(final User user) {
        new AMLoginTask(user.getUsername(), user.getPassword()) {
            @Override
            public void onFinish(AMLoginResult result) {
                if (result.pass()) {
                    doSave();
                } else {
                    progressDialog.dismiss();
                    ViewAnimations.crossFade(sectionText, sectionError);
                    sectionError.setVisibility(View.VISIBLE);
                }
            }
        }.execute();
    }

    private void doLegacyLoginAndSave(final User user) {
        new LoginTask(app, app.getSite(), user.getUsername(), user.getPassword()) {
            @Override
            public void onFinish(LoginResult result) {
                if ("Success".equals(result.getCode())) {
                    doSave();
                } else {
                    progressDialog.dismiss();
                    ViewAnimations.crossFade(sectionText, sectionError);
                    sectionError.setVisibility(View.VISIBLE);
                }
            }
        }.execute();
    }

    private void handleAbuseFilter() {
        if (abusefilterEditResult == null) {
            return;
        }
        if (abusefilterEditResult.getType() == AbuseFilterEditResult.TYPE_ERROR) {
            funnel.logAbuseFilterError(abusefilterEditResult.getCode());
            abuseFilterImage.setImageResource(R.drawable.abusefilter_disallow);
            abusefilterTitle.setText(getString(R.string.abusefilter_title_disallow));
            abusefilterText.setText(Html.fromHtml(getString(R.string.abusefilter_text_disallow)));
        } else {
            funnel.logAbuseFilterWarning(abusefilterEditResult.getCode());
            abuseFilterImage.setImageResource(R.drawable.abusefilter_warn);
            abusefilterTitle.setText(getString(R.string.abusefilter_title_warn));
            abusefilterText.setText(Html.fromHtml(getString(R.string.abusefilter_text_warn)));
        }

        hideSoftKeyboard(this);
        ViewAnimations.fadeIn(abusefilterContainer, new Runnable() {
            @Override
            public void run() {
                supportInvalidateOptionsMenu();
            }
        });

        if (progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }


    private void cancelAbuseFilter() {
        abusefilterEditResult = null;
        ViewAnimations.fadeOut(abusefilterContainer, new Runnable() {
            @Override
            public void run() {
                supportInvalidateOptionsMenu();
            }
        });
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
            doSave();
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
                return ActivityUtil.defaultOnOptionsItemSelected(this, item)
                        || super.onOptionsItemSelected(item);
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
            if (abusefilterEditResult.getType() == AbuseFilterEditResult.TYPE_ERROR) {
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
        TextView txtView = (TextView) v.findViewById(R.id.edit_actionbar_button_text);
        txtView.setText(item.getTitle());
        txtView.setTypeface(null, item.isEnabled() ? Typeface.BOLD : Typeface.NORMAL);
        v.setTag(item);
        v.setClickable(true);
        v.setEnabled(item.isEnabled());
        v.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onOptionsItemSelected((MenuItem) view.getTag());
            }
        });

        if (editSummaryFragment.isActive()) {
            v.setBackgroundResource(R.drawable.button_selector_progressive);
        } else if (editPreviewFragment.isActive()) {
            v.setBackgroundResource(R.drawable.button_selector_complete);
        } else {
            v.setBackgroundResource(R.drawable.button_selector_progressive);
        }

        return true;
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
            new FetchSectionWikitextTask(this, title, sectionID) {
                @Override
                public void onFinish(String result) {
                    sectionWikitext = result;
                    displaySectionText();
                }

                @Override
                public void onCatch(Throwable caught) {
                    ViewAnimations.crossFade(sectionProgress, sectionError);
                    // Not sure why this is required, but without it tapping retry hides langLinksError
                    // FIXME: INVESTIGATE WHY THIS HAPPENS!
                    // Also happens in {@link PageFragment}
                    sectionError.setVisibility(View.VISIBLE);
                }
            }.execute();
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
        sectionText.post(new Runnable() {
            @Override
            public void run() {
                sectionContainer.fullScroll(View.FOCUS_DOWN);
                final int scrollDelayMs = 500;
                sectionText.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        setHighlight(highlightText);
                    }
                }, scrollDelayMs);
            }
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
            if (abusefilterEditResult.getType() == AbuseFilterEditResult.TYPE_WARNING) {
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
            alert.setPositiveButton(getString(R.string.yes), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int id) {
                    dialog.dismiss();
                    finish();
                }
            });
            alert.setNegativeButton(getString(R.string.no), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int id) {
                    dialog.dismiss();
                }
            });
            alert.create().show();
        } else {
            finish();
        }
    }

    @Override
    protected void onStop() {
        if (progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
        if (bus != null) {
            bus.unregister(this);
            bus = null;
            Log.d("Wikipedia", "Deregistering bus");
        }
        syntaxHighlighter.cleanup();
        super.onStop();
    }
}
