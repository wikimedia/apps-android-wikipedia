package org.wikipedia.compose.components

import android.text.Spanned
import android.text.style.URLSpan
import androidx.compose.foundation.clickable
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import org.wikipedia.compose.theme.WikipediaTheme

@Composable
fun HtmlText(
    html: String,
    modifier: Modifier = Modifier,
    linkStyle: TextLinkStyles = TextLinkStyles(
        style = SpanStyle(
            color = WikipediaTheme.colors.progressiveColor,
            fontSize = 14.sp
        )
    ),
    normalStyle: TextStyle = TextStyle(
        color = WikipediaTheme.colors.secondaryColor,
        fontSize = 14.sp
    )
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

@Composable
fun AnnotatedHtmlText(
    html: Spanned,
    modifier: Modifier = Modifier,
    onLinkClick: (String) -> Unit
) {
    val uriHandler = LocalUriHandler.current
    val spanned = html
    val annotatedString = buildAnnotatedString {
        append(spanned.toString())
        spanned.getSpans(0, spanned.length, URLSpan::class.java).forEach { span ->
            val start = spanned.getSpanStart(span)
            val end = spanned.getSpanEnd(span)
            addStyle(
                style = SpanStyle(color = WikipediaTheme.colors.progressiveColor),
                start = start,
                end = end
            )
            addStringAnnotation(
                tag = "URL",
                annotation = span.url,
                start = start,
                end = end
            )
        }
    }

    Text(
        text = annotatedString,
        modifier = modifier.then(
            Modifier.clickable { offset ->
                annotatedString.getStringAnnotations(
                    tag = "URL",
                    start = offset,
                    end = offset
                ).firstOrNull()?.let { annotation ->
                    onLinkClick(annotation.item)
                }
            }
        ),
        color = WikipediaTheme.colors.primaryColor,
        fontSize = 14.sp
    )
}

