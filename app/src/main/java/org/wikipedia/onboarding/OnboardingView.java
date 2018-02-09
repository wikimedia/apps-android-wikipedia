package org.wikipedia.onboarding;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.wikipedia.R;
import org.wikipedia.page.LinkMovementMethodExt;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class OnboardingView extends LinearLayout {
    public interface Callback {
        void onPositiveAction();
        void onNegativeAction();
    }

    @BindView(R.id.view_onboarding_title) TextView titleView;
    @BindView(R.id.view_onboarding_text) TextView textView;
    @BindView(R.id.view_onboarding_action_positive) TextView actionViewPositive;
    @BindView(R.id.view_onboarding_action_negative) TextView actionViewNegative;

    @Nullable private Callback callback;

    public OnboardingView(@NonNull Context context) {
        super(context);
        setOrientation(VERTICAL);
        inflate(context, R.layout.view_onboarding, this);
        ButterKnife.bind(this);
        textView.setMovementMethod(LinkMovementMethodExt.getInstance());
    }

    public void setCallback(@Nullable Callback callback) {
        this.callback = callback;
    }

    public void setTitle(@StringRes int id) {
        titleView.setText(id);
    }

    public void setText(@StringRes int id) {
        textView.setText(id);
    }

    public void setText(@NonNull CharSequence text) {
        textView.setText(text);
    }

    public void setPositiveAction(@StringRes int id) {
        actionViewPositive.setText(id);
    }

    public void setNegativeAction(@StringRes int id) {
        actionViewNegative.setText(id);
    }

    @OnClick(R.id.view_onboarding_action_positive)
    void onPositiveActionClick() {
        if (callback != null) {
            callback.onPositiveAction();
        }
    }

    @OnClick(R.id.view_onboarding_action_negative)
    void onNegativeActionClick() {
        if (callback != null) {
            callback.onNegativeAction();
        }
    }
}
