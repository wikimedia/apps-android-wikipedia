package org.wikipedia.page

import android.webkit.WebView
import org.wikipedia.analytics.FindInPageFunnel
import org.wikipedia.views.FindInPageActionProvider
import org.wikipedia.views.FindInPageActionProvider.FindInPageListener

class FindInWebPageActionProvider(private val fragment: PageFragment,
                                  private val funnel: FindInPageFunnel) : FindInPageActionProvider(fragment.requireContext()), FindInPageListener {
    private var searchQuery: String? = null
    fun findInPage(s: String?) {
        fragment.getWebView().setFindListener(WebView.FindListener { activeMatchOrdinal: Int, numberOfMatches: Int, isDoneCounting: Boolean ->
            if (!isDoneCounting) {
                return@setFindListener
            }
            setMatchesResults(activeMatchOrdinal, numberOfMatches)
        })
        fragment.getWebView().findAllAsync(s)
    }

    override fun onFindNextClicked() {
        funnel.addFindNext()
        fragment.getWebView().findNext(true)
    }

    override fun onFindNextLongClicked() {
        // Go to the last match by going to the first one and then going one back.
        funnel.addFindPrev()
        fragment.getWebView().clearMatches()
        fragment.getWebView().findAllAsync(searchQuery)
    }

    override fun onFindPrevClicked() {
        funnel.addFindPrev()
        fragment.getWebView().findNext(false)
    }

    override fun onFindPrevLongClicked() {
        // Go to the first match by "restarting" the search.
        funnel.addFindNext()
        fragment.getWebView().clearMatches()
        fragment.getWebView().findAllAsync(searchQuery)
    }

    override fun onCloseClicked() {
        fragment.callback()!!.onPageCloseActionMode()
    }

    override fun onSearchTextChanged(text: String?) {
        funnel.setFindText(text)
        if (text!!.length > 0) {
            searchQuery = text
            findInPage(text)
        } else {
            searchQuery = null
            fragment.getWebView().clearMatches()
        }
    }

    init {
        listener = this
        enableLastOccurrenceSearchFlag = true
    }
}