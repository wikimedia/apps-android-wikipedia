package org.wikipedia.editing;

import android.app.*;
import android.content.*;
import android.os.*;
import android.support.v7.app.*;
import android.util.*;
import android.view.*;
import android.webkit.*;
import android.widget.*;
import com.github.kevinsawicki.http.*;
import de.keyboardsurfer.android.widget.crouton.*;
import org.json.*;
import org.mediawiki.api.json.*;
import org.wikipedia.*;
import org.wikipedia.Utils;
import org.wikipedia.analytics.*;
import org.wikipedia.bridge.*;
import org.wikipedia.editing.summaries.*;
import org.wikipedia.login.*;
import org.wikipedia.page.*;

import java.util.*;

public class EditSectionActivity extends ActionBarActivity {
    public static final String ACTION_EDIT_SECTION = "org.wikipedia.edit_section";
    public static final String EXTRA_TITLE = "org.wikipedia.edit_section.title";
    public static final String EXTRA_SECTION = "org.wikipedia.edit_section.section";

    private WikipediaApp app;

    private PageTitle title;
    private Section section;

    private String sectionWikitext;

    private EditText sectionText;
    private View sectionProgress;
    private View sectionContainer;
    private View sectionError;
    private Button sectionErrorRetry;

    private View abusefilterContainer;
    private WebView abusefilterWebView;
    private CommunicationBridge abusefilterBridge;
    private View abuseFilterBackAction;

    private AbuseFilterEditResult abusefilterEditResult;

    private CaptchaHandler captchaHandler;
    private EditSummaryHandler editSummaryHandler;

    private EditPreviewFragment editPreviewFragment;

    private View editSaveOptionsContainer;
    private View editSaveOptionAnon;
    private View editSaveOptionLogIn;

