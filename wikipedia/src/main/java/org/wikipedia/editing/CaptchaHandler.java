package org.wikipedia.editing;

import android.app.*;
import android.net.*;
import android.os.*;
import android.support.v7.app.ActionBarActivity;
import android.util.*;
import android.view.*;
import android.widget.*;
import com.squareup.picasso.*;
import org.mediawiki.api.json.*;
import org.wikipedia.*;

public class CaptchaHandler {
    private final Activity activity;

    private View captchaContainer;
    private View captchaProgress;
    private ImageView captchaImage;
    private EditText captchaText;
    private Site site;
    private ProgressDialog progressDialog;
    private View primaryView;

    private int prevTitleId;

    private CaptchaResult captchaResult;

    public CaptchaHandler(final Activity activity, final Site site, final ProgressDialog progressDialog, final View primaryView, final int prevTitleId) {
        this.activity = activity;
        this.site = site;
        this.progressDialog = progressDialog;
        this.primaryView = primaryView;
        this.prevTitleId = prevTitleId;

        captchaContainer = activity.findViewById(R.id.captcha_container);
        captchaImage = (ImageView) activity.findViewById(R.id.captcha_image);
        captchaText = (EditText) activity.findViewById(R.id.captcha_text);
        captchaProgress = activity.findViewById(R.id.captcha_image_progress);

        captchaImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new RefreshCaptchaTask(activity, site) {
                    @Override
                    public void onBeforeExecute() {
                        Utils.crossFade(captchaImage, captchaProgress);
                    }

                    @Override
                    public void onFinish(CaptchaResult result) {
                        captchaResult = result;
                        handleCaptcha(true);
                    }
                }.execute();

            }
        });
    }

    public void restoreState(Bundle savedInstanceState) {
        if (savedInstanceState != null && savedInstanceState.containsKey("captcha")) {
            handleCaptcha((CaptchaResult) savedInstanceState.getParcelable("captcha"));
        }
    }

    public void saveState(Bundle outState) {
        outState.putParcelable("captcha", captchaResult);
    }

    public void handleCaptcha(CaptchaResult captchaResult) {
        this.captchaResult = captchaResult;
        handleCaptcha(false);
    }

    private void handleCaptcha(final boolean isReload) {
        if (captchaResult == null) {
            return;
        }
        Picasso.with(activity)
                .load(Uri.parse(captchaResult.getCaptchaUrl(site)))
                        // Don't use .fit() here - seems to cause the loading to fail
                        // See https://github.com/square/picasso/issues/249
                .into(captchaImage, new Callback() {
                    @Override
                    public void onSuccess() {
                        ((ActionBarActivity)activity).getSupportActionBar().setTitle(R.string.title_captcha);
                        progressDialog.hide();

                        // In case there was a captcha attempt before
                        captchaText.setText("");
                        if (isReload) {
                            Utils.crossFade(captchaProgress, captchaImage);
                        } else {
                            Utils.crossFade(primaryView, captchaContainer);
                        }
                    }

                    @Override
                    public void onError() {
                    }
                });
    }

    public void hideCaptcha() {
        ((ActionBarActivity)activity).getSupportActionBar().setTitle(prevTitleId);
        Utils.crossFade(captchaContainer, primaryView);
    }

    public boolean cancelCaptcha() {
        if (captchaResult == null) {
            return false;
        }
        captchaResult = null;
        captchaText.setText("");
        hideCaptcha();
        return true;
    }

    public RequestBuilder populateBuilder(RequestBuilder builder) {
        if (captchaResult != null) {
            builder.param("captchaid", captchaResult.getCaptchaId())
                    .param("captchaword", captchaText.getText().toString());
        }
        return builder;
    }
}
