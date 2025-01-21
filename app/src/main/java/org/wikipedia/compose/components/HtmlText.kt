package org.wikipedia.compose.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.fromHtml
import org.wikipedia.compose.theme.WikipediaTheme

@Composable
fun HtmlText(
    html: String,
    linkStyles: TextLinkStyles = TextLinkStyles(
        style = SpanStyle(
            color = WikipediaTheme.colors.progressiveColor
        )
    ),
    modifier: Modifier = Modifier
) {
    Text(
        modifier = modifier,
        text = AnnotatedString.fromHtml(
            htmlString = html,
            linkStyles = linkStyles
        )
    )
}