    private EditFunnel funnel;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_section);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        if (!getIntent().getAction().equals(ACTION_EDIT_SECTION)) {
            throw new RuntimeException("Much wrong action. Such exception. Wow");
        }

        app = (WikipediaApp)getApplicationContext();

        title = getIntent().getParcelableExtra(EXTRA_TITLE);
        section = getIntent().getParcelableExtra(EXTRA_SECTION);

        progressDialog = new ProgressDialog(this);

        getSupportActionBar().setTitle(getString(R.string.editsection_activity_title));

        sectionText = (EditText) findViewById(R.id.edit_section_text);
        sectionProgress = findViewById(R.id.edit_section_load_progress);
        sectionContainer = findViewById(R.id.edit_section_container);
        sectionError = findViewById(R.id.edit_section_error);
        sectionErrorRetry = (Button) findViewById(R.id.edit_section_error_retry);

        abusefilterContainer = findViewById(R.id.edit_section_abusefilter_container);
        abusefilterWebView = (WebView) findViewById(R.id.edit_section_abusefilter_webview);
        abusefilterBridge = new CommunicationBridge(abusefilterWebView, "file:///android_asset/abusefilter.html");
        Utils.setupDirectionality(title.getSite().getLanguage(), Locale.getDefault().getLanguage(), abusefilterBridge);
        abusefilterBridge.injectStyleBundle(new PackagedStyleBundle("abusefilter.css"));
        abuseFilterBackAction = findViewById(R.id.edit_section_abusefilter_back);

        captchaHandler = new CaptchaHandler(this, title.getSite(), progressDialog, sectionContainer, R.string.edit_section_activity_title);
        editSummaryHandler = new EditSummaryHandler(this, title);
        editPreviewFragment = (EditPreviewFragment) getSupportFragmentManager().findFragmentById(R.id.edit_section_preview_fragment);

        editSaveOptionsContainer = findViewById(R.id.edit_section_save_options_container);
        editSaveOptionLogIn = findViewById(R.id.edit_section_save_option_login);
        editSaveOptionAnon = findViewById(R.id.edit_section_save_option_anon);

        editPreviewFragment.setEditSummaryHandler(editSummaryHandler);

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

        abuseFilterBackAction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cancelAbuseFilter();
            }
        });

        editSaveOptionAnon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                wasSaveOptionsUsed = true;
                ViewAnimations.fadeOut(editSaveOptionsContainer);
                funnel.logSaveAnonExplicit();
                doSave();
            }
        });

        editSaveOptionLogIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                wasSaveOptionsUsed = true;
                funnel.logLoginAttempt();
                Intent loginIntent = new Intent(EditSectionActivity.this, LoginActivity.class);
                loginIntent.putExtra(LoginActivity.LOGIN_REQUEST_SOURCE, LoginFunnel.SOURCE_EDIT);
                loginIntent.putExtra(LoginActivity.EDIT_SESSION_TOKEN, funnel.getEditSessionToken());
                startActivityForResult(loginIntent, LoginActivity.REQUEST_LOGIN);
            }
        });

        Utils.setTextDirection(sectionText, title.getSite().getLanguage());

        fetchSectionText();

        funnel = app.getFunnelManager().getEditFunnel(title);

        funnel.logStart();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == LoginActivity.REQUEST_LOGIN) {
            if (resultCode == LoginActivity.RESULT_LOGIN_SUCCESS) {
                ViewAnimations.fadeOut(editSaveOptionsContainer);
                doSave();
                funnel.logLoginSuccess();
            } else {
                funnel.logLoginFailure();
            }
        }
    }

    private ProgressDialog progressDialog;
    private void doSave() {
        progressDialog.setIndeterminate(true);
        progressDialog.setCancelable(false);
        progressDialog.setMessage(getString(R.string.dialog_saving_in_progress));
        captchaHandler.hideCaptcha();
        app.getEditTokenStorage().get(title.getSite(), new EditTokenStorage.TokenRetreivedCallback() {
            @Override
            public void onTokenRetreived(final String token) {
                new DoEditTask(EditSectionActivity.this, title, sectionText.getText().toString(), section.getId(), token, editSummaryHandler.getSummary(section.getHeading())) {
                    @Override
                    public void onBeforeExecute() {
                        progressDialog.show();
                    }

                    @Override
                    public RequestBuilder buildRequest(Api api) {
                        return captchaHandler.populateBuilder(super.buildRequest(api));
                    }

                    @Override
                    public void onCatch(Throwable caught) {
                        if (caught instanceof EditingException) {
                            EditingException ee = (EditingException) caught;
                            if (app.getUserInfoStorage().isLoggedIn() && ee.getCode().equals("badtoken")) {
                                // looks like our session expired.
                                app.getEditTokenStorage().clearAllTokens();
                                app.getCookieManager().clearAllCookies();

                                User user = app.getUserInfoStorage().getUser();
                                new LoginTask(app, app.getPrimarySite(), user.getUsername(), user.getPassword()) {
                                    @Override
                                    public void onFinish(LoginResult result) {
                                        if (result.getCode().equals("Success")) {
                                            doSave();
                                        } else {
                                            progressDialog.dismiss();
                                            ViewAnimations.crossFade(sectionText, sectionError);
                                            sectionError.setVisibility(View.VISIBLE);
                                        }
                                    }
                                }.execute();
                                return;
                            }
                        }
                        if (!(caught instanceof HttpRequest.HttpRequestException)) {
                            throw new RuntimeException(caught);
                        }
                        Log.d("Wikipedia", caught.toString());
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

                    @Override
                    public void onFinish(EditingResult result) {
                        if (result instanceof SuccessEditResult) {
                            funnel.logSaved(((SuccessEditResult) result).getRevID());
                            progressDialog.dismiss();
                            setResult(EditHandler.RESULT_REFRESH_PAGE);
                            Toast.makeText(EditSectionActivity.this, R.string.edit_saved_successfully, Toast.LENGTH_LONG).show();
                            Utils.hideSoftKeyboard(EditSectionActivity.this);
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
                        } else if (result instanceof SpamBlacklistEditResult) {
                            Crouton.makeText(
                                    EditSectionActivity.this,
                                    getString(R.string.editing_error_spamblacklist, ((SpamBlacklistEditResult) result).getDomain()),
                                    Style.ALERT
                                    ).show();
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
        });
    }

    private void handleAbuseFilter() {
        if (abusefilterEditResult == null) {
            return;
        }
        if (abusefilterEditResult.getType() == AbuseFilterEditResult.TYPE_ERROR) {
            funnel.logAbuseFilterError(abusefilterEditResult.getCode());
        } else {
            funnel.logAbuseFilterWarning(abusefilterEditResult.getCode());
        }
        JSONObject payload = new JSONObject();
        try {
            payload.putOpt("html", abusefilterEditResult.getWarning());
        } catch (JSONException e) {
            // Goddamn Java
            throw new RuntimeException(e);
        }
        abusefilterBridge.sendMessage("displayWarning", payload);
        Utils.hideSoftKeyboard(this);
        ViewAnimations.fadeIn(abusefilterContainer);
        progressDialog.dismiss();
    }


    private void cancelAbuseFilter() {
        abusefilterEditResult = null;
        getSupportActionBar().setTitle(R.string.editsection_activity_title);
        ViewAnimations.crossFade(abusefilterContainer, sectionContainer);
    }

    /**
     * Set to true if the Save Options were ever used - if any one was tapped on.
     * If they were, we do not show it again.
     */
    private boolean wasSaveOptionsUsed = false;
    private void showSaveOptions() {
        if (editSaveOptionsContainer.getVisibility() == View.VISIBLE
                || wasSaveOptionsUsed) {
            doSave();
        } else {
            ViewAnimations.fadeIn(editSaveOptionsContainer);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                Utils.hideSoftKeyboard(this);
                finish();
                return true;
            case R.id.menu_save_section:
                if (editPreviewFragment.isActive()) {
                    if (app.getUserInfoStorage().isLoggedIn()) {
                        editPreviewFragment.hide();
                        doSave();
                    } else {
                        showSaveOptions();
                    }
                    editSummaryHandler.persistSummary();
                } else {
                    Utils.hideSoftKeyboard(this);
                    editPreviewFragment.showPreview(title, sectionText.getText().toString());
                    funnel.logPreview();
                }
                return true;
            default:
                throw new RuntimeException("WAT");
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_edit_section, menu);
        menu.findItem(R.id.menu_save_section).setEnabled(sectionWikitext != null);
        return true;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("sectionWikitext", sectionWikitext);
        outState.putParcelable("abusefilter", abusefilterEditResult);
        captchaHandler.saveState(outState);
    }

    private void fetchSectionText() {
        if (sectionWikitext == null) {
            new FetchSectionWikitextTask(this, title, section.getId()) {
                @Override
                public void onFinish(String result) {
                    sectionWikitext = result;
                    supportInvalidateOptionsMenu();
                    displaySectionText();
                }

                @Override
                public void onCatch(Throwable caught) {
                    ViewAnimations.crossFade(sectionProgress, sectionError);
                    // Not sure why this is required, but without it tapping retry hides langLinksError
                    // FIXME: INVESTIGATE WHY THIS HAPPENS!
                    // Also happens in {@link PageViewFragment}
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
    }

    @Override
    public void onBackPressed() {
        if (editSaveOptionsContainer.getVisibility() == View.VISIBLE) {
            ViewAnimations.fadeOut(editSaveOptionsContainer);
            return;
        }
        if (!(editPreviewFragment.handleBackPressed())) {
            if (!captchaHandler.cancelCaptcha() && abusefilterEditResult != null) {
                cancelAbuseFilter();
            } else {
                Utils.hideSoftKeyboard(this);
                finish();
            }
        }
    }
}