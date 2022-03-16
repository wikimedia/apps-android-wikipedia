package org.wikipedia.edit.richtext

import android.content.Context
import android.text.Spanned
import android.widget.EditText
import androidx.core.text.getSpans
import androidx.core.widget.doAfterTextChanged
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
import org.wikipedia.util.log.L
import java.util.*
import java.util.concurrent.Callable
import java.util.concurrent.TimeUnit

class SyntaxHighlighter(
    private var context: Context,
    private val textBox: EditText,
    private var syntaxHighlightListener: OnSyntaxHighlightListener? = null
) {
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
    private val disposables = CompositeDisposable()
    private var currentHighlightTask: SyntaxHighlightTask? = null

    init {
        textBox.doAfterTextChanged { runHighlightTasks(1000) }
    }

    private fun runHighlightTasks(delayMillis: Long) {
        currentHighlightTask?.cancel()
        currentHighlightTask = SyntaxHighlightTask(textBox.text)
        disposables.clear()
        disposables.add(Observable.timer(delayMillis, TimeUnit.MILLISECONDS)
                .flatMap {
                    Observable.zip<MutableList<SpanExtents>, List<SpanExtents>, List<SpanExtents>>(Observable.fromCallable(currentHighlightTask!!),
                            if (searchText.isNullOrEmpty()) Observable.just(emptyList())
                            else Observable.fromCallable(SyntaxHighlightSearchMatchesTask(textBox.text, searchText!!, selectedMatchResultPosition))) { f, s ->
                        f.addAll(s)
                        f
                    }
                }
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ result ->
                    syntaxHighlightListener?.syntaxHighlightResults(result)

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
                    L.d("Took $time ms to remove ${oldSpans.size} spans and add ${newSpans.size} new.")
                }) { L.e(it) })
    }

    fun applyFindTextSyntax(searchText: String?, listener: OnSyntaxHighlightListener?) {
        this.searchText = searchText
        syntaxHighlightListener = listener
        setSelectedMatchResultPosition(0)
        runHighlightTasks(500)
    }

    fun setSelectedMatchResultPosition(selectedMatchResultPosition: Int) {
        this.selectedMatchResultPosition = selectedMatchResultPosition
        runHighlightTasks(0)
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
                var incrementDone = false
                for (syntaxItem in syntaxRules) {
                    if (i + syntaxItem.startChars.size > textChars.size) {
                        continue
                    }
                    if (syntaxItem.isStartEndSame) {
                        var pass = true
                        for (j in 0 until syntaxItem.startChars.size) {
                            if (textChars[i + j] != syntaxItem.startChars[j]) {
                                pass = false
                                break
                            }
                        }
                        if (pass) {
                            if (spanStack.size > 0 && spanStack.peek().syntaxRule == syntaxItem) {
                                newSpanInfo = spanStack.pop()
                                newSpanInfo.end = i + syntaxItem.startChars.size
                                spansToSet.add(newSpanInfo)
                            } else {
                                val sp = syntaxItem.spanStyle.createSpan(context, i, syntaxItem)
                                spanStack.push(sp)
                            }
                            i += syntaxItem.startChars.size
                            incrementDone = true
                        }
                    } else {
                        var pass = true
                        for (j in 0 until syntaxItem.startChars.size) {
                            if (textChars[i + j] != syntaxItem.startChars[j]) {
                                pass = false
                                break
                            }
                        }
                        if (pass) {
                            val sp = syntaxItem.spanStyle.createSpan(context, i, syntaxItem)
                            spanStack.push(sp)
                            i += syntaxItem.startChars.size
                            incrementDone = true
                        }
                        // skip the check of end symbol when start symbol is found at end of the text
                        if (i + syntaxItem.startChars.size > textChars.size) {
                            continue
                        }
                        pass = true
                        for (j in 0 until syntaxItem.endChars.size) {
                            if (textChars[i + j] != syntaxItem.endChars[j]) {
                                pass = false
                                break
                            }
                        }
                        if (pass) {
                            if (spanStack.size > 0 && spanStack.peek().syntaxRule == syntaxItem) {
                                newSpanInfo = spanStack.pop()
                                newSpanInfo.end = i + syntaxItem.endChars.size
                                spansToSet.add(newSpanInfo)
                            }
                            i += syntaxItem.endChars.size
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

    private inner class SyntaxHighlightSearchMatchesTask constructor(text: CharSequence, searchText: String, private val selectedMatchResultPosition: Int) : Callable<List<SpanExtents>> {
        private val searchText = searchText.lowercase(Locale.getDefault())
        private val text = text.toString().lowercase(Locale.getDefault())

        override fun call(): List<SpanExtents> {
            val spansToSet = mutableListOf<SpanExtents>()
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
