package org.wikipedia.editing;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.*;
import com.github.kevinsawicki.http.HttpRequest;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Transformation;
import org.mediawiki.api.json.Api;
import org.mediawiki.api.json.RequestBuilder;
import org.wikipedia.PageTitle;
import org.wikipedia.R;
import org.wikipedia.Utils;
import org.wikipedia.WikipediaApp;
import org.wikipedia.page.Section;

public class EditSectionActivity extends Activity {
    public static final String ACTION_EDIT_SECTION = "org.wikipedia.edit_section";
    public static final String EXTRA_TITLE = "org.wikipedia.edit_section.title";
    public static final String EXTRA_SECTION = "org.wikipedia.edit_section.section";

    private PageTitle title;
    private Section section;

    private String sectionWikitext;

    private EditText sectionText;
    private View sectionProgress;
    private View sectionContainer;
    private View sectionError;
    private Button sectionErrorRetry;
    private View captchaContainer;
    private ImageView captchaImage;
    private EditText captchaText;
    private Button captchaConfirm;

    private CaptchaEditResult captchaEditResult;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_section);

        getActionBar().setDisplayHomeAsUpEnabled(true);

        if (!getIntent().getAction().equals(ACTION_EDIT_SECTION)) {
            throw new RuntimeException("Much wrong action. Such exception. Wow");
        }

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

        if (savedInstanceState != null && savedInstanceState.containsKey("sectionWikitext")) {
            sectionWikitext = savedInstanceState.getString("sectionWikitext");
        }

        if (savedInstanceState != null && savedInstanceState.containsKey("captcha")) {
            captchaEditResult = savedInstanceState.getParcelable("captcha");
            handleCaptcha();
        }

        sectionErrorRetry.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Utils.crossFade(sectionError, sectionProgress);
                fetchSectionText();
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
        new DoEditTask(this, title, sectionText.getText().toString(), section.getId()) {
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
                } else {
                    // Expand to do everything.
                    onCatch(null);
                }

            }
        }.execute();
    }

    private void handleCaptcha() {
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
                        Utils.crossFade(sectionContainer, captchaContainer);
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

    public boolean onMenuItemSelected(int featureId, MenuItem item) {
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
        } else {
            finish();
        }
    }
}