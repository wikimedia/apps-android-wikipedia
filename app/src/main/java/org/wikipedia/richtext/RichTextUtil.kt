package org.wikipedia.richtext

import android.text.*
import android.text.style.URLSpan
import android.widget.TextView
import androidx.annotation.IntRange
import androidx.core.text.getSpans
import androidx.core.text.toSpannable
import androidx.core.text.toSpanned
import org.wikipedia.util.StringUtil
import org.wikipedia.util.log.L

object RichTextUtil {
    /**
     * Apply only the spans from src to dst specific by spans.
     *
     * @see {@link android.text.TextUtils.copySpansFrom}
     */
    private fun copySpans(src: Spanned, dst: Spannable, spans: Collection<Any?>) {
        for (span in spans) {
            val start = src.getSpanStart(span)
            val end = src.getSpanEnd(span)
            val flags = src.getSpanFlags(span)
            dst.setSpan(span, start, end, flags)
        }
    }

    /** Strips all rich text except spans used to provide compositional hints.  */
    fun stripRichText(str: CharSequence, start: Int, end: Int): CharSequence {
        val plainText = str.toString()
        val ret = plainText.toSpannable()
        if (str is Spanned) {
            val keyboardHintSpans = getComposingSpans(str, start, end)
            copySpans(str, ret, keyboardHintSpans)
        }
        return ret
    }

    /**
     * @return Temporary spans, often applied by the keyboard to provide hints such as typos.
     *
     * @see {@link android.view.inputmethod.BaseInputConnection.removeComposingSpans}
     */
    private fun getComposingSpans(spanned: Spanned, start: Int, end: Int): List<Any?> {
        return getSpans(spanned, start, end).filter { isComposingSpan(spanned, it) }
    }

    fun getSpans(spanned: Spanned, start: Int, end: Int): Array<out Any> {
        return spanned.getSpans(start, end)
    }

    private fun isComposingSpan(spanned: Spanned, span: Any?): Boolean {
        return spanned.getSpanFlags(span) and Spanned.SPAN_COMPOSING == Spanned.SPAN_COMPOSING
    }

    fun removeUnderlinesFromLinks(textView: TextView) {
        val text = textView.text
        if (text is Spanned) {
            val spannable = text.toSpannable()
            removeUnderlinesFromLinks(spannable, spannable.getSpans(0, spannable.length, URLSpan::class.java))
            textView.text = spannable
        }
    }

    private fun removeUnderlinesFromLinks(spannable: Spannable, spans: Array<URLSpan>) {
        for (span in spans) {
            val start = spannable.getSpanStart(span)
            val end = spannable.getSpanEnd(span)
            spannable.removeSpan(span)
            spannable.setSpan(URLSpanNoUnderline(span.url), start, end, 0)
        }
    }

    fun removeUnderlinesFromLinksAndMakeBold(textView: TextView) {
        val text = textView.text
        if (text is Spanned) {
            val spannable = text.toSpannable()
            removeUnderlinesFromLinksAndMakeBold(spannable, spannable.getSpans(0, spannable.length, URLSpan::class.java))
            textView.text = spannable
        }
    }

    private fun removeUnderlinesFromLinksAndMakeBold(spannable: Spannable, spans: Array<URLSpan>) {
        for (span in spans) {
            val start = spannable.getSpanStart(span)
            val end = spannable.getSpanEnd(span)
            spannable.removeSpan(span)
            spannable.setSpan(URLSpanBoldNoUnderline(span.url), start, end, 0)
        }
    }

    fun stripHtml(html: String): String {
        return StringUtil.fromHtml(html).toString()
    }

    fun remove(text: CharSequence, @IntRange(from = 1) start: Int, end: Int): CharSequence {
        try {
            return TextUtils.concat(text.subSequence(0, start - 1), text.subSequence(end, text.length)).toSpanned()
        } catch (e: Exception) {
            // A number of possible exceptions can be thrown by the system from handling even
            // slightly malformed spans or paragraphs, so let's ignore them for now and just
            // return the original text.
            L.e(e)
        }
        return text
    }
}
