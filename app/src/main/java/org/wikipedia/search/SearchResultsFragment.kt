package org.wikipedia.search

import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnLongClickListener
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.ListView
import android.widget.TextView
import androidx.collection.LruCache
import androidx.fragment.app.Fragment
import butterknife.BindView
import butterknife.ButterKnife
import butterknife.OnClick
import butterknife.Unbinder
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import org.wikipedia.Constants.InvokeSource
import org.wikipedia.LongPressHandler
import org.wikipedia.LongPressHandler.ListViewOverflowMenuListener
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.activity.FragmentUtil
import org.wikipedia.analytics.SearchFunnel
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.mwapi.MwQueryResponse
import org.wikipedia.history.HistoryEntry
import org.wikipedia.page.PageTitle
import org.wikipedia.util.L10nUtil
import org.wikipedia.util.StringUtil
import org.wikipedia.views.GoneIfEmptyTextView
import org.wikipedia.views.ViewUtil
import org.wikipedia.views.WikiErrorView
import java.util.*

class SearchResultsFragment : Fragment() {
    interface Callback {
        fun onSearchResultCopyLink(title: PageTitle)
        fun onSearchResultAddToList(title: PageTitle, source: InvokeSource)
        fun onSearchResultShareLink(title: PageTitle)
        fun onSearchProgressBar(enabled: Boolean)
        fun navigateToTitle(item: PageTitle, inNewTab: Boolean, position: Int)
        fun setSearchText(text: CharSequence)
        val funnel: SearchFunnel
    }

    @JvmField
    @BindView(R.id.search_results_display)
    var searchResultsDisplay: View? = null

    @JvmField
    @BindView(R.id.search_results_container)
    var searchResultsContainer: View? = null

    @JvmField
    @BindView(R.id.search_results_list)
    var searchResultsList: ListView? = null

    @JvmField
    @BindView(R.id.search_error_view)
    var searchErrorView: WikiErrorView? = null

    @JvmField
    @BindView(R.id.search_empty_view)
    var searchEmptyView: View? = null

