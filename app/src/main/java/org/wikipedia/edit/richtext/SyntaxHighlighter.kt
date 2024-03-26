package org.wikipedia.edit.richtext

import android.content.Context
import android.os.Build
import android.text.Spanned
import androidx.core.text.getSpans
import androidx.core.widget.NestedScrollView
import androidx.core.widget.doAfterTextChanged
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
import org.wikipedia.edit.SyntaxHighlightableEditText
import org.wikipedia.util.log.L
import java.util.*
import java.util.concurrent.Callable
import java.util.concurrent.TimeUnit

class SyntaxHighlighter(
    private var context: Context,
    private val textBox: SyntaxHighlightableEditText,
    private val scrollView: NestedScrollView?,
    private val highlightDelayMillis: Long = HIGHLIGHT_DELAY_MILLIS) {

    private val syntaxRules = listOf(
            SyntaxRule("{{{", "}}}", SyntaxRuleStyle.PRE_TEMPLATE),
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

    private val disposables = CompositeDisposable()
    private var currentHighlightTask: SyntaxHighlightTask? = null
    private var lastScrollY = -1
    private val highlightOnScrollRunnable = Runnable { postHighlightOnScroll() }

    private var searchQueryPositions: List<Int>? = null
    private var searchQueryLength = 0
    private var searchQueryPositionIndex = 0

    var enabled = true
        set(value) {
            field = value
            if (!value) {
                currentHighlightTask?.cancel()
                disposables.clear()
                textBox.text.getSpans<SpanExtents>().forEach { textBox.text.removeSpan(it) }
            } else {
                runHighlightTasks(highlightDelayMillis)
            }
        }

    init {
        textBox.doAfterTextChanged { runHighlightTasks(highlightDelayMillis * 2) }
        textBox.scrollView = scrollView
        postHighlightOnScroll()
    }

    private fun runHighlightTasks(delayMillis: Long) {

        currentHighlightTask?.cancel()
        disposables.clear()
        if (!enabled) {
            return
        }
        disposables.add(Observable.timer(delayMillis, TimeUnit.MILLISECONDS)
                .flatMap {
                    if (textBox.layout == null) {
                        throw IllegalArgumentException()
                    }

                    var firstVisibleLine = if (scrollView != null) textBox.layout.getLineForVertical(scrollView.scrollY) else 0
                    if (firstVisibleLine < 0) firstVisibleLine = 0

                    var lastVisibleLine = if (scrollView != null) textBox.layout.getLineForVertical(scrollView.scrollY + scrollView.height) else textBox.layout.lineCount - 1
                    if (lastVisibleLine < firstVisibleLine) lastVisibleLine = firstVisibleLine
                    else if (lastVisibleLine >= textBox.lineCount) lastVisibleLine = textBox.lineCount - 1

                    val firstVisibleIndex = textBox.layout.getLineStart(firstVisibleLine)
                    val lastVisibleIndex = textBox.layout.getLineEnd(lastVisibleLine)

                    val textToHighlight = textBox.text.substring(firstVisibleIndex, lastVisibleIndex)
                    currentHighlightTask = SyntaxHighlightTask(textToHighlight, firstVisibleIndex)

                    Observable.zip<MutableList<SpanExtents>, List<SpanExtents>, List<SpanExtents>>(Observable.fromCallable(currentHighlightTask!!),
                            if (searchQueryPositions.isNullOrEmpty()) Observable.just(emptyList())
                            else Observable.fromCallable(SyntaxHighlightSearchMatchesTask(firstVisibleIndex, textToHighlight.length))) { f, s ->
                        f.addAll(s)
                        f
                    }
                }
                .retry(10)
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ result ->
                    textBox.enqueueNoScrollingLayoutChange()

                    var time = System.currentTimeMillis()
                    val oldSpans = textBox.text.getSpans<SpanExtents>().toMutableList()
                    val newSpans = result.toMutableList()

                    val dupes = oldSpans.filter { item ->
                        val r = result.find {
                            it.start == textBox.text.getSpanStart(item) && it.end == textBox.text.getSpanEnd(item) && it.syntaxRule == item.syntaxRule
                        }
                        if (r != null) {
                            newSpans.remove(r)
                        }
                        r != null
                    }
                    oldSpans.removeAll(dupes)

                    oldSpans.forEach { textBox.text.removeSpan(it) }
                    newSpans.forEach { textBox.text.setSpan(it, it.start, it.end, Spanned.SPAN_INCLUSIVE_INCLUSIVE) }

                    time = System.currentTimeMillis() - time
                    L.d("Took $time ms to remove ${oldSpans.size} spans and add ${newSpans.size} new.")
                }) { L.e(it) })
    }

    fun setSearchQueryInfo(searchQueryPositions: List<Int>?, searchQueryLength: Int, searchQueryPositionIndex: Int) {
        this.searchQueryPositions = searchQueryPositions
        this.searchQueryLength = searchQueryLength
        this.searchQueryPositionIndex = searchQueryPositionIndex
        runHighlightTasks(0)
    }

    fun clearSearchQueryInfo() {
        setSearchQueryInfo(null, 0, 0)
    }

    fun cleanup() {
        scrollView?.removeCallbacks(highlightOnScrollRunnable)
        disposables.clear()
        textBox.text.clearSpans()
    }

    private fun postHighlightOnScroll() {
        scrollView?.let {
            if (lastScrollY != it.scrollY) {
                lastScrollY = it.scrollY
                runHighlightTasks(0)
            }
            it.postDelayed(highlightOnScrollRunnable, highlightDelayMillis)
        }
    }

    private inner class SyntaxHighlightTask constructor(private val text: CharSequence, private val startOffset: Int) : Callable<MutableList<SpanExtents>> {
        private var cancelled = false

        fun cancel() {
            cancelled = true
        }

        override fun call(): MutableList<SpanExtents> {
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
                            val sp = rule.spanStyle.createSpan(context, i, rule)
                            spanStack.push(sp)
                            i += rule.startChars.size - 1
                            break
                        }
                    }
                    if (cancelled) {
                        break
                    }
                }

                i++
            }
            spansToSet.forEach {
                it.start += startOffset
                it.end += startOffset
            }
            spansToSet.sortWith { a, b -> a.syntaxRule.spanStyle.compareTo(b.syntaxRule.spanStyle) }
            return spansToSet
        }
    }

    private inner class SyntaxHighlightSearchMatchesTask constructor(private val startOffset: Int, private val textLength: Int) : Callable<List<SpanExtents>> {
        override fun call(): List<SpanExtents> {
            val spansToSet = mutableListOf<SpanExtents>()
            val syntaxItem = SyntaxRule("", "", SyntaxRuleStyle.SEARCH_MATCHES)

            searchQueryPositions?.let {
                for (i in it.indices) {
                    if (it[i] >= startOffset && it[i] < startOffset + textLength) {
                        val newSpanInfo = if (i == searchQueryPositionIndex) {
                            SyntaxRuleStyle.SEARCH_MATCH_SELECTED.createSpan(context, it[i], syntaxItem)
                        } else {
                            SyntaxRuleStyle.SEARCH_MATCHES.createSpan(context, it[i], syntaxItem)
                        }
                        newSpanInfo.start = it[i]
                        newSpanInfo.end = it[i] + searchQueryLength
                        spansToSet.add(newSpanInfo)
                    }
                }
            }
            return spansToSet
        }
    }

    companion object {
        const val HIGHLIGHT_DELAY_MILLIS = 500L
    }
}
