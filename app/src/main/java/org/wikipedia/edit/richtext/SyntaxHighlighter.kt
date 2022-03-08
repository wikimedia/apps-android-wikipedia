package org.wikipedia.edit.richtext

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.text.Spanned
import android.widget.EditText
import androidx.core.text.getSpans
import androidx.core.widget.doOnTextChanged
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
import org.wikipedia.util.log.L
import java.util.*
import java.util.concurrent.Callable
import java.util.concurrent.TimeUnit

class SyntaxHighlighter(private var context: Context, val textBox: EditText, var syntaxHighlightListener: OnSyntaxHighlightListener?) {
    constructor(context: Context, textBox: EditText) : this(context, textBox, null)

    interface OnSyntaxHighlightListener {
        fun syntaxHighlightResults(spanExtents: List<SpanExtents>)
        fun findTextMatches(spanExtents: List<SpanExtents>)
    }

    private val syntaxRules = listOf(
            SyntaxRule("{{", "}}", SyntaxRuleStyle.TEMPLATE),
            SyntaxRule("[[", "]]", SyntaxRuleStyle.INTERNAL_LINK),
            SyntaxRule("[", "]", SyntaxRuleStyle.EXTERNAL_LINK),
            SyntaxRule("<", ">", SyntaxRuleStyle.REF),
            SyntaxRule("'''''", "'''''", SyntaxRuleStyle.BOLD_ITALIC),
            SyntaxRule("'''", "'''", SyntaxRuleStyle.BOLD),
            SyntaxRule("''", "''", SyntaxRuleStyle.ITALIC)
    )

    private var searchText: String? = null
    private var selectedMatchResultPosition = 0
    private val handler = Handler(Looper.getMainLooper())
    private val disposables = CompositeDisposable()
    private var currentHighlightTask: SyntaxHighlightTask? = null

    init {
        textBox.doOnTextChanged { text, start, before, count ->
            if (text != null) {
                L.d(">>>> $start, $before, $count")
                runHighlightTasks(text, start, before, count)
            }
        }
    }

