package org.wikipedia.captcha;

import android.app.Activity;
import android.app.ProgressDialog;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.drawable.Animatable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.controller.BaseControllerListener;
import com.facebook.drawee.view.SimpleDraweeView;
import com.facebook.imagepipeline.image.ImageInfo;

import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.util.FeedbackUtil;
import org.wikipedia.views.ViewAnimations;

import retrofit2.Call;

public class CaptchaHandler {
    private final Activity activity;
    private final View captchaContainer;
    private final View captchaProgress;
    private final SimpleDraweeView captchaImage;
    private final EditText captchaText;
    private final WikiSite wiki;
    private final View primaryView;
    private final String prevTitle;
    private ProgressDialog progressDialog;

    @Nullable private String token;
    @Nullable private CaptchaResult captchaResult;

    public CaptchaHandler(final Activity activity, final WikiSite wiki, final ProgressDialog progressDialog,
                          final View primaryView, final String prevTitle, final String submitButtonText) {
        this.activity = activity;
        this.wiki = wiki;
        this.progressDialog = progressDialog;
        this.primaryView = primaryView;
        this.prevTitle = prevTitle;

        captchaContainer = activity.findViewById(R.id.captcha_container);
        captchaImage = activity.findViewById(R.id.captcha_image);
        captchaText = ((TextInputLayout) activity.findViewById(R.id.captcha_text)).getEditText();
        captchaProgress = activity.findViewById(R.id.captcha_image_progress);
        TextView submitButton = activity.findViewById(R.id.captcha_submit_button);

        if (submitButtonText != null) {
            submitButton.setText(submitButtonText);
            submitButton.setVisibility(View.VISIBLE);
        }

        captchaImage.setOnClickListener((v) -> {
            captchaProgress.setVisibility(View.VISIBLE);

            new CaptchaClient().request(wiki, new CaptchaClient.Callback() {
                @Override
                public void success(@NonNull Call<Captcha> call, @NonNull CaptchaResult result) {
                    captchaResult = result;
                    captchaProgress.setVisibility(View.GONE);
                    handleCaptcha(true);
                }

                @Override
                public void failure(@NonNull Call<Captcha> call, @NonNull Throwable caught) {
                    cancelCaptcha();
                    captchaProgress.setVisibility(View.GONE);
                    FeedbackUtil.showError(activity, caught);
                }
            });
        });
    }

    @Nullable
    public String token() {
        return token;
    }

    @Nullable
    public String captchaId() {
        if (captchaResult != null) {
            return captchaResult.getCaptchaId();
        }
        return null;
    }

    @Nullable
    public String captchaWord() {
        if (captchaText != null) {
            return captchaText.getText().toString();
        }
        return null;
    }

    public void restoreState(Bundle savedInstanceState) {
        if (savedInstanceState != null
                && savedInstanceState.containsKey("token")
                && savedInstanceState.containsKey("captcha")) {
            handleCaptcha(savedInstanceState.getString("token"), savedInstanceState.getParcelable("captcha"));
        }
    }

    public void saveState(Bundle outState) {
        outState.putString("token", token);
        outState.putParcelable("captcha", captchaResult);
    }

    public boolean isActive() {
        return captchaResult != null;
    }

    public void handleCaptcha(@Nullable String token, @NonNull CaptchaResult captchaResult) {
        this.token = token;
        this.captchaResult = captchaResult;
        handleCaptcha(false);
    }

    private void handleCaptcha(boolean isReload) {
        if (captchaResult == null) {
            return;
        }
        if (!isReload) {
            ViewAnimations.crossFade(primaryView, captchaContainer);
        }
        captchaImage.setController(Fresco.newDraweeControllerBuilder()
                .setUri(captchaResult.getCaptchaUrl(wiki))
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
                        if (WikipediaApp.getInstance().getCurrentTheme().isDark()) {
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
                    }
                })
                .build());
    }

    public void hideCaptcha() {
        ((AppCompatActivity) activity).getSupportActionBar().setTitle(prevTitle);
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
}
