package org.wikipedia.compose.components.error

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.wikipedia.R
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.compose.theme.WikipediaTheme
import org.wikipedia.dataclient.mwapi.MwException
import org.wikipedia.page.Namespace
import org.wikipedia.page.PageTitle
import org.wikipedia.theme.Theme
import org.wikipedia.util.StringUtil
import org.wikipedia.util.ThrowableUtil.is404
import org.wikipedia.util.ThrowableUtil.isEmptyException
import org.wikipedia.util.ThrowableUtil.isNotLoggedIn
import org.wikipedia.util.ThrowableUtil.isOffline
import org.wikipedia.util.ThrowableUtil.isTimeout

@Composable
fun WikiErrorView(
    modifier: Modifier = Modifier,
    caught: Throwable?,
    pageTitle: PageTitle? = null,
    errorClickEvents: WikiErrorClickEvents? = null,
) {
    val errorType = getErrorType(caught, pageTitle)

    val errorMessage = when {
        caught is MwException -> caught.message
        errorType is ComposeErrorType.UserPageMissing && pageTitle != null -> stringResource(
            errorType.text,
            pageTitle.uri,
            pageTitle.displayText,
            StringUtil.removeNamespace(pageTitle.displayText)
        )
        else -> stringResource(errorType.text)
    }

    val footerErrorMessage = when {
        errorType.hasFooterText -> stringResource(errorType.footerText)
        caught != null && caught !is MwException -> caught.message
        else -> null
    }

    Column(
        modifier = modifier
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(
            modifier = Modifier
                .height(16.dp)
        )
        Image(
            modifier = Modifier
                .size(72.dp),
            painter = painterResource(errorType.icon),
            colorFilter = ColorFilter.tint(color = WikipediaTheme.colors.placeholderColor),
            contentDescription = null
        )
        Spacer(
            modifier = Modifier
                .height(24.dp)
        )

        if (errorMessage.isNotEmpty()) {
            Text(
                text = AnnotatedString.fromHtml(
                    htmlString = errorMessage,
                    linkStyles = TextLinkStyles(
                        style = SpanStyle(
                            color = WikipediaTheme.colors.progressiveColor,
                            fontSize = 14.sp
                        )
                    )
                ),
                style = TextStyle(
                    textAlign = TextAlign.Center,
                    fontSize = 16.sp,
                    lineHeight = 19.2.sp,
                    color = WikipediaTheme.colors.placeholderColor
                )
            )
        }

        Spacer(
            modifier = Modifier
                .height(16.dp)
        )
        Button(
            modifier = Modifier
                .heightIn(min = 48.dp)
                .widthIn(min = 0.dp),
            onClick = { getClickEventForErrorType(errorClickEvents, errorType)?.invoke() },
            colors = ButtonDefaults.buttonColors(
                containerColor = WikipediaTheme.colors.backgroundColor,
                contentColor = WikipediaTheme.colors.placeholderColor
            ),
            content = {
                Text(
                    text = stringResource(errorType.buttonText),
                    fontSize = 16.sp
                )
            }
        )
        if (!footerErrorMessage.isNullOrEmpty()) {
            Spacer(
                modifier = Modifier
                    .height(16.dp)
            )
            Text(
                modifier = Modifier
                    .padding(horizontal = 16.dp),
                text = footerErrorMessage,
                color = WikipediaTheme.colors.placeholderColor,
                fontSize = 14.sp
            )
        }
    }
}

@Preview
@Composable
private fun WikiErrorViewPreview() {
    BaseTheme(
        currentTheme = Theme.DARK
    ) {
        WikiErrorView(
            caught = Exception()
        )
    }
}

private fun getClickEventForErrorType(
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
