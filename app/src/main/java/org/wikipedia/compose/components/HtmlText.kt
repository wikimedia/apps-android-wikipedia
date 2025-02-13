package org.wikipedia.compose.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import org.wikipedia.compose.theme.WikipediaTheme

@Composable
fun HtmlText(
    html: String,
    linkStyle: TextLinkStyles = TextLinkStyles(
        style = SpanStyle(
            color = WikipediaTheme.colors.progressiveColor,
            fontSize = 14.sp
        )
    ),
    normalStyle: TextStyle = TextStyle(
        color = WikipediaTheme.colors.secondaryColor,
        fontSize = 14.sp
    ),
    modifier: Modifier = Modifier
) {
    Text(
        modifier = modifier,
        text = AnnotatedString.fromHtml(
            htmlString = html,
            linkStyles = linkStyle
        ),
        style = normalStyle,
        lineHeight = 1.6.em
    )
}
