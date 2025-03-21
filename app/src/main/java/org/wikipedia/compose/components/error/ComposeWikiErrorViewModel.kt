package org.wikipedia.compose.components.error

import android.content.Context
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.wikipedia.R
import org.wikipedia.dataclient.mwapi.MwException
import org.wikipedia.page.Namespace
import org.wikipedia.page.PageTitle
import org.wikipedia.util.StringUtil
import org.wikipedia.util.ThrowableUtil.is404
import org.wikipedia.util.ThrowableUtil.isEmptyException
import org.wikipedia.util.ThrowableUtil.isNotLoggedIn
import org.wikipedia.util.ThrowableUtil.isOffline
import org.wikipedia.util.ThrowableUtil.isTimeout

class ComposeWikiErrorViewModel : ViewModel() {
    data class WikiErrorState(
        val errorType: ComposeErrorType = ComposeErrorType.Generic(),
        val errorMessage: String? = null,
        val footerErrorMessage: String? = null,
        val clickEvents: WikiErrorClickEvents? = null
    )

    private val _uiState = MutableStateFlow(WikiErrorState())
    val uiState: StateFlow<WikiErrorState> = _uiState.asStateFlow()

    fun setError(
        caught: Throwable?,
        pageTitle: PageTitle? = null,
        context: Context
    ) {
        val errorType = getErrorType(caught, pageTitle)

        val errorMessage = when {
            caught is MwException -> caught.message
            errorType is ComposeErrorType.UserPageMissing && pageTitle != null -> context.getString(
                errorType.text,
                pageTitle.uri,
                pageTitle.displayText,
                StringUtil.removeNamespace(pageTitle.displayText)
            )

            else -> context.getString(errorType.text)
        }
        val footerErrorMessage = when {
            errorType.hasFooterText -> context.getString(errorType.footerText)
            caught != null && caught !is MwException -> caught.message
            else -> null
        }

        _uiState.value = WikiErrorState(
            errorType = errorType,
            errorMessage = errorMessage,
            footerErrorMessage = footerErrorMessage
        )
    }

    fun getClickEventForErrorType(
        wikiErrorClickEvents: WikiErrorClickEvents?,
        errorType: ComposeErrorType
    ): (() -> Unit)? {
        return when (errorType) {
            is ComposeErrorType.UserPageMissing,
            is ComposeErrorType.PageMissing,
            is ComposeErrorType.Generic -> wikiErrorClickEvents?.backClickListener

            is ComposeErrorType.LoggedOut -> wikiErrorClickEvents?.loginClickListener

            is ComposeErrorType.Empty -> wikiErrorClickEvents?.nextClickListener

            is ComposeErrorType.Offline,
            is ComposeErrorType.Timeout -> wikiErrorClickEvents?.retryClickListener
        }
    }

    private fun getErrorType(caught: Throwable?, pageTitle: PageTitle?): ComposeErrorType {
        caught?.let {
            when {
                is404(it) -> {
                    return if (pageTitle?.namespace() == Namespace.USER) ComposeErrorType.UserPageMissing()
                    else ComposeErrorType.PageMissing()
                }

                isTimeout(it) -> {
                    return ComposeErrorType.Timeout()
                }

                isOffline(it) -> {
                    return ComposeErrorType.Offline()
                }

                isEmptyException(it) -> {
                    return ComposeErrorType.Empty()
                }

                isNotLoggedIn(it) -> {
                    return ComposeErrorType.LoggedOut()
                }

                else -> {}
            }
        }
        return ComposeErrorType.Generic()
    }
}

sealed class ComposeErrorType(
    @DrawableRes val icon: Int,
    @StringRes val text: Int,
    @StringRes val buttonText: Int,
    val hasFooterText: Boolean = false,
    @StringRes val footerText: Int = 0,
) {
    class UserPageMissing : ComposeErrorType(
        icon = R.drawable.ic_userpage_error_icon,
        text = R.string.error_user_page_does_not_exist,
        buttonText = R.string.page_error_back_to_main
    )

    class PageMissing : ComposeErrorType(
        icon = R.drawable.ic_error_black_24dp,
        text = R.string.error_page_does_not_exist,
        buttonText = R.string.page_error_back_to_main
    )

    class Timeout : ComposeErrorType(
        icon = R.drawable.ic_error_black_24dp,
        text = R.string.view_wiki_error_message_timeout,
        buttonText = R.string.offline_load_error_retry
    )

    class Offline : ComposeErrorType(
        icon = R.drawable.ic_portable_wifi_off_black_24px,
        text = R.string.view_wiki_error_message_offline,
        buttonText = R.string.offline_load_error_retry
    )

    class Empty : ComposeErrorType(
        icon = R.drawable.ic_error_black_24dp,
        text = R.string.error_message_generic,
        buttonText = R.string.error_next
    )

    class LoggedOut : ComposeErrorType(
        icon = R.drawable.ic_error_black_24dp,
        text = R.string.error_message_generic,
        buttonText = R.string.reading_lists_login_button
    )

    class Generic : ComposeErrorType(
        icon = R.drawable.ic_error_black_24dp,
        text = R.string.error_message_generic,
        buttonText = R.string.error_back
    )
}

data class WikiErrorClickEvents(
    var retryClickListener: (() -> Unit)? = null,
    var backClickListener: (() -> Unit)? = null,
    var nextClickListener: (() -> Unit)? = null,
    var loginClickListener: (() -> Unit)? = null,
)
