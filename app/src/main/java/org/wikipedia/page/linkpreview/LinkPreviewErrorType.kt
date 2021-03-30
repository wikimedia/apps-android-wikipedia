package org.wikipedia.page.linkpreview

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import org.wikipedia.R
import org.wikipedia.ktx.is404
import org.wikipedia.ktx.isOffline
import org.wikipedia.ktx.isTimeout

enum class LinkPreviewErrorType(@DrawableRes val icon: Int,
                                @StringRes val text: Int,
                                @StringRes val buttonText: Int) {

    OFFLINE(R.drawable.ic_no_article, R.string.error_message_generic, R.string.button_add_to_reading_list) {
        override fun buttonAction(errorView: LinkPreviewErrorView): LinkPreviewOverlayView.Callback {
            return errorView.addToListCallback
        }
    },
    PAGE_MISSING(R.drawable.ic_error_black_24dp, R.string.error_page_does_not_exist, R.string.view_link_preview_error_button_dismiss) {
        override fun buttonAction(errorView: LinkPreviewErrorView): LinkPreviewOverlayView.Callback {
            return errorView.dismissCallback
        }
    },
    GENERIC(R.drawable.ic_error_black_24dp, R.string.error_message_generic, R.string.view_link_preview_error_button_dismiss) {
        override fun buttonAction(errorView: LinkPreviewErrorView): LinkPreviewOverlayView.Callback {
            return errorView.dismissCallback
        }
    };

    abstract fun buttonAction(errorView: LinkPreviewErrorView): LinkPreviewOverlayView.Callback

    companion object {
        @JvmStatic
        operator fun get(caught: Throwable?): LinkPreviewErrorType {
            return if (caught.is404) {
                PAGE_MISSING
            } else if (caught.isOffline || caught.isTimeout) {
                OFFLINE
            } else {
                GENERIC
            }
        }
    }
}
