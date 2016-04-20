package org.wikipedia.editing;

import android.app.Activity;
import android.app.ProgressDialog;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.drawable.Animatable;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.controller.BaseControllerListener;
import com.facebook.drawee.view.SimpleDraweeView;
import com.facebook.imagepipeline.image.ImageInfo;
import org.mediawiki.api.json.RequestBuilder;
import org.wikipedia.R;
import org.wikipedia.Site;
import org.wikipedia.ViewAnimations;
import org.wikipedia.WikipediaApp;
import org.wikipedia.util.FeedbackUtil;

public class CaptchaHandler {
    private final Activity activity;
    private final View captchaContainer;
    private final View captchaProgress;
    private final SimpleDraweeView captchaImage;
    private final EditText captchaText;
    private final Site site;
    private final View primaryView;
    private final String prevTitle;
    private ProgressDialog progressDialog;
    private CaptchaResult captchaResult;
    private boolean authManagerRequest;

    public CaptchaHandler(final Activity activity, final Site site, final ProgressDialog progressDialog,
                          final View primaryView, final String prevTitle, final String submitButtonText) {
        this.activity = activity;
        this.site = site;
        this.progressDialog = progressDialog;
        this.primaryView = primaryView;
        this.prevTitle = prevTitle;

        captchaContainer = activity.findViewById(R.id.captcha_container);
        captchaImage = (SimpleDraweeView) activity.findViewById(R.id.captcha_image);
        captchaText = (EditText) activity.findViewById(R.id.captcha_text);
        captchaProgress = activity.findViewById(R.id.captcha_image_progress);
        Button submitButton = (Button) activity.findViewById(R.id.captcha_submit_button);

        if (submitButtonText != null) {
            submitButton.setText(submitButtonText);
            submitButton.setVisibility(View.VISIBLE);
        }

        captchaImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new RefreshCaptchaTask(activity, site) {
                    @Override
                    public void onBeforeExecute() {
                        captchaProgress.setVisibility(View.VISIBLE);
                    }

                    @Override
                    public void onFinish(CaptchaResult result) {
                        captchaResult = result;
                        captchaProgress.setVisibility(View.GONE);
                        handleCaptcha(true);
                    }

                    @Override
                    public void onCatch(Throwable caught) {
                        cancelCaptcha();
                        captchaProgress.setVisibility(View.GONE);
                        FeedbackUtil.showError(activity, caught);
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

    public boolean isAuthManagerRequest() {
        return authManagerRequest;
    }

    public boolean isActive() {
        return captchaResult != null;
    }

    public void handleCaptcha(CaptchaResult captchaResult) {
        this.captchaResult = captchaResult;
        handleCaptcha(false);
    }

    public void handleCaptcha(CaptchaResult captchaResult, boolean authManagerRequest) {
        this.captchaResult = captchaResult;
        this.authManagerRequest = authManagerRequest;
        handleCaptcha(false);
    }

    private void handleCaptcha(final boolean isReload) {
        if (captchaResult == null) {
            return;
        }
        captchaImage.setController(Fresco.newDraweeControllerBuilder()
                .setUri(captchaResult.getCaptchaUrl(site))
                .setAutoPlayAnimations(true)
                .setControllerListener(new BaseControllerListener<ImageInfo>() {
                    @Override
                    public void onFinalImageSet(String id, ImageInfo imageInfo, Animatable animatable) {
                        ((AppCompatActivity)activity).getSupportActionBar().setTitle(R.string.title_captcha);
                        if (progressDialog.isShowing()) {
                            progressDialog.hide();
                        }

                        // for our Dark theme, show a "negative image" of the captcha!
                        final int maxColorVal = 255;
                        if (WikipediaApp.getInstance().isCurrentThemeDark()) {
                            float[] colorMatrixNegative = {
                                    -1.0f, 0, 0, 0, maxColorVal, //red
                                    0, -1.0f, 0, 0, maxColorVal, //green
                                    0, 0, -1.0f, 0, maxColorVal, //blue
                                    0, 0, 0, 1.0f, 0 //alpha
                            };
                            captchaImage.getDrawable().setColorFilter(new ColorMatrixColorFilter(colorMatrixNegative));
                        } else {
                            captchaImage.getDrawable().clearColorFilter();
                        }

                        // In case there was a captcha attempt before
                        captchaText.setText("");
                        if (!isReload) {
                            ViewAnimations.crossFade(primaryView, captchaContainer);
                        }
                    }
                })
                .build());
    }

    public void hideCaptcha() {
        ((AppCompatActivity)activity).getSupportActionBar().setTitle(prevTitle);
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
        if (captchaResult == null) {
            return builder;
        }

        if (authManagerRequest) {
            authManagerRequest = false;
            return builder.param("captchaId", captchaResult.getCaptchaId())
                    .param("captchaWord", captchaText.getText().toString());
        }

        return builder.param("captchaid", captchaResult.getCaptchaId())
                .param("captchaword", captchaText.getText().toString());
    }

}
