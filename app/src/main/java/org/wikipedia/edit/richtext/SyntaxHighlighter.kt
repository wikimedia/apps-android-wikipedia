package org.wikipedia.edit.richtext

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.Spanned
import android.text.format.DateUtils
import android.widget.EditText
import androidx.core.widget.doAfterTextChanged
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
import org.wikipedia.util.log.L
import java.util.*
import java.util.concurrent.Callable

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

    init {
        // add a text-change listener that will trigger syntax highlighting
        // whenever text is modified.
        textBox.doAfterTextChanged { postHighlightCallback() }
    }

    private val syntaxHighlightCallback: Runnable = object : Runnable {
        private var currentTask: SyntaxHighlightTask? = null
        private var searchTask: SyntaxHighlightSearchMatchesTask? = null

        override fun run() {
            currentTask?.cancel()
            currentTask = SyntaxHighlightTask(textBox.text)
            searchTask = SyntaxHighlightSearchMatchesTask(textBox.text, searchText, selectedMatchResultPosition)
            disposables.clear()
            disposables.add(Observable.zip<MutableList<SpanExtents>, List<SpanExtents>, List<SpanExtents>>(Observable.fromCallable(currentTask),
                    Observable.fromCallable(searchTask), { f, s ->
                        f.addAll(s)
                        f
                    })
                    .subscribeOn(Schedulers.computation())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({ result ->
                        syntaxHighlightListener?.syntaxHighlightResults(result)

                        // TODO: probably possible to make this more efficient...
                        // Right now, on longer articles, this is quite heavy on the UI thread.
                        // remove any of our custom spans from the previous cycle...
                        var time = System.currentTimeMillis()
                        val prevSpans = textBox.text.getSpans(0, textBox.text.length, SpanExtents::class.java)
                        for (sp in prevSpans) {
                            textBox.text.removeSpan(sp)
                        }
                        val findTextList = result
                                .onEach { textBox.text.setSpan(it, it.start, it.end, Spanned.SPAN_INCLUSIVE_INCLUSIVE) }
                                .filter { it.syntaxRule.spanStyle == SyntaxRuleStyle.SEARCH_MATCHES } // and add our new spans

                        if (!searchText.isNullOrEmpty()) {
                            syntaxHighlightListener?.findTextMatches(findTextList)
                        }
                        time = System.currentTimeMillis() - time
                        L.d("That took " + time + "ms")
                    }) { L.e(it) })
        }
    }

    fun applyFindTextSyntax(searchText: String?, listener: OnSyntaxHighlightListener?) {
        this.searchText = searchText
        syntaxHighlightListener = listener
        setSelectedMatchResultPosition(0)
        postHighlightCallback()
    }

    fun setSelectedMatchResultPosition(selectedMatchResultPosition: Int) {
        this.selectedMatchResultPosition = selectedMatchResultPosition
        postHighlightCallback()
    }

    private fun postHighlightCallback() {
        // queue up syntax highlighting.
        // if the user adds more text within 1/2 second, the previous request
        // is cancelled, and a new one is placed.
        handler.removeCallbacks(syntaxHighlightCallback)
        handler.postDelayed(syntaxHighlightCallback, if (searchText.isNullOrEmpty()) DateUtils.SECOND_IN_MILLIS / 2 else 0)
    }

    fun cleanup() {
        handler.removeCallbacks(syntaxHighlightCallback)
        textBox.text.clearSpans()
        disposables.clear()
    }

    private inner class SyntaxHighlightTask constructor(private val text: Editable) : Callable<MutableList<SpanExtents>> {
        private var cancelled = false

        fun cancel() {
            cancelled = true
        }

        override fun call(): MutableList<SpanExtents> {
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

    private inner class SyntaxHighlightSearchMatchesTask constructor(text: Editable, searchText: String?, private val selectedMatchResultPosition: Int) : Callable<List<SpanExtents>> {
        private val searchText = searchText.orEmpty().toLowerCase(Locale.getDefault())
        private val text = text.toString().toLowerCase(Locale.getDefault())

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
