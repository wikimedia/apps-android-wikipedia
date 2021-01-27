package org.wikipedia.edit

import android.graphics.Rect
import android.view.ActionMode
import android.view.View
import android.widget.ScrollView
import org.wikipedia.edit.richtext.SyntaxHighlighter
import org.wikipedia.util.DimenUtil
import org.wikipedia.views.FindInPageActionProvider
import org.wikipedia.views.FindInPageActionProvider.FindInPageListener
import org.wikipedia.views.PlainPasteEditText

class FindInEditorActionProvider(private val scrollView: ScrollView,
                                 private val textView: PlainPasteEditText,
                                 private val syntaxHighlighter: SyntaxHighlighter,
                                 private val actionMode: ActionMode) : FindInPageActionProvider(textView.context), FindInPageListener {
    private var searchQuery: String? = null
    override fun onCreateActionView(): View {
        val view = super.onCreateActionView()
        setSearchViewQuery((textView.tag as String))
        return view
    }

    fun findInPage(s: String?) {
        textView.setFindListener { activeMatchOrdinal: Int, numberOfMatches: Int, textPosition: Int, findingNext: Boolean ->
            setMatchesResults(activeMatchOrdinal, numberOfMatches)
            textView.setSelection(textPosition, textPosition + s!!.length)
            val r = Rect()
            textView.getFocusedRect(r)
            val scrollTopOffset = 32
            scrollView.scrollTo(0, r.top - DimenUtil.roundedDpToPx(scrollTopOffset.toFloat()))
            if (findingNext) {
                textView.requestFocus()
            }
        }
        textView.findInEditor(s, syntaxHighlighter)
    }

    override fun onFindNextClicked() {
        textView.findNext()
    }

    override fun onFindNextLongClicked() {
        textView.findFirstOrLast(false)
    }

    override fun onFindPrevClicked() {
        textView.findPrevious()
    }

    override fun onFindPrevLongClicked() {
        textView.findFirstOrLast(true)
    }

    override fun onCloseClicked() {
        textView.tag = searchQuery
        actionMode.finish()
    }

    override fun onSearchTextChanged(text: String?) {
        if (text!!.length > 0) {
            findInPage(text)
        } else {
            textView.clearMatches(syntaxHighlighter)
            syntaxHighlighter.applyFindTextSyntax(text, null)
        }
        searchQuery = text
    }

    init {
        listener = this
    }
}