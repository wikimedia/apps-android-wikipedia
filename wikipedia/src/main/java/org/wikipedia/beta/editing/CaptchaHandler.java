package org.wikipedia.beta.editing;

import android.app.Activity;
import android.app.ProgressDialog;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;
import org.mediawiki.api.json.RequestBuilder;
import org.wikipedia.beta.R;
import org.wikipedia.beta.Site;
import org.wikipedia.beta.ViewAnimations;

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
                        ViewAnimations.crossFade(captchaImage, captchaProgress);
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

    public boolean isActive() {
        return captchaResult != null;
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
                            ViewAnimations.crossFade(captchaProgress, captchaImage);
                        } else {
                            ViewAnimations.crossFade(primaryView, captchaContainer);
                        }
                    }

                    @Override
                    public void onError() {
                    }
                });
    }

    public void hideCaptcha() {
        ((ActionBarActivity)activity).getSupportActionBar().setTitle(prevTitleId);
        ViewAnimations.crossFade(captchaContainer, primaryView);
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
