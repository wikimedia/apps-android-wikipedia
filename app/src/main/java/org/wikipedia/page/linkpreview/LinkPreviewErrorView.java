package org.wikipedia.page.linkpreview;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;

import org.wikipedia.R;
import org.wikipedia.databinding.ViewLinkPreviewErrorBinding;

public class LinkPreviewErrorView extends LinearLayout {
    private ImageView icon;
    private TextView textView;

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
        final ViewLinkPreviewErrorBinding binding = ViewLinkPreviewErrorBinding.bind(this);

        icon = binding.viewLinkPreviewErrorIcon;
        textView = binding.viewLinkPreviewErrorText;
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
        icon.setImageDrawable(AppCompatResources.getDrawable(getContext(), errorType.icon()));

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

        @Override public void onTertiaryClick() {
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

        @Override public void onTertiaryClick() {
        }
    }
}