    @JvmField
    @BindView(R.id.search_suggestion)
    var searchSuggestion: TextView? = null
    private var unbinder: Unbinder? = null
    private val searchResultsCache = LruCache<String, MutableList<SearchResult>>(MAX_CACHE_SIZE_SEARCH_RESULTS)
    private var searchHandler: Handler? = null
    private var currentSearchTerm: String? = ""
    private var lastFullTextResults: SearchResults? = null
    private val totalResults: MutableList<SearchResult> = ArrayList()
    private val disposables = CompositeDisposable()
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_search_results, container, false)
        unbinder = ButterKnife.bind(this, view)
        val adapter = SearchResultAdapter()
        searchResultsList!!.adapter = adapter
        searchErrorView!!.setBackClickListener { requireActivity().finish() }
        searchErrorView!!.setRetryClickListener {
            searchErrorView!!.visibility = View.GONE
            startSearch(currentSearchTerm, true)
        }
        searchHandler = Handler(SearchHandlerCallback())
        return view
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        LongPressHandler(searchResultsList!!, HistoryEntry.SOURCE_SEARCH,
                SearchResultsFragmentLongPressHandler())
    }

    override fun onDestroyView() {
        searchErrorView!!.setRetryClickListener(null)
        unbinder!!.unbind()
        unbinder = null
        disposables.clear()
        super.onDestroyView()
    }

    @OnClick(R.id.search_suggestion)
    fun onSuggestionClick(view: View?) {
        val callback = callback()
        val suggestion = searchSuggestion!!.tag as String
        if (callback != null) {
            callback.funnel.searchDidYouMean(searchLanguageCode)
            callback.setSearchText(suggestion)
            startSearch(suggestion, true)
        }
    }

    fun show() {
        searchResultsDisplay!!.visibility = View.VISIBLE
    }

    fun hide() {
        searchResultsDisplay!!.visibility = View.GONE
    }

    val isShowing: Boolean
        get() = searchResultsDisplay!!.visibility == View.VISIBLE

    fun setLayoutDirection(langCode: String) {
        L10nUtil.setConditionalLayoutDirection(searchResultsList, langCode)
    }

    /**
     * Kick off a search, based on a given search term.
     * @param term Phrase to search for.
     * @param force Whether to "force" starting this search. If the search is not forced, the
     * search may be delayed by a small time, so that network requests are not sent
     * too often.  If the search is forced, the network request is sent immediately.
     */
    fun startSearch(term: String?, force: Boolean) {
        if (!force && currentSearchTerm == term) {
            return
        }
        cancelSearchTask()
        currentSearchTerm = term
        if (term.isNullOrBlank()) {
            clearResults()
            return
        }
        val cacheResult: List<SearchResult>? = searchResultsCache["$searchLanguageCode-$term"]
        if (!cacheResult.isNullOrEmpty()) {
            clearResults()
            displayResults(cacheResult)
            return
        }
        val searchMessage = Message.obtain()
        searchMessage.what = MESSAGE_SEARCH
        searchMessage.obj = term
        if (force) {
            searchHandler!!.sendMessage(searchMessage)
        } else {
            searchHandler!!.sendMessageDelayed(searchMessage, DELAY_MILLIS.toLong())
        }
    }

    private inner class SearchHandlerCallback : Handler.Callback {
        override fun handleMessage(msg: Message): Boolean {
            if (!isAdded) {
                return true
            }
            val mySearchTerm = msg.obj as String
            doTitlePrefixSearch(mySearchTerm)
            return true
        }
    }

    private fun doTitlePrefixSearch(searchTerm: String) {
        cancelSearchTask()
        val startTime = System.nanoTime()
        updateProgressBar(true)
        disposables.add(ServiceFactory.get(WikiSite.forLanguageCode(searchLanguageCode)).prefixSearch(searchTerm, BATCH_SIZE, searchTerm)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .map { response: PrefixSearchResponse? ->
                    if (response?.query() != null && response.query()!!.pages() != null) {
                        // noinspection ConstantConditions
                        return@map SearchResults(response.query()!!.pages()!!,
                                WikiSite.forLanguageCode(searchLanguageCode), response.continuation(),
                                response.suggestion())
                    }
                    SearchResults()
                }
                .doAfterTerminate { updateProgressBar(false) }
                .subscribe({ results: SearchResults ->
                    searchErrorView!!.visibility = View.GONE
                    handleResults(results, searchTerm, startTime)
                }) { caught: Throwable? ->
                    searchEmptyView!!.visibility = View.GONE
                    searchErrorView!!.visibility = View.VISIBLE
                    searchErrorView!!.setError(caught)
                    searchResultsContainer!!.visibility = View.GONE
                    logError(false, startTime)
                })
    }

    private fun handleResults(results: SearchResults, searchTerm: String, startTime: Long) {
        val resultList = results.results
        // To ease data analysis and better make the funnel track with user behaviour,
        // only transmit search results events if there are a nonzero number of results
        if (resultList.isNotEmpty()) {
            clearResults()
            displayResults(resultList)
            log(resultList, startTime)
        }
        handleSuggestion(results.suggestion)

        // add titles to cache...
        searchResultsCache.put("$searchLanguageCode-$searchTerm", resultList)

        // scroll to top, but post it to the message queue, because it should be done
        // after the data set is updated.
        searchResultsList!!.post {
            if (!isAdded) {
                return@post
            }
            searchResultsList!!.setSelectionAfterHeaderView()
        }
        if (resultList.isEmpty()) {
            // kick off full text search if we get no results
            doFullTextSearch(currentSearchTerm, null, true)
        }
    }

    private fun handleSuggestion(suggestion: String?) {
        if (suggestion != null) {
            searchSuggestion!!.text = StringUtil.fromHtml("<u>"
                    + String.format(getString(R.string.search_did_you_mean), suggestion) + "</u>")
            searchSuggestion!!.tag = suggestion
            searchSuggestion!!.visibility = View.VISIBLE
        } else {
            searchSuggestion!!.visibility = View.GONE
        }
    }

    private fun cancelSearchTask() {
        updateProgressBar(false)
        searchHandler!!.removeMessages(MESSAGE_SEARCH)
        disposables.clear()
    }

    private fun doFullTextSearch(searchTerm: String?,
                                 continueOffset: Map<String, String>?,
                                 clearOnSuccess: Boolean) {
        val startTime = System.nanoTime()
        updateProgressBar(true)
        disposables.add(ServiceFactory.get(WikiSite.forLanguageCode(searchLanguageCode)).fullTextSearch(searchTerm, BATCH_SIZE,
                continueOffset?.get("continue"), continueOffset?.get("gsroffset"))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .map { response: MwQueryResponse ->
                    if (response.query() != null) {
                        // noinspection ConstantConditions
                        return@map SearchResults(response.query()!!.pages()!!, WikiSite.forLanguageCode(searchLanguageCode),
                                response.continuation(), null)
                    }
                    SearchResults()
                }
                .doAfterTerminate { updateProgressBar(false) }
                .subscribe({ results: SearchResults ->
                    val resultList = results.results
                    cache(resultList, searchTerm!!)
                    log(resultList, startTime)
                    if (clearOnSuccess) {
                        clearResults(false)
                    }
                    searchErrorView!!.visibility = View.GONE

                    // full text special:
                    lastFullTextResults = results
                    displayResults(resultList)
                }) {
                    // If there's an error, just log it and let the existing prefix search results be.
                    logError(true, startTime)
                })
    }

    val firstResult: PageTitle?
        get() = if (totalResults.isNotEmpty()) totalResults[0].pageTitle else null

    private fun updateProgressBar(enabled: Boolean) {
        callback()?.onSearchProgressBar(enabled)
    }

    private fun clearResults(clearSuggestion: Boolean = true) {
        searchResultsContainer!!.visibility = View.GONE
        searchEmptyView!!.visibility = View.GONE
        searchErrorView!!.visibility = View.GONE
        if (clearSuggestion) {
            searchSuggestion!!.visibility = View.GONE
        }
        lastFullTextResults = null
        totalResults.clear()
        adapter.notifyDataSetChanged()
    }

    private val adapter: BaseAdapter
        get() = searchResultsList!!.adapter as BaseAdapter

    /**
     * Displays results passed to it as search suggestions.
     *
     * @param results List of results to display. If null, clears the list of suggestions & hides it.
     */
    private fun displayResults(results: List<SearchResult>) {
        totalResults.addAll(results.filter { newResult ->
            totalResults.asSequence().none { it.pageTitle == newResult.pageTitle }
        })
        if (totalResults.isEmpty()) {
            searchEmptyView!!.visibility = View.VISIBLE
            searchResultsContainer!!.visibility = View.GONE
        } else {
            searchEmptyView!!.visibility = View.GONE
            searchResultsContainer!!.visibility = View.VISIBLE
        }
        adapter.notifyDataSetChanged()
    }

    private inner class SearchResultsFragmentLongPressHandler : ListViewOverflowMenuListener {
        private var lastPositionRequested = 0
        override fun getTitleForListPosition(position: Int): PageTitle {
            lastPositionRequested = position
            return (adapter.getItem(position) as SearchResult).pageTitle
        }

        override fun onOpenLink(title: PageTitle, entry: HistoryEntry) {
            val callback = callback()
            callback?.navigateToTitle(title, false, lastPositionRequested)
        }

        override fun onOpenInNewTab(title: PageTitle, entry: HistoryEntry) {
            val callback = callback()
            callback?.navigateToTitle(title, true, lastPositionRequested)
        }

        override fun onCopyLink(title: PageTitle) {
            val callback = callback()
            callback?.onSearchResultCopyLink(title)
        }

        override fun onShareLink(title: PageTitle) {
            val callback = callback()
            callback?.onSearchResultShareLink(title)
        }

        override fun onAddToList(title: PageTitle, source: InvokeSource) {
            val callback = callback()
            callback?.onSearchResultAddToList(title, source)
        }
    }

    private inner class SearchResultAdapter internal constructor() : BaseAdapter(), View.OnClickListener, OnLongClickListener {
        override fun getCount() = totalResults.size

        override fun getItem(position: Int) = totalResults[position]

        override fun getItemId(position: Int) = position.toLong()

        override fun getView(position: Int, convertView: View, parent: ViewGroup): View {
            val pageTitleText = convertView.findViewById<TextView>(R.id.page_list_item_title)
            val result = getItem(position)
            val searchResultItemImage = convertView.findViewById<ImageView>(R.id.page_list_item_image)
            val descriptionText: GoneIfEmptyTextView = convertView.findViewById(R.id.page_list_item_description)
            val redirectText = convertView.findViewById<TextView>(R.id.page_list_item_redirect)
            val redirectArrow = convertView.findViewById<View>(R.id.page_list_item_redirect_arrow)
            if (TextUtils.isEmpty(result.redirectFrom)) {
                redirectText.visibility = View.GONE
                redirectArrow.visibility = View.GONE
                descriptionText.text = result.pageTitle.description
            } else {
                redirectText.visibility = View.VISIBLE
                redirectArrow.visibility = View.VISIBLE
                redirectText.text = String.format(getString(R.string.search_redirect_from), result.redirectFrom)
                descriptionText.visibility = View.GONE
            }

            // highlight search term within the text
            StringUtil.boldenKeywordText(pageTitleText, result.pageTitle.displayText, currentSearchTerm)
            searchResultItemImage.visibility = if (result.pageTitle.thumbUrl == null) View.GONE else View.VISIBLE
            ViewUtil.loadImageWithRoundedCorners(searchResultItemImage, result.pageTitle.thumbUrl)

            // ...and lastly, if we've scrolled to the last item in the list, then
            // continue searching!
            if (position == totalResults.size - 1 && WikipediaApp.getInstance().isOnline) {
                if (lastFullTextResults == null) {
                    // the first full text search
                    doFullTextSearch(currentSearchTerm, null, false)
                } else if (lastFullTextResults!!.continuation != null) {
                    // subsequent full text searches
                    doFullTextSearch(currentSearchTerm, lastFullTextResults!!.continuation, false)
                }
            }
            convertView.tag = position
            return convertView
        }

        override fun onClick(v: View) {
            val callback = callback()
            val position = v.tag as Int
            if (callback != null && position < totalResults.size) {
                callback.navigateToTitle(totalResults[position].pageTitle, false, position)
            }
        }

        override fun onLongClick(v: View) = false
    }

    private fun cache(resultList: List<SearchResult>, searchTerm: String) {
        val cacheKey = "$searchLanguageCode-$searchTerm"
        val cachedTitles = searchResultsCache[cacheKey]
        if (cachedTitles != null) {
            cachedTitles.addAll(resultList)
            searchResultsCache.put(cacheKey, cachedTitles)
        }
    }

    private fun log(resultList: List<SearchResult>, startTime: Long) {
        // To ease data analysis and better make the funnel track with user behaviour,
        // only transmit search results events if there are a nonzero number of results
        if (callback() != null && resultList.isNotEmpty()) {
            // noinspection ConstantConditions
            callback()!!.funnel.searchResults(true, resultList.size, displayTime(startTime), searchLanguageCode)
        }
    }

    private fun logError(fullText: Boolean, startTime: Long) {
        if (callback() != null) {
            // noinspection ConstantConditions
            callback()!!.funnel.searchError(fullText, displayTime(startTime), searchLanguageCode)
        }
    }

    private fun displayTime(startTime: Long): Int {
        return ((System.nanoTime() - startTime) / NANO_TO_MILLI).toInt()
    }

    private fun callback(): Callback? {
        return FragmentUtil.getCallback(this, Callback::class.java)
    }

    private val searchLanguageCode: String
        get() = (parentFragment as SearchFragment?)!!.searchLanguageCode

    companion object {
        private const val BATCH_SIZE = 20
        private const val DELAY_MILLIS = 300
        private const val MESSAGE_SEARCH = 1
        private const val MAX_CACHE_SIZE_SEARCH_RESULTS = 4

        /**
         * Constant to ease in the conversion of timestamps from nanoseconds to milliseconds.
         */
        private const val NANO_TO_MILLI = 1000000
    }
}
