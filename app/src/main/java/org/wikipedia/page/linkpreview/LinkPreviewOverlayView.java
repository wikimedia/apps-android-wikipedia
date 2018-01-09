package org.wikipedia.page.linkpreview;

import android.content.Context;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import org.wikipedia.R;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class LinkPreviewOverlayView extends FrameLayout {
    public interface Callback {
        void onPrimaryClick();
        void onSecondaryClick();
    }

    @BindView(R.id.link_preview_primary_button) TextView primaryButton;
    @BindView(R.id.link_preview_secondary_button) View secondaryButton;

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

    @OnClick(R.id.link_preview_primary_button) void onPrimaryClick(View view) {
        if (callback != null) {
            callback.onPrimaryClick();
        }
    }

    @OnClick(R.id.link_preview_secondary_button) void onSecondaryClick(View view) {
        if (callback != null) {
            callback.onSecondaryClick();
        }
    }

    private void init() {
        inflate(getContext(), R.layout.view_link_preview_overlay, this);
        ButterKnife.bind(this);
    }
}
