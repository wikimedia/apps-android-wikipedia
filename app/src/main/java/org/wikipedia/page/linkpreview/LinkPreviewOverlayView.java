package org.wikipedia.page.linkpreview;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.Button;
import android.widget.FrameLayout;

import androidx.annotation.Nullable;

import org.wikipedia.databinding.ViewLinkPreviewOverlayBinding;

public class LinkPreviewOverlayView extends FrameLayout {
    public interface Callback {
        void onPrimaryClick();
        void onSecondaryClick();
        void onTertiaryClick();
    }

    private Button primaryButton;
    private Button secondaryButton;
    private Button tertiaryButton;

    @Nullable private Callback callback;

    public LinkPreviewOverlayView(Context context) {
        super(context);
        init();
    }

    public LinkPreviewOverlayView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public LinkPreviewOverlayView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public void setCallback(@Nullable Callback callback) {
        this.callback = callback;
    }

    public void setPrimaryButtonText(@Nullable CharSequence text) {
        primaryButton.setText(text);
    }

    public void showSecondaryButton(boolean show) {
        secondaryButton.setVisibility(show ? VISIBLE : GONE);
    }

    public void setSecondaryButtonText(@Nullable CharSequence text) {
        secondaryButton.setText(text);
    }

    public void showTertiaryButton(boolean show) {
        tertiaryButton.setVisibility(show ? VISIBLE : GONE);
    }

    private void init() {
        final ViewLinkPreviewOverlayBinding binding =
                ViewLinkPreviewOverlayBinding.inflate(LayoutInflater.from(getContext()));
        primaryButton = binding.linkPreviewPrimaryButton;
        secondaryButton = binding.linkPreviewSecondaryButton;
        tertiaryButton = binding.linkPreviewTertiaryButton;

        primaryButton.setOnClickListener(v -> {
            if (callback != null) {
                callback.onPrimaryClick();
            }
        });
        secondaryButton.setOnClickListener(v -> {
            if (callback != null) {
                callback.onSecondaryClick();
            }
        });
        tertiaryButton.setOnClickListener(v -> {
            if (callback != null) {
                callback.onTertiaryClick();
            }
        });
    }
}
