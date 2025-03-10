package org.wikipedia.compose.components.error

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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
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
import androidx.lifecycle.viewmodel.compose.viewModel
import org.wikipedia.compose.ComposeColors
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.compose.theme.WikipediaTheme
import org.wikipedia.page.PageTitle
import org.wikipedia.theme.Theme

@Composable
fun ComposeWikiErrorParentView(
    modifier: Modifier = Modifier,
    caught: Throwable?,
    pageTitle: PageTitle? = null,
    viewModel: ComposeWikiErrorViewModel = viewModel(),
    errorClickEvents: WikiErrorClickEvents? = null,
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    LaunchedEffect(Unit) {
        viewModel.setError(caught, pageTitle, context)
    }
    ComposeWikiErrorView(
        modifier = modifier,
        errorType = uiState.errorType,
        errorMessage = uiState.errorMessage,
        footerErrorMessage = uiState.footerErrorMessage,
        onButtonClick = viewModel.getClickEventForErrorType(errorClickEvents, uiState.errorType)
    )
}

@Composable
fun ComposeWikiErrorView(
    modifier: Modifier = Modifier,
    errorType: ComposeErrorType,
    errorMessage: String?,
    footerErrorMessage: String?,
    onButtonClick: (() -> Unit)? = null,
) {
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
            colorFilter = ColorFilter.tint(color = ComposeColors.Gray500),
            contentDescription = null
        )
        Spacer(
            modifier = Modifier
                .height(24.dp)
        )

        if (errorMessage != null) {
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
                    color = ComposeColors.Gray500
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
            onClick = { onButtonClick?.invoke() },
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
        if (footerErrorMessage != null) {
            Spacer(
                modifier = Modifier
                    .height(16.dp)
            )
            Text(
                modifier = Modifier
                    .padding(horizontal = 16.dp),
                text = footerErrorMessage,
                color = ComposeColors.Gray500,
                fontSize = 14.sp
            )
        }
    }
}

@Preview
@Composable
private fun ComposeWikiErrorParentViewPreview() {
    BaseTheme(
        currentTheme = Theme.DARK
    ) {
        ComposeWikiErrorParentView(
            caught = Exception()
        )
    }
}
