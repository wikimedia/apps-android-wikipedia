package org.wikipedia.edit

import android.graphics.Rect
import android.view.ActionMode
import android.view.MenuItem
import android.view.View
import org.wikipedia.edit.richtext.SyntaxHighlighter
import org.wikipedia.util.DeviceUtil
import org.wikipedia.util.DimenUtil
import org.wikipedia.util.StringUtil
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
        scrollToCurrentResult()
    }

    override fun onFindNextLongClicked() {
        currentResultIndex = resultPositions.size - 1
        scrollToCurrentResult()
    }

    override fun onFindPrevClicked() {
        currentResultIndex = if (currentResultIndex == 0) resultPositions.size - 1 else --currentResultIndex
        scrollToCurrentResult()
    }

    override fun onFindPrevLongClicked() {
        currentResultIndex = 0
        scrollToCurrentResult()
    }

    override fun onCloseClicked() {
        textView.tag = searchQuery
        actionMode.finish()
    }

    override fun onSearchTextChanged(text: String?) {
        searchQuery = text?.ifEmpty { null }
        currentResultIndex = 0
        resultPositions.clear()

        searchQuery?.let { query ->
            resultPositions += query.toRegex(StringUtil.SEARCH_REGEX_OPTIONS).findAll(textView.text)
                .map { it.range.first }
        }
        scrollToCurrentResult()
    }

    private fun scrollToCurrentResult() {
        setMatchesResults(currentResultIndex, resultPositions.size)
        var highlightLength = searchQuery.orEmpty().length
        val textPosition = resultPositions.getOrElse(currentResultIndex) {
            highlightLength = 0
            0
        }
        textView.setSelection(textPosition, textPosition + highlightLength)
        val r = Rect()
        textView.getFocusedRect(r)
        scrollView.scrollTo(0, r.top - DimenUtil.roundedDpToPx(32f))
        syntaxHighlighter.setSearchQueryInfo(resultPositions, highlightLength, currentResultIndex)
    }
}
