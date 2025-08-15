package org.wikipedia.compose.components

import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import org.wikipedia.compose.theme.WikipediaTheme

@Composable
fun TextWithInlineElement(
    text: String,
    position: InlinePosition,
    placeholder: Placeholder,
    style: TextStyle = MaterialTheme.typography.bodyMedium.copy(
        color = WikipediaTheme.colors.primaryColor
    ),
    content: @Composable (String) -> Unit

) {
    val inlineElementId = "element"
    val text = text
    val annotatedString = buildAnnotatedString {
        when (position) {
            InlinePosition.START -> {
                appendInlineContent(inlineElementId, "[$inlineElementId]")
                append(text)
            }
            InlinePosition.END -> {
                append(text)
                appendInlineContent(inlineElementId, "[$inlineElementId]")
            }
        }
    }
    val inlineContent = mapOf(
        Pair(inlineElementId, InlineTextContent(placeholder = placeholder, children = content))
    )
    Text(
        text = annotatedString,
        inlineContent = inlineContent,
        style = style
    )
}

enum class InlinePosition {
    START, END
}
