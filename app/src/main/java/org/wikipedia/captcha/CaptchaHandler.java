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
import org.wikipedia.dataclient.ServiceFactory;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.page.LinkMovementMethodExt;
import org.wikipedia.util.DeviceUtil;
import org.wikipedia.util.FeedbackUtil;
import org.wikipedia.util.StringUtil;
import org.wikipedia.views.ViewAnimations;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

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
    private CompositeDisposable disposables = new CompositeDisposable();

    @Nullable private String token;
    @Nullable private CaptchaResult captchaResult;

    public CaptchaHandler(final Activity activity, final WikiSite wiki, final ProgressDialog progressDialog,
                          final View primaryView, final String prevTitle, final String submitButtonText) {
        this.activity = activity;
        this.wiki = wiki;
        this.progressDialog = progressDialog;
        this.primaryView = primaryView;
        this.prevTitle = prevTitle;

        TextView requestAccountText = activity.findViewById(R.id.request_account_text);
        captchaContainer = activity.findViewById(R.id.captcha_container);
        captchaImage = activity.findViewById(R.id.captcha_image);
        captchaText = ((TextInputLayout) activity.findViewById(R.id.captcha_text)).getEditText();
        captchaProgress = activity.findViewById(R.id.captcha_image_progress);
        TextView submitButton = activity.findViewById(R.id.captcha_submit_button);

        if (submitButtonText != null) {
            submitButton.setText(submitButtonText);
            submitButton.setVisibility(View.VISIBLE);
        }

        requestAccountText.setText(StringUtil.fromHtml(activity.getString(R.string.edit_section_captcha_request_an_account_message)));
        requestAccountText.setMovementMethod(new LinkMovementMethodExt((url) -> FeedbackUtil.showAndroidAppRequestAnAccount(activity)));
        captchaImage.setOnClickListener((v) -> {
            captchaProgress.setVisibility(View.VISIBLE);

            disposables.add(ServiceFactory.get(wiki).getNewCaptcha()
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .doFinally(() -> captchaProgress.setVisibility(View.GONE))
                    .subscribe(response -> {
                        captchaResult = new CaptchaResult(response.captchaId());
                        handleCaptcha(true);
                    }, caught -> {
                        cancelCaptcha();
                        FeedbackUtil.showError(activity, caught);
                    }));
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

    public void dispose() {
        disposables.clear();
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
        DeviceUtil.hideSoftKeyboard(activity);
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

    public void cancelCaptcha() {
        if (captchaResult == null) {
            return;
        }
        captchaResult = null;
        captchaText.setText("");
        hideCaptcha();
    }
}
