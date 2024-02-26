package org.wikipedia.edit.richtext

import android.os.Build
import android.text.Spanned
import androidx.activity.ComponentActivity
import androidx.core.text.getSpans
import androidx.core.widget.NestedScrollView
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.wikipedia.edit.SyntaxHighlightableEditText
import org.wikipedia.util.log.L
import java.util.*

class SyntaxHighlighter(
    private val activity: ComponentActivity,
    private val textBox: SyntaxHighlightableEditText,
    private val scrollView: NestedScrollView?,
    private val highlightDelayMillis: Long = HIGHLIGHT_DELAY_MILLIS,
) {
    private val syntaxRules = listOf(
            SyntaxRule("{{", "}}", SyntaxRuleStyle.TEMPLATE),
            SyntaxRule("[[", "]]", SyntaxRuleStyle.INTERNAL_LINK),
            SyntaxRule("[", "]", SyntaxRuleStyle.EXTERNAL_LINK),
            SyntaxRule("<big>", "</big>", SyntaxRuleStyle.TEXT_LARGE),
            SyntaxRule("<small>", "</small>", SyntaxRuleStyle.TEXT_SMALL),
            SyntaxRule("<sub>", "</sub>", SyntaxRuleStyle.SUBSCRIPT),
            SyntaxRule("<sup>", "</sup>", SyntaxRuleStyle.SUPERSCRIPT),
            SyntaxRule("<code>", "</code>", if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) SyntaxRuleStyle.CODE else SyntaxRuleStyle.BOLD),
            SyntaxRule("<u>", "</u>", SyntaxRuleStyle.UNDERLINE),
            SyntaxRule("<s>", "</s>", SyntaxRuleStyle.STRIKETHROUGH),
            SyntaxRule("<", ">", SyntaxRuleStyle.REF),
            SyntaxRule("'''", "'''", SyntaxRuleStyle.BOLD),
            SyntaxRule("''", "''", SyntaxRuleStyle.ITALIC),
            SyntaxRule("=====", "=====", SyntaxRuleStyle.HEADING_SMALL),
            SyntaxRule("====", "====", SyntaxRuleStyle.HEADING_SMALL),
            SyntaxRule("===", "===", SyntaxRuleStyle.HEADING_MEDIUM),
            SyntaxRule("==", "==", SyntaxRuleStyle.HEADING_LARGE),
    )

    private var lastScrollY = -1
    private var searchQueryPositions: List<Int>? = null
    private var searchQueryLength = 0
    private var searchQueryPositionIndex = 0

    var enabled = true
        set(value) {
            field = value
            if (!value) {
                textBox.text.getSpans<SpanExtents>().forEach { textBox.text.removeSpan(it) }
            } else {
                activity.lifecycleScope.launch {
                    runHighlightTasks(highlightDelayMillis)
                }
            }
        }

    init {
        textBox.doAfterTextChanged {
            activity.lifecycleScope.launch {
                runHighlightTasks(highlightDelayMillis * 2)
            }
        }
        textBox.scrollView = scrollView
        activity.lifecycleScope.launch {
            highlightOnScroll()
        }
    }

    private suspend fun runHighlightTasks(delayMillis: Long) {
        if (!enabled) {
            return
        }
        delay(delayMillis)
        flow {
            val layout = textBox.layout!!
            val maxLast = layout.lineCount - 1

            val firstVisibleLine = scrollView?.let { layout.getLineForVertical(it.scrollY) } ?: 0
            val lastVisibleLine = scrollView?.let {
                layout.getLineForVertical(it.scrollY + it.height)
                    .coerceIn(firstVisibleLine, maxLast)
            } ?: maxLast

            val firstVisibleIndex = layout.getLineStart(firstVisibleLine)
            val lastVisibleIndex = layout.getLineEnd(lastVisibleLine)
            val textToHighlight = textBox.text.substring(firstVisibleIndex, lastVisibleIndex)

            val list = coroutineScope {
                listOf(
                    async { getHighlightSpans(textToHighlight, firstVisibleIndex) },
                    async { getSyntaxMatches(firstVisibleIndex, textToHighlight.length) },
                ).awaitAll().flatten()
            }
            emit(list)
        }
            .retry(10)
            .flowOn(Dispatchers.Default)
            .catch { L.e(it) }
            .collectLatest { result ->
                textBox.enqueueNoScrollingLayoutChange()

                var time = System.currentTimeMillis()
                val oldSpans = textBox.text.getSpans<SpanExtents>().toMutableList()
                val newSpans = result.toMutableList()

                val dupes = oldSpans.filter { item ->
                    val r = result.find {
                        it.start == textBox.text.getSpanStart(item) &&
                                it.end == textBox.text.getSpanEnd(item) &&
                                it.syntaxRule == item.syntaxRule
                    }
                    if (r != null) {
                        newSpans.remove(r)
                    }
                    r != null
                }
                oldSpans.removeAll(dupes)

                oldSpans.forEach { textBox.text.removeSpan(it) }
                newSpans.forEach {
                    textBox.text.setSpan(it, it.start, it.end, Spanned.SPAN_INCLUSIVE_INCLUSIVE)
                }

                time = System.currentTimeMillis() - time
                L.d("Took $time ms to remove ${oldSpans.size} spans and add ${newSpans.size} new.")
            }
    }

    private fun getHighlightSpans(text: CharSequence, startOffset: Int): List<SpanExtents> {
        val spanStack = Stack<SpanExtents>()
        val spansToSet = mutableListOf<SpanExtents>()
        val textChars = text.toString().toCharArray()

        /*
        The (naïve) algorithm:
        Iterate through the text string, and maintain a stack of matched syntax rules.
        When the "start" and "end" symbol of a rule are matched in sequence, create a new
        Span to be added to the EditText at the corresponding location.
         */
        var i = 0
        while (i < textChars.size) {
            var newSpanInfo: SpanExtents
            var completed = false

            for (rule in syntaxRules) {
                if (i + rule.endChars.size > textChars.size) {
                    continue
                }
                var pass = true
                for (j in 0 until rule.endChars.size) {
                    if (textChars[i + j] != rule.endChars[j]) {
                        pass = false
                        break
                    }
                }
                if (pass) {
                    val sr = spanStack.find { it.syntaxRule == rule }
                    if (sr != null) {
                        newSpanInfo = sr
                        spanStack.remove(sr)
                        newSpanInfo.end = i + rule.endChars.size
                        spansToSet.add(newSpanInfo)
                        i += rule.endChars.size - 1
                        completed = true
                        break
                    }
                }
            }

            if (!completed) {
                for (rule in syntaxRules) {
                    if (i + rule.startChars.size > textChars.size) {
                        continue
                    }
                    var pass = true
                    for (j in 0 until rule.startChars.size) {
                        if (textChars[i + j] != rule.startChars[j]) {
                            pass = false
                            break
                        }
                    }
                    if (pass) {
                        val sp = rule.spanStyle.createSpan(activity, i, rule)
                        spanStack.push(sp)
                        i += rule.startChars.size - 1
                        break
                    }
                }
            }

            i++
        }
        spansToSet.forEach {
            it.start += startOffset
            it.end += startOffset
        }
        spansToSet.sortBy { it.syntaxRule.spanStyle }
        return spansToSet
    }

    private fun getSyntaxMatches(startOffset: Int, textLength: Int): List<SpanExtents> {
        val syntaxItem = SyntaxRule("", "", SyntaxRuleStyle.SEARCH_MATCHES)

        return searchQueryPositions.orEmpty().asSequence()
            .filter { it >= startOffset && it < startOffset + textLength }
            .mapIndexed { index, i ->
                val newSpanInfoCreator = if (index == searchQueryPositionIndex) {
                    SyntaxRuleStyle.SEARCH_MATCH_SELECTED
                } else {
                    SyntaxRuleStyle.SEARCH_MATCHES
                }
                newSpanInfoCreator.createSpan(activity, i, syntaxItem).apply {
                    start = i
                    end = i + searchQueryLength
                }
            }
            .toList()
    }

    fun setSearchQueryInfo(searchQueryPositions: List<Int>?, searchQueryLength: Int, searchQueryPositionIndex: Int) {
        this.searchQueryPositions = searchQueryPositions
        this.searchQueryLength = searchQueryLength
        this.searchQueryPositionIndex = searchQueryPositionIndex
        activity.lifecycleScope.launch {
            runHighlightTasks(0)
        }
    }

    fun clearSearchQueryInfo() {
        setSearchQueryInfo(null, 0, 0)
    }

    fun cleanup() {
        textBox.text.clearSpans()
    }

    private suspend fun highlightOnScroll() {
        scrollView?.let {
            if (lastScrollY != it.scrollY) {
                lastScrollY = it.scrollY
                runHighlightTasks(0)
            }
            delay(highlightDelayMillis)
            highlightOnScroll()
        }
    }

    companion object {
        const val HIGHLIGHT_DELAY_MILLIS = 500L
    }
}
