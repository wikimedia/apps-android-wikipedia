package org.wikipedia.editing;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebView;
import android.widget.*;
import com.github.kevinsawicki.http.HttpRequest;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;
import org.json.JSONException;
import org.json.JSONObject;
import org.mediawiki.api.json.Api;
import org.mediawiki.api.json.RequestBuilder;
import org.wikipedia.*;
import org.wikipedia.login.LoginTask;
import org.wikipedia.login.LogoutTask;
import org.wikipedia.login.User;
import org.wikipedia.page.Section;

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
    private View captchaContainer;
    private View captchaProgress;
    private ImageView captchaImage;
    private EditText captchaText;

    private View abusefilterContainer;
    private WebView abusefilterWebView;
    private CommunicationBridge abusefilterBridge;
    private View abuseFilterBackAction;

    private CaptchaEditResult captchaEditResult;
    private AbuseFilterEditResult abusefilterEditResult;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_section);

        getActionBar().setDisplayHomeAsUpEnabled(true);

        if (!getIntent().getAction().equals(ACTION_EDIT_SECTION)) {
            throw new RuntimeException("Much wrong action. Such exception. Wow");
        }

        app = (WikipediaApp)getApplicationContext();

        title = getIntent().getParcelableExtra(EXTRA_TITLE);
        section = getIntent().getParcelableExtra(EXTRA_SECTION);

        progressDialog = new ProgressDialog(this);

        getActionBar().setTitle(getString(R.string.editsection_activity_title));

        sectionText = (EditText) findViewById(R.id.edit_section_text);
        sectionProgress = findViewById(R.id.edit_section_load_progress);
        sectionContainer = findViewById(R.id.edit_section_container);
        sectionError = findViewById(R.id.edit_section_error);
        sectionErrorRetry = (Button) findViewById(R.id.edit_section_error_retry);

        captchaContainer = findViewById(R.id.edit_section_captcha_container);
        captchaImage = (ImageView) findViewById(R.id.edit_section_captcha_image);
        captchaText = (EditText) findViewById(R.id.edit_section_captcha_text);
        captchaProgress = findViewById(R.id.edit_section_captcha_image_progress);

        abusefilterContainer = findViewById(R.id.edit_section_abusefilter_container);
        abusefilterWebView = (WebView) findViewById(R.id.edit_section_abusefilter_webview);
        abusefilterBridge = new CommunicationBridge(abusefilterWebView, "file:///android_asset/abusefilter.html");
        abuseFilterBackAction = findViewById(R.id.edit_section_abusefilter_back);

        if (savedInstanceState != null && savedInstanceState.containsKey("sectionWikitext")) {
            sectionWikitext = savedInstanceState.getString("sectionWikitext");
        }

        if (savedInstanceState != null && savedInstanceState.containsKey("captcha")) {
            captchaEditResult = savedInstanceState.getParcelable("captcha");
            handleCaptcha();
        }

        if (savedInstanceState != null && savedInstanceState.containsKey("abusefilter")) {
            abusefilterEditResult = savedInstanceState.getParcelable("abusefilter");
            handleAbuseFilter();
        }

        sectionErrorRetry.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Utils.crossFade(sectionError, sectionProgress);
                fetchSectionText();
            }
        });

        captchaImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new RefreshCaptchaTask(EditSectionActivity.this, title) {
                    @Override
                    public void onBeforeExecute() {
                        Utils.crossFade(captchaImage, captchaProgress);
                    }

                    @Override
                    public void onFinish(CaptchaEditResult result) {
                        captchaEditResult = result;
                        handleCaptcha(true);
                    }
                }.execute();

            }
        });

        abuseFilterBackAction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cancelAbuseFilter();
            }
        });

        fetchSectionText();
    }

    private ProgressDialog progressDialog;
    private void doSave() {
        progressDialog.setIndeterminate(true);
        progressDialog.setCancelable(false);
        progressDialog.setMessage(getString(R.string.dialog_saving_in_progress));
        if (captchaEditResult != null) {
            // Show the wikitext in the background when captcha is being saved=
            Utils.crossFade(captchaContainer, sectionContainer);
        }
        app.getEditTokenStorage().get(title.getSite(), new EditTokenStorage.TokenRetreivedCallback() {
            @Override
            public void onTokenRetreived(final String token) {

                new DoEditTask(EditSectionActivity.this, title, sectionText.getText().toString(), section.getId(), token) {
                    @Override
                    public void onBeforeExecute() {
                        progressDialog.show();
                    }

                    @Override
                    public RequestBuilder buildRequest(Api api) {
                        RequestBuilder builder = super.buildRequest(api);
                        if (captchaEditResult != null) {
                            builder.param("captchaid", captchaEditResult.getCaptchaId())
                                    .param("captchaword", captchaText.getText().toString());
                        }
                        return builder;
                    }

                    @Override
                    public void onCatch(Throwable caught) {
                        if (caught instanceof EditingException) {
                            EditingException ee = (EditingException) caught;
                            if (app.getUserInfoStorage().isLoggedIn() && ee.getCode() == "badtoken") {
                                // looks like our session expired.
                                app.getEditTokenStorage().clearAllTokens();
                                app.getCookieManager().clearAllCookies();

                                User user = app.getUserInfoStorage().getUser();
                                new LoginTask(app, app.getPrimarySite(), user.getUsername(), user.getPassword()) {
                                    @Override
                                    public void onFinish(String result) {
                                        doSave();
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
                            progressDialog.hide();
                            setResult(EditHandler.RESULT_REFRESH_PAGE);
                            Toast.makeText(EditSectionActivity.this, R.string.edit_saved_successfully, Toast.LENGTH_LONG).show();
                            finish();
                        } else if (result instanceof CaptchaEditResult) {
                            captchaEditResult = (CaptchaEditResult) result;
                            handleCaptcha();
                        } else if (result instanceof AbuseFilterEditResult) {
                            abusefilterEditResult = (AbuseFilterEditResult) result;
                            handleAbuseFilter();
                        } else {
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
        JSONObject payload = new JSONObject();
        try {
            payload.putOpt("html", abusefilterEditResult.getWarning());
        } catch (JSONException e) {
            // Goddamn Java
            throw new RuntimeException(e);
        }
        abusefilterBridge.sendMessage("displayWarning", payload);
        if (getCurrentFocus() != null) {
            InputMethodManager keyboard = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
            keyboard.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
        }
        Utils.fadeIn(abusefilterContainer);
        progressDialog.dismiss();
    }

    private void handleCaptcha() {
        handleCaptcha(false);
    }

    private void handleCaptcha(final boolean isReload) {
        if (captchaEditResult == null) {
            return;
        }
        Picasso.with(EditSectionActivity.this)
                .load(Uri.parse(captchaEditResult.getCaptchaUrl(title.getSite())))
                // Don't use .fit() here - seems to cause the loading to fail
                // See https://github.com/square/picasso/issues/249
                .into(captchaImage, new Callback() {
                    @Override
                    public void onSuccess() {
                        getActionBar().setTitle(R.string.edit_section_title_captcha);
                        progressDialog.hide();

                        // In case there was a captcha attempt before
                        captchaText.setText("");
                        if (isReload) {
                            Utils.crossFade(captchaProgress, captchaImage);
                        } else {
                            Utils.crossFade(sectionContainer, captchaContainer);
                        }
                    }

                    @Override
                    public void onError() {
                    }
                });
    }

    private void cancelCaptcha() {
        captchaEditResult = null;
        captchaText.setText("");
        getActionBar().setTitle(R.string.editsection_activity_title);
        Utils.crossFade(captchaContainer, sectionContainer);
    }

    private void cancelAbuseFilter() {
        abusefilterEditResult = null;
        getActionBar().setTitle(R.string.editsection_activity_title);
        Utils.crossFade(abusefilterContainer, sectionContainer);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            case R.id.menu_save_section:
                doSave();
                return true;
            default:
                throw new RuntimeException("WAT");
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_edit_section, menu);
        return true;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("sectionWikitext", sectionWikitext);
        outState.putParcelable("captcha", captchaEditResult);
        outState.putParcelable("abusefilter", abusefilterEditResult);
    }

    private void fetchSectionText() {
        if (sectionWikitext == null) {
            new FetchSectionWikitextTask(this, title, section.getId()) {
                @Override
                public void onFinish(String result) {
                    sectionWikitext = result;
                    displaySectionText();
                }

                @Override
                public void onCatch(Throwable caught) {
                    Utils.crossFade(sectionProgress, sectionError);
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
        Utils.crossFade(sectionProgress, sectionContainer);
        invalidateOptionsMenu();
    }

    @Override
    public void onBackPressed() {
        if (captchaEditResult != null) {
            cancelCaptcha();
        } else if (abusefilterEditResult != null) {
            cancelAbuseFilter();
        } else {
            finish();
        }
    }
}