    private fun runHighlightTasks(text: CharSequence, cursorStart: Int, cursorBeforeCount: Int, cursorAfterCount: Int) {
        currentHighlightTask?.cancel()
        currentHighlightTask = SyntaxHighlightTask(textBox.text)
        val searchTask = SyntaxHighlightSearchMatchesTask(textBox.text, searchText, selectedMatchResultPosition)
        disposables.clear()
        disposables.add(Observable.just(Unit)
                .delay(1000, TimeUnit.MILLISECONDS)
                .flatMap {
                    Observable.zip<MutableList<SpanExtents>, List<SpanExtents>, List<SpanExtents>>(Observable.fromCallable(currentHighlightTask!!),
                            Observable.fromCallable(searchTask!!)) { f, s ->
                        f.addAll(s)
                        f
                    }
                }
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ result ->
                    syntaxHighlightListener?.syntaxHighlightResults(result)

                    var time = System.currentTimeMillis()
                    val afterDiff = cursorAfterCount - cursorBeforeCount
                    val prevSpans = textBox.text.getSpans<SpanExtents>()
                    val resultDupes = mutableListOf<SpanExtents>()

                    val dupes = prevSpans.filter { item ->

                        if (item.start > cursorStart) { item.start += afterDiff }
                        if (item.end > cursorStart) { item.end += afterDiff }

                        val r = result.find {
                            //if (it.start > cursorStart && item.start > cursorStart) {
                            //    it.start == item.start + afterDiff && it.end == item.end + afterDiff && it.syntaxRule == item.syntaxRule
                            //} else {
                                it.start == item.start && it.end == item.end && it.syntaxRule == item.syntaxRule
                            //}
                        }
                        if (r != null) {
                            resultDupes.add(r)
                        }
                        r != null
                    }

                    val oldSpans = prevSpans.toMutableList()
                    oldSpans.removeAll(dupes)

                    val newSpans = result.toMutableList()
                    newSpans.removeAll(resultDupes)


                    for (sp in oldSpans) {
                        textBox.text.removeSpan(sp)
                    }
                    val findTextList = newSpans
                            .onEach { textBox.text.setSpan(it, it.start, it.end, Spanned.SPAN_INCLUSIVE_INCLUSIVE) }
                            .filter { it.syntaxRule.spanStyle == SyntaxRuleStyle.SEARCH_MATCHES } // and add our new spans

                    if (!searchText.isNullOrEmpty()) {
                        syntaxHighlightListener?.findTextMatches(findTextList)
                    }
                    time = System.currentTimeMillis() - time
                    L.d("That took " + time + "ms")
                }) { L.e(it) })
    }

    fun applyFindTextSyntax(searchText: String?, listener: OnSyntaxHighlightListener?) {
        this.searchText = searchText
        syntaxHighlightListener = listener
        setSelectedMatchResultPosition(0)
        runHighlightTasks(textBox.text, 0, 0, 0)
    }

    fun setSelectedMatchResultPosition(selectedMatchResultPosition: Int) {
        this.selectedMatchResultPosition = selectedMatchResultPosition
        runHighlightTasks(textBox.text, 0, 0, 0)
    }

    fun cleanup() {
        disposables.clear()
        textBox.text.clearSpans()
    }

    private inner class SyntaxHighlightTask constructor(private val text: CharSequence) : Callable<MutableList<SpanExtents>> {
        private var cancelled = false

        fun cancel() {
            cancelled = true
        }

        override fun call(): MutableList<SpanExtents> {
            L.d(">>>> SyntaxHighlightTask called.")
            val spanStack = Stack<SpanExtents>()
            val spansToSet = mutableListOf<SpanExtents>()

            /*
            The (naïve) algorithm:
            Iterate through the text string, and maintain a stack of matched syntax rules.
            When the "start" and "end" symbol of a rule are matched in sequence, create a new
            Span to be added to the EditText at the corresponding location.
             */
            var i = 0
            while (i < text.length) {
                var newSpanInfo: SpanExtents
                var incrementDone = false
                for (syntaxItem in syntaxRules) {
                    if (i + syntaxItem.startSymbol.length > text.length) {
                        continue
                    }
                    if (syntaxItem.isStartEndSame) {
                        var pass = true
                        for (j in syntaxItem.startSymbol.indices) {
                            if (text[i + j] != syntaxItem.startSymbol[j]) {
                                pass = false
                                break
                            }
                        }
                        if (pass) {
                            if (spanStack.size > 0 && spanStack.peek().syntaxRule == syntaxItem) {
                                newSpanInfo = spanStack.pop()
                                newSpanInfo.end = i + syntaxItem.startSymbol.length
                                spansToSet.add(newSpanInfo)
                            } else {
                                val sp = syntaxItem.spanStyle.createSpan(context, i, syntaxItem)
                                spanStack.push(sp)
                            }
                            i += syntaxItem.startSymbol.length
                            incrementDone = true
                        }
                    } else {
                        var pass = true
                        for (j in syntaxItem.startSymbol.indices) {
                            if (text[i + j] != syntaxItem.startSymbol[j]) {
                                pass = false
                                break
                            }
                        }
                        if (pass) {
                            val sp = syntaxItem.spanStyle.createSpan(context, i, syntaxItem)
                            spanStack.push(sp)
                            i += syntaxItem.startSymbol.length
                            incrementDone = true
                        }
                        // skip the check of end symbol when start symbol is found at end of the text
                        if (i + syntaxItem.startSymbol.length > text.length) {
                            continue
                        }
                        pass = true
                        for (j in syntaxItem.endSymbol.indices) {
                            if (text[i + j] != syntaxItem.endSymbol[j]) {
                                pass = false
                                break
                            }
                        }
                        if (pass) {
                            if (spanStack.size > 0 && spanStack.peek().syntaxRule == syntaxItem) {
                                newSpanInfo = spanStack.pop()
                                newSpanInfo.end = i + syntaxItem.endSymbol.length
                                spansToSet.add(newSpanInfo)
                            }
                            i += syntaxItem.endSymbol.length
                            incrementDone = true
                        }
                    }
                }
                if (cancelled) {
                    break
                }
                if (!incrementDone) {
                    i++
                }
            }
            return spansToSet
        }
    }

    private inner class SyntaxHighlightSearchMatchesTask constructor(text: CharSequence, searchText: String?, private val selectedMatchResultPosition: Int) : Callable<List<SpanExtents>> {
        private val searchText = searchText.orEmpty().lowercase(Locale.getDefault())
        private val text = text.toString().lowercase(Locale.getDefault())

        override fun call(): List<SpanExtents> {
            val spansToSet = mutableListOf<SpanExtents>()
            if (searchText.isEmpty()) {
                return spansToSet
            }
            val syntaxItem = SyntaxRule("", "", SyntaxRuleStyle.SEARCH_MATCHES)
            var position = 0
            var matches = 0
            do {
                position = text.indexOf(searchText, position)
                if (position >= 0) {
                    val newSpanInfo = if (matches == selectedMatchResultPosition) {
                        SyntaxRuleStyle.SEARCH_MATCH_SELECTED.createSpan(context, position, syntaxItem)
                    } else {
                        SyntaxRuleStyle.SEARCH_MATCHES.createSpan(context, position, syntaxItem)
                    }
                    newSpanInfo.start = position
                    newSpanInfo.end = position + searchText.length
                    spansToSet.add(newSpanInfo)
                    position += searchText.length
                    matches++
                }
            } while (position >= 0)
            return spansToSet
        }
    }
}
