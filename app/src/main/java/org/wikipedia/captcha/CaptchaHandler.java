package org.wikipedia.captcha;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputLayout;

import org.wikipedia.R;
import org.wikipedia.dataclient.ServiceFactory;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.page.LinkMovementMethodExt;
import org.wikipedia.util.DeviceUtil;
import org.wikipedia.util.FeedbackUtil;
import org.wikipedia.util.StringUtil;
import org.wikipedia.views.ViewAnimations;
import org.wikipedia.views.ViewUtil;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class CaptchaHandler {
    private final Activity activity;
    private final View captchaContainer;
    private final View captchaProgress;
    private final ImageView captchaImage;
    private final EditText captchaText;
    private final WikiSite wiki;
    private final View primaryView;
    private final String prevTitle;
    private CompositeDisposable disposables = new CompositeDisposable();

    @Nullable private String token;
    @Nullable private CaptchaResult captchaResult;

    public CaptchaHandler(final Activity activity, final WikiSite wiki, final View primaryView,
                          final String prevTitle, final String submitButtonText) {
        this.activity = activity;
        this.wiki = wiki;
        this.primaryView = primaryView;
        this.prevTitle = prevTitle;

        TextView requestAccountText = activity.findViewById(R.id.request_account_text);
        captchaContainer = activity.findViewById(R.id.captcha_container);
        captchaImage = activity.findViewById(R.id.captcha_image);
        captchaText = ((TextInputLayout) activity.findViewById(R.id.captcha_text)).getEditText();
        captchaProgress = activity.findViewById(R.id.captcha_image_progress);
        Button submitButton = activity.findViewById(R.id.captcha_submit_button);

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
                    .doAfterTerminate(() -> captchaProgress.setVisibility(View.GONE))
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
        // In case there was a captcha attempt before
        captchaText.setText("");

        ViewUtil.loadImage(captchaImage, captchaResult.getCaptchaUrl(wiki), false, false, true);
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
