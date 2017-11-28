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
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.SwitchCompat;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.wikipedia.R;
import org.wikipedia.page.LinkMovementMethodExt;
import org.wikipedia.util.StringUtil;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnCheckedChanged;

public class OnboardingPageView extends LinearLayout {
    public interface Callback {
        void onSwitchChange(@NonNull OnboardingPageView view, boolean checked);
        void onLinkClick(@NonNull OnboardingPageView view, @NonNull String url);
    }

    public static class DefaultCallback implements Callback {
        @Override
        public void onSwitchChange(@NonNull OnboardingPageView view, boolean checked) { }

        @Override
        public void onLinkClick(@NonNull OnboardingPageView view, @NonNull String url) { }
    }

    @BindView(R.id.view_onboarding_page_image_centered) ImageView imageViewCentered;
    @BindView(R.id.view_onboarding_page_primary_text) TextView primaryTextView;
    @BindView(R.id.view_onboarding_page_secondary_text) TextView secondaryTextView;
    @BindView(R.id.view_onboarding_page_tertiary_text) TextView tertiaryTextView;
    @BindView(R.id.view_onboarding_page_switch_container) View switchContainer;
    @BindView(R.id.view_onboarding_page_switch) SwitchCompat switchView;

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

    public void setSwitchChecked(boolean checked) {
        switchView.setChecked(checked);
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
            Drawable centeredImage = ContextCompat.getDrawable(getContext(),
                    array.getResourceId(R.styleable.OnboardingPageView_centeredImage, -1));
            String primaryText = array.getString(R.styleable.OnboardingPageView_primaryText);
            String secondaryText = array.getString(R.styleable.OnboardingPageView_secondaryText);
            String tertiaryText = array.getString(R.styleable.OnboardingPageView_tertiaryText);
            String switchText = array.getString(R.styleable.OnboardingPageView_switchText);
            int imageSize = array.getDimensionPixelSize(R.styleable.OnboardingPageView_imageSize, 0);
            Drawable background = array.getDrawable(R.styleable.OnboardingPageView_background);

            if (background != null) {
                setBackground(background);
            }
            FrameLayout.LayoutParams imageParams = (FrameLayout.LayoutParams) imageViewCentered.getLayoutParams();
            imageParams.width = imageSize;
            imageParams.height = imageSize;
            imageViewCentered.setLayoutParams(imageParams);
            imageViewCentered.setImageDrawable(centeredImage);
            primaryTextView.setText(primaryText);
            secondaryTextView.setText(StringUtil.fromHtml(secondaryText));
            tertiaryTextView.setText(tertiaryText);

            switchContainer.setVisibility(TextUtils.isEmpty(switchText) ? GONE : VISIBLE);
            switchView.setText(switchText);

            secondaryTextView.setMovementMethod(new LinkMovementMethodExt(
                    (@NonNull String url, @Nullable String notUsed) -> {
                            if (callback != null) {
                                callback.onLinkClick(OnboardingPageView.this, url);
                            }
                    }));

            array.recycle();
        }
    }
}
