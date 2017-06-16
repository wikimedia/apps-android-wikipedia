package org.wikipedia.onboarding;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.AttrRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StyleRes;
import android.support.v7.widget.SwitchCompat;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.wikipedia.R;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnCheckedChanged;
import butterknife.OnClick;

public class OnboardingPageView extends LinearLayout {
    public interface Callback {
        void onButtonClick(@NonNull OnboardingPageView view);
        void onSkipClick(@NonNull OnboardingPageView view);
        void onSwitchChange(@NonNull OnboardingPageView view, boolean checked);
    }

    public static class DefaultCallback implements Callback {
        @Override
        public void onButtonClick(@NonNull OnboardingPageView view) { }

        @Override
        public void onSkipClick(@NonNull OnboardingPageView view) { }

        @Override
        public void onSwitchChange(@NonNull OnboardingPageView view, boolean checked) { }
    }

    @BindView(R.id.view_onboarding_page_image_protrude) ImageView imageViewProtrude;
    @BindView(R.id.view_onboarding_page_image_centered) ImageView imageViewCentered;
    @BindView(R.id.view_onboarding_page_primary_text) TextView primaryTextView;
    @BindView(R.id.view_onboarding_page_secondary_text) TextView secondaryTextView;
    @BindView(R.id.view_onboarding_page_tertiary_text) TextView tertiaryTextView;
    @BindView(R.id.view_onboarding_page_switch_container) View switchContainer;
    @BindView(R.id.view_onboarding_page_switch) SwitchCompat switchView;
    @BindView(R.id.view_onboarding_page_button) TextView button;

    @Nullable private Callback callback;

    public OnboardingPageView(Context context) {
        super(context);
        init(null, 0, 0);
    }

    public OnboardingPageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs, 0, 0);
    }

    public OnboardingPageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs, defStyleAttr, 0);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public OnboardingPageView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(attrs, defStyleAttr, defStyleRes);
    }

    public void setCallback(@Nullable Callback callback) {
        this.callback = callback;
    }

    @OnClick(R.id.view_onboarding_page_button) public void onButtonClick() {
        if (callback != null) {
            callback.onButtonClick(this);
        }
    }

    @OnClick(R.id.view_onboarding_page_skip_button) void onSkipClick() {
        if (callback != null) {
            callback.onSkipClick(this);
        }
    }

    @OnCheckedChanged(R.id.view_onboarding_page_switch) void onSwitchChange(boolean checked) {
        if (callback != null) {
            callback.onSwitchChange(this, checked);
        }
    }

    private void init(@Nullable AttributeSet attrs, @AttrRes int defStyleAttr, @StyleRes int defStyleRes) {
        setOrientation(getResources().getConfiguration().orientation
                == Configuration.ORIENTATION_PORTRAIT ? VERTICAL : HORIZONTAL);
        inflate(getContext(), R.layout.view_onboarding_page, this);
        ButterKnife.bind(this);
        if (attrs != null) {
            TypedArray array = getContext().obtainStyledAttributes(attrs,
                    R.styleable.OnboardingPageView, defStyleAttr, defStyleRes);
            Drawable protrudeImage = array.getDrawable(R.styleable.OnboardingPageView_protrudeImage);
            Drawable centeredImage = array.getDrawable(R.styleable.OnboardingPageView_centeredImage);
            String primaryText = array.getString(R.styleable.OnboardingPageView_primaryText);
            String secondaryText = array.getString(R.styleable.OnboardingPageView_secondaryText);
            String tertiaryText = array.getString(R.styleable.OnboardingPageView_tertiaryText);
            String buttonText = array.getString(R.styleable.OnboardingPageView_buttonText);
            String switchText = array.getString(R.styleable.OnboardingPageView_switchText);

            imageViewProtrude.setImageDrawable(protrudeImage);
            imageViewCentered.setImageDrawable(centeredImage);
            primaryTextView.setText(primaryText);
            secondaryTextView.setText(secondaryText);
            tertiaryTextView.setText(tertiaryText);
            button.setText(buttonText);

            switchContainer.setVisibility(TextUtils.isEmpty(switchText) ? GONE : VISIBLE);
            switchView.setText(switchText);

            array.recycle();
        }
    }
}
