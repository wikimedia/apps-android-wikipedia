package org.wikipedia.page.linkpreview;

import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;

import org.wikipedia.R;
import org.wikipedia.util.ThrowableUtil;

enum LinkPreviewErrorType {
    // Note: The string resource corresponding to the 'text' field will be updated programmatically
    // for the OFFLINE type to accommodate error view design requirements.  See
    // LinkPreviewErrorView.setError() for further discussion.
    OFFLINE(R.drawable.ic_no_article, R.string.error_message_generic,
            R.string.button_add_to_reading_list) {
        @NonNull @Override
        LinkPreviewOverlayView.Callback buttonAction(@NonNull LinkPreviewErrorView errorView) {
            return errorView.addToListCallback();
        }
    },

    PAGE_MISSING(R.drawable.ic_error_black_24dp, R.string.error_page_does_not_exist,
            R.string.view_link_preview_error_button_dismiss) {
        @NonNull @Override
        LinkPreviewOverlayView.Callback buttonAction(@NonNull LinkPreviewErrorView errorView) {
            return errorView.dismissCallback();
        }
    },

    GENERIC(R.drawable.ic_error_black_24dp, R.string.error_message_generic,
            R.string.view_link_preview_error_button_dismiss) {
        @NonNull @Override
        LinkPreviewOverlayView.Callback buttonAction(@NonNull LinkPreviewErrorView errorView) {
            return errorView.dismissCallback();
        }
    };

    @DrawableRes private final int icon;
    @StringRes private final int text;
    @StringRes private final int buttonText;

    LinkPreviewErrorType(@DrawableRes int icon, @StringRes int text, @StringRes int buttonText) {
        this.icon = icon;
        this.text = text;
        this.buttonText = buttonText;
    }

    @DrawableRes int icon() {
        return icon;
    }

    @StringRes int text() {
        return text;
    }

    @StringRes public int buttonText() {
        return buttonText;
    }

    @NonNull abstract LinkPreviewOverlayView.Callback buttonAction(@NonNull LinkPreviewErrorView errorView);

    @NonNull public static LinkPreviewErrorType get(@Nullable Throwable caught) {
        if (caught != null && ThrowableUtil.is404(caught)) {
            return LinkPreviewErrorType.PAGE_MISSING;
        }
        if (caught != null && ThrowableUtil.isOffline(caught)) {
            return LinkPreviewErrorType.OFFLINE;
        }
        return LinkPreviewErrorType.GENERIC;
    }
}
