package org.wikipedia.onboarding;

import android.content.Context;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import org.wikipedia.databinding.ViewOnboardingBinding;
import org.wikipedia.page.LinkMovementMethodExt;

public class OnboardingView extends LinearLayout {
    public interface Callback {
        void onPositiveAction();
        void onNegativeAction();
    }

    private TextView titleView;
    private TextView textView;
    private Button actionViewPositive;
    private Button actionViewNegative;

    @Nullable private Callback callback;

    public OnboardingView(@NonNull Context context) {
        super(context);
        setOrientation(VERTICAL);

        final ViewOnboardingBinding binding = ViewOnboardingBinding.bind(this);
        titleView = binding.viewOnboardingTitle;
        textView = binding.viewOnboardingText;
        actionViewPositive = binding.viewOnboardingActionPositive;
        actionViewNegative = binding.viewOnboardingActionNegative;

        actionViewPositive.setOnClickListener(v -> {
            if (callback != null) {
                callback.onPositiveAction();
            }
        });
        actionViewNegative.setOnClickListener(v -> {
            if (callback != null) {
                callback.onNegativeAction();
            }
        });

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
}
