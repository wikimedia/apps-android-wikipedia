package org.wikipedia.compose.components

import android.graphics.Typeface
import android.text.Spanned
import android.text.style.StyleSpan
import android.text.style.URLSpan
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
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
    onLinkClick: (String) -> Unit,
    content: @Composable (AnnotatedString) -> Unit
) {
    val annotatedString = buildAnnotatedString {
        append(html.toString())
        html.getSpans(0, html.length, Any::class.java).forEach { span ->
            val start = html.getSpanStart(span)
            val end = html.getSpanEnd(span)
            when (span) {
                is URLSpan -> {
                    val url = span.url
                    addStyle(
                        style = SpanStyle(color = WikipediaTheme.colors.progressiveColor),
                        start = start,
                        end = end
                    )
                    addLink(
                        url = LinkAnnotation.Url(
                            url = url,
                            linkInteractionListener = {
                                onLinkClick(url)
                            }
                        ),
                        start = start,
                        end = end
                    )
                }
                is StyleSpan -> {
                    addStyle(
                        style = when (span.style) {
                            Typeface.BOLD -> SpanStyle(fontWeight = FontWeight.Bold)
                            Typeface.ITALIC -> SpanStyle(fontStyle = FontStyle.Italic)
                            Typeface.BOLD_ITALIC -> SpanStyle(fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic)
                            else -> SpanStyle()
                        },
                        start = start,
                        end = end
                    )
                }
            }
        }
    }
    content(annotatedString)
}
