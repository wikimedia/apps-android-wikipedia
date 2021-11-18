package org.wikipedia.page.linkpreview

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import org.wikipedia.R
import org.wikipedia.page.Namespace
import org.wikipedia.page.PageTitle
import org.wikipedia.util.ThrowableUtil

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
    USER_PAGE_MISSING(R.drawable.ic_userpage_error_icon, R.string.error_user_page_does_not_exist, R.string.view_link_preview_error_button_dismiss) {
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
        operator fun get(caught: Throwable?, pageTitle: PageTitle?): LinkPreviewErrorType {
            return if (caught != null && ThrowableUtil.is404(caught)) {
                if (pageTitle?.namespace() == Namespace.USER)
                    USER_PAGE_MISSING
                else
                    PAGE_MISSING
            } else if (caught != null && (ThrowableUtil.isOffline(caught) || ThrowableUtil.isTimeout(caught))) {
                OFFLINE
            } else {
                GENERIC
            }
        }
    }
}
