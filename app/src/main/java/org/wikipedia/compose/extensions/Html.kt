/*
 * Copyright 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wikipedia.compose.extensions

import android.graphics.Typeface
import android.text.Layout
import android.text.Spanned
import android.text.style.AbsoluteSizeSpan
import android.text.style.AlignmentSpan
import android.text.style.BackgroundColorSpan
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.text.style.StrikethroughSpan
import android.text.style.StyleSpan
import android.text.style.SubscriptSpan
import android.text.style.SuperscriptSpan
import android.text.style.TypefaceSpan
import android.text.style.URLSpan
import android.text.style.UnderlineSpan
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.LinkInteractionListener
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.em
import org.wikipedia.util.StringUtil

fun AnnotatedString.Companion.composeFromHtml(
    htmlString: String,
    linkStyles: TextLinkStyles?,
    linkInteractionListener: LinkInteractionListener? = null
): AnnotatedString {
    val spanned = StringUtil.fromHtml(htmlString)
    return spanned.toAnnotatedString(linkStyles, linkInteractionListener)
}

// TODO
// https://issuetracker.google.com/issues/374066408
// Everything below this point is copied from Html.android.kt from the Compose library itself.
// Once the above issue is resolved upstream, we'll be able to plug in our own TagHandler.
// Until then, keep an eye on updates of this file (also upstream) for any bugs/fixes.

internal fun Spanned.toAnnotatedString(
    linkStyles: TextLinkStyles? = null,
    linkInteractionListener: LinkInteractionListener? = null
): AnnotatedString {
    return AnnotatedString.Builder(capacity = length)
        .append(this)
        .also { it.addSpans(
            this,
            linkStyles,
            linkInteractionListener
        ) }
        .toAnnotatedString()
}

private fun AnnotatedString.Builder.addSpans(
    spanned: Spanned,
    linkStyles: TextLinkStyles?,
    linkInteractionListener: LinkInteractionListener?
) {
    spanned.getSpans(0, length, Any::class.java).forEach { span ->
        val range = TextRange(spanned.getSpanStart(span), spanned.getSpanEnd(span))
        addSpan(
            span,
            range.start,
            range.end,
            linkStyles,
            linkInteractionListener
        )
    }
}

private fun AnnotatedString.Builder.addSpan(
    span: Any,
    start: Int,
    end: Int,
    linkStyles: TextLinkStyles?,
    linkInteractionListener: LinkInteractionListener?
) {
    when (span) {
        is AbsoluteSizeSpan -> {
            // TODO(soboleva) need density object or make dip/px new units in TextUnit
        }
        is AlignmentSpan -> {
            addStyle(span.toParagraphStyle(), start, end)
        }
        is BackgroundColorSpan -> {
            addStyle(SpanStyle(background = Color(span.backgroundColor)), start, end)
        }
        is ForegroundColorSpan -> {
            addStyle(SpanStyle(color = Color(span.foregroundColor)), start, end)
        }
        is RelativeSizeSpan -> {
            addStyle(SpanStyle(fontSize = span.sizeChange.em), start, end)
        }
        is StrikethroughSpan -> {
            addStyle(SpanStyle(textDecoration = TextDecoration.LineThrough), start, end)
        }
        is StyleSpan -> {
            span.toSpanStyle()?.let { addStyle(it, start, end) }
        }
        is SubscriptSpan -> {
            addStyle(SpanStyle(baselineShift = BaselineShift.Subscript), start, end)
        }
        is SuperscriptSpan -> {
            addStyle(SpanStyle(baselineShift = BaselineShift.Superscript), start, end)
        }
        is TypefaceSpan -> {
            addStyle(span.toSpanStyle(), start, end)
        }
        is UnderlineSpan -> {
            addStyle(SpanStyle(textDecoration = TextDecoration.Underline), start, end)
        }
        is URLSpan -> {
            span.url?.let { url ->
                val link = LinkAnnotation.Url(
                    url,
                    linkStyles,
                    linkInteractionListener
                )
                addLink(link, start, end)
            }
        }
    }
}

private fun AlignmentSpan.toParagraphStyle(): ParagraphStyle {
    val alignment = when (this.alignment) {
        Layout.Alignment.ALIGN_NORMAL -> TextAlign.Start
        Layout.Alignment.ALIGN_CENTER -> TextAlign.Center
        Layout.Alignment.ALIGN_OPPOSITE -> TextAlign.End
        else -> TextAlign.Unspecified
    }
    return ParagraphStyle(textAlign = alignment)
}

private fun StyleSpan.toSpanStyle(): SpanStyle? {
    /** StyleSpan doc: styles are cumulative -- if both bold and italic are set in
     * separate spans, or if the base style is bold and a span calls for italic,
     * you get bold italic.  You can't turn off a style from the base style.
     */
    return when (style) {
        Typeface.BOLD -> {
            SpanStyle(fontWeight = FontWeight.Bold)
        }
        Typeface.ITALIC -> {
            SpanStyle(fontStyle = FontStyle.Italic)
        }
        Typeface.BOLD_ITALIC -> {
            SpanStyle(fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic)
        }
        else -> null
    }
}

private fun TypefaceSpan.toSpanStyle(): SpanStyle {
    val fontFamily = when (family) {
        FontFamily.Cursive.name -> FontFamily.Cursive
        FontFamily.Monospace.name -> FontFamily.Monospace
        FontFamily.SansSerif.name -> FontFamily.SansSerif
        FontFamily.Serif.name -> FontFamily.Serif
        else -> { optionalFontFamilyFromName(family) }
    }
    return SpanStyle(fontFamily = fontFamily)
}

/**
 * Mirrors [androidx.compose.ui.text.font.PlatformTypefaces.optionalOnDeviceFontFamilyByName]
 * behavior with both font weight and font style being Normal in this case */
private fun optionalFontFamilyFromName(familyName: String?): FontFamily? {
    if (familyName.isNullOrEmpty()) return null
    val typeface = Typeface.create(familyName, Typeface.NORMAL)
    return typeface.takeIf { typeface != Typeface.DEFAULT &&
            typeface != Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
    }?.let { FontFamily(it) }
}

/**
 * This tag is added at the beginning of a string fed to the HTML parser in order to trigger
 * a TagHandler's callback early on so we can replace the ContentHandler with our
 * own [AnnotationContentHandler]. This is needed to handle the opening <annotation> tags since by
 * the time TagHandler is triggered, the parser already visited and left the opening <annotation>
 * tag which contains the attributes. Note that closing tag doesn't have the attributes and
 * therefore not enough to construct the intermediate [AnnotationSpan] object that is later
 * transformed into [AnnotatedString]'s string annotation.
 */
private const val ContentHandlerReplacementTag = "ContentHandlerReplacementTag"
private const val AnnotationTag = "annotation"
