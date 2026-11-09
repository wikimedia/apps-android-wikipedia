package org.wikipedia.diff

import android.text.Spanned
import android.text.style.BackgroundColorSpan
import android.text.style.StrikethroughSpan
import android.view.ContextThemeWrapper
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.dataclient.restbase.DiffResponse

@RunWith(RobolectricTestRunner::class)
class DiffUtilTest {
    @Test
    fun testHighlightsAfterFlag() {
        val diff = DiffResponse.DiffItem(
            type = DiffResponse.DIFF_TYPE_LINE_WITH_DIFF,
            text = "🇯🇵https://ampwww",
            highlightRanges = listOf(
                DiffResponse.HighlightRange(16, 3, DiffResponse.HIGHLIGHT_TYPE_DELETE),
                DiffResponse.HighlightRange(19, 3, DiffResponse.HIGHLIGHT_TYPE_ADD)
            )
        )
        val context = ContextThemeWrapper(WikipediaApp.instance, R.style.AppTheme)
        val text = DiffUtil.buildDiffLinesList(context, listOf(diff)).single().parsedText as Spanned
        val highlights = text.getSpans(0, text.length, BackgroundColorSpan::class.java)
        assertEquals(listOf("amp", "www"), highlights.map { text.subSequence(text.getSpanStart(it), text.getSpanEnd(it)).toString() })
        val deletions = text.getSpans(0, text.length, StrikethroughSpan::class.java)
        assertEquals(listOf("amp"), deletions.map { text.subSequence(text.getSpanStart(it), text.getSpanEnd(it)).toString() })
    }

    @Test
    fun testHighlightsSupplementaryCharacterAtEnd() {
        val diff = DiffResponse.DiffItem(
            type = DiffResponse.DIFF_TYPE_LINE_WITH_DIFF,
            text = "A😀",
            highlightRanges = listOf(DiffResponse.HighlightRange(1, 4, DiffResponse.HIGHLIGHT_TYPE_ADD))
        )
        val context = ContextThemeWrapper(WikipediaApp.instance, R.style.AppTheme)
        val text = DiffUtil.buildDiffLinesList(context, listOf(diff)).single().parsedText as Spanned
        val highlights = text.getSpans(0, text.length, BackgroundColorSpan::class.java)
        assertEquals(listOf("😀"), highlights.map { text.subSequence(text.getSpanStart(it), text.getSpanEnd(it)).toString() })
    }

    @Test
    fun testHighlightsAfterAsciiAndBmpCharacters() {
        for ((source, start) in listOf("https://ampwww" to 8, "ñ—https://ampwww" to 13)) {
            val diff = DiffResponse.DiffItem(
                type = DiffResponse.DIFF_TYPE_LINE_WITH_DIFF,
                text = source,
                highlightRanges = listOf(
                    DiffResponse.HighlightRange(start, 3, DiffResponse.HIGHLIGHT_TYPE_DELETE),
                    DiffResponse.HighlightRange(start + 3, 3, DiffResponse.HIGHLIGHT_TYPE_ADD)
                )
            )
            val context = ContextThemeWrapper(WikipediaApp.instance, R.style.AppTheme)
            val text = DiffUtil.buildDiffLinesList(context, listOf(diff)).single().parsedText as Spanned
            val highlights = text.getSpans(0, text.length, BackgroundColorSpan::class.java)
            assertEquals(listOf("amp", "www"), highlights.map { text.subSequence(text.getSpanStart(it), text.getSpanEnd(it)).toString() })
        }
    }

    @Test
    fun testHighlightsContainingSupplementaryCharacter() {
        for ((source, start) in listOf("A😀B!" to 1, "😀B!" to 0)) {
            val diff = DiffResponse.DiffItem(
                type = DiffResponse.DIFF_TYPE_LINE_WITH_DIFF,
                text = source,
                highlightRanges = listOf(DiffResponse.HighlightRange(start, 5, DiffResponse.HIGHLIGHT_TYPE_ADD))
            )
            val context = ContextThemeWrapper(WikipediaApp.instance, R.style.AppTheme)
            val text = DiffUtil.buildDiffLinesList(context, listOf(diff)).single().parsedText as Spanned
            val highlights = text.getSpans(0, text.length, BackgroundColorSpan::class.java)
            assertEquals(listOf("😀B"), highlights.map { text.subSequence(text.getSpanStart(it), text.getSpanEnd(it)).toString() })
        }
    }
}
