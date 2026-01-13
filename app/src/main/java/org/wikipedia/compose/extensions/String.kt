package org.wikipedia.compose.extensions

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import org.wikipedia.util.StringUtil

fun String.toAnnotatedStringWithBoldQuery(query: String?): AnnotatedString {
    val hasHtml = this.contains("<") || this.contains("&")
    val annotated =
        if (hasHtml) StringUtil.fromHtml(this).toAnnotatedString() else AnnotatedString(this)
    if (query.isNullOrEmpty()) {
        return annotated
    }

    val startIndex = annotated.text.indexOf(query, ignoreCase = true)
    if (startIndex >= 0) {
        val builder = AnnotatedString.Builder(annotated)
        builder.addStyle(
            SpanStyle(fontWeight = FontWeight.Bold),
            startIndex,
            startIndex + query.length
        )
        return builder.toAnnotatedString()
    }
    return annotated
}
