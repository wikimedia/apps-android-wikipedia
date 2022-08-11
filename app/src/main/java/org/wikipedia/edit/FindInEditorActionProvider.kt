package org.wikipedia.edit

import android.graphics.Rect
import android.view.ActionMode
import android.view.MenuItem
import android.view.View
import org.wikipedia.edit.richtext.SpanExtents
import org.wikipedia.edit.richtext.SyntaxHighlighter
import org.wikipedia.util.DeviceUtil
import org.wikipedia.util.DimenUtil
import org.wikipedia.views.FindInPageActionProvider
import org.wikipedia.views.FindInPageActionProvider.FindInPageListener

class FindInEditorActionProvider(private val scrollView: View,
                                 private val textView: SyntaxHighlightableEditText,
                                 private val syntaxHighlighter: SyntaxHighlighter,
                                 private val actionMode: ActionMode) : FindInPageActionProvider(textView.context), FindInPageListener {

    private val resultPositions = mutableListOf<Int>()
    private var currentResultIndex = 0
    private var searchQuery: String? = null

    init {
        listener = this
    }

    override fun onCreateActionView(menuItem: MenuItem): View {
        val view = super.onCreateActionView(menuItem)
        textView.tag?.let {
            setSearchViewQuery(it as String)
            DeviceUtil.showSoftKeyboard(view)
        }
        return view
    }

    override fun onFindNextClicked() {
        currentResultIndex = if (currentResultIndex == resultPositions.size - 1) 0 else ++currentResultIndex
        scrollAndHighlightCurrentResult()
    }

    override fun onFindNextLongClicked() {
        currentResultIndex = resultPositions.size - 1
        scrollAndHighlightCurrentResult()
    }

    override fun onFindPrevClicked() {
        currentResultIndex = if (currentResultIndex == 0) resultPositions.size - 1 else --currentResultIndex
        scrollAndHighlightCurrentResult()
    }

    override fun onFindPrevLongClicked() {
        currentResultIndex = 0
        scrollAndHighlightCurrentResult()
    }

    override fun onCloseClicked() {
        textView.tag = searchQuery
        actionMode.finish()
    }

    override fun onSearchTextChanged(text: String?) {
        searchQuery = if (text.isNullOrEmpty()) null else text
        currentResultIndex = 0
        syntaxHighlighter.applyFindTextSyntax(searchQuery, object : SyntaxHighlighter.OnFindTextListener {
            override fun findTextMatches(spanExtents: List<SpanExtents>) {
                resultPositions.clear()
                resultPositions.addAll(spanExtents.map { textView.text.getSpanStart(it) })
                scrollToCurrentResult()
            }
        })
    }

    private fun scrollAndHighlightCurrentResult() {
        scrollToCurrentResult()
        textView.requestFocus()
        syntaxHighlighter.setSelectedMatchResultPosition(searchQuery, currentResultIndex)
    }

    private fun scrollToCurrentResult() {
        setMatchesResults(currentResultIndex, resultPositions.size)
        val textPosition = if (resultPositions.isEmpty()) 0 else resultPositions[currentResultIndex]
        textView.setSelection(textPosition, textPosition + searchQuery.orEmpty().length)
        val r = Rect()
        textView.getFocusedRect(r)
        scrollView.scrollTo(0, r.top - DimenUtil.roundedDpToPx(32f))
    }
}
