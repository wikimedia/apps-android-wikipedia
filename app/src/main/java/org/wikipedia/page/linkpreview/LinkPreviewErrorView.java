package org.wikipedia.page.linkpreview;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.wikipedia.R;

import butterknife.BindView;
import butterknife.ButterKnife;

public class LinkPreviewErrorView extends LinearLayout {
    @BindView(R.id.view_link_preview_error_icon) ImageView icon;
    @BindView(R.id.view_link_preview_error_text) TextView textView;

    public interface Callback {
        void onAddToList();
        void onDismiss();
    }

    @Nullable private Callback callback;
    @NonNull private OverlayViewAddToListCallback addToListCallback = new OverlayViewAddToListCallback();
    @NonNull private OverlayViewDismissCallback dismissCallback = new OverlayViewDismissCallback();

    public LinkPreviewErrorView(Context context) {
        this(context, null);
    }

    public LinkPreviewErrorView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public LinkPreviewErrorView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        inflate(context, R.layout.view_link_preview_error, this);
        ButterKnife.bind(this);
    }

    public void setCallback(@Nullable Callback callback) {
        this.callback = callback;
    }

    @NonNull OverlayViewAddToListCallback addToListCallback() {
        return addToListCallback;
    }

    @NonNull OverlayViewDismissCallback dismissCallback() {
        return dismissCallback;
    }

    public void setError(@Nullable Throwable caught) {
        LinkPreviewErrorType errorType = LinkPreviewErrorType.get(caught);
        icon.setImageDrawable(ContextCompat.getDrawable(getContext(), errorType.icon()));

        // HACK: This message is delivered in one piece in a link preview but as a separate primary
        // message and subtitle in the full page view.  Figure out a good way to handle this.
        if (errorType == LinkPreviewErrorType.OFFLINE) {
            String message = getResources().getString(R.string.page_offline_notice_cannot_load_while_offline)
                    + "\n" + getResources().getString(R.string.page_offline_notice_add_to_reading_list);
            textView.setText(message);
        } else {
            textView.setText(getContext().getResources().getString(errorType.text()));
        }
    }

    private class OverlayViewAddToListCallback implements LinkPreviewOverlayView.Callback {
        @Override public void onPrimaryClick() {
            if (callback != null) {
                callback.onAddToList();
            }
        }

        @Override public void onSecondaryClick() {
        }
    }

    private class OverlayViewDismissCallback implements LinkPreviewOverlayView.Callback {
        @Override public void onPrimaryClick() {
            if (callback != null) {
                callback.onDismiss();
            }
        }

        @Override public void onSecondaryClick() {
        }
    }
}
