package org.wikipedia.page

import org.wikipedia.analytics.eventplatform.ArticleFindInPageInteractionEvent
import org.wikipedia.analytics.metricsplatform.ArticleFindInPageInteraction
import org.wikipedia.views.FindInPageActionProvider
import org.wikipedia.views.FindInPageActionProvider.FindInPageListener

class FindInWebPageActionProvider(private val fragment: PageFragment,
                                  private val articleFindInPageInteractionEvent: ArticleFindInPageInteractionEvent,
                                  private val articleFindInPageInteractionEventMetricsPlatform: ArticleFindInPageInteraction) :
        FindInPageActionProvider(fragment.requireContext()), FindInPageListener {

    private var searchQuery: String? = null

    init {
        listener = this
        enableLastOccurrenceSearchFlag = true
    }

    private fun findInPage(text: String) {
        fragment.webView.setFindListener { activeMatchOrdinal: Int, numberOfMatches: Int, isDoneCounting: Boolean ->
            if (!isDoneCounting) {
                return@setFindListener
            }
            setMatchesResults(activeMatchOrdinal, numberOfMatches)
        }
        fragment.webView.findAllAsync(text)
    }

    override fun onFindNextClicked() {
        articleFindInPageInteractionEvent.addFindNext()
        articleFindInPageInteractionEventMetricsPlatform.addFindNext()
        fragment.webView.findNext(true)
    }

    override fun onFindNextLongClicked() {
        // Go to the last match by going to the first one and then going one back.
        articleFindInPageInteractionEvent.addFindPrev()
        articleFindInPageInteractionEventMetricsPlatform.addFindPrev()
        fragment.webView.clearMatches()
        searchQuery?.let {
            fragment.webView.findAllAsync(it)
        }
    }

    override fun onFindPrevClicked() {
        articleFindInPageInteractionEvent.addFindPrev()
        articleFindInPageInteractionEventMetricsPlatform.addFindPrev()
        fragment.webView.findNext(false)
    }

    override fun onFindPrevLongClicked() {
        // Go to the first match by "restarting" the search.
        articleFindInPageInteractionEvent.addFindNext()
        articleFindInPageInteractionEventMetricsPlatform.addFindNext()
        fragment.webView.clearMatches()
        searchQuery?.let {
            fragment.webView.findAllAsync(it)
        }
    }

    override fun onCloseClicked() {
        fragment.callback()?.onPageCloseActionMode()
    }

    override fun onSearchTextChanged(text: String?) {
        articleFindInPageInteractionEvent.findText = text.orEmpty()
        articleFindInPageInteractionEventMetricsPlatform.findText = text.orEmpty()
        if (!text.isNullOrEmpty()) {
            searchQuery = text
            findInPage(text)
        } else {
            searchQuery = null
            fragment.webView.clearMatches()
        }
    }
}
