package org.wikipedia.compose.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkInteractionListener
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import org.wikipedia.compose.extensions.composeFromHtml
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.compose.theme.WikipediaTheme
import org.wikipedia.theme.Theme

@Composable
fun HtmlText(
    text: String,
    modifier: Modifier = Modifier,
    linkStyle: TextLinkStyles = TextLinkStyles(
        style = SpanStyle(
            color = WikipediaTheme.colors.progressiveColor,
            fontSize = 14.sp
        )
    ),
    style: TextStyle = TextStyle(
        color = WikipediaTheme.colors.secondaryColor,
        fontSize = 14.sp
    ),
    color: Color = Color.Unspecified,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Ellipsis,
    lineHeight: TextUnit = 1.6.em,
    linkInteractionListener: LinkInteractionListener? = null
) {
    Text(
        modifier = modifier,
        text = AnnotatedString.composeFromHtml(
            htmlString = text,
            linkStyles = linkStyle,
            linkInteractionListener = linkInteractionListener
        ),
        lineHeight = lineHeight,
        style = style,
        color = color,
        maxLines = maxLines,
        overflow = overflow,
    )
}

@Preview
@Composable
private fun HtmlTextPreview() {
    BaseTheme(currentTheme = Theme.LIGHT) {
        HtmlText("This is an <em>example</em> of <strong>text</strong><br />with " +
                "<a href=\"#foo\">html</a>, with nonstandard stuff<br />like <code>monospace</code>" +
                " and <sup>superscript</sup>, too!")
    }
}